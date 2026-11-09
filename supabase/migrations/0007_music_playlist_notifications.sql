-- Generate durable in-app notifications for Music playlist collaboration.
-- These trigger functions are private and cannot be called through the public Data API.

create or replace function private.notify_music_playlist_invitation()
returns trigger
language plpgsql
security definer
set search_path = ''
as $function$
declare
  v_playlist_name text;
begin
  select p.name into v_playlist_name
  from public.playlists p
  where p.id = new.playlist_id;

  insert into public.notifications(user_id, category, title, body, data)
  values (
    new.user_id,
    'playlist_invite',
    'Invitación a una lista',
    'Te invitaron a «' || coalesce(v_playlist_name, 'una lista') || '».',
    jsonb_build_object('playlist_id', new.playlist_id, 'role', new.role)
  );
  return new;
end;
$function$;

revoke all on function private.notify_music_playlist_invitation() from public, anon, authenticated;

drop trigger if exists music_playlist_invitation_notification on public.playlist_members;
create trigger music_playlist_invitation_notification
after insert on public.playlist_members
for each row execute function private.notify_music_playlist_invitation();

create or replace function private.notify_music_playlist_track_added()
returns trigger
language plpgsql
security definer
set search_path = ''
as $function$
declare
  v_playlist_name text;
  v_track_title text;
  v_recipient uuid;
begin
  select p.name into v_playlist_name
  from public.playlists p
  where p.id = new.playlist_id;

  select t.title into v_track_title
  from public.tracks t
  where t.id = new.track_id;

  for v_recipient in
    select p.owner_id
    from public.playlists p
    where p.id = new.playlist_id
      and p.owner_id is distinct from new.added_by
    union
    select pm.user_id
    from public.playlist_members pm
    where pm.playlist_id = new.playlist_id
      and pm.user_id is distinct from new.added_by
  loop
    insert into public.notifications(user_id, category, title, body, data)
    values (
      v_recipient,
      'playlist_track_added',
      'Nueva canción en una lista',
      coalesce(v_track_title, 'Una canción') || ' se añadió a «' || coalesce(v_playlist_name, 'una lista') || '».',
      jsonb_build_object('playlist_id', new.playlist_id, 'track_id', new.track_id)
    );
  end loop;

  return new;
end;
$function$;

revoke all on function private.notify_music_playlist_track_added() from public, anon, authenticated;

drop trigger if exists music_playlist_track_added_notification on public.playlist_items;
create trigger music_playlist_track_added_notification
after insert on public.playlist_items
for each row execute function private.notify_music_playlist_track_added();
