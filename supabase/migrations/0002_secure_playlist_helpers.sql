-- Move playlist authorization helpers out of the exposed public API schema.
create schema if not exists private;

create or replace function private.is_playlist_owner(p_playlist_id uuid, p_user_id uuid)
returns boolean language sql stable security definer set search_path = ''
as $function$
  select exists (
    select 1 from public.playlists p
    where p.id = p_playlist_id and p.owner_id = p_user_id
  );
$function$;

create or replace function private.is_playlist_member(p_playlist_id uuid, p_user_id uuid)
returns boolean language sql stable security definer set search_path = ''
as $function$
  select exists (
    select 1 from public.playlist_members pm
    where pm.playlist_id = p_playlist_id and pm.user_id = p_user_id
  );
$function$;

create or replace function private.can_manage_playlist(p_playlist_id uuid, p_user_id uuid)
returns boolean language sql stable security definer set search_path = ''
as $function$
  select private.is_playlist_owner(p_playlist_id, p_user_id)
    or exists (
      select 1 from public.playlist_members pm
      where pm.playlist_id = p_playlist_id
        and pm.user_id = p_user_id
        and pm.role = 'editor'
    );
$function$;

create or replace function private.can_view_playlist(p_playlist_id uuid, p_user_id uuid)
returns boolean language sql stable security definer set search_path = ''
as $function$
  select private.is_playlist_owner(p_playlist_id, p_user_id)
    or private.is_playlist_member(p_playlist_id, p_user_id);
$function$;

revoke all on schema private from public, anon;
grant usage on schema private to authenticated;

revoke all on function private.is_playlist_owner(uuid, uuid) from public, anon;
revoke all on function private.is_playlist_member(uuid, uuid) from public, anon;
revoke all on function private.can_manage_playlist(uuid, uuid) from public, anon;
revoke all on function private.can_view_playlist(uuid, uuid) from public, anon;

grant execute on function private.is_playlist_owner(uuid, uuid) to authenticated;
grant execute on function private.is_playlist_member(uuid, uuid) to authenticated;
grant execute on function private.can_manage_playlist(uuid, uuid) to authenticated;
grant execute on function private.can_view_playlist(uuid, uuid) to authenticated;

drop policy if exists "playlists_read_owner_or_member" on public.playlists;
create policy "playlists_read_owner_or_member" on public.playlists
  for select to authenticated using (private.can_view_playlist(id, auth.uid()));

drop policy if exists "playlist_members_read_related" on public.playlist_members;
create policy "playlist_members_read_related" on public.playlist_members
  for select to authenticated using (
    user_id = auth.uid() or private.is_playlist_owner(playlist_id, auth.uid())
  );

drop policy if exists "playlist_members_owner_manage" on public.playlist_members;
create policy "playlist_members_owner_manage" on public.playlist_members
  for all to authenticated using (private.is_playlist_owner(playlist_id, auth.uid()))
  with check (private.is_playlist_owner(playlist_id, auth.uid()));

drop policy if exists "playlist_items_read_related" on public.playlist_items;
create policy "playlist_items_read_related" on public.playlist_items
  for select to authenticated using (private.can_view_playlist(playlist_id, auth.uid()));

drop policy if exists "playlist_items_add_owner_or_editor" on public.playlist_items;
create policy "playlist_items_add_owner_or_editor" on public.playlist_items
  for insert to authenticated with check (
    added_by = auth.uid() and private.can_manage_playlist(playlist_id, auth.uid())
  );

drop policy if exists "playlist_items_remove_owner_or_editor" on public.playlist_items;
create policy "playlist_items_remove_owner_or_editor" on public.playlist_items
  for delete to authenticated using (private.can_manage_playlist(playlist_id, auth.uid()));

drop policy if exists "playlist_items_update_owner_or_editor" on public.playlist_items;
create policy "playlist_items_update_owner_or_editor" on public.playlist_items
  for update to authenticated using (private.can_manage_playlist(playlist_id, auth.uid()))
  with check (private.can_manage_playlist(playlist_id, auth.uid()));

drop function if exists public.is_playlist_owner(uuid, uuid);
drop function if exists public.is_playlist_member(uuid, uuid);
drop function if exists public.can_manage_playlist(uuid, uuid);
drop function if exists public.can_view_playlist(uuid, uuid);
