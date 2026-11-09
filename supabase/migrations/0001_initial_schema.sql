-- Music initial schema. Apply to the shared Interaccion Supabase project; Music tables are isolated by name.
create extension if not exists pgcrypto;

create table if not exists public.music_profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  display_name text not null default 'Music user',
  avatar_path text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.artists (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  biography text,
  image_path text,
  version bigint not null default 1,
  updated_at timestamptz not null default now()
);

create table if not exists public.albums (
  id uuid primary key default gen_random_uuid(),
  artist_id uuid references public.artists(id) on delete set null,
  title text not null,
  artwork_path text,
  release_date date,
  version bigint not null default 1,
  updated_at timestamptz not null default now()
);

create table if not exists public.tracks (
  id uuid primary key default gen_random_uuid(),
  artist_id uuid references public.artists(id) on delete set null,
  album_id uuid references public.albums(id) on delete set null,
  title text not null,
  duration_ms bigint,
  media_source text,
  artwork_path text,
  content_type text not null default 'audio' check (content_type in ('audio','video')),
  version bigint not null default 1,
  updated_at timestamptz not null default now()
);

create table if not exists public.videos (
  id uuid primary key default gen_random_uuid(),
  artist_id uuid references public.artists(id) on delete set null,
  title text not null,
  thumbnail_path text,
  provider text not null,
  provider_video_id text,
  authorized_media_path text,
  version bigint not null default 1,
  updated_at timestamptz not null default now(),
  constraint videos_source_check check (
    (provider = 'youtube' and provider_video_id is not null and authorized_media_path is null)
    or (provider <> 'youtube' and authorized_media_path is not null)
  )
);

