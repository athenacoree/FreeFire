-- Allow an authenticated user to mark only their own notification as read.
create or replace function public.music_mark_notification_read(p_notification_id uuid)
returns boolean language plpgsql security definer set search_path = ''
as $function$
declare v_updated integer;
begin
  if (select auth.uid()) is null then raise exception 'Authentication required'; end if;
  update public.notifications set read_at = now()
  where id = p_notification_id and user_id = (select auth.uid()) and read_at is null;
  get diagnostics v_updated = row_count;
  return v_updated > 0;
end;
$function$;
revoke all on function public.music_mark_notification_read(uuid) from public, anon;
grant execute on function public.music_mark_notification_read(uuid) to authenticated;
