-- Keep a forward migration that scopes the shared-project trigger to Music accounts only.
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
    coalesce(nullif(new.raw_user_meta_data ->> 'display_name', ''), 'Music user')
  )
  on conflict (id) do nothing;

  return new;
end;
$function$;

revoke all on function private.handle_new_music_auth_user() from public, anon, authenticated;
