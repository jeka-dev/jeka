---
name: mkdocs
description: MkDocs documentation project reference covering CLI commands, mkdocs.yml configuration, Material theme setup, and plugin integration. Bundled references include complete CLI parameters, all mkdocs.yml settings with valid values, Material theme customization options, and plugin configs for mkdocstrings, mermaid2, mkdocs-gen-files, mkdocs-literate-nav, and mkdocs-typer2. Use when initializing a MkDocs site, configuring mkdocs.yml, customizing the Material theme, integrating plugins, building static docs from Markdown, or generating API documentation from Python docstrings.
---

# MkDocs Skill

This skill provides reference material and guidance for working with MkDocs documentation sites — including `mkdocs.yml` configuration, the Material theme, plugins, and CLI usage.

**Loaded references (read them before acting):**

- `references/configuration_reference.md` — all `mkdocs.yml` keys with valid values
- `references/material_theme_reference.md` — Material theme features, palette, features flags, fonts
- `references/cli_reference.md` — `mkdocs build/serve/gh-deploy` options
- `references/plugins_reference.md` — mkdocstrings, mermaid2, gen-files, literate-nav, typer2
- `references/real_world_examples.md` — production-grade `mkdocs.yml` patterns

## Workflow Decision Tree

```
What do you need to do?
│
├── Initialize a new site ──────────────────→ [Init]
├── Fix / improve mkdocs.yml ───────────────→ [Configure]
├── Customize the Material theme ───────────→ [Theme]
├── Add or configure plugins ───────────────→ [Plugins]
├── Improve doc content / structure ────────→ [Content]
└── Deploy (GitHub Pages, CI) ──────────────→ [Deploy]
```

---

## [Init] Starting a new MkDocs site

```bash
pip install mkdocs-material
mkdocs new my-project
cd my-project
mkdocs serve   # preview at http://127.0.0.1:8000
```

Minimal `mkdocs.yml` to start with Material:

```yaml
site_name: My Project
theme:
  name: material
  font:
    text: Roboto
    code: Roboto Mono
  features:
    - navigation.instant
    - navigation.tracking
    - content.code.copy

repo_url: https://github.com/org/repo
edit_uri: edit/main/docs/
```

> **Font note:** Code font must be a valid Google Font. Common choices: `Roboto Mono`, `Fira Code`, `JetBrains Mono`, `Source Code Pro`. `Fire Code` is NOT a valid font name.

---

## [Configure] mkdocs.yml — Key Settings and Common Mistakes

### `edit_uri` (not `edit_url`)

The correct top-level key is `edit_uri`. It must NOT be placed under `theme:`.

```yaml
# ✅ Correct
repo_url: https://github.com/org/repo
edit_uri: edit/main/docs/

# ❌ Wrong — edit_url under theme: is ignored
theme:
  edit_url: https://github.com/org/repo/main/docs/
```

The `edit_uri` value is appended to `repo_url`, so for GitHub the path should include `edit/<branch>/`:
- `edit/main/docs/` → produces `https://github.com/org/repo/edit/main/docs/page.md`

### Navigation

Files present in `docs/` but absent from `nav:` produce warnings and are unreachable from the site.
Always keep `nav:` in sync with the actual files.

```yaml
nav:
  - Home: index.md
  - Installation: installation.md
  - Tutorials:
      - Basics: tutorials/basics.md
  - Migration Guide: migration-guide.md   # don't forget orphan files
```

Use `not_in_nav` for files intentionally excluded from nav (e.g. auto-generated API pages):

```yaml
not_in_nav: |
  api/**
  tags.md
```

### Strict mode

Enable in CI to catch broken links and missing pages:

```yaml
strict: true
```

---

## [Theme] Material Theme Configuration

### Palette (light/dark toggle)

```yaml
theme:
  name: material
  palette:
    - scheme: default
      primary: deep purple
      accent: purple
      toggle:
        icon: material/brightness-7
        name: Switch to dark mode
    - scheme: slate
      primary: deep purple
      accent: purple
      toggle:
        icon: material/brightness-4
        name: Switch to light mode
```

