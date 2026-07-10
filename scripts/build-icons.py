"""Build Android adaptive + legacy launcher icons from the chosen concept art."""
from pathlib import Path

from PIL import Image

PROJ = Path(r"C:\Users\Charles\AI Projects\rotation-control")
ASSETS = PROJ / "assets" / "icon"
RES = PROJ / "app" / "src" / "main" / "res"
# User-chosen ship icon
SRC = ASSETS / "concept-dial.jpg"
BG = (26, 29, 36, 255)  # #1A1D24


def square_1024(img: Image.Image) -> Image.Image:
    img = img.convert("RGBA")
    w, h = img.size
    side = min(w, h)
    left = (w - side) // 2
    top = (h - side) // 2
    return img.crop((left, top, left + side, top + side)).resize(
        (1024, 1024), Image.Resampling.LANCZOS
    )


def extract_foreground(src: Image.Image) -> Image.Image:
    """Knock out dark plate; keep dial, phone, blue, green."""
    fg = Image.new("RGBA", src.size, (0, 0, 0, 0))
    sp, fp = src.load(), fg.load()
    w, h = src.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = sp[x, y]
            if r + g + b > 95 or g > 140 or b > 150:
                fp[x, y] = (r, g, b, 255)
            elif max(r, g, b) - min(r, g, b) > 25 and max(r, g, b) > 60:
                fp[x, y] = (r, g, b, 255)
    return fg


def fit_safe_zone(fg: Image.Image, canvas_size: int = 1024, fill: float = 0.72) -> Image.Image:
    canvas = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    bbox = fg.getbbox()
    if not bbox:
        return canvas
    cropped = fg.crop(bbox)
    max_side = int(canvas_size * fill)
    cw, ch = cropped.size
    scale = min(max_side / cw, max_side / ch)
    nw, nh = max(1, int(cw * scale)), max(1, int(ch * scale))
    cropped = cropped.resize((nw, nh), Image.Resampling.LANCZOS)
    canvas.paste(cropped, ((canvas_size - nw) // 2, (canvas_size - nh) // 2), cropped)
    return canvas


def main() -> None:
    if not SRC.exists():
        raise SystemExit(f"Missing source icon: {SRC}")

    ASSETS.mkdir(parents=True, exist_ok=True)
    full = square_1024(Image.open(SRC))
    full.save(ASSETS / "icon-full-1024.png")

    fg = fit_safe_zone(extract_foreground(full))
    if not fg.getbbox():
        fg = full
    fg.save(ASSETS / "ic_launcher_foreground_1024.png")

    Image.new("RGBA", (1024, 1024), BG).save(ASSETS / "ic_launcher_background_1024.png")

    preview = Image.new("RGBA", (1024, 1024), BG)
    preview.paste(fg, (0, 0), fg)
    preview.save(ASSETS / "icon-preview.png")
    full.save(ASSETS / "ic_launcher_legacy_1024.png")

    densities = {
        "mdpi": 1.0,
        "hdpi": 1.5,
        "xhdpi": 2.0,
        "xxhdpi": 3.0,
        "xxxhdpi": 4.0,
    }
    for name, scale in densities.items():
        folder = RES / f"mipmap-{name}"
        folder.mkdir(parents=True, exist_ok=True)
        adaptive = int(108 * scale)
        launcher = int(48 * scale)
        fg.resize((adaptive, adaptive), Image.Resampling.LANCZOS).save(
            folder / "ic_launcher_foreground.png"
        )
        Image.new("RGBA", (adaptive, adaptive), BG).save(folder / "ic_launcher_background.png")
        full.resize((launcher, launcher), Image.Resampling.LANCZOS).save(folder / "ic_launcher.png")
        full.resize((launcher, launcher), Image.Resampling.LANCZOS).save(
            folder / "ic_launcher_round.png"
        )
        print(f"mipmap-{name}: adaptive={adaptive} launcher={launcher}")

    print("Ship icon source:", SRC.name)
    print("Done →", ASSETS)


if __name__ == "__main__":
    main()
