# MkDocs Configuration Reference

Complete mkdocs.yml configuration settings documentation.

Official Documentation: <https://www.mkdocs.org/user-guide/configuration/>

## Configuration File

MkDocs uses a YAML configuration file named `mkdocs.yml` (or `mkdocs.yaml`) in the project root.

## Project Information Settings

### site_name

- **Type:** `string`
- **Required:** Yes
- **Description:** The name of your documentation site
- **Example:**
  ```yaml
  site_name: My Documentation
  ```

### site_url

- **Type:** `string` (URL)
- **Default:** `null`
- **Description:** The canonical URL of the site. Adds canonical link and helps with sitemap generation
- **Example:**
  ```yaml
  site_url: https://example.com/docs/
  ```

### site_description

- **Type:** `string`
- **Default:** `null`
- **Description:** A description of the documentation site (added as meta tag)
- **Example:**
  ```yaml
  site_description: Comprehensive API documentation for our project
  ```

### site_author

- **Type:** `string`
- **Default:** `null`
- **Description:** The author of the documentation (added as meta tag)
- **Example:**
  ```yaml
  site_author: John Doe
  ```

### repo_url

- **Type:** `string` (URL)
- **Default:** `null`
- **Description:** Repository URL. Adds a link to your repository (GitHub, GitLab, Bitbucket, etc.)
- **Example:**
  ```yaml
  repo_url: https://github.com/username/repository
  ```

### repo_name

- **Type:** `string`
- **Default:** Automatically detected from `repo_url`
- **Description:** Repository link text
- **Example:**
  ```yaml
  repo_name: username/repository
  ```

### edit_uri

- **Type:** `string`
- **Default:** Automatically computed from `repo_url`
- **Description:** Path from `repo_url` to the docs directory for edit links
- **Examples:**

  ```yaml
  # For GitHub (default)
  edit_uri: edit/main/docs/

  # For Bitbucket
  edit_uri: src/default/docs/

  # For GitLab
  edit_uri: -/edit/main/docs/

  # Disable edit links
  edit_uri: ""
  ```

### edit_uri_template

- **Type:** `string`
- **Default:** `null`
- **Description:** Template for edit links using Python string formatting
- **Example:**
  ```yaml
  edit_uri_template: "https://github.com/org/repo/edit/main/docs/{path}"
  ```

### copyright

- **Type:** `string`
- **Default:** `null`
- **Description:** Copyright notice to display in the footer
- **Example:**
  ```yaml
  copyright: Copyright &copy; 2024 My Company
  ```

## Repository Settings

### remote_branch

- **Type:** `string`
- **Default:** `gh-pages`
- **Description:** The remote branch to commit to for `mkdocs gh-deploy`
- **Example:**
  ```yaml
  remote_branch: gh-pages
  ```

### remote_name

- **Type:** `string`
- **Default:** `origin`
- **Description:** The remote name to push to for `mkdocs gh-deploy`
- **Example:**
  ```yaml
  remote_name: origin
  ```

## Documentation Layout

### nav

- **Type:** `list`
- **Default:** Automatically generated from file structure
- **Description:** Defines the navigation structure
- **Example:**
  ```yaml
  nav:
    - Home: index.md
    - User Guide:
        - Installation: user-guide/installation.md
        - Configuration: user-guide/configuration.md
        - Advanced:
            - Plugins: user-guide/advanced/plugins.md
            - Themes: user-guide/advanced/themes.md
    - API Reference: api/
    - About:
        - License: about/license.md
        - Release Notes: about/changelog.md
  ```

### exclude_docs

- **Type:** `string` or `list of strings` (glob patterns)
- **Default:** See below
- **Description:** Patterns for files to exclude from the built site
- **Default patterns:**
  - Any file named `__pycache__`
  - Any file with `.pyc` extension
  - Hidden files (starting with `.`)
- **Example:**
  ```yaml
  exclude_docs: |
    *.py
    /drafts/**
    api-draft.md
  ```

### draft_docs

- **Type:** `string` or `list of strings` (glob patterns)
- **Default:** `null`
- **Description:** Files to treat as drafts (shown in `serve`, excluded from `build`)
- **Example:**
  ```yaml
  draft_docs: |
    drafts/**
    *.draft.md
  ```

