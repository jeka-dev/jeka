# Material for MkDocs Theme Reference

Complete configuration reference for Material for MkDocs theme. Material is a powerful theme that transforms MkDocs into a modern, feature-rich documentation platform with extensive customization options.

**Official Documentation**: <https://squidfunk.github.io/mkdocs-material/>

## Table of Contents

1. [Installation](#installation)
2. [Basic Theme Configuration](#basic-theme-configuration)
3. [Color Schemes and Palettes](#color-schemes-and-palettes)
4. [Typography and Fonts](#typography-and-fonts)
5. [Navigation Configuration](#navigation-configuration)
6. [Search Configuration](#search-configuration)
7. [Header Configuration](#header-configuration)
8. [Footer Configuration](#footer-configuration)
9. [Social Cards](#social-cards)
10. [Analytics Integration](#analytics-integration)
11. [Version Management](#version-management)
12. [Privacy and Cookie Consent](#privacy-and-cookie-consent)
13. [Git Repository Integration](#git-repository-integration)
14. [Language Configuration](#language-configuration)
15. [Built-in Plugins](#built-in-plugins)
16. [Page Metadata](#page-metadata)
17. [Icons and Logos](#icons-and-logos)
18. [Advanced Features](#advanced-features)

---

## Installation

Install Material for MkDocs via pip or uv:

```bash
# Using pip
pip install mkdocs-material

# Using uv (recommended)
uv add mkdocs-material
```

## Basic Theme Configuration

Enable Material theme in `mkdocs.yml`:

```yaml
theme:
  name: material

  # Optional: Set custom directory for overrides
  custom_dir: overrides
```

## Color Schemes and Palettes

### Basic Color Configuration

```yaml
theme:
  palette:
    # Color scheme: 'default' (light) or 'slate' (dark)
    scheme: default

    # Primary color (header, sidebar, links)
    # Options: red, pink, purple, deep purple, indigo, blue, light blue,
    # cyan, teal, green, light green, lime, yellow, amber, orange,
    # deep orange, brown, grey, blue grey, black, white
    primary: indigo

    # Accent color (interactive elements)
    # Options: red, pink, purple, deep purple, indigo, blue, light blue,
    # cyan, teal, green, light green, lime, yellow, amber, orange, deep orange
    accent: indigo
```

### Multiple Color Palettes with Toggle

```yaml
theme:
  palette:
    # Light mode
    - scheme: default
      primary: indigo
      accent: indigo
      toggle:
        icon: material/brightness-7
        name: Switch to dark mode

    # Dark mode
    - scheme: slate
      primary: indigo
      accent: indigo
      toggle:
        icon: material/brightness-4
        name: Switch to light mode
```

### System Preference Integration

```yaml
theme:
  palette:
    # Light mode
    - media: "(prefers-color-scheme: light)"
      scheme: default
      toggle:
        icon: material/brightness-7
        name: Switch to dark mode

    # Dark mode
    - media: "(prefers-color-scheme: dark)"
      scheme: slate
      toggle:
        icon: material/brightness-4
        name: Switch to light mode
```

### Custom Colors

Define custom colors using CSS variables:

```yaml
theme:
  palette:
    primary: custom
    accent: custom

extra_css:
  - stylesheets/extra.css
```

`docs/stylesheets/extra.css`:

```css
:root > * {
  /* Primary color variations */
  --md-primary-fg-color: #ee0f0f;
  --md-primary-fg-color--light: #ecb7b7;
  --md-primary-fg-color--dark: #90030c;

  /* Accent color variations */
  --md-accent-fg-color: #448aff;
  --md-accent-fg-color--light: #69b7ff;
  --md-accent-fg-color--dark: #2979ff;
}
```

### Custom Color Schemes

```css
[data-md-color-scheme="custom"] {
  --md-primary-fg-color: #4051b5;
  --md-primary-fg-color--light: #5d6cc0;
  --md-primary-fg-color--dark: #303f9f;
}
```

```yaml
theme:
  palette:
    scheme: custom
```

## Typography and Fonts

### Google Fonts

```yaml
theme:
  font:
    # Body text font
    text: Roboto

    # Code block font
    code: Roboto Mono
```

### Disable External Fonts

```yaml
theme:
  font: false # Use system fonts only
```

### Custom Fonts

```yaml
extra_css:
  - stylesheets/fonts.css
```

`docs/stylesheets/fonts.css`:

```css
@font-face {
  font-family: "Custom Font";
  src: url("../fonts/custom-font.woff2") format("woff2");
}

:root {
  --md-text-font: "Custom Font";
  --md-code-font: "Custom Mono";
}
```

## Navigation Configuration

### Core Navigation Features

```yaml
theme:
  features:
    # Instant loading (SPA behavior)
    - navigation.instant
    - navigation.instant.prefetch # Prefetch on hover (Insiders)
    - navigation.instant.progress # Progress indicator

    # Navigation tabs
    - navigation.tabs # Top-level tabs
    - navigation.tabs.sticky # Sticky tabs

    # Navigation sections
    - navigation.sections # Group sections in sidebar

    # Navigation expansion
    - navigation.expand # Auto-expand subsections
    # OR
    - navigation.prune # Only show visible items (saves 33%+ size)

    # Section index pages
    - navigation.indexes # Attach index.md to sections

    # Navigation path (breadcrumbs)
    - navigation.path # Show breadcrumbs (Insiders)

    # Anchor tracking
    - navigation.tracking # Update URL with active anchor

    # Back to top button
    - navigation.top # Show back-to-top button

    # Footer navigation
    - navigation.footer # Previous/next page links
```

### Table of Contents

```yaml
theme:
  features:
    # TOC configuration
    - toc.follow # Auto-scroll TOC to active anchor
    - toc.integrate # Render TOC in sidebar
```

### Navigation Structure

```yaml
nav:
  - Home: index.md
  - Getting Started:
      - Installation: getting-started/installation.md
      - Quick Start: getting-started/quick-start.md
  - User Guide:
      - user-guide/index.md # Section index with navigation.indexes
      - Configuration: user-guide/configuration.md
      - Advanced: user-guide/advanced.md
  - API Reference:
      - api/index.md
      - Classes: api/classes.md
      - Functions: api/functions.md
```

### Hide Navigation Elements

Use front matter to hide elements per page:

```markdown
---
hide:
  - navigation # Hide left navigation
  - toc # Hide table of contents
  - path # Hide breadcrumbs
  - footer # Hide footer
---

# Page Content
```

## Search Configuration

### Basic Search

```yaml
plugins:
  - search
```

### Search Features

```yaml
theme:
  features:
    - search.suggest # Search suggestions
    - search.highlight # Highlight search results
    - search.share # Deep linking to searches
```

### Search Configuration Options

```yaml
plugins:
  - search:
      lang: en
      separator: '[\s\-\.]'
      prebuild_index: true
```

### Search Boosting

Boost specific pages in search results:

```markdown
---
search:
  boost: 2
---

# Important Page
```

### Search Exclusion

Exclude entire pages:

```markdown
---
search:
  exclude: true
---

# Hidden Page
```

Exclude sections (requires Attribute Lists extension):

```markdown
## Section Name { data-search-exclude }
```

## Header Configuration

### Header Features

```yaml
theme:
  features:
    - header.autohide # Hide header on scroll
```

### Announcement Bar

```yaml
theme:
  features:
    - announce.dismiss # Allow dismissing announcements
```

Custom announcement in `overrides/main.html`:

```html
{% extends "base.html" %} {% block announce %}
<p>
  Important: Version 2.0 released!
  <a href="/changelog/">See what's new</a>
</p>
{% endblock %}
```

## Footer Configuration

### Copyright

```yaml
copyright: >
  Copyright &copy; 2024 Your Name – <a href="#__consent">Change cookie settings</a>
```

### Social Links

```yaml
extra:
  social:
    - icon: fontawesome/brands/github
      link: https://github.com/yourusername
      name: GitHub
    - icon: fontawesome/brands/twitter
      link: https://twitter.com/yourusername
      name: Twitter
    - icon: fontawesome/brands/linkedin
      link: https://linkedin.com/in/yourusername
      name: LinkedIn
```

## Social Cards

### Basic Setup

```yaml
plugins:
  - social

site_url: https://example.com # Required
```

### Custom Fonts for Social Cards

```yaml
plugins:
  - social:
      cards_layout_options:
        font_family: Roboto # or Noto Sans SC for CJK
```

### Per-Page Customization

```markdown
---
social:
  cards_layout: custom
  cards_layout_options:
    background_color: blue
---

# Page Title
```

### Disable Social Cards

```markdown
---
social:
  cards: false
---

# Page Without Social Card
```

### Advanced Layout (Insiders)

Create `layouts/custom.yml`:

```yaml
size: { width: 1200, height: 630 }
layers:
  - background:
      color: "#4051b5"
  - typography:
      content: "{{ page.title }}"
      align: start
      color: white
      font:
        family: Roboto
        style: Bold
  - icon:
      value: material/book
      color: white
```

Reference in `mkdocs.yml`:

```yaml
plugins:
  - social:
      cards_layout_dir: layouts
      cards_layout: custom
```

## Analytics Integration

### Google Analytics 4

```yaml
extra:
  analytics:
    provider: google
    property: G-XXXXXXXXXX
```

### Feedback Widget

```yaml
extra:
  analytics:
    feedback:
      title: Was this page helpful?
      ratings:
        - icon: material/emoticon-happy-outline
          name: This page was helpful
          data: 1
          note: >-
            Thanks for your feedback!


        - icon: material/emoticon-sad-outline
          name: This page could be improved
          data: 0
          note: >-
            Thanks for your feedback! Help us improve this page by using our <a href="..." target="_blank">feedback form</a>.
```

### Custom Analytics Provider

Create `overrides/partials/integrations/analytics/custom.html`:

```html
<!-- Your analytics code -->
```

Configure:

```yaml
extra:
  analytics:
    provider: custom
    property: your-tracking-id
```

## Version Management

### Mike Integration

```yaml
extra:
  version:
    provider: mike

    # Default version
    default: stable

    # Or multiple defaults
    default:
      - stable
      - latest

    # Show aliases (v9.5.23+)
    alias: true
```

### Version Warning

Create `overrides/main.html`:

```html
{% extends "base.html" %} {% block outdated %} You're viewing an older version.
<a href="{{ '../' ~ base_url }}">
  <strong>View latest documentation</strong>
</a>
{% endblock %}
```

### Deploy Versions

```bash
# Deploy a new version
mike deploy --push --update-aliases 0.1 latest

# Set default version
mike set-default --push latest
```

## Privacy and Cookie Consent

### Cookie Consent

```yaml
extra:
  consent:
    title: Cookie consent
    description: >-
      We use cookies to recognize your repeated visits and preferences, as well as to measure the effectiveness of our documentation.


    actions:
      - accept
      - manage # or 'reject'
    cookies:
      analytics:
        name: Google Analytics
        checked: true
      github:
        name: GitHub
        checked: true
      custom:
        name: Custom Cookie
        checked: false
```

### Privacy Plugin

Automatically download and self-host external assets:

```yaml
plugins:
  - privacy
```

### Access Cookie Consent

```javascript
var consent = __md_get("__consent");
if (consent && consent.analytics) {
  // User accepted analytics
}
```

## Git Repository Integration

### Repository Link

```yaml
# Repository URL
repo_url: https://github.com/username/repository

# Repository name (optional)
repo_name: username/repository

# Edit URI for edit button
edit_uri: edit/main/docs/
```

### Repository Icon

```yaml
theme:
  icon:
    repo: fontawesome/brands/github # or gitlab, bitbucket, git-alt
```

### Code Actions

```yaml
theme:
  features:
    - content.action.edit # Edit page button
    - content.action.view # View source button

  icon:
    edit: material/pencil
    view: material/eye
```

### Document Dates

With `git-revision-date-localized` plugin:

```yaml
plugins:
  - git-revision-date-localized:
      enable_creation_date: true
      type: date
      fallback_to_build_date: true
      enabled: !ENV [CI, false]
```

### Document Contributors

With `git-committers` plugin:

```yaml
plugins:
  - git-committers:
      repository: username/repository
      branch: main
      enabled: !ENV [CI, false]
```

## Language Configuration

### Set Site Language

```yaml
theme:
  language: en # ISO 639-1 code
  direction: ltr # or rtl for right-to-left
```

### Multiple Languages

```yaml
extra:
  alternate:
    - name: English
      link: /en/
      lang: en
    - name: Español
      link: /es/
      lang: es
    - name: Français
      link: /fr/
      lang: fr
    - name: 中文
      link: /zh/
      lang: zh
```

### Custom Translations

Create `overrides/partials/languages/custom.html` for custom translations:

```yaml
theme:
  language: custom
```

## Built-in Plugins

Material for MkDocs includes 12 built-in plugins:

### Management Plugins

```yaml
plugins:
  # Group plugins for conditional enabling
  - group:
      enabled: !ENV [ENABLE_INSIDERS, false]
      plugins:
        - optimize
        - privacy

  # Metadata management
  - meta

  # Multi-project documentation
  - projects

  # Bug report assistance
  - info
```

### Optimization Plugins

```yaml
plugins:
  # Social media cards
  - social:
      cards: true
      cards_layout: default

  # Media optimization
  - optimize:
      enabled: !ENV [CI, false]

  # Asset self-hosting
  - privacy:
      enabled: !ENV [CI, false]

  # Offline documentation
  - offline:
      enabled: !ENV [OFFLINE, false]
```

### Content Plugins

```yaml
plugins:
  # Blog functionality
  - blog:
      blog_dir: blog
      post_dir: posts
      post_date_format: full
      post_url_date_format: yyyy/MM/dd
      archive: true
      categories: true

  # Search functionality
  - search:
      lang: en

  # Tag system
  - tags:
      tags_file: tags.md

  # Typography preservation
  - typeset
```

## Page Metadata

Front matter options for pages:

```markdown
---
# Page title
title: Custom Page Title

# Page description for SEO
description: This page describes...

# Page icon
icon: material/book

# Page status
status: new # or deprecated, or custom status

# Page subtitle
subtitle: Additional context

# Custom template
template: custom.html

# Hide elements
hide:
  - navigation
  - toc
  - footer
  - path

# Search configuration
search:
  exclude: false
  boost: 2

# Social cards
social:
  cards: true
  cards_layout: default

# Tags
tags:
  - introduction
  - tutorial
  - guide
---

# Page Content
```

## Icons and Logos

### Site Logo

```yaml
theme:
  logo: assets/logo.png # or use icon

  # Or use bundled icon
  icon:
    logo: material/library
```

### Favicon

```yaml
theme:
  favicon: assets/favicon.png
```

### Icon Sets

Material for MkDocs bundles 8,000+ icons from:

- Material Design
- FontAwesome
- Octicons
- Simple Icons

Use in markdown:

```markdown
:material-github: GitHub :fontawesome-brands-twitter: Twitter :octicons-repo-24: Repository
```

## Advanced Features

### Custom Directory

```yaml
theme:
  name: material
  custom_dir: overrides
```

Structure:

<eg>
overrides/
├── .icons/                # Custom icons
├── assets/               # Custom assets
│   ├── images/
│   ├── javascripts/
│   └── stylesheets/
├── partials/            # Template overrides
│   ├── header.html
│   ├── footer.html
│   └── ...
└── main.html           # Main template override
</eg>

### Extra CSS and JavaScript

```yaml
extra_css:
  - stylesheets/extra.css
  - stylesheets/custom.css

extra_javascript:
  - javascripts/extra.js
  - javascripts/custom.js
```

### Content Tabs

Enable content tabs:

```yaml
markdown_extensions:
  - pymdownx.tabbed:
      alternate_style: true
```

Usage:

```markdown
=== "Tab 1"

    Content for tab 1

=== "Tab 2"

    Content for tab 2
```

### Admonitions

Enable admonitions:

```yaml
markdown_extensions:
  - admonition
  - pymdownx.details
  - pymdownx.superfences
```

Usage:

```markdown
!!! note "Optional Title"

    This is a note admonition.

!!! warning

    This is a warning without custom title.

??? info "Collapsible"

    This is collapsible content.
```

### Code Highlighting

```yaml
markdown_extensions:
  - pymdownx.highlight:
      anchor_linenums: true
      line_spans: __span
      pygments_lang_class: true
  - pymdownx.inlinehilite
  - pymdownx.snippets
  - pymdownx.superfences

theme:
  features:
    - content.code.copy # Copy button
    - content.code.annotate # Code annotations
```

### Data Tables

```yaml
markdown_extensions:
  - tables
```

Enable sortable tables:

```yaml
extra_javascript:
  - https://unpkg.com/tablesort@latest/dist/tablesort.min.js
  - javascripts/tablesort.js
```

### Diagrams

Enable Mermaid diagrams:

```yaml
markdown_extensions:
  - pymdownx.superfences:
      custom_fences:
        - name: mermaid
          class: mermaid
          format: !!python/name:pymdownx.superfences.fence_code_format
```

### Mathematical Notation

```yaml
markdown_extensions:
  - pymdownx.arithmatex:
      generic: true

extra_javascript:
  - javascripts/mathjax.js
  - https://polyfill.io/v3/polyfill.min.js?features=es6
  - https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-mml-chtml.js
```

## Best Practices

1. **Performance**

   - Use `navigation.instant` for SPA-like experience
   - Enable `navigation.prune` to reduce site size
   - Use the `optimize` plugin for media compression
   - Implement lazy loading for images

2. **SEO**

   - Always set `site_url` with trailing slash
   - Configure meta descriptions for all pages
   - Use the `social` plugin for social media cards
   - Implement proper heading hierarchy

3. **Accessibility**

   - Provide alt text for all images
   - Use semantic HTML in custom templates
   - Ensure sufficient color contrast
   - Test keyboard navigation

4. **Privacy**

   - Use the `privacy` plugin for GDPR compliance
   - Implement cookie consent for analytics
   - Self-host fonts and assets when required
   - Document data collection practices

5. **Version Management**
   - Use mike for documentation versioning
   - Maintain clear version aliases
   - Implement version warnings for outdated docs
   - Archive old versions appropriately

## Resources

- **Official Documentation**: <https://squidfunk.github.io/mkdocs-material/>
- **GitHub Repository**: <https://github.com/squidfunk/mkdocs-material>
- **Insiders Edition**: <https://squidfunk.github.io/mkdocs-material/insiders/>
- **Community Forum**: <https://github.com/squidfunk/mkdocs-material/discussions>
- **Issue Tracker**: <https://github.com/squidfunk/mkdocs-material/issues>

---

_This reference covers Material for MkDocs version 9.x. Features may vary based on version and Insiders subscription._
