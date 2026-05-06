"""
CLI principal de ytmusicdl v0.3
Fixes: cover se muestra solo 1 vez (post-descarga), year no muestra None,
       progreso con bytes reales, cover width aumentado a 28.
"""

import click
from pathlib import Path

from rich.progress import (
    Progress, SpinnerColumn, BarColumn,
    TextColumn, TaskProgressColumn, TimeRemainingColumn,
)

from ytmusicdl import cache as cache_mod
from ytmusicdl import search as search_mod
from ytmusicdl import downloader, lyrics as lyr_mod, cover as cover_mod
from ytmusicdl.utils import display as ui
from ytmusicdl.utils.config import get as cfg_get, set_value, ensure_config_exists
from ytmusicdl.utils.paths import expand


def _boot():
    ensure_config_exists()
    cache_mod.init()


@click.group()
@click.version_option("0.3.0", prog_name="ytmusicdl")
def cli():
    """🎵  ytmusicdl — Descarga música de YouTube Music"""
    _boot()


# ── Cover helper ──────────────────────────────────────────────────────────────

def _show_cover(track: dict, cover_data: bytes | None = None):
    """Muestra la carátula en terminal. cover_data ya descargada o la baja."""
    if not cfg_get("display", "show_cover", True):
        return
    data  = cover_data or (cover_mod.fetch_best(track["cover_url"]) if track.get("cover_url") else None)
    width = cfg_get("display", "cover_width", 28)

    # Filtrar year=None antes de pasar a render
    year = track.get("year")
    year_str = str(year) if year and str(year) != "None" else ""

    rendered = cover_mod.render(
        data, width=width,
        title=track.get("title", ""),
        artist=track.get("artist", ""),
        album=track.get("album", ""),
        year=year_str,
        duration=track.get("duration", ""),
    )
    ui.console.print()
    print(rendered)
    ui.console.print()


# ── Descarga con progreso ─────────────────────────────────────────────────────

def _run_downloads(tracks: list[dict], out_dir, fmt):
    total = len(tracks)

    with Progress(
        SpinnerColumn(),
        TextColumn("[cyan]{task.description}"),
        BarColumn(bar_width=26),
        TaskProgressColumn(),
        TextColumn("[dim]{task.fields[mb]}"),
        TimeRemainingColumn(),
        console=ui.console,
    ) as progress:

        overall   = progress.add_task(f"[bold]0/{total} canciones", total=total, mb="")
        byte_task = progress.add_task("", total=100, mb="", visible=False)

        for i, track in enumerate(tracks, 1):
            label = f"[white]{track.get('title','?')}[/] [dim]— {track.get('artist','')}[/]"
            progress.update(byte_task, description=label, completed=0, total=100,
                            visible=True, mb="")

            def _on_bytes(dl: int, tot: int, _bt=byte_task):
                if tot > 0:
                    progress.update(_bt,
                        completed=int(dl / tot * 100), total=100,
                        mb=f"{dl/1_048_576:.1f}/{tot/1_048_576:.1f} MB")

            result = downloader.download_track(track, out_dir, fmt, on_bytes=_on_bytes)

            progress.update(byte_task, visible=False, mb="")
            progress.update(overall, advance=1,
                            description=f"[bold]{i}/{total} canciones")

            _report(track, result)

            # Cover SOLO aquí, una vez, después de la descarga exitosa
            if result["status"] == "ok":
                _show_cover(track, result.get("cover_data"))


def _report(track: dict, result: dict) -> None:
    title = track.get("title", "?")
    if result["status"] == "ok":
        fname = Path(result["path"]).name if result.get("path") else ""
        ui.console.print(f"  [green]✓[/]  [white]{title}[/] [dim]→ {fname}[/]")
    elif result["status"] == "skipped":
        ui.console.print(f"  [dim]↩  {title} (ya descargado)[/]")
    else:
        ui.error(f"{title}: {result.get('error','?')}")


# ── song ──────────────────────────────────────────────────────────────────────

@cli.command()
@click.argument("query")
@click.option("-f", "--format", "fmt", default=None,
              type=click.Choice(["m4a", "mp3", "opus"]))
@click.option("-o", "--output",  default=None)
@click.option("-n", "--top",     default=1,
              help="Descargar los primeros N resultados sin preguntar")