### not_in_nav

- **Type:** `string` or `list of strings` (glob patterns)
- **Default:** `null`
- **Description:** Pages to include in build but not warn about if missing from nav
- **Example:**
  ```yaml
  not_in_nav: |
    api/**
    /tags.md
  ```

## Build Directories

### docs_dir

- **Type:** `string` (path)
- **Default:** `docs`
- **Description:** Directory containing documentation source files
- **Example:**
  ```yaml
  docs_dir: documentation
  ```

### site_dir

- **Type:** `string` (path)
- **Default:** `site`
- **Description:** Directory where the output HTML is built
- **Example:**
  ```yaml
  site_dir: public
  ```

## Theme Configuration

### theme

- **Type:** `string` or `mapping`
- **Default:** `mkdocs`
- **Description:** The theme to use for the documentation
- **Simple Example:**
  ```yaml
  theme: readthedocs
  ```
- **Advanced Example:**
  ```yaml
  theme:
    name: material
    palette:
      - scheme: default
        toggle:
          icon: material/brightness-7
          name: Switch to dark mode
      - scheme: slate
        toggle:
          icon: material/brightness-4
          name: Switch to light mode
    features:
      - navigation.tabs
      - navigation.sections
      - toc.integrate
      - search.suggest
      - search.highlight
    language: en
    font:
      text: Roboto
      code: Roboto Mono
  ```

### Theme Sub-settings

#### theme.name

- **Type:** `string`
- **Required:** When using mapping format
- **Description:** Name of installed theme
- **Built-in themes:** `mkdocs`, `readthedocs`

#### theme.custom_dir

- **Type:** `string` (path)
- **Default:** `null`
- **Description:** Directory with custom theme overrides
- **Example:**
  ```yaml
  theme:
    name: material
    custom_dir: overrides
  ```

#### theme.static_templates

- **Type:** `list`
- **Default:** `[]`
- **Description:** Static template pages to render
- **Example:**
  ```yaml
  theme:
    name: mkdocs
    static_templates:
      - sitemap.xml
      - 404.html
  ```

#### theme.locale

- **Type:** `string` (ISO 639-1 language code)
- **Default:** `en`
- **Description:** Language locale for the theme
- **Example:**
  ```yaml
  theme:
    name: material
    locale: es
  ```

## CSS and JavaScript

### extra_css

- **Type:** `list`
- **Default:** `[]`
- **Description:** Additional CSS files to include
- **Example:**
  ```yaml
  extra_css:
    - css/custom.css
    - css/print.css
    - https://cdn.example.com/styles.css
  ```

### extra_javascript

- **Type:** `list` or `list of mappings`
- **Default:** `[]`
- **Description:** Additional JavaScript files to include
- **Simple Example:**
  ```yaml
  extra_javascript:
    - js/custom.js
    - https://cdn.example.com/script.js
  ```
- **Advanced Example with attributes:**
  ```yaml
  extra_javascript:
    - path: js/custom.js
      async: true
    - path: js/analytics.js
      defer: true
    - path: js/module.js
      type: module
  ```

### extra_templates

- **Type:** `list`
- **Default:** `[]`
- **Description:** Additional template files to render
- **Example:**
  ```yaml
  extra_templates:
    - custom.html
    - robots.txt
  ```

### extra

- **Type:** `mapping`
- **Default:** `{}`
- **Description:** Extra context variables for templates
- **Example:**
  ```yaml
  extra:
    version:
      provider: mike
      default: stable
    social:
      - icon: fontawesome/brands/github
        link: https://github.com/username
      - icon: fontawesome/brands/twitter
        link: https://twitter.com/username
    analytics:
      provider: google
      property: G-XXXXXXXXXX
  ```

## Preview Server

### use_directory_urls

- **Type:** `boolean`
- **Default:** `true`
- **Description:** Use directory URLs (`page/` vs `page.html`)
- **Example:**
  ```yaml
  use_directory_urls: true  # Results in /page/
  use_directory_urls: false # Results in /page.html
  ```

### strict