create table if not exists public.playlists (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  description text,
  is_collaborative boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.playlist_members (
  playlist_id uuid not null references public.playlists(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  role text not null default 'viewer' check (role in ('viewer','editor')),
  created_at timestamptz not null default now(),
  primary key (playlist_id, user_id)
);

create table if not exists public.playlist_items (
  id uuid primary key default gen_random_uuid(),
  playlist_id uuid not null references public.playlists(id) on delete cascade,
  track_id uuid not null references public.tracks(id) on delete cascade,
  added_by uuid references auth.users(id) on delete set null,
  position integer not null default 0,
  created_at timestamptz not null default now(),
  unique (playlist_id, track_id)
);

create table if not exists public.push_devices (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  fcm_token text not null unique,
  platform text not null default 'android' check (platform in ('android')),
  notifications_enabled boolean not null default true,
  last_seen_at timestamptz not null default now(),
  created_at timestamptz not null default now()
);

create index if not exists push_devices_user_id_idx on public.push_devices(user_id);

create table if not exists public.notifications (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  category text not null default 'general',
  title text not null,
  body text not null,
  data jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  read_at timestamptz,
  push_sent_at timestamptz
);

create index if not exists notifications_user_created_idx
  on public.notifications(user_id, created_at desc);

alter table public.music_profiles enable row level security;
alter table public.artists enable row level security;
alter table public.albums enable row level security;
alter table public.tracks enable row level security;
alter table public.videos enable row level security;
alter table public.playlists enable row level security;
alter table public.playlist_members enable row level security;
alter table public.playlist_items enable row level security;
alter table public.push_devices enable row level security;
alter table public.notifications enable row level security;

create policy "music_profiles_select_authenticated" on public.music_profiles
  for select to authenticated using (true);
create policy "music_profiles_insert_own" on public.music_profiles
  for insert to authenticated with check (auth.uid() = id);
create policy "music_profiles_update_own" on public.music_profiles
  for update to authenticated using (auth.uid() = id) with check (auth.uid() = id);

-- Catalog is readable by app users. Only trusted project administrators should write catalog rows.
create policy "artists_read_public_catalog" on public.artists
  for select to anon, authenticated using (true);
create policy "albums_read_public_catalog" on public.albums
  for select to anon, authenticated using (true);
create policy "tracks_read_public_catalog" on public.tracks
  for select to anon, authenticated using (true);
create policy "videos_read_public_catalog" on public.videos
  for select to anon, authenticated using (true);

-- SECURITY DEFINER helpers avoid recursive RLS when playlist policies inspect memberships.
create or replace function public.is_playlist_owner(p_playlist_id uuid, p_user_id uuid)
returns boolean language sql stable security definer set search_path = public
as $function$ select exists (select 1 from public.playlists p where p.id = p_playlist_id and p.owner_id = p_user_id); $function$;

create or replace function public.is_playlist_member(p_playlist_id uuid, p_user_id uuid)
returns boolean language sql stable security definer set search_path = public
as $function$ select exists (select 1 from public.playlist_members pm where pm.playlist_id = p_playlist_id and pm.user_id = p_user_id); $function$;

create or replace function public.can_manage_playlist(p_playlist_id uuid, p_user_id uuid)
returns boolean language sql stable security definer set search_path = public
as $function$
  select public.is_playlist_owner(p_playlist_id, p_user_id)
    or exists (select 1 from public.playlist_members pm where pm.playlist_id = p_playlist_id and pm.user_id = p_user_id and pm.role = 'editor');
$function$;

create or replace function public.can_view_playlist(p_playlist_id uuid, p_user_id uuid)
returns boolean language sql stable security definer set search_path = public
as $function$ select public.is_playlist_owner(p_playlist_id, p_user_id) or public.is_playlist_member(p_playlist_id, p_user_id); $function$;

revoke all on function public.is_playlist_owner(uuid, uuid) from public;
revoke all on function public.is_playlist_member(uuid, uuid) from public;
revoke all on function public.can_manage_playlist(uuid, uuid) from public;
revoke all on function public.can_view_playlist(uuid, uuid) from public;
grant execute on function public.is_playlist_owner(uuid, uuid) to authenticated;
grant execute on function public.is_playlist_member(uuid, uuid) to authenticated;
grant execute on function public.can_manage_playlist(uuid, uuid) to authenticated;
grant execute on function public.can_view_playlist(uuid, uuid) to authenticated;

create policy "playlists_read_owner_or_member" on public.playlists
  for select to authenticated using (
    public.can_view_playlist(id, auth.uid())
  );
create policy "playlists_insert_owner" on public.playlists
  for insert to authenticated with check (owner_id = auth.uid());
create policy "playlists_update_owner" on public.playlists
  for update to authenticated using (owner_id = auth.uid()) with check (owner_id = auth.uid());
create policy "playlists_delete_owner" on public.playlists
  for delete to authenticated using (owner_id = auth.uid());

create policy "playlist_members_read_related" on public.playlist_members
  for select to authenticated using (
    user_id = auth.uid() or public.is_playlist_owner(playlist_id, auth.uid())
  );
create policy "playlist_members_owner_manage" on public.playlist_members
  for all to authenticated using (
    public.is_playlist_owner(playlist_id, auth.uid())
  ) with check (
    public.is_playlist_owner(playlist_id, auth.uid())
  );

create policy "playlist_items_read_related" on public.playlist_items
  for select to authenticated using (
    public.can_view_playlist(playlist_id, auth.uid())
  );
create policy "playlist_items_add_owner_or_editor" on public.playlist_items
  for insert to authenticated with check (
    added_by = auth.uid() and public.can_manage_playlist(playlist_id, auth.uid())
  );
create policy "playlist_items_remove_owner_or_editor" on public.playlist_items
  for delete to authenticated using (
    public.can_manage_playlist(playlist_id, auth.uid())
  );
create policy "playlist_items_update_owner_or_editor" on public.playlist_items
  for update to authenticated using (
    public.can_manage_playlist(playlist_id, auth.uid())
  ) with check (
    public.can_manage_playlist(playlist_id, auth.uid())
  );

create policy "push_devices_select_own" on public.push_devices
  for select to authenticated using (user_id = auth.uid());
create policy "push_devices_insert_own" on public.push_devices
  for insert to authenticated with check (user_id = auth.uid());
create policy "push_devices_update_own" on public.push_devices
  for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "push_devices_delete_own" on public.push_devices
  for delete to authenticated using (user_id = auth.uid());

create policy "notifications_read_own" on public.notifications
  for select to authenticated using (user_id = auth.uid());
create policy "notifications_mark_own_read" on public.notifications
  for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());

-- Guests are intentionally local-only. Do not create cloud rows until the user signs in.
