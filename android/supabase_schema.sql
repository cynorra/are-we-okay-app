create table users (
  id uuid primary key references auth.users(id) on delete cascade,
  email text unique,
  username text unique not null,
  avatar_emoji text not null default '🌙',
  role text not null default 'user',
  created_at timestamptz not null default now()
);

create table checkins (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references users(id) on delete cascade,
  mood text not null check (mood in ('good','bad','unsure')),
  note text,
  is_public boolean not null default true,
  created_at timestamptz not null default now()
);

create table posts (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references users(id) on delete cascade,
  checkin_id uuid references checkins(id) on delete set null,
  content text not null,
  mood text check (mood in ('good','bad','unsure')),
  is_anonymous boolean not null default true,
  status text not null default 'approved' check (status in ('pending','approved','rejected')),
  deleted_at timestamptz,
  created_at timestamptz not null default now()
);

create table reactions (
  id uuid primary key default gen_random_uuid(),
  post_id uuid not null references posts(id) on delete cascade,
  user_id uuid not null references users(id) on delete cascade,
  type text not null check (type in ('hug','feel_this','strength','you_got_this')),
  created_at timestamptz not null default now(),
  unique (post_id, user_id, type)
);

create table friendships (
  id uuid primary key default gen_random_uuid(),
  requester_id uuid not null references users(id) on delete cascade,
  addressee_id uuid not null references users(id) on delete cascade,
  status text not null default 'pending' check (status in ('pending','accepted','blocked')),
  created_at timestamptz not null default now(),
  unique (requester_id, addressee_id)
);

create table comments (
  id uuid primary key default gen_random_uuid(),
  post_id uuid not null references posts(id) on delete cascade,
  user_id uuid not null references users(id) on delete cascade,
  content text not null,
  created_at timestamptz not null default now()
);

alter table users enable row level security;
alter table checkins enable row level security;
alter table posts enable row level security;
alter table reactions enable row level security;
alter table friendships enable row level security;
alter table comments enable row level security;

create policy users_select_all on users for select using (true);
create policy users_insert_own on users for insert with check (auth.uid() = id);
create policy users_update_own on users for update using (auth.uid() = id);

create policy checkins_own on checkins for all
  using (auth.uid() = user_id) with check (auth.uid() = user_id);

create policy posts_select_approved on posts for select
  using (status = 'approved' and deleted_at is null);
create policy posts_insert_own on posts for insert
  with check (auth.uid() = user_id);

create policy reactions_select_all on reactions for select using (true);
create policy reactions_insert_own on reactions for insert with check (auth.uid() = user_id);
create policy reactions_delete_own on reactions for delete using (auth.uid() = user_id);

create policy friendships_select_own on friendships for select
  using (auth.uid() = requester_id or auth.uid() = addressee_id);
create policy friendships_insert_own on friendships for insert
  with check (auth.uid() = requester_id);
create policy friendships_update_addressee on friendships for update
  using (auth.uid() = addressee_id);

create policy comments_select_all on comments for select using (true);
create policy comments_insert_own on comments for insert with check (auth.uid() = user_id);
