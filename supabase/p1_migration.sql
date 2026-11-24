-- ============================================================================
-- Are We Okay - P1 migration (psychologist platform: roles, verification, directory, booking,
-- messaging, consent-based sharing, content). NOT EXECUTED. Review, then run once in the Supabase
-- SQL editor AFTER p0_migration.sql.
--
-- Policy stance: RLS enabled on every table; NO policy = deny. Writes that change state machines
-- (appointments, grants, verification) go through SECURITY DEFINER functions, never direct UPDATE.
-- Nothing here is granted to the anon role.
--
-- Legal: therapist-client data and mood summaries are special-category health data (GDPR Art. 9,
-- KVKK m.6). Supabase region should be EU. Messages are protected by TLS in transit and encryption
-- at rest by Supabase; they are NOT end-to-end encrypted (operator/DB admin can technically read).
-- Do not describe them as "end-to-end encrypted" in the store listing or UI.
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ---------- helpers ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.is_admin() RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT EXISTS (SELECT 1 FROM users u WHERE u.id = auth.uid() AND u.role = 'admin');
$$;

CREATE OR REPLACE FUNCTION public.is_verified_psychologist(p uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT EXISTS (SELECT 1 FROM psychologist_profiles WHERE user_id = p AND verified_status = 'verified');
$$;

-- ---------- psychologist profile ---------------------------------------------------------------
-- "Is a psychologist" = a row exists here. users.role is NOT writable by users, so we never rely on it.
CREATE TABLE IF NOT EXISTS psychologist_profiles (
  user_id uuid PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  display_name text NOT NULL CHECK (char_length(display_name) BETWEEN 2 AND 80),
  title text NOT NULL CHECK (char_length(title) <= 80),            -- e.g. "Clinical Psychologist"
  bio text NOT NULL DEFAULT '' CHECK (char_length(bio) <= 2000),
  specialties text[] NOT NULL DEFAULT '{}',
  languages text[] NOT NULL DEFAULT '{}',                           -- ISO 639-1 codes
  country text NOT NULL DEFAULT '',                                 -- ISO 3166-1 alpha-2
  city text NOT NULL DEFAULT '',
  timezone text NOT NULL DEFAULT 'UTC',                             -- IANA zone, availability is stored in this zone
  offers_online boolean NOT NULL DEFAULT true,
  offers_in_person boolean NOT NULL DEFAULT false,
  session_minutes smallint NOT NULL DEFAULT 50 CHECK (session_minutes BETWEEN 15 AND 180),
  price_amount numeric(10,2) CHECK (price_amount IS NULL OR price_amount >= 0),  -- informational only; the app has NO payment feature
  price_currency text NOT NULL DEFAULT 'USD',
  license_country text,
  license_no_hash text,                                             -- hash only, never the raw license number
  verified_status text NOT NULL DEFAULT 'pending' CHECK (verified_status IN ('none','pending','verified','rejected')),
  verified_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS psych_dir_idx ON psychologist_profiles (verified_status, country);
ALTER TABLE psychologist_profiles ENABLE ROW LEVEL SECURITY;

-- Directory: ONLY verified profiles are visible to others; owner sees own; admin sees all.
DROP POLICY IF EXISTS "psych profiles select" ON psychologist_profiles;
CREATE POLICY "psych profiles select" ON psychologist_profiles FOR SELECT
  USING (verified_status = 'verified' OR user_id = auth.uid() OR public.is_admin());
DROP POLICY IF EXISTS "psych profiles insert" ON psychologist_profiles;
CREATE POLICY "psych profiles insert" ON psychologist_profiles FOR INSERT WITH CHECK (user_id = auth.uid());
DROP POLICY IF EXISTS "psych profiles update" ON psychologist_profiles;
CREATE POLICY "psych profiles update" ON psychologist_profiles FOR UPDATE USING (user_id = auth.uid() OR public.is_admin());

-- Users can never self-verify: force 'pending' on insert and keep status/verified_at on update unless admin.
CREATE OR REPLACE FUNCTION public.protect_psych_verification() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF public.is_admin() OR auth.uid() IS NULL THEN RETURN NEW; END IF;   -- admin / service role (SQL editor)
  IF TG_OP = 'INSERT' THEN
    NEW.verified_status := 'pending'; NEW.verified_at := NULL;
  ELSE
    NEW.verified_status := OLD.verified_status; NEW.verified_at := OLD.verified_at;
  END IF;
  RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS psych_protect_verification ON psychologist_profiles;
CREATE TRIGGER psych_protect_verification BEFORE INSERT OR UPDATE ON psychologist_profiles
  FOR EACH ROW EXECUTE FUNCTION public.protect_psych_verification();

-- ---------- verification documents (private bucket) --------------------------------------------
CREATE TABLE IF NOT EXISTS verification_documents (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  storage_path text NOT NULL,                 -- '<user_id>/<file>' inside bucket verification-docs
  doc_type text NOT NULL DEFAULT 'license' CHECK (doc_type IN ('license','diploma','other')),
  status text NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','accepted','rejected')),
  reviewer_id uuid REFERENCES users(id),
  reason text,
  created_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE verification_documents ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "vdocs insert own" ON verification_documents;
CREATE POLICY "vdocs insert own" ON verification_documents FOR INSERT
  WITH CHECK (user_id = auth.uid() AND status = 'pending' AND storage_path LIKE auth.uid()::text || '/%');
DROP POLICY IF EXISTS "vdocs select" ON verification_documents;
CREATE POLICY "vdocs select" ON verification_documents FOR SELECT USING (user_id = auth.uid() OR public.is_admin());
DROP POLICY IF EXISTS "vdocs admin update" ON verification_documents;
CREATE POLICY "vdocs admin update" ON verification_documents FOR UPDATE USING (public.is_admin());

INSERT INTO storage.buckets (id, name, public) VALUES ('verification-docs', 'verification-docs', false)
  ON CONFLICT (id) DO UPDATE SET public = false;
DROP POLICY IF EXISTS "vdocs upload own folder" ON storage.objects;
CREATE POLICY "vdocs upload own folder" ON storage.objects FOR INSERT TO authenticated
  WITH CHECK (bucket_id = 'verification-docs' AND (storage.foldername(name))[1] = auth.uid()::text);
DROP POLICY IF EXISTS "vdocs read own or admin" ON storage.objects;
CREATE POLICY "vdocs read own or admin" ON storage.objects FOR SELECT TO authenticated
  USING (bucket_id = 'verification-docs' AND ((storage.foldername(name))[1] = auth.uid()::text OR public.is_admin()));
DROP POLICY IF EXISTS "vdocs delete own" ON storage.objects;
CREATE POLICY "vdocs delete own" ON storage.objects FOR DELETE TO authenticated
  USING (bucket_id = 'verification-docs' AND (storage.foldername(name))[1] = auth.uid()::text);

-- Admin decision (call from SQL editor / admin panel): SELECT admin_set_verification('<uid>','verified',NULL);
CREATE OR REPLACE FUNCTION public.admin_set_verification(p_user uuid, p_status text, p_reason text)
RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NOT (public.is_admin() OR auth.uid() IS NULL) THEN RAISE EXCEPTION 'forbidden'; END IF;
  IF p_status NOT IN ('verified','rejected','pending') THEN RAISE EXCEPTION 'bad status'; END IF;
  UPDATE psychologist_profiles SET verified_status = p_status,
    verified_at = CASE WHEN p_status = 'verified' THEN now() END WHERE user_id = p_user;
  UPDATE verification_documents SET status = CASE WHEN p_status = 'verified' THEN 'accepted' WHEN p_status='rejected' THEN 'rejected' ELSE status END,
    reviewer_id = auth.uid(), reason = p_reason WHERE user_id = p_user AND status = 'pending';
END $$;
REVOKE ALL ON FUNCTION public.admin_set_verification(uuid,text,text) FROM PUBLIC, anon, authenticated;

-- ---------- favorites, reviews -----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS psychologist_favorites (
  client_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  psychologist_id uuid NOT NULL REFERENCES psychologist_profiles(user_id) ON DELETE CASCADE,
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (client_id, psychologist_id)
);
ALTER TABLE psychologist_favorites ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "fav own" ON psychologist_favorites;
CREATE POLICY "fav own" ON psychologist_favorites FOR ALL USING (client_id = auth.uid()) WITH CHECK (client_id = auth.uid());

-- ---------- appointments -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS availability_rules (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  psychologist_id uuid NOT NULL REFERENCES psychologist_profiles(user_id) ON DELETE CASCADE,
  weekday smallint NOT NULL CHECK (weekday BETWEEN 1 AND 7),       -- ISO: 1 = Monday
  start_min smallint NOT NULL CHECK (start_min BETWEEN 0 AND 1439), -- minutes from local midnight, in profile.timezone
  end_min smallint NOT NULL CHECK (end_min BETWEEN 1 AND 1440),
  CHECK (end_min > start_min)
);
ALTER TABLE availability_rules ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "avail read" ON availability_rules;
CREATE POLICY "avail read" ON availability_rules FOR SELECT
  USING (psychologist_id = auth.uid() OR public.is_verified_psychologist(psychologist_id));
DROP POLICY IF EXISTS "avail write" ON availability_rules;
CREATE POLICY "avail write" ON availability_rules FOR ALL USING (psychologist_id = auth.uid()) WITH CHECK (psychologist_id = auth.uid());

CREATE TABLE IF NOT EXISTS appointments (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  psychologist_id uuid NOT NULL REFERENCES psychologist_profiles(user_id) ON DELETE CASCADE,
  client_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  start_at timestamptz NOT NULL,
  end_at timestamptz NOT NULL,
  mode text NOT NULL DEFAULT 'online' CHECK (mode IN ('online','in_person')),
  status text NOT NULL DEFAULT 'requested'
    CHECK (status IN ('requested','confirmed','declined','reschedule_proposed','cancelled','completed')),
  proposed_start_at timestamptz,                 -- psychologist's counter-proposal
  client_note text CHECK (client_note IS NULL OR char_length(client_note) <= 500),
  room_name text,                                -- unguessable Jitsi room, set only on confirmation
  cancelled_by uuid REFERENCES users(id),
  late_cancel boolean NOT NULL DEFAULT false,    -- confirmed session cancelled < 24 h before start
  price_amount numeric(10,2), price_currency text,   -- snapshot of the displayed price
  created_at timestamptz NOT NULL DEFAULT now(),
  CHECK (end_at > start_at),
  EXCLUDE USING gist (psychologist_id WITH =, tstzrange(start_at, end_at) WITH &&)
    WHERE (status IN ('requested','confirmed','reschedule_proposed'))
);
CREATE INDEX IF NOT EXISTS appt_client_idx ON appointments (client_id, start_at);
CREATE INDEX IF NOT EXISTS appt_psych_idx ON appointments (psychologist_id, start_at);
ALTER TABLE appointments ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "appt select parties" ON appointments;
CREATE POLICY "appt select parties" ON appointments FOR SELECT USING (client_id = auth.uid() OR psychologist_id = auth.uid());
DROP POLICY IF EXISTS "appt insert client" ON appointments;
CREATE POLICY "appt insert client" ON appointments FOR INSERT
  WITH CHECK (client_id = auth.uid() AND client_id <> psychologist_id AND status = 'requested'
              AND public.is_verified_psychologist(psychologist_id));
-- No UPDATE/DELETE policy: transitions only via appointment_action().

-- Server-side slot validation: inside an availability rule (in the psychologist's zone), >= 2 h ahead.
CREATE OR REPLACE FUNCTION public.validate_appointment() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE pp psychologist_profiles; loc timestamp; wd int; m int; ok boolean;
BEGIN
  SELECT * INTO pp FROM psychologist_profiles WHERE user_id = NEW.psychologist_id;
  NEW.end_at := NEW.start_at + make_interval(mins => pp.session_minutes);
  IF NEW.start_at < now() + interval '2 hours' THEN RAISE EXCEPTION 'slot too soon'; END IF;
  loc := NEW.start_at AT TIME ZONE pp.timezone;
  wd := extract(isodow FROM loc); m := extract(hour FROM loc) * 60 + extract(minute FROM loc);
  SELECT EXISTS (SELECT 1 FROM availability_rules r WHERE r.psychologist_id = NEW.psychologist_id
                 AND r.weekday = wd AND r.start_min <= m AND r.end_min >= m + pp.session_minutes) INTO ok;
  IF NOT ok THEN RAISE EXCEPTION 'outside availability'; END IF;
  NEW.price_amount := pp.price_amount; NEW.price_currency := pp.price_currency;
  NEW.status := 'requested'; NEW.room_name := NULL;
  RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS appt_validate ON appointments;
CREATE TRIGGER appt_validate BEFORE INSERT ON appointments FOR EACH ROW EXECUTE FUNCTION public.validate_appointment();

-- Busy windows of a psychologist without exposing who booked.
CREATE OR REPLACE FUNCTION public.psychologist_busy(p_psych uuid, p_from timestamptz, p_to timestamptz)
RETURNS TABLE (start_at timestamptz, end_at timestamptz)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT a.start_at, a.end_at FROM appointments a
  WHERE a.psychologist_id = p_psych AND a.status IN ('requested','confirmed','reschedule_proposed')
    AND a.start_at < p_to AND a.end_at > p_from AND public.is_verified_psychologist(p_psych);
$$;

-- State machine. Actions: confirm | decline | propose (p_new_start) | accept_proposal | cancel | complete
CREATE OR REPLACE FUNCTION public.appointment_action(p_id uuid, p_action text, p_new_start timestamptz DEFAULT NULL)
RETURNS appointments LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE a appointments; me uuid := auth.uid(); is_p boolean; is_c boolean; mins int;
BEGIN
  SELECT * INTO a FROM appointments WHERE id = p_id FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'not found'; END IF;
  is_p := (a.psychologist_id = me); is_c := (a.client_id = me);
  IF NOT (is_p OR is_c) THEN RAISE EXCEPTION 'forbidden'; END IF;

  IF p_action = 'confirm' AND is_p AND a.status = 'requested' THEN
    a.status := 'confirmed';
    a.room_name := 'awo-' || substr(replace(gen_random_uuid()::text || gen_random_uuid()::text, '-', ''), 1, 40);
  ELSIF p_action = 'decline' AND is_p AND a.status IN ('requested','reschedule_proposed') THEN
    a.status := 'declined';
  ELSIF p_action = 'propose' AND is_p AND a.status = 'requested' AND p_new_start IS NOT NULL THEN
    -- the counter-proposal must also be free and inside availability: re-check by moving the row
    mins := EXTRACT(EPOCH FROM (a.end_at - a.start_at)) / 60;
    a.proposed_start_at := p_new_start; a.status := 'reschedule_proposed';
  ELSIF p_action = 'accept_proposal' AND is_c AND a.status = 'reschedule_proposed' THEN
    mins := EXTRACT(EPOCH FROM (a.end_at - a.start_at)) / 60;
    a.start_at := a.proposed_start_at; a.end_at := a.proposed_start_at + make_interval(mins => mins);
    a.proposed_start_at := NULL; a.status := 'confirmed';
    a.room_name := 'awo-' || substr(replace(gen_random_uuid()::text || gen_random_uuid()::text, '-', ''), 1, 40);
  ELSIF p_action = 'cancel' AND a.status IN ('requested','confirmed','reschedule_proposed') AND a.start_at > now() THEN
    a.late_cancel := (a.status = 'confirmed' AND a.start_at < now() + interval '24 hours');
    a.status := 'cancelled'; a.cancelled_by := me; a.room_name := NULL;
  ELSIF p_action = 'complete' AND is_p AND a.status = 'confirmed' AND a.end_at < now() THEN
    a.status := 'completed';
  ELSE
    RAISE EXCEPTION 'invalid transition';
  END IF;
  UPDATE appointments SET status = a.status, room_name = a.room_name, proposed_start_at = a.proposed_start_at,
    start_at = a.start_at, end_at = a.end_at, cancelled_by = a.cancelled_by, late_cancel = a.late_cancel
  WHERE id = a.id RETURNING * INTO a;
  RETURN a;
END $$;
REVOKE ALL ON FUNCTION public.appointment_action(uuid,text,timestamptz) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.appointment_action(uuid,text,timestamptz) TO authenticated;

-- Join window enforced on the server: from 10 min before start to 15 min after end. Returns the room only then.
CREATE OR REPLACE FUNCTION public.get_join_room(p_id uuid) RETURNS text
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path = public AS $$
DECLARE a appointments;
BEGIN
  SELECT * INTO a FROM appointments WHERE id = p_id AND (client_id = auth.uid() OR psychologist_id = auth.uid());
  IF NOT FOUND OR a.status <> 'confirmed' OR a.room_name IS NULL THEN RETURN NULL; END IF;
  IF now() < a.start_at - interval '10 minutes' OR now() > a.end_at + interval '15 minutes' THEN RETURN NULL; END IF;
  RETURN a.room_name;
END $$;
REVOKE ALL ON FUNCTION public.get_join_room(uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.get_join_room(uuid) TO authenticated;

-- ---------- reviews (only after a completed session) ------------------------------------------
CREATE TABLE IF NOT EXISTS psychologist_reviews (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  psychologist_id uuid NOT NULL REFERENCES psychologist_profiles(user_id) ON DELETE CASCADE,
  client_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  appointment_id uuid NOT NULL UNIQUE REFERENCES appointments(id) ON DELETE CASCADE,
  rating smallint NOT NULL CHECK (rating BETWEEN 1 AND 5),
  comment text CHECK (comment IS NULL OR char_length(comment) <= 1000),
  status text NOT NULL DEFAULT 'visible' CHECK (status IN ('visible','hidden')),
  created_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE psychologist_reviews ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "reviews read" ON psychologist_reviews;
CREATE POLICY "reviews read" ON psychologist_reviews FOR SELECT
  USING (client_id = auth.uid() OR (status = 'visible' AND public.is_verified_psychologist(psychologist_id)));
DROP POLICY IF EXISTS "reviews insert after session" ON psychologist_reviews;
CREATE POLICY "reviews insert after session" ON psychologist_reviews FOR INSERT WITH CHECK (
  client_id = auth.uid() AND EXISTS (
    SELECT 1 FROM appointments a WHERE a.id = appointment_id AND a.client_id = auth.uid()
      AND a.psychologist_id = psychologist_reviews.psychologist_id AND a.status = 'completed'));
-- Reviews are reportable; extend the reports target types.
ALTER TABLE reports DROP CONSTRAINT IF EXISTS reports_target_type_check;
ALTER TABLE reports ADD CONSTRAINT reports_target_type_check
  CHECK (target_type IN ('post','comment','user','review','message','psychologist'));

CREATE OR REPLACE VIEW public.psychologist_rating_stats AS
  SELECT psychologist_id, round(avg(rating)::numeric, 2) AS avg_rating, count(*)::int AS review_count
  FROM psychologist_reviews WHERE status = 'visible' GROUP BY psychologist_id;
GRANT SELECT ON public.psychologist_rating_stats TO authenticated;

-- ---------- messaging (RLS: only the two parties) ---------------------------------------------
CREATE TABLE IF NOT EXISTS conversations (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  client_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  psychologist_id uuid NOT NULL REFERENCES psychologist_profiles(user_id) ON DELETE CASCADE,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (client_id, psychologist_id),
  CHECK (client_id <> psychologist_id)
);
ALTER TABLE conversations ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "conv select parties" ON conversations;
CREATE POLICY "conv select parties" ON conversations FOR SELECT USING (client_id = auth.uid() OR psychologist_id = auth.uid());
DROP POLICY IF EXISTS "conv insert client" ON conversations;
CREATE POLICY "conv insert client" ON conversations FOR INSERT
  WITH CHECK (
    (client_id = auth.uid() AND public.is_verified_psychologist(psychologist_id))
    OR (psychologist_id = auth.uid() AND EXISTS (SELECT 1 FROM appointments a WHERE a.psychologist_id = auth.uid()
          AND a.client_id = conversations.client_id AND a.status IN ('confirmed','completed'))));

CREATE TABLE IF NOT EXISTS messages (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  conversation_id uuid NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
  sender_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  body text NOT NULL CHECK (char_length(body) BETWEEN 1 AND 4000),
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS messages_conv_idx ON messages (conversation_id, id);
ALTER TABLE messages ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "msg select parties" ON messages;
CREATE POLICY "msg select parties" ON messages FOR SELECT USING (EXISTS (
  SELECT 1 FROM conversations c WHERE c.id = conversation_id AND (c.client_id = auth.uid() OR c.psychologist_id = auth.uid())));
DROP POLICY IF EXISTS "msg insert parties" ON messages;
CREATE POLICY "msg insert parties" ON messages FOR INSERT WITH CHECK (sender_id = auth.uid() AND EXISTS (
  SELECT 1 FROM conversations c WHERE c.id = conversation_id AND (c.client_id = auth.uid() OR c.psychologist_id = auth.uid())));
-- No UPDATE/DELETE: messages are immutable (also supports reports/evidence).
-- Realtime: postgres_changes honours the SELECT policy above for each subscriber.
DO $$ BEGIN
  ALTER PUBLICATION supabase_realtime ADD TABLE messages;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- ---------- share with my psychologist (explicit, time-boxed, revocable, audited) ---------------
CREATE TABLE IF NOT EXISTS share_grants (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  psychologist_id uuid NOT NULL REFERENCES psychologist_profiles(user_id) ON DELETE CASCADE,
  scopes text[] NOT NULL DEFAULT '{mood}' CHECK (scopes <@ ARRAY['mood','emotions','journal']),
  consent_text_version int NOT NULL DEFAULT 1,
  created_at timestamptz NOT NULL DEFAULT now(),
  expires_at timestamptz NOT NULL,
  revoked_at timestamptz,
  CHECK (expires_at > created_at AND expires_at <= created_at + interval '90 days')
);
ALTER TABLE share_grants ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "grants owner select" ON share_grants;
CREATE POLICY "grants owner select" ON share_grants FOR SELECT USING (user_id = auth.uid());
DROP POLICY IF EXISTS "grants psych select active" ON share_grants;
CREATE POLICY "grants psych select active" ON share_grants FOR SELECT
  USING (psychologist_id = auth.uid() AND revoked_at IS NULL AND expires_at > now());
DROP POLICY IF EXISTS "grants owner insert" ON share_grants;
-- Only toward a psychologist the user actually has a relationship with (conversation or appointment).
CREATE POLICY "grants owner insert" ON share_grants FOR INSERT WITH CHECK (
  user_id = auth.uid() AND revoked_at IS NULL AND public.is_verified_psychologist(psychologist_id) AND (
    EXISTS (SELECT 1 FROM conversations c WHERE c.client_id = auth.uid() AND c.psychologist_id = share_grants.psychologist_id)
    OR EXISTS (SELECT 1 FROM appointments a WHERE a.client_id = auth.uid() AND a.psychologist_id = share_grants.psychologist_id
               AND a.status IN ('requested','confirmed','completed'))));

CREATE TABLE IF NOT EXISTS share_snapshots (
  grant_id uuid PRIMARY KEY REFERENCES share_grants(id) ON DELETE CASCADE,
  payload jsonb NOT NULL,                      -- mood summary (+ optional journal excerpts the user picked)
  updated_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE share_snapshots ENABLE ROW LEVEL SECURITY;   -- no policies: access only via the RPCs below

CREATE TABLE IF NOT EXISTS share_audit_log (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  grant_id uuid NOT NULL REFERENCES share_grants(id) ON DELETE CASCADE,
  actor_id uuid NOT NULL,
  action text NOT NULL CHECK (action IN ('grant','update','view','revoke')),
  created_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE share_audit_log ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "audit owner read" ON share_audit_log;
CREATE POLICY "audit owner read" ON share_audit_log FOR SELECT USING (EXISTS (
  SELECT 1 FROM share_grants g WHERE g.id = grant_id AND g.user_id = auth.uid()));

CREATE OR REPLACE FUNCTION public.log_grant_created() RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN INSERT INTO share_audit_log (grant_id, actor_id, action) VALUES (NEW.id, NEW.user_id, 'grant'); RETURN NEW; END $$;
DROP TRIGGER IF EXISTS grant_audit ON share_grants;
CREATE TRIGGER grant_audit AFTER INSERT ON share_grants FOR EACH ROW EXECUTE FUNCTION public.log_grant_created();

CREATE OR REPLACE FUNCTION public.upsert_share_snapshot(p_grant uuid, p_payload jsonb) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM share_grants WHERE id = p_grant AND user_id = auth.uid() AND revoked_at IS NULL AND expires_at > now())
    THEN RAISE EXCEPTION 'grant not active'; END IF;
  INSERT INTO share_snapshots (grant_id, payload) VALUES (p_grant, p_payload)
    ON CONFLICT (grant_id) DO UPDATE SET payload = EXCLUDED.payload, updated_at = now();
  INSERT INTO share_audit_log (grant_id, actor_id, action) VALUES (p_grant, auth.uid(), 'update');
END $$;

CREATE OR REPLACE FUNCTION public.revoke_share(p_grant uuid) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  UPDATE share_grants SET revoked_at = now() WHERE id = p_grant AND user_id = auth.uid() AND revoked_at IS NULL;
  IF FOUND THEN
    DELETE FROM share_snapshots WHERE grant_id = p_grant;           -- revocation also removes the copy
    INSERT INTO share_audit_log (grant_id, actor_id, action) VALUES (p_grant, auth.uid(), 'revoke');
  END IF;
END $$;

CREATE OR REPLACE FUNCTION public.read_share_snapshot(p_grant uuid) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE r jsonb;
BEGIN
  IF NOT EXISTS (SELECT 1 FROM share_grants WHERE id = p_grant AND psychologist_id = auth.uid()
                 AND revoked_at IS NULL AND expires_at > now()) THEN RAISE EXCEPTION 'grant not active'; END IF;
  SELECT payload INTO r FROM share_snapshots WHERE grant_id = p_grant;
  INSERT INTO share_audit_log (grant_id, actor_id, action) VALUES (p_grant, auth.uid(), 'view');
  RETURN r;
END $$;
REVOKE ALL ON FUNCTION public.upsert_share_snapshot(uuid,jsonb), public.revoke_share(uuid), public.read_share_snapshot(uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.upsert_share_snapshot(uuid,jsonb), public.revoke_share(uuid), public.read_share_snapshot(uuid) TO authenticated;

-- Schedule (pg_cron / Edge Function): purge snapshots of expired grants.
CREATE OR REPLACE FUNCTION public.purge_expired_snapshots() RETURNS void LANGUAGE sql SECURITY DEFINER SET search_path = public AS $$
  DELETE FROM share_snapshots s USING share_grants g WHERE g.id = s.grant_id AND (g.expires_at <= now() OR g.revoked_at IS NOT NULL);
$$;
REVOKE ALL ON FUNCTION public.purge_expired_snapshots() FROM PUBLIC, anon, authenticated;

-- ---------- psychologist-only session notes (never visible to the client) -----------------------
CREATE TABLE IF NOT EXISTS psychologist_notes (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  psychologist_id uuid NOT NULL REFERENCES psychologist_profiles(user_id) ON DELETE CASCADE,
  client_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  appointment_id uuid REFERENCES appointments(id) ON DELETE SET NULL,
  body text NOT NULL CHECK (char_length(body) BETWEEN 1 AND 8000),
  created_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE psychologist_notes ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "notes psych only" ON psychologist_notes;
CREATE POLICY "notes psych only" ON psychologist_notes FOR ALL
  USING (psychologist_id = auth.uid())
  WITH CHECK (psychologist_id = auth.uid() AND EXISTS (
    SELECT 1 FROM appointments a WHERE a.psychologist_id = auth.uid() AND a.client_id = psychologist_notes.client_id));

-- Client list for the psychologist dashboard (clients with an appointment or conversation).
CREATE OR REPLACE FUNCTION public.my_clients()
RETURNS TABLE (client_id uuid, username text, avatar_emoji text, appointments int, next_start timestamptz,
               conversation_id uuid, grant_id uuid)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT u.id, u.username, u.avatar_emoji,
    (SELECT count(*)::int FROM appointments a WHERE a.psychologist_id = auth.uid() AND a.client_id = u.id AND a.status IN ('confirmed','completed')),
    (SELECT min(a.start_at) FROM appointments a WHERE a.psychologist_id = auth.uid() AND a.client_id = u.id AND a.status = 'confirmed' AND a.start_at > now()),
    (SELECT c.id FROM conversations c WHERE c.psychologist_id = auth.uid() AND c.client_id = u.id),
    (SELECT g.id FROM share_grants g WHERE g.psychologist_id = auth.uid() AND g.user_id = u.id AND g.revoked_at IS NULL AND g.expires_at > now() ORDER BY g.created_at DESC LIMIT 1)
  FROM users u
  WHERE EXISTS (SELECT 1 FROM appointments a WHERE a.psychologist_id = auth.uid() AND a.client_id = u.id)
     OR EXISTS (SELECT 1 FROM conversations c WHERE c.psychologist_id = auth.uid() AND c.client_id = u.id);
$$;
REVOKE ALL ON FUNCTION public.my_clients() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.my_clients() TO authenticated;

-- ---------- content: follows + post kinds (psychologist posts reuse `posts`, non-anonymous) -----
CREATE TABLE IF NOT EXISTS psychologist_follows (
  follower_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  psychologist_id uuid NOT NULL REFERENCES psychologist_profiles(user_id) ON DELETE CASCADE,
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (follower_id, psychologist_id)
);
ALTER TABLE psychologist_follows ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "follows own" ON psychologist_follows;
CREATE POLICY "follows own" ON psychologist_follows FOR ALL USING (follower_id = auth.uid()) WITH CHECK (follower_id = auth.uid());
-- Psychologist sees only a count, via RPC (no follower identities).
CREATE OR REPLACE FUNCTION public.follower_count(p uuid) RETURNS int LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT count(*)::int FROM psychologist_follows WHERE psychologist_id = p;
$$;
GRANT EXECUTE ON FUNCTION public.follower_count(uuid) TO authenticated;

ALTER TABLE posts ADD COLUMN IF NOT EXISTS kind text NOT NULL DEFAULT 'post' CHECK (kind IN ('post','tip','article'));
ALTER TABLE posts ADD COLUMN IF NOT EXISTS tags text[] NOT NULL DEFAULT '{}';
-- The feed shows a "Verified" badge by checking the author id against psychologist_profiles (verified only).
-- Make sure the existing posts INSERT policy requires status to default to your moderation queue value.

-- ---------- notes ------------------------------------------------------------------------------
-- * delete_my_account(): all new tables reference users(id) ON DELETE CASCADE, so deleting the users row
--   (last step of the existing function) removes them. Appointments/messages disappear for BOTH parties;
--   if you need professional record retention for the psychologist, change those FKs to SET NULL + a retention policy
--   and have counsel approve it.
-- * Storage: verification documents should be deleted after the decision + a short retention window
--   (e.g. 30 days) - schedule a job; store only the verified status long term.
-- * Admin workflow: open table verification_documents (status = 'pending'), view the file in Storage
--   (bucket verification-docs/<uid>/...), check the license against the national register, then
--   SELECT admin_set_verification('<uid>', 'verified' | 'rejected', 'reason');
-- * Notifications: the app schedules LOCAL reminders (WorkManager) for confirmed sessions. For push on new
--   requests/messages when the app is closed, add FCM + a Database Webhook / Edge Function (not in P1).
