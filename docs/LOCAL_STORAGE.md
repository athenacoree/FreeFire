# Local storage and offline library

Music now has a Room-backed local library for tracks, favorites, playlists and playlist membership. The guest experience is local-first: these records stay on the device and are not uploaded automatically.

Preferences are stored with DataStore (light/dark appearance, performance mode and data-saver preference). The premium dark appearance is the first-run default; users can still switch to light mode.

## Media ownership and safety

- A local track can reference a user-selected device URI or an authorized media URI.
- A catalog record is metadata, not a downloaded media file.
- YouTube records must continue to use the official YouTube player/embed flow. The app must not extract, download, cache or play YouTube media in the background.
- The database stores metadata and URI references; it does not copy media files into app storage.

## Current scope

The Room schema is currently version 4. It stores artist/album relationships on catalog entries so detail views can query related songs and videos locally. The repository and library screen are wired together. Users can select audio files through Android’s document picker, keep their URI permission, see them in the library, mark favorites and start native playback through a Media3 session service. DataStore persists the appearance choice. Catalog synchronization now uses version-aware merges and paged remote reads; previously cached records remain available when offline. Never replace a local file URI with a remote catalog URL during sync.


## Remote catalog and artwork

The catalog sync repository downloads artist, album, track and video metadata in bounded 500-record pages and writes each page to Room in a batch while preserving version-aware updates. Each entry carries the server version; a cached entry is replaced only when the incoming version is equal or newer. The sync is user-triggered from the catalog screen and leaves previously cached content available when offline.

Artwork is loaded from the public `music-artwork` Storage bucket. Coil is configured with a bounded memory cache and a persistent 128 MB disk cache in the app's private files directory. Catalog artwork is requested by visible LazyColumn items, shown with lightweight loading placeholders, and reused from cache when available. Data Saver suppresses catalog artwork requests to reduce network use.

## Playlists and sharing

Local playlists and their track membership stay in Room. Signed-in users can create collaborative cloud playlists, invite another account by Supabase user UUID as an editor, and add catalog tracks. RLS remains the authorization boundary. Local media files are never uploaded by playlist sync.
