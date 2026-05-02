# ytmusicdl — standalone

App Android completamente independiente para descargar música de YouTube Music.
**Sin Termux. Sin Python. Sin configuración.**

## Stack

| Componente | Librería |
|---|---|
| Búsqueda + extracción de streams | NewPipe Extractor v0.24.2 |
| Conversión de audio | ffmpeg-kit-fork (audio) |
| Tags ID3/MP4 | JAudioTagger 3.0.1 |
| Letras sincronizadas LRC | lrclib.net API (gratis, sin key) |
| UI | Jetpack Compose + Material 3 |
| Caché | Room (SQLite) |

## Compilar

```bash
./gradlew assembleDebug
# APK en: app/build/outputs/apk/debug/app-debug.apk
```

## Publicar con GitHub Actions

```bash
git tag v0.1.0-alpha
git push origin v0.1.0-alpha
# → Actions compila y publica en GitHub Releases
```

## Nota sobre ffmpeg-kit

El ffmpeg-kit original fue archivado en abril 2025.
Este proyecto usa el fork de la comunidad `pgahq/ffmpeg-kit-fork`.

## CLI rápido para yt-dlp

Puedes ejecutar `yt-dlp` desde este repo con un wrapper mínimo:

```bash
python3 tools/ytdlp_cli.py -- --help
python3 tools/ytdlp_cli.py "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
python3 tools/ytdlp_cli.py -- -F "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
```

> Nota: requiere `yt-dlp` instalado en tu `PATH`.