@click.option("--no-lyrics",     is_flag=True)
@click.option("--no-lrc",        is_flag=True)
@click.option("--no-cover",      is_flag=True, help="No mostrar carátula en terminal")
def song(query, fmt, output, top, no_lyrics, no_lrc, no_cover):
    """Busca y descarga una canción de YouTube Music."""
    ui.header("ytmusicdl · song", query)

    with ui.make_simple_progress("Buscando...") as p:
        p.add_task("")
        results = search_mod.songs(query, limit=max(top, 8))

    if not results:
        ui.error("No se encontraron resultados.")
        raise click.exceptions.Exit(1)

    if top == 1:
        ui.print_search_results(results[:8])
        choice = click.prompt("\n  Elige un número (o Enter para el 1)",
                              default="1", show_default=False)
        try:
            idx = int(choice) - 1
            if not (0 <= idx < len(results)):
                raise ValueError
        except ValueError:
            ui.error("Selección inválida.")
            raise click.exceptions.Exit(1)
        to_download = [results[idx]]
    else:
        to_download = results[:top]

    # Deshabilitar cover temporalmente si --no-cover
    if no_cover:
        import ytmusicdl.utils.config as _cfg
        _cfg._loaded.setdefault("display", {})["show_cover"] = False

    _run_downloads(to_download, expand(output) if output else None, fmt)


# ── album ──────────────────────────────────────────────────────────────────────

@cli.command()
@click.argument("query")
@click.option("-f", "--format", "fmt", default=None,
              type=click.Choice(["m4a", "mp3", "opus"]))
@click.option("-o", "--output", default=None)
@click.option("--no-lyrics",   is_flag=True)
@click.option("--no-cover",    is_flag=True)
def album(query, fmt, output, no_lyrics, no_cover):
    """Descarga un álbum completo."""
    ui.header("ytmusicdl · album", query)

    with ui.make_simple_progress("Buscando álbum...") as p:
        p.add_task("")
        albums = search_mod.albums(query, limit=5)

    if not albums:
        ui.error("No se encontraron álbumes.")
        raise click.exceptions.Exit(1)

    for i, a in enumerate(albums, 1):
        ui.console.print(
            f"  [dim]{i}[/]  [cyan]{a['title']}[/] — [white]{a['artist']}[/]"
            f" [dim]({a.get('year','?')})[/]"
        )

    choice = click.prompt("\n  Elige un número", default="1", show_default=False)
    try:
        selected = albums[int(choice) - 1]
    except (ValueError, IndexError):
        ui.error("Selección inválida.")
        raise click.exceptions.Exit(1)

    with ui.make_simple_progress("Cargando pistas...") as p:
        p.add_task("")
        tracks = search_mod.album_tracks(selected["browse_id"])

    if not tracks:
        ui.error("No se pudieron cargar las pistas.")
        raise click.exceptions.Exit(1)

    if no_cover:
        import ytmusicdl.utils.config as _cfg
        _cfg._loaded.setdefault("display", {})["show_cover"] = False

    ui.success(f"{len(tracks)} pistas · {selected['title']} — {selected['artist']}")
    _run_downloads(tracks, expand(output) if output else None, fmt)


# ── playlist ───────────────────────────────────────────────────────────────────

@cli.command()
@click.argument("url")
@click.option("-f", "--format", "fmt", default=None,
              type=click.Choice(["m4a", "mp3", "opus"]))
@click.option("-o", "--output", default=None)
@click.option("--no-lyrics",   is_flag=True)
@click.option("--no-cover",    is_flag=True)
def playlist(url, fmt, output, no_lyrics, no_cover):
    """Descarga una playlist de YouTube Music."""
    ui.header("ytmusicdl · playlist", url)

    with ui.make_simple_progress("Cargando playlist...") as p:
        p.add_task("")
        tracks = search_mod.playlist_tracks(url)

    if not tracks:
        ui.error("No se pudieron cargar las pistas.")
        raise click.exceptions.Exit(1)

    if no_cover:
        import ytmusicdl.utils.config as _cfg
        _cfg._loaded.setdefault("display", {})["show_cover"] = False

    ui.success(f"{len(tracks)} pistas encontradas")
    _run_downloads(tracks, expand(output) if output else None, fmt)


