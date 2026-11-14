-- Public share tokens for catalog entities and collaborative playlists.
-- The raw token is returned once; only its SHA-256 digest is stored.
create table if not exists public.music_share_links (
  id uuid primary key default gen_random_uuid(),
  token_hash text not null unique,
  resource_type text not null check (resource_type in ('artist','album','track','video','playlist')),
  resource_id uuid not null,
  created_by uuid references auth.users(id) on delete set null,
  created_at timestamptz not null default now(),
  expires_at timestamptz,
  revoked_at timestamptz
);

create index if not exists music_share_links_resource_idx
  on public.music_share_links(resource_type, resource_id);
create index if not exists music_share_links_expiry_idx
  on public.music_share_links(expires_at) where revoked_at is null;

alter table public.music_share_links enable row level security;
revoke all on public.music_share_links from anon, authenticated;

create or replace function public.create_music_share_link(p_resource_type text, p_resource_id uuid)
returns text
language plpgsql
security definer
set search_path = public, extensions, pg_temp
as $function$
declare
  v_token text;
  v_user uuid := auth.uid();
  v_exists boolean := false;
begin
  if p_resource_type not in ('artist','album','track','video','playlist') then
    raise exception 'Unsupported share type';
  end if;

  if p_resource_type = 'playlist' then
    if v_user is null then raise exception 'Authentication required'; end if;
    select exists (
      select 1 from public.playlists p
      where p.id = p_resource_id and (
        p.owner_id = v_user or exists (
          select 1 from public.playlist_members pm
          where pm.playlist_id = p.id and pm.user_id = v_user and pm.role = 'editor'
        )
      )
    ) into v_exists;
  elsif p_resource_type = 'artist' then
    select exists(select 1 from public.artists where id = p_resource_id) into v_exists;
  elsif p_resource_type = 'album' then
    select exists(select 1 from public.albums where id = p_resource_id) into v_exists;
  elsif p_resource_type = 'track' then
    select exists(select 1 from public.tracks where id = p_resource_id) into v_exists;
  elsif p_resource_type = 'video' then
    select exists(select 1 from public.videos where id = p_resource_id) into v_exists;
  end if;

  if not v_exists then raise exception 'Content not found or not shareable'; end if;

  v_token := encode(extensions.gen_random_bytes(32), 'hex');
  insert into public.music_share_links(token_hash, resource_type, resource_id, created_by)
  values (encode(extensions.digest(v_token, 'sha256'), 'hex'), p_resource_type, p_resource_id, v_user);
  return v_token;
end;
$function$;

create or replace function public.get_music_shared_content(p_token text)
returns jsonb
language plpgsql
stable
security definer
set search_path = public, extensions, pg_temp
as $function$
declare
  v_link public.music_share_links%rowtype;
  v_result jsonb;
begin
  if p_token is null or p_token !~ '^[0-9a-f]{64}$' then return null; end if;

  select * into v_link
  from public.music_share_links
  where token_hash = encode(extensions.digest(p_token, 'sha256'), 'hex')
    and revoked_at is null
    and (expires_at is null or expires_at > now())
  limit 1;

  if not found then return null; end if;

  if v_link.resource_type = 'artist' then
    select jsonb_build_object(
      'type','artist','id',a.id,'title',a.name,'description',coalesce(a.biography,''),
      'image_path',a.image_path
    ) into v_result from public.artists a where a.id = v_link.resource_id;
  elsif v_link.resource_type = 'album' then
    select jsonb_build_object(
      'type','album','id',al.id,'title',al.title,'description',coalesce(ar.name,''),
      'image_path',al.artwork_path,'artist_id',al.artist_id
    ) into v_result
    from public.albums al left join public.artists ar on ar.id = al.artist_id
    where al.id = v_link.resource_id;
  elsif v_link.resource_type = 'track' then
    select jsonb_build_object(
      'type','track','id',t.id,'title',t.title,'description',coalesce(ar.name,''),
      'image_path',t.artwork_path,'artist_id',t.artist_id,'album_id',t.album_id,
      'duration_ms',t.duration_ms,'content_type',t.content_type
    ) into v_result
    from public.tracks t left join public.artists ar on ar.id = t.artist_id
    where t.id = v_link.resource_id;
  elsif v_link.resource_type = 'video' then
    select jsonb_build_object(
      'type','video','id',v.id,'title',v.title,'description',coalesce(ar.name,''),
      'image_path',v.thumbnail_path,'provider',v.provider,'provider_video_id',v.provider_video_id
    ) into v_result
    from public.videos v left join public.artists ar on ar.id = v.artist_id
    where v.id = v_link.resource_id;
  elsif v_link.resource_type = 'playlist' then
    select jsonb_build_object(
      'type','playlist','id',p.id,'title',p.name,'description',coalesce(p.description,''),
      'is_collaborative',p.is_collaborative,
      'tracks',coalesce((
        select jsonb_agg(jsonb_build_object(
          'id',t.id,'title',t.title,'artist',coalesce(ar.name,''),
          'album',coalesce(al.title,''),'image_path',t.artwork_path,
          'duration_ms',t.duration_ms,'content_type',t.content_type
        ) order by pi.position, pi.created_at)
        from public.playlist_items pi
        join public.tracks t on t.id = pi.track_id
        left join public.artists ar on ar.id = t.artist_id
        left join public.albums al on al.id = t.album_id
        where pi.playlist_id = p.id
      ), '[]'::jsonb)
    ) into v_result from public.playlists p where p.id = v_link.resource_id;
  end if;

  return v_result;
end;
$function$;

revoke all on function public.create_music_share_link(text, uuid) from public;
revoke all on function public.get_music_shared_content(text) from public;
grant execute on function public.create_music_share_link(text, uuid) to anon, authenticated;
grant execute on function public.get_music_shared_content(text) to anon, authenticated;
