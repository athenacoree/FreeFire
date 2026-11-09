-- Music security/performance refinement. This only changes Music-specific policies.
create index if not exists music_playlist_items_added_by_idx on public.playlist_items(added_by);
create index if not exists music_playlist_items_track_id_idx on public.playlist_items(track_id);
create index if not exists music_playlists_owner_id_idx on public.playlists(owner_id);
create index if not exists music_tracks_album_id_idx on public.tracks(album_id);
create index if not exists music_videos_artist_id_idx on public.videos(artist_id);

create or replace function public.music_invite_playlist_member(
  p_playlist_id uuid, p_user_id uuid, p_role text default 'viewer'
)
returns void language plpgsql security invoker set search_path = ''
as $function$
begin
  if (select auth.uid()) is null then raise exception 'Authentication required'; end if;
  if p_role not in ('viewer', 'editor') then raise exception 'Invalid playlist role'; end if;
  if not private.is_playlist_owner(p_playlist_id, (select auth.uid())) then
    raise exception 'Only the playlist owner can invite members';
  end if;
  if p_user_id = (select auth.uid()) then raise exception 'Owner is already a member'; end if;
  insert into public.playlist_members(playlist_id, user_id, role)
  values (p_playlist_id, p_user_id, p_role)
  on conflict (playlist_id, user_id) do update set role = excluded.role;
end;
$function$;
revoke all on function public.music_invite_playlist_member(uuid, uuid, text) from public, anon;
grant execute on function public.music_invite_playlist_member(uuid, uuid, text) to authenticated;

drop policy if exists "playlist_members_owner_manage" on public.playlist_members;
create policy "playlist_members_owner_insert" on public.playlist_members
for insert to authenticated with check (private.is_playlist_owner(playlist_id, (select auth.uid())));
create policy "playlist_members_owner_update" on public.playlist_members
for update to authenticated using (private.is_playlist_owner(playlist_id, (select auth.uid())))
with check (private.is_playlist_owner(playlist_id, (select auth.uid())));
create policy "playlist_members_owner_delete" on public.playlist_members
for delete to authenticated using (private.is_playlist_owner(playlist_id, (select auth.uid())));

alter policy music_profiles_insert_own on public.music_profiles with check ((select auth.uid()) = id);
alter policy music_profiles_update_own on public.music_profiles using ((select auth.uid()) = id) with check ((select auth.uid()) = id);
alter policy playlists_read_owner_or_member on public.playlists using (private.can_view_playlist(id, (select auth.uid())));
alter policy playlists_insert_owner on public.playlists with check (owner_id = (select auth.uid()));
alter policy playlists_update_owner_or_editor on public.playlists using (private.can_manage_playlist(id, (select auth.uid()))) with check (private.can_manage_playlist(id, (select auth.uid())));
alter policy playlists_delete_owner on public.playlists using (owner_id = (select auth.uid()));
alter policy playlist_members_read_related on public.playlist_members using (user_id = (select auth.uid()) or private.is_playlist_owner(playlist_id, (select auth.uid())));
alter policy playlist_items_read_related on public.playlist_items using (private.can_view_playlist(playlist_id, (select auth.uid())));
alter policy playlist_items_add_owner_or_editor on public.playlist_items with check (added_by = (select auth.uid()) and private.can_manage_playlist(playlist_id, (select auth.uid())));
alter policy playlist_items_remove_owner_or_editor on public.playlist_items using (private.can_manage_playlist(playlist_id, (select auth.uid())));
alter policy playlist_items_update_owner_or_editor on public.playlist_items using (private.can_manage_playlist(playlist_id, (select auth.uid()))) with check (private.can_manage_playlist(playlist_id, (select auth.uid())));
alter policy push_devices_select_own on public.push_devices using (user_id = (select auth.uid()));
alter policy push_devices_insert_own on public.push_devices with check (user_id = (select auth.uid()));
alter policy push_devices_update_own on public.push_devices using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));
alter policy push_devices_delete_own on public.push_devices using (user_id = (select auth.uid()));
alter policy notifications_read_own on public.notifications using (user_id = (select auth.uid()));
alter policy notifications_mark_own_read on public.notifications using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));
