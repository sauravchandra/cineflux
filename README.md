# CineFlux

A native Android TV app for discovering and watching movies. Built with Kotlin, Compose for TV, and libtorrent4j.

## Features

- **Movie Discovery** — Browse curated categories from Letterboxd (65+ genres, decades, countries, popular user lists)
- **Search** — Find any movie via TMDB
- **Torrent Sources** — Aggregates from 1337x, The Pirate Bay, and YTS with smart quality dedup
- **Built-in Downloader** — libtorrent4j with protocol encryption, aggressive tuning, background downloads
- **Transmission Integration** — Send magnet links to Transmission for downloading
- **Auto Subtitles** — SubDL-powered English subtitle auto-download
- **VLC Playback** — Launches VLC with video + subtitles, ExoPlayer as fallback
- **TV Optimized** — D-pad navigation, shimmer loading, auto-rotating hero carousel, infinite scroll

## Setup

1. Copy `gradle.properties.example` to `gradle.properties`
2. Add your API keys:
   - **TMDB** — Free at [themoviedb.org/settings/api](https://www.themoviedb.org/settings/api)
   - **SubDL** — Free at [subdl.com/panel](https://subdl.com/panel)
3. Build: `./gradlew assembleDebug`
4. Install on your TV: `adb install app/build/outputs/apk/debug/app-debug.apk`

## Tech Stack

- Kotlin + Jetpack Compose for TV
- libtorrent4j (torrent engine)
- Retrofit + OkHttp with DNS-over-HTTPS
- Room (download history)
- Hilt (dependency injection)
- Media3 ExoPlayer (fallback player)
- Coil (image loading)

## Disclaimer

This application is provided for educational and personal use only. The developers do not host, distribute, or promote any copyrighted content. Users are solely responsible for ensuring their use of this application complies with applicable laws in their jurisdiction. The developers assume no liability for how the application is used.

## License

MIT License — see [LICENSE](LICENSE)
