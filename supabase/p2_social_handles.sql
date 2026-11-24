-- Optional social handles on the public profile. Safe to run more than once.
alter table users add column if not exists instagram text;
alter table users add column if not exists tiktok text;
alter table users add column if not exists x_handle text;
alter table users add column if not exists website text;