- **Type:** `boolean`
- **Default:** `false`
- **Description:** Enable strict mode (halt on warnings)
- **Example:**
  ```yaml
  strict: true
  ```

### dev_addr

- **Type:** `string`
- **Default:** `127.0.0.1:8000`
- **Description:** Address to use for `mkdocs serve`
- **Example:**
  ```yaml
  dev_addr: 0.0.0.0:8080
  ```

### watch

- **Type:** `list`
- **Default:** `[]`
- **Description:** Additional directories to watch for changes
- **Example:**
  ```yaml
  watch:
    - ../my_module
    - ../tests
  ```

## Validation Settings

### validation

- **Type:** `mapping`
- **Description:** Controls for build warnings and validation
- **Full Example:**
  ```yaml
  validation:
    nav:
      omitted_files: warn # warn | info | ignore
      not_found: warn # warn | info | ignore
      absolute_links: warn # warn | info | ignore
    links:
      not_found: warn # warn | info | ignore
      anchors: warn # warn | info | ignore
      absolute_links: warn # warn | info | ignore | relative_to_docs
      unrecognized_links: warn # warn | info | ignore
  ```

#### validation.nav.omitted_files

- **Values:** `warn`, `info`, `ignore`
- **Default:** `info`
- **Description:** Files in docs_dir not included in nav

#### validation.nav.not_found

- **Values:** `warn`, `info`, `ignore`
- **Default:** `warn`
- **Description:** Files in nav that don't exist

#### validation.nav.absolute_links

- **Values:** `warn`, `info`, `ignore`
- **Default:** `info`
- **Description:** Absolute paths in nav configuration

#### validation.links.not_found

- **Values:** `warn`, `info`, `ignore`
- **Default:** `warn`
- **Description:** Links to missing pages

#### validation.links.anchors

- **Values:** `warn`, `info`, `ignore`
- **Default:** `info`
- **Description:** Links to missing anchors

#### validation.links.absolute_links

- **Values:** `warn`, `info`, `ignore`, `relative_to_docs`
- **Default:** `info`
- **Description:** Absolute links starting with `/`
- **Special value:** `relative_to_docs` - Treat `/path` as relative to docs_dir

#### validation.links.unrecognized_links

- **Values:** `warn`, `info`, `ignore`
- **Default:** `info`
- **Description:** Unrecognized link formats

## Markdown Extensions

### markdown_extensions

- **Type:** `list` or `mapping`
- **Default:** `[]`
- **Description:** Python Markdown extensions to enable
- **Example:**

  ```yaml
  markdown_extensions:
    # Simple extensions
    - toc
    - tables
    - footnotes

    # Extensions with configuration
    - toc:
        permalink: true
        slugify: !!python/object/apply:pymdownx.slugs.slugify
          kwds:
            case: lower
    - pymdownx.highlight:
        anchor_linenums: true
        line_spans: __span
        pygments_lang_class: true
    - pymdownx.superfences:
        custom_fences:
          - name: mermaid
            class: mermaid
            format: !!python/name:pymdownx.superfences.fence_code_format
    - admonition
    - pymdownx.details
    - pymdownx.tabbed:
        alternate_style: true
    - attr_list
    - md_in_html
  ```

## Plugins

### plugins

- **Type:** `list` or `mapping`
- **Default:** `['search']` if not specified
- **Description:** Plugins to enable
- **Example:**
  ```yaml
  plugins:
    - search:
        separator: '[\s\-\.]+'
        min_search_length: 3
        lang:
          - en
          - es
        prebuild_index: false
        indexing: full
    - tags:
        tags_file: tags.md
    - social:
        cards: true
        cards_dir: assets/images/social
    - minify:
        minify_html: true
        minify_js: true
        minify_css: true
    - git-revision-date-localized:
        enable_creation_date: true
        type: timeago
  ```

### Search Plugin Configuration

The built-in search plugin accepts these options:

#### plugins.search.separator

- **Type:** `string` (regex)
- **Default:** `r'[\s\-]+'`
- **Description:** Word separator pattern

#### plugins.search.min_search_length

- **Type:** `integer`
- **Default:** `3`
- **Description:** Minimum search query length

#### plugins.search.lang

