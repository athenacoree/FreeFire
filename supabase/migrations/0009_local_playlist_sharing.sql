-- Share local playlists as metadata-only public pages. Device media files are never uploaded.
create table if not exists public.music_shared_local_playlists (
  id uuid primary key default gen_random_uuid(),
  title text not null check (char_length(title) between 1 and 100),
  description text not null default '' check (char_length(description) <= 500),
  tracks jsonb not null default '[]'::jsonb check (jsonb_typeof(tracks) = 'array'),
  created_by uuid references auth.users(id) on delete set null,
  created_at timestamptz not null default now()
);
alter table public.music_shared_local_playlists enable row level security;
revoke all on public.music_shared_local_playlists from anon, authenticated;

alter table public.music_share_links
  drop constraint if exists music_share_links_resource_type_check;
alter table public.music_share_links
  add constraint music_share_links_resource_type_check
  check (resource_type in ('artist','album','track','video','playlist','local_playlist'));

create or replace function public.create_music_local_playlist_share(
  p_title text,
  p_description text default '',
  p_tracks jsonb default '[]'::jsonb
)
returns text
language plpgsql
security definer
set search_path = public, extensions, pg_temp
as $function$
declare
  v_playlist_id uuid;
  v_token text;
  v_safe_tracks jsonb;
begin
  if p_title is null or char_length(trim(p_title)) not between 1 and 100 then
    raise exception 'Invalid playlist title';
  end if;
  if coalesce(p_description, '') <> '' and char_length(p_description) > 500 then
    raise exception 'Playlist description is too long';
  end if;
  if p_tracks is null or jsonb_typeof(p_tracks) <> 'array' then
    raise exception 'Playlist tracks must be an array';
  end if;
  if jsonb_array_length(p_tracks) > 200 or pg_column_size(p_tracks) > 65536 then
    raise exception 'Playlist is too large to share';
  end if;
  if exists (
    select 1 from jsonb_array_elements(p_tracks) as item
    where jsonb_typeof(item) <> 'object'
      or coalesce(item->>'title','') = ''
      or char_length(item->>'title') > 180
      or char_length(coalesce(item->>'artist','')) > 120
      or char_length(coalesce(item->>'album','')) > 120
  ) then
    raise exception 'Playlist contains invalid track metadata';
  end if;

  select coalesce(jsonb_agg(jsonb_build_object(
    'id', left(coalesce(item->>'id',''), 80),
    'title', item->>'title',
    'artist', left(coalesce(item->>'artist',''), 120),
    'album', left(coalesce(item->>'album',''), 120),
    'duration_ms', case when coalesce(item->>'duration_ms','') ~ '^[0-9]{1,12}$'
      then (item->>'duration_ms')::bigint else null end,
    'content_type', case when item->>'content_type' = 'video' then 'video' else 'audio' end
  )), '[]'::jsonb)
  into v_safe_tracks
  from jsonb_array_elements(p_tracks) as item;

  insert into public.music_shared_local_playlists(title, description, tracks, created_by)
  values (trim(p_title), coalesce(p_description,''), v_safe_tracks, auth.uid())
  returning id into v_playlist_id;

  v_token := encode(extensions.gen_random_bytes(32), 'hex');
  insert into public.music_share_links(token_hash, resource_type, resource_id, created_by)
  values (encode(extensions.digest(v_token, 'sha256'), 'hex'), 'local_playlist', v_playlist_id, auth.uid());
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

  if v_link.resource_type = 'local_playlist' then
    select jsonb_build_object(
      'type','local_playlist','id',p.id,'title',p.title,
      'description',p.description,'is_collaborative',false,'tracks',p.tracks
    ) into v_result
    from public.music_shared_local_playlists p where p.id = v_link.resource_id;
  elsif v_link.resource_type = 'artist' then
    select jsonb_build_object('type','artist','id',a.id,'title',a.name,
      'description',coalesce(a.biography,''),'image_path',a.image_path)
    into v_result from public.artists a where a.id = v_link.resource_id;
  elsif v_link.resource_type = 'album' then
    select jsonb_build_object('type','album','id',al.id,'title',al.title,
      'description',coalesce(ar.name,''),'image_path',al.artwork_path,'artist_id',al.artist_id)
    into v_result from public.albums al left join public.artists ar on ar.id = al.artist_id
    where al.id = v_link.resource_id;
  elsif v_link.resource_type = 'track' then
    select jsonb_build_object('type','track','id',t.id,'title',t.title,
      'description',coalesce(ar.name,''),'image_path',t.artwork_path,
      'artist_id',t.artist_id,'album_id',t.album_id,'duration_ms',t.duration_ms,
      'content_type',t.content_type)
    into v_result from public.tracks t left join public.artists ar on ar.id = t.artist_id
    where t.id = v_link.resource_id;
  elsif v_link.resource_type = 'video' then
    select jsonb_build_object('type','video','id',v.id,'title',v.title,
      'description',coalesce(ar.name,''),'image_path',v.thumbnail_path,
      'provider',v.provider,'provider_video_id',v.provider_video_id)
    into v_result from public.videos v left join public.artists ar on ar.id = v.artist_id
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

revoke all on function public.create_music_local_playlist_share(text, text, jsonb) from public;
grant execute on function public.create_music_local_playlist_share(text, text, jsonb) to anon, authenticated;
