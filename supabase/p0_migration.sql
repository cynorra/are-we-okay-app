-- ============================================================================
-- Are We Okay - P0 migration (consent, mood v2, moderation, referrals, account deletion)
-- NOT EXECUTED. Review, then run once in the Supabase SQL editor (idempotent where possible).
--
-- Data residency / GDPR note: mood, emotion labels and notes are health-related special-category
-- data (GDPR Art. 9, KVKK Art. 6). Prefer the Supabase project region in the EU (e.g. eu-central-1
-- Frankfurt). If the current project is in another region, migrate or create a new EU project before
-- scaling beyond a closed test, and describe the transfer/processors in the privacy policy.
-- Policy stance: RLS enabled on every new table with NO policy = deny by default; only the
-- policies below open specific operations. Never grant anon access to these tables.
-- ============================================================================

-- ---------- users: consent record + locale -----------------------------------------------------
ALTER TABLE users ADD COLUMN IF NOT EXISTS consent_version int;
ALTER TABLE users ADD COLUMN IF NOT EXISTS consent_at timestamptz;
ALTER TABLE users ADD COLUMN IF NOT EXISTS locale text;
ALTER TABLE users ADD COLUMN IF NOT EXISTS country text;
ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted_at timestamptz;

-- ---------- checkins: mood v2 ------------------------------------------------------------------
-- Legacy `mood` (good|bad|unsure) stays and is still written by the app (derived from level) so old
-- app versions and the feed keep working.
ALTER TABLE checkins ADD COLUMN IF NOT EXISTS mood_level smallint CHECK (mood_level BETWEEN 1 AND 5);
ALTER TABLE checkins ADD COLUMN IF NOT EXISTS emotions text[] NOT NULL DEFAULT '{}';
ALTER TABLE checkins ADD COLUMN IF NOT EXISTS activities text[] NOT NULL DEFAULT '{}';
ALTER TABLE checkins ADD COLUMN IF NOT EXISTS sleep_hours numeric(3,1);

-- Backfill: good -> 4, unsure -> 3, bad -> 1 (matches the Android MoodScale.fromLegacy mapping).
UPDATE checkins SET mood_level = CASE mood::text WHEN 'good' THEN 4 WHEN 'unsure' THEN 3 WHEN 'bad' THEN 1 END
WHERE mood_level IS NULL;

-- Owners must be able to read their own (including private) check-ins; existing policy only exposes public ones.
DROP POLICY IF EXISTS "Users can read own checkins." ON checkins;
CREATE POLICY "Users can read own checkins." ON checkins FOR SELECT USING (auth.uid() = user_id);

-- ---------- reports ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reports (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  reporter_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  target_type text NOT NULL CHECK (target_type IN ('post','comment','user')),
  target_id uuid NOT NULL,
  reason text NOT NULL CHECK (reason IN ('spam','harassment','self_harm','other')),
  status text NOT NULL DEFAULT 'open' CHECK (status IN ('open','reviewed','actioned','dismissed')),
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS reports_status_idx ON reports(status, created_at);
ALTER TABLE reports ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Users can file reports." ON reports;
CREATE POLICY "Users can file reports." ON reports FOR INSERT WITH CHECK (auth.uid() = reporter_id);
DROP POLICY IF EXISTS "Admins can read reports." ON reports;
CREATE POLICY "Admins can read reports." ON reports FOR SELECT
  USING (EXISTS (SELECT 1 FROM users u WHERE u.id = auth.uid() AND u.role = 'admin'));
DROP POLICY IF EXISTS "Admins can update reports." ON reports;
CREATE POLICY "Admins can update reports." ON reports FOR UPDATE
  USING (EXISTS (SELECT 1 FROM users u WHERE u.id = auth.uid() AND u.role = 'admin'));

-- ---------- blocks -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS blocks (
  blocker_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  blocked_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (blocker_id, blocked_id),
  CHECK (blocker_id <> blocked_id)
);
ALTER TABLE blocks ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Users manage own blocks (select)." ON blocks;
CREATE POLICY "Users manage own blocks (select)." ON blocks FOR SELECT USING (auth.uid() = blocker_id);
DROP POLICY IF EXISTS "Users manage own blocks (insert)." ON blocks;
CREATE POLICY "Users manage own blocks (insert)." ON blocks FOR INSERT WITH CHECK (auth.uid() = blocker_id);
DROP POLICY IF EXISTS "Users manage own blocks (delete)." ON blocks;
CREATE POLICY "Users manage own blocks (delete)." ON blocks FOR DELETE USING (auth.uid() = blocker_id);

-- ---------- referrals (invite deep link) -------------------------------------------------------
CREATE TABLE IF NOT EXISTS referrals (
  inviter_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  invitee_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  source text,
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (invitee_id),            -- an account can be attributed to one inviter only
  CHECK (inviter_id <> invitee_id)
);
ALTER TABLE referrals ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Invitee records own referral." ON referrals;
CREATE POLICY "Invitee records own referral." ON referrals FOR INSERT WITH CHECK (auth.uid() = invitee_id);
DROP POLICY IF EXISTS "Inviter reads own referrals." ON referrals;
CREATE POLICY "Inviter reads own referrals." ON referrals FOR SELECT USING (auth.uid() = inviter_id OR auth.uid() = invitee_id);

-- ---------- account deletion RPC ---------------------------------------------------------------
-- Called by the app as POST /rest/v1/rpc/delete_my_account with the user's own JWT.
-- Deletes all of the caller's rows and then the auth user itself (GDPR Art. 17 / KVKK Art. 7).
-- Tables are processed only if they exist, so this works with the current schema and later additions.
CREATE OR REPLACE FUNCTION public.delete_my_account()
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth
AS $$
DECLARE
  uid uuid := auth.uid();
  t record;
BEGIN
  IF uid IS NULL THEN
    RAISE EXCEPTION 'not authenticated';
  END IF;

  -- (table, column) pairs that reference the user. Order: children before parents.
  FOR t IN
    SELECT * FROM (VALUES
      ('reactions','user_id'), ('comments','user_id'),
      ('reports','reporter_id'), ('blocks','blocker_id'), ('blocks','blocked_id'),
      ('referrals','inviter_id'), ('referrals','invitee_id'),
      ('friendships','requester_id'), ('friendships','addressee_id'),
      ('posts','user_id'), ('checkins','user_id'),
      ('journal_entries','user_id'), ('user_badges','user_id'), ('device_tokens','user_id'),
      ('users','id')
    ) AS v(tbl, col)
  LOOP
    IF to_regclass('public.' || t.tbl) IS NOT NULL THEN
      EXECUTE format('DELETE FROM public.%I WHERE %I = $1', t.tbl, t.col) USING uid;
    END IF;
  END LOOP;

  DELETE FROM auth.users WHERE id = uid;
END;
$$;

REVOKE ALL ON FUNCTION public.delete_my_account() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.delete_my_account() TO authenticated;

-- ---------- notes ------------------------------------------------------------------------------
-- * Posts of blocked users are filtered client-side (also consider a view/policy excluding blocks).
-- * Reports have no automatic action yet: review them in the Supabase dashboard (table `reports`,
--   filter status = 'open') or build the web admin panel (plan P0.8). Content moderation SLA and the
--   word filter are still TODO.
-- * Journal is local-only (encrypted on device); no server table is required for P0.
-- * Backups: deleted rows persist in Supabase backups until they rotate; state this in the privacy policy.