- **Type:** `string` or `list`
- **Default:** Theme locale or `['en']`
- **Description:** Language(s) for search stemming

#### plugins.search.prebuild_index

- **Type:** `boolean` or `string`
- **Default:** `false`
- **Description:** Pre-build search index (requires Node.js)

#### plugins.search.indexing

- **Type:** `string`
- **Default:** `full`
- **Values:** `full`, `sections`, `titles`
- **Description:** What to include in search index

## Hooks

### hooks

- **Type:** `list`
- **Default:** `[]`
- **Description:** Python scripts with event hooks
- **Example:**
  ```yaml
  hooks:
    - scripts/hooks.py
  ```

**Hook Script Example:**

```python
# scripts/hooks.py
import logging

def on_pre_build(config):
    """Called before the build process starts."""
    logging.info("Starting documentation build...")

def on_page_markdown(markdown, page, config, files):
    """Called when markdown is loaded, before conversion to HTML."""
    return markdown.replace('{{VERSION}}', config['extra']['version'])

def on_page_content(html, page, config, files):
    """Called after markdown is converted to HTML."""
    return html
```

## Environment Variables

### Using !ENV tag

- **Description:** Reference environment variables in configuration
- **Syntax:** `!ENV [VAR_NAME, 'default_value']` or `!ENV VAR_NAME`
- **Example:**

  ```yaml
  site_name: !ENV [SITE_NAME, "My Documentation"]
  site_url: !ENV SITE_URL

  theme:
    name: !ENV [THEME_NAME, "material"]

  extra:
    analytics:
      property: !ENV GOOGLE_ANALYTICS_KEY
    version: !ENV [VERSION, "dev"]
  ```

## Path References

### Using !relative tag

- **Description:** Specify paths relative to config file or docs directory
- **Syntax:** `!relative $config_dir/path` or `!relative $docs_dir/path`
- **Example:**

  ```yaml
  theme:
    custom_dir: !relative $config_dir/custom_theme

  extra_css:
    - !relative $docs_dir/stylesheets/extra.css

  plugins:
    - macros:
        include_dir: !relative $config_dir/includes
  ```

## Configuration Inheritance

### INHERIT

- **Type:** `string` (path)
- **Description:** Inherit configuration from a parent file
- **Example:**

**base.yml:**

```yaml
site_name: My Documentation
theme:
  name: material
  palette:
    primary: indigo
plugins:
  search: {}
  tags: {}
```

**mkdocs.yml:**

```yaml
INHERIT: base.yml

site_name: My Documentation - Development
theme:
  palette:
    primary: green

# Plugins are merged, not replaced
plugins:
  minify: {}
```

## Complete Configuration Example

