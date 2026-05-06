# 🎵 ytmusicdl

Descarga música de **YouTube Music** desde la terminal. Funciona en Termux (Android), Linux y macOS.

Inspirado en spotdl, pero usando exclusivamente fuentes **gratuitas y abiertas**.

---

## Instalación en Termux

```bash
git clone https://github.com/tu-usuario/ytmusic-dl
cd ytmusic-dl
bash install.sh
```

## Instalación en Linux / macOS

```bash
git clone https://github.com/tu-usuario/ytmusic-dl
cd ytmusic-dl
pip install -e ".[all]"
```

---

## Uso

```bash
# Buscar y descargar una canción
ytmusicdl song "Never Gonna Give You Up"

# Descargar los primeros 3 resultados directamente
ytmusicdl song "Bohemian Rhapsody" --top 3

# Álbum completo
ytmusicdl album "OK Computer Radiohead"

# Playlist (URL de YT Music)
ytmusicdl playlist "https://music.youtube.com/playlist?list=..."

# Solo buscar, sin descargar
ytmusicdl search "Arctic Monkeys" --top 10

# Buscar letras
ytmusicdl lyrics "Creep Radiohead"

# Caché
ytmusicdl cache stats
ytmusicdl cache clear --older-than 30

# Configuración
ytmusicdl config show
ytmusicdl config set download.format mp3
ytmusicdl config set download.output_dir ~/storage/music
```

---

## Formatos de audio

| Formato | Calidad | Compatibilidad Android |
|---------|---------|----------------------|
| `m4a`   | ★★★★★  | Nativa ✓             |
| `opus`  | ★★★★★  | VLC, Poweramp ✓      |
| `mp3`   | ★★★☆☆  | Universal ✓          |

---

## Configuración

El archivo de configuración se crea automáticamente en `~/.config/ytmusicdl/config.toml`.

```toml
[download]
format = "m4a"
output_dir = "~/storage/music"
skip_downloaded = true

[metadata]
embed_lyrics = true
embed_cover = true
save_lrc = true

[lyrics]
synced = true
providers = ["lrclib", "musixmatch", "genius"]
```

---

## Stack

- **yt-dlp** — Motor de descarga
- **ytmusicapi** — API no oficial de YouTube Music
- **mutagen** — Escritura de tags de audio
- **syncedlyrics** — Letras LRC sincronizadas
- **rich** — UI de terminal
- **click** — CLI framework
- SQLite (stdlib) — Caché offline

> Sin Spotify. Sin APIs de pago.