# ── search ─────────────────────────────────────────────────────────────────────

@cli.command()
@click.argument("query")
@click.option("-n", "--top", default=8, show_default=True)
@click.option("--album", "is_album", is_flag=True)
def search(query, top, is_album):
    """Busca en YouTube Music sin descargar."""
    ui.header("ytmusicdl · search", query)
    if is_album:
        for i, a in enumerate(search_mod.albums(query, limit=top), 1):
            ui.console.print(
                f"  [dim]{i:2}[/]  [cyan]{a['title']}[/] — [white]{a['artist']}[/]"
                f"  [dim]({a.get('year','?')})[/]"
            )
    else:
        ui.print_search_results(search_mod.songs(query, limit=top))


# ── lyrics ─────────────────────────────────────────────────────────────────────

@cli.command()
@click.argument("query")
@click.option("--save-lrc",   is_flag=True)
@click.option("--audio-file", default=None)
@click.option("--embed",      is_flag=True)
def lyrics(query, save_lrc, audio_file, embed):
    """Busca y muestra letras de una canción."""
    ui.header("ytmusicdl · lyrics", query)
    parts  = query.rsplit(" ", 1)
    title  = parts[0]
    artist = parts[1] if len(parts) == 2 else ""

    with ui.make_simple_progress("Buscando letras...") as p:
        p.add_task("")
        result = lyr_mod.get(title, artist)

    if not result["lrc"] and not result["plain"]:
        ui.error("No se encontraron letras.")
        raise click.exceptions.Exit(1)

    src = result.get("source", "?")
    text = result["lrc"] or result["plain"]
    kind = "sincronizadas (LRC)" if result["lrc"] else "planas"
    ui.success(f"Letras {kind} · fuente: {src}")
    ui.console.print(text)

    if audio_file:
        if embed:
            ok = lyr_mod.embed_in_m4a(audio_file, result) or lyr_mod.embed_in_mp3(audio_file, result)
            ui.success("Letras embebidas.") if ok else ui.error("No se pudieron embeber.")
        if save_lrc and result["lrc"]:
            path = lyr_mod.save_lrc_file(audio_file, result["lrc"])
            ui.success(f"LRC guardado: {path}")


# ── cache ──────────────────────────────────────────────────────────────────────

@cli.group()
def cache():
    """Gestión del caché SQLite offline."""
    pass

@cache.command("stats")
def cache_stats():
    s = cache_mod.stats()
    ui.console.print(
        f"\n  [bold]Búsquedas:[/]  {s['searches']}\n"
        f"  [bold]Metadata:[/]   {s['metadata']}\n"
        f"  [bold]Letras:[/]     {s['lyrics']}\n"
        f"  [bold]Descargas:[/]  {s['downloads']}\n"
        f"  [bold]Tamaño DB:[/]  {s['db_size_kb']} KB\n"
    )

@cache.command("clear")
@click.option("--older-than", "days", default=0)
@click.confirmation_option(prompt="¿Seguro que quieres limpiar el caché?")
def cache_clear(days):
    deleted = cache_mod.clear(older_than_days=days)
    ui.success(f"{sum(deleted.values())} entradas eliminadas.")


# ── config ─────────────────────────────────────────────────────────────────────

@cli.group()
def config():
    """Gestión de configuración."""
    pass

@config.command("set")
@click.argument("key")
@click.argument("value")
def config_set(key, value):
    """Establece un valor. Ej: config set download.format mp3"""
    if "." not in key:
        ui.error("Formato: seccion.clave  (ej: download.format)")
        raise click.exceptions.Exit(1)
    section, k = key.split(".", 1)
    if value.lower() in ("true", "false"):
        value = value.lower() == "true"
    elif value.isdigit():
        value = int(value)
    set_value(section, k, value)
    ui.success(f"{key} = {value}")

@config.command("show")
def config_show():
    from ytmusicdl.utils.config import load
    for section, values in load().items():
        ui.console.print(f"\n[bold magenta][{section}][/]")
        for k, v in values.items():
            ui.console.print(f"  [cyan]{k}[/] = [white]{v}[/]")
    ui.console.print()


if __name__ == "__main__":
    cli()
