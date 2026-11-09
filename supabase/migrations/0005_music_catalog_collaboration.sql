-- Music-only catalog versioning, collaborative playlist roles and artwork bucket.
create index if not exists music_artists_updated_at_idx on public.artists(updated_at desc);
create index if not exists music_albums_artist_updated_idx on public.albums(artist_id, updated_at desc);
create index if not exists music_tracks_artist_album_idx on public.tracks(artist_id, album_id);
create index if not exists music_videos_updated_at_idx on public.videos(updated_at desc);
create index if not exists music_playlist_items_order_idx on public.playlist_items(playlist_id, position, created_at);
create index if not exists music_playlist_members_user_idx on public.playlist_members(user_id, playlist_id);

create or replace function private.bump_music_catalog_version()
returns trigger language plpgsql set search_path = ''
as $function$
begin
  new.version := old.version + 1;
  new.updated_at := now();
  return new;
end;
$function$;
drop trigger if exists music_artists_bump_version on public.artists;
create trigger music_artists_bump_version before update on public.artists for each row execute function private.bump_music_catalog_version();
drop trigger if exists music_albums_bump_version on public.albums;
create trigger music_albums_bump_version before update on public.albums for each row execute function private.bump_music_catalog_version();
drop trigger if exists music_tracks_bump_version on public.tracks;
create trigger music_tracks_bump_version before update on public.tracks for each row execute function private.bump_music_catalog_version();
drop trigger if exists music_videos_bump_version on public.videos;
create trigger music_videos_bump_version before update on public.videos for each row execute function private.bump_music_catalog_version();

drop policy if exists "playlists_update_owner" on public.playlists;
drop policy if exists "playlists_update_owner_or_editor" on public.playlists;
create policy "playlists_update_owner_or_editor" on public.playlists for update to authenticated
using (private.can_manage_playlist(id, auth.uid()))
with check (private.can_manage_playlist(id, auth.uid()));

create or replace function public.music_invite_playlist_member(p_playlist_id uuid, p_user_id uuid, p_role text default 'viewer')
returns void language plpgsql security definer set search_path = ''
as $function$
begin
  if auth.uid() is null then raise exception 'Authentication required'; end if;
  if p_role not in ('viewer', 'editor') then raise exception 'Invalid playlist role'; end if;
  if not private.is_playlist_owner(p_playlist_id, auth.uid()) then raise exception 'Only the playlist owner can invite members'; end if;
  if p_user_id = auth.uid() then raise exception 'Owner is already a member'; end if;
  insert into public.playlist_members(playlist_id, user_id, role)
  values (p_playlist_id, p_user_id, p_role)
  on conflict (playlist_id, user_id) do update set role = excluded.role;
end;
$function$;
revoke all on function public.music_invite_playlist_member(uuid, uuid, text) from public, anon;
grant execute on function public.music_invite_playlist_member(uuid, uuid, text) to authenticated;

insert into storage.buckets(id, name, public, file_size_limit, allowed_mime_types)
values ('music-artwork', 'music-artwork', true, 5242880, array['image/jpeg','image/png','image/webp','image/avif'])
on conflict (id) do update set public = true, file_size_limit = 5242880, allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists "music_artwork_read_public" on storage.objects;
create policy "music_artwork_read_public" on storage.objects for select to anon, authenticated
using (bucket_id = 'music-artwork');
drop policy if exists "music_artwork_insert_own" on storage.objects;
create policy "music_artwork_insert_own" on storage.objects for insert to authenticated
with check (bucket_id = 'music-artwork' and (storage.foldername(name))[1] = auth.uid()::text);
drop policy if exists "music_artwork_update_own" on storage.objects;
create policy "music_artwork_update_own" on storage.objects for update to authenticated
using (bucket_id = 'music-artwork' and (storage.foldername(name))[1] = auth.uid()::text)
with check (bucket_id = 'music-artwork' and (storage.foldername(name))[1] = auth.uid()::text);
drop policy if exists "music_artwork_delete_own" on storage.objects;
create policy "music_artwork_delete_own" on storage.objects for delete to authenticated
using (bucket_id = 'music-artwork' and (storage.foldername(name))[1] = auth.uid()::text);
