-- Create a Music-only profile whenever Supabase Auth creates a new account.
-- This trigger does not touch Interaccion's existing profiles table or triggers.

create or replace function private.handle_new_music_auth_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $function$
begin
  if (new.raw_user_meta_data ->> 'app') is distinct from 'music' then
    return new;
  end if;

  insert into public.music_profiles (id, display_name)
  values (
    new.id,
    coalesce(
      nullif(new.raw_user_meta_data ->> 'display_name', ''),
      'Music user'
    )
  )
  on conflict (id) do nothing;

  return new;
end;
$function$;

revoke all on function private.handle_new_music_auth_user() from public, anon, authenticated;

drop trigger if exists on_auth_user_created_music on auth.users;
create trigger on_auth_user_created_music
  after insert on auth.users
  for each row execute function private.handle_new_music_auth_user();