### Fonts

```yaml
theme:
  font:
    text: Inter          # body text — any Google Font
    code: Fira Code      # monospace — must be a valid Google Font name
```

To disable Google Fonts (privacy / offline):

```yaml
theme:
  font: false
```

### Navigation features (most useful)

```yaml
theme:
  features:
    - navigation.instant       # SPA-style instant loading
    - navigation.tracking      # anchor tracking in URL
    - navigation.tabs          # top-level sections as tabs
    - navigation.sections      # render sections in sidebar
    - navigation.expand        # expand all sections by default
    - navigation.top           # back-to-top button
    - navigation.footer        # prev/next links in footer
    - toc.follow               # sidebar TOC follows scroll
    - content.code.copy        # copy button on code blocks
    - content.action.edit      # "Edit this page" button (requires edit_uri)
    - search.suggest           # search autocomplete
    - search.highlight         # highlight search terms on page
```

### Logo and icons

```yaml
theme:
  logo: images/logo.svg
  favicon: images/favicon.png
  icon:
    repo: fontawesome/brands/github
```

---

## [Plugins] Common Plugin Configurations

### Mermaid diagrams (via superfences)

No extra plugin needed — use `pymdownx.superfences`:

```yaml
markdown_extensions:
  - pymdownx.superfences:
      custom_fences:
        - name: mermaid
          class: mermaid
          format: !!python/name:pymdownx.superfences.fence_code_format
extra_javascript:
  - https://unpkg.com/mermaid@10/dist/mermaid.min.js
```

### Search

```yaml
plugins:
  - search:
      separator: '[\s\-\.]+'
      lang: en
```

### Git revision dates

```yaml
plugins:
  - git-revision-date-localized:
      enable_creation_date: true
      type: timeago
```

### mkdocstrings (API docs from docstrings)

```yaml
plugins:
  - mkdocstrings:
      handlers:
        python:
          options:
            docstring_style: google
            show_source: true
```

---

## [Content] Writing Good MkDocs Pages

### Admonitions

```markdown
!!! note
    Use for supplementary information.

!!! tip
    Use for helpful hints.

!!! warning
    Use for potential pitfalls.

??? example "Collapsible example"
    Hidden by default, click to expand.
```

Requires `admonition` and `pymdownx.details` extensions.

### Code blocks with titles and line highlights

````markdown
```python title="my_module.py" hl_lines="2 3"
def hello():
    name = "world"
    print(f"Hello {name}")
```
````

Requires `pymdownx.highlight` and `pymdownx.superfences`.

### Tabs

````markdown
=== "Python"
    ```python
    print("hello")
    ```

=== "Java"
    ```java
    System.out.println("hello");
    ```
````

Requires `pymdownx.tabbed` with `alternate_style: true`.

---

## [Deploy] GitHub Actions deployment

```yaml
# .github/workflows/docs.yml
name: Deploy docs
on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0   # needed for git-revision-date plugin
      - uses: actions/setup-python@v5
        with:
          python-version: '3.x'
      - run: pip install mkdocs-material
      - run: mkdocs gh-deploy --force
```

---

## Checklist: Reviewing an Existing mkdocs.yml

Before reporting an mkdocs.yml as correct, check:

- [ ] `site_name` is set
- [ ] `theme.name` is `material` (or another installed theme)
- [ ] `theme.font.code` is a real Google Font name (not `Fire Code` — it's `Fira Code`)
- [ ] `edit_uri` is at **top level**, not under `theme:`, and includes `edit/<branch>/`
- [ ] Every file in `docs/` referenced by `nav:` actually exists
- [ ] Every `.md` file in `docs/` is reachable via `nav:` or listed under `not_in_nav:`
- [ ] `pymdownx.superfences` is not listed twice (it's a common dupe)
- [ ] Google Analytics property uses `G-XXXXXXXX` format (UA- is legacy)
- [ ] `strict: true` is set (or recommended) for CI builds
