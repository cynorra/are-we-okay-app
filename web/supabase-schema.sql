-- Okayness Supabase Schema
-- Run this in your Supabase SQL editor to bootstrap the database.

-- WARNING: This will reset the tables for the Okayness app.
DROP TABLE IF EXISTS posts, reactions, checkins, friendships, waitlist, users CASCADE;


-- 1. Create Custom Types (safely check if they exist first)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'mood_state') THEN
        CREATE TYPE mood_state AS ENUM ('good', 'bad', 'unsure');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_role') THEN
        CREATE TYPE user_role AS ENUM ('user', 'consultant', 'moderator', 'admin');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'reaction_type') THEN
        CREATE TYPE reaction_type AS ENUM ('hug', 'feel_this', 'strength', 'you_got_this');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'moderation_status') THEN
        CREATE TYPE moderation_status AS ENUM ('pending', 'approved', 'rejected', 'flagged');
    END IF;
END$$;


-- 2. Users Table
CREATE TABLE users (
  id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  email TEXT UNIQUE,
  username TEXT UNIQUE NOT NULL,
  display_name TEXT,
  avatar_emoji TEXT DEFAULT '🌙',
  age_verified BOOLEAN DEFAULT true,
  role user_role DEFAULT 'user',
  is_verified_consultant BOOLEAN DEFAULT false,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  deleted_at TIMESTAMPTZ
);

-- 3. Daily Check-ins
CREATE TABLE checkins (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  mood mood_state NOT NULL,
  note TEXT,
  is_public BOOLEAN DEFAULT true,
  language TEXT DEFAULT 'en',
  country_code TEXT,
  city_slug TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 4. Posts
CREATE TABLE posts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  checkin_id UUID REFERENCES checkins(id) ON DELETE SET NULL,
  content TEXT NOT NULL,
  mood mood_state,
  has_content_warning BOOLEAN DEFAULT false,
  content_warning_label TEXT,
  is_anonymous BOOLEAN DEFAULT true,
  is_moderated BOOLEAN DEFAULT false,
  status moderation_status DEFAULT 'approved',
  language TEXT DEFAULT 'en',
  created_at TIMESTAMPTZ DEFAULT NOW(),
  deleted_at TIMESTAMPTZ
);

-- 5. Reactions
CREATE TABLE reactions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  post_id UUID REFERENCES posts(id) ON DELETE CASCADE,
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  type reaction_type NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(post_id, user_id, type)
);

-- 6. Friendships
CREATE TABLE friendships (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  requester_id UUID REFERENCES users(id) ON DELETE CASCADE,
  addressee_id UUID REFERENCES users(id) ON DELETE CASCADE,
  status TEXT DEFAULT 'pending', -- 'pending', 'accepted', 'blocked'
  created_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(requester_id, addressee_id)
);

-- 7. Waitlist (For Phase 0)
CREATE TABLE waitlist (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email TEXT UNIQUE NOT NULL,
  mood TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Set up Row Level Security (RLS)
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE checkins ENABLE ROW LEVEL SECURITY;
ALTER TABLE posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE reactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE waitlist ENABLE ROW LEVEL SECURITY;

-- Basic Policies (Update these for production)
CREATE POLICY "Public profiles are viewable by everyone." ON users FOR SELECT USING (true);
CREATE POLICY "Users can insert their own profile." ON users FOR INSERT WITH CHECK (auth.uid() = id);
CREATE POLICY "Users can update own profile." ON users FOR UPDATE USING (auth.uid() = id);

CREATE POLICY "Public posts are viewable by everyone." ON posts FOR SELECT USING (deleted_at IS NULL AND status = 'approved');
CREATE POLICY "Users can create posts." ON posts FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Public checkins are viewable." ON checkins FOR SELECT USING (is_public = true);
CREATE POLICY "Users can create checkins." ON checkins FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Waitlist can be inserted anonymously." ON waitlist FOR INSERT WITH CHECK (true);
