# MinhutBillboards

A Minecraft plugin for displaying images on large map billboards.

---

## Screenshots

| Description | Preview |
|-------------|---------|
| Billboard Example | ![Screenshot](screenshots/example.png) |

---

## Features

- Display images from URLs or local files
- Configurable billboard dimensions
- Multiple billboard presets
- Green glow effect when looking at billboards
- Floyd-Steinberg dithering for better colors
- Undo support

---

## Installation

1. Download the latest release
2. Place `MinhutBillboards.jar` in your `plugins/` folder
3. Restart your server
4. Add images to `plugins/MinhutBillboards/images/`

---

## Commands

| Command | Description |
|---------|-------------|
| `/billboard spawn <name>` | Spawn billboard at the block you're looking at |
| `/billboard undo` | Remove your last spawned billboard |
| `/billboard remove <name>` | Remove a specific billboard |
| `/billboard list` | List all configured billboards |
| `/billboard reload` | Reload the configuration |

### Billboard Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `width` | int | 6 | Width in blocks |
| `height` | int | 5 | Height in blocks |
| `material` | string | BLACK_CONCRETE | Background block |
| `image` | string | - | Filename or URL |

### Effect Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `glow_when_looking` | boolean | true | Enable glow effect |
| `glow_color` | string | GREEN | Glow color |
| `look_range` | int | 32 | Detection range |
| `dithering` | string | floyd_steinberg | Dithering mode |

### Dithering Modes

| Mode | Description |
|------|-------------|
| `floyd_steinberg` | Best quality |
| `ordered` | Faster, patterned |
| `none` | No dithering |

---

## Images

### Recommended Sizes

| Billboard | Image Size |
|-----------|------------|
| 6x5 | 768x640 px |
| 8x6 | 1024x768 px |
| 10x8 | 1280x1024 px |


