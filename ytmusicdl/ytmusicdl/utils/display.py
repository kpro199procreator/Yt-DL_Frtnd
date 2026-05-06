"""
Helpers de UI usando Rich — tablas, progreso, mensajes de estado.
"""

from rich.console import Console
from rich.table import Table
from rich.panel import Panel
from rich.progress import (
    Progress,
    SpinnerColumn,
    BarColumn,
    TextColumn,
    TimeRemainingColumn,
    DownloadColumn,
    TransferSpeedColumn,
)
from rich.theme import Theme

THEME = Theme({
    "info":    "cyan",
    "success": "bold green",
    "warning": "bold yellow",
    "error":   "bold red",
    "dim":     "grey50",
    "title":   "bold magenta",
})

console = Console(theme=THEME)


def info(msg: str) -> None:
    console.print(f"[info]ℹ[/]  {msg}")


def success(msg: str) -> None:
    console.print(f"[success]✓[/]  {msg}")


def warning(msg: str) -> None:
    console.print(f"[warning]⚠[/]  {msg}")


def error(msg: str) -> None:
    console.print(f"[error]✗[/]  {msg}")


def header(title: str, subtitle: str = "") -> None:
    content = f"[title]{title}[/]"
    if subtitle:
        content += f"\n[dim]{subtitle}[/]"
    console.print(Panel(content, border_style="magenta", padding=(0, 2)))


def print_search_results(results: list[dict]) -> None:
    if not results:
        warning("No se encontraron resultados.")
        return

    table = Table(show_header=True, header_style="bold magenta", border_style="grey30")
    table.add_column("#", style="dim", width=3, justify="right")
    table.add_column("Título", style="cyan", max_width=40)
    table.add_column("Artista", style="white", max_width=25)
    table.add_column("Álbum", style="grey70", max_width=30)
    table.add_column("Dur.", justify="right", style="dim", width=6)

    for i, r in enumerate(results, 1):
        table.add_row(
            str(i),
            r.get("title", "—"),
            r.get("artist", "—"),
            r.get("album", "—"),
            r.get("duration", "—"),
        )
    console.print(table)


def make_download_progress() -> Progress:
    return Progress(
        SpinnerColumn(),
        TextColumn("[progress.description]{task.description}", table_column=None),
        BarColumn(bar_width=30),
        DownloadColumn(),
        TransferSpeedColumn(),
        TimeRemainingColumn(),
        console=console,
        transient=False,
    )


def make_simple_progress(description: str = "Procesando...") -> Progress:
    return Progress(
        SpinnerColumn(),
        TextColumn(f"[cyan]{description}"),
        console=console,
        transient=True,
    )