```yaml
# Project information
site_name: My Project Documentation
site_url: https://example.com/
site_description: Comprehensive documentation for My Project
site_author: Development Team

# Repository
repo_url: https://github.com/myorg/myproject
repo_name: myorg/myproject
edit_uri: edit/main/docs/

# Copyright
copyright: Copyright &copy; 2024 My Organization

# Configuration
theme:
  name: material
  custom_dir: overrides

  # Theme configuration
  palette:
    - media: "(prefers-color-scheme: light)"
      scheme: default
      primary: indigo
      accent: indigo
      toggle:
        icon: material/brightness-7
        name: Switch to dark mode
    - media: "(prefers-color-scheme: dark)"
      scheme: slate
      primary: indigo
      accent: indigo
      toggle:
        icon: material/brightness-4
        name: Switch to light mode

  features:
    - announce.dismiss
    - content.action.edit
    - content.action.view
    - content.code.annotate
    - content.code.copy
    - content.tabs.link
    - content.tooltips
    - header.autohide
    - navigation.expand
    - navigation.footer
    - navigation.indexes
    - navigation.instant
    - navigation.sections
    - navigation.tabs
    - navigation.tabs.sticky
    - navigation.top
    - navigation.tracking
    - search.highlight
    - search.share
    - search.suggest
    - toc.follow
    - toc.integrate

  font:
    text: Roboto
    code: Roboto Mono

  language: en

  icon:
    logo: material/library
    repo: fontawesome/brands/github

# Extensions
markdown_extensions:
  # Python Markdown
  - abbr
  - admonition
  - attr_list
  - def_list
  - footnotes
  - md_in_html
  - toc:
      permalink: true
      toc_depth: 3
  - tables

  # Python Markdown Extensions
  - pymdownx.arithmatex:
      generic: true
  - pymdownx.betterem
  - pymdownx.caret
  - pymdownx.details
  - pymdownx.emoji:
      emoji_index: !!python/name:material.extensions.emoji.twemoji
      emoji_generator: !!python/name:material.extensions.emoji.to_svg
  - pymdownx.highlight:
      anchor_linenums: true
      line_spans: __span
      pygments_lang_class: true
  - pymdownx.inlinehilite
  - pymdownx.keys
  - pymdownx.mark
  - pymdownx.smartsymbols
  - pymdownx.superfences:
      custom_fences:
        - name: mermaid
          class: mermaid
          format: !!python/name:pymdownx.superfences.fence_code_format
  - pymdownx.tabbed:
      alternate_style: true
  - pymdownx.tasklist:
      custom_checkbox: true
  - pymdownx.tilde

# Plugins
plugins:
  - search:
      separator: '[\s\-\.]+'
      lang:
        - en
  - tags:
      tags_file: tags.md
  - git-revision-date-localized:
      enable_creation_date: true
      type: timeago
  - minify:
      minify_html: true
      minify_js: true
      minify_css: true
      htmlmin_opts:
        remove_comments: true

# Customization
extra:
  version:
    provider: mike
    default: latest

  social:
    - icon: fontawesome/brands/github
      link: https://github.com/myorg
    - icon: fontawesome/brands/twitter
      link: https://twitter.com/myorg
    - icon: fontawesome/brands/linkedin
      link: https://linkedin.com/company/myorg

  analytics:
    provider: google
    property: !ENV GOOGLE_ANALYTICS_KEY
    feedback:
      title: Was this page helpful?
      ratings:
        - icon: material/emoticon-happy-outline
          name: This page was helpful
          data: 1
          note: Thanks for your feedback!
        - icon: material/emoticon-sad-outline
          name: This page could be improved
          data: 0
          note: Thanks for your feedback!

  consent:
    title: Cookie consent
    description: >-
      We use cookies to recognize your repeated visits and preferences, as well as to measure the effectiveness of our documentation and whether users find what they're searching for. With your consent, you're helping us to make our documentation better.


# Additional CSS
extra_css:
  - stylesheets/extra.css

# Additional JavaScript
extra_javascript:
  - javascripts/mathjax.js
  - https://polyfill.io/v3/polyfill.min.js?features=es6
  - https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-mml-chtml.js

# Navigation
nav:
  - Home: index.md
  - Getting Started:
      - Installation: getting-started/installation.md
      - Quick Start: getting-started/quickstart.md
      - Configuration: getting-started/configuration.md
  - User Guide:
      - user-guide/index.md
      - Basics: user-guide/basics.md
      - Advanced: user-guide/advanced.md
      - Best Practices: user-guide/best-practices.md
  - API Reference:
      - api/index.md
      - Core API: api/core.md
      - Extensions: api/extensions.md
  - Contributing:
      - contributing/index.md
      - Development Setup: contributing/setup.md
      - Guidelines: contributing/guidelines.md
  - About:
      - Release Notes: about/changelog.md
      - License: about/license.md

# Page tree
exclude_docs: |
  *.py
  /templates/

not_in_nav: |
  /tags.md

# Strict mode
strict: !ENV [STRICT, false]

# Development server
dev_addr: 127.0.0.1:8000

# Build directories
docs_dir: docs
site_dir: site

# Validation
validation:
  nav:
    omitted_files: warn
    not_found: warn
  links:
    not_found: warn
    anchors: info
    absolute_links: relative_to_docs
```

## Official Resources

- Configuration Guide: <https://www.mkdocs.org/user-guide/configuration/>
- YAML Syntax: <https://yaml.org/>
- Environment Variables: <https://www.mkdocs.org/user-guide/configuration/#environment-variables>
- Plugin Catalog: <https://github.com/mkdocs/catalog>
