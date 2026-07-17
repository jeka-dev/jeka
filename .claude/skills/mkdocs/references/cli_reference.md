# MkDocs CLI Reference

Complete command-line interface documentation for MkDocs.

Official Documentation: <https://www.mkdocs.org/user-guide/cli/>

## Global Command

```bash
mkdocs [OPTIONS] COMMAND [ARGS]...
```

**Description:** MkDocs - Project documentation with Markdown.

### Global Options

| Option                   | Short | Description                                                |
| ------------------------ | ----- | ---------------------------------------------------------- |
| `--version`              | `-V`  | Display the MkDocs version and exit                        |
| `--quiet`                | `-q`  | Suppress warnings (can be repeated for less output)        |
| `--verbose`              | `-v`  | Enable verbose output (can be repeated for more verbosity) |
| `--color` / `--no-color` |       | Force enable/disable color output (default: auto-detect)   |
| `--help`                 |       | Show help message and exit                                 |

## Commands

### mkdocs build

Build the MkDocs documentation.

```bash
mkdocs build [OPTIONS]
```

**Options:**

| Option                                         | Short  | Description                                      | Default      |
| ---------------------------------------------- | ------ | ------------------------------------------------ | ------------ |
| `--config-file`                                | `-f`   | Path to mkdocs.yml configuration file            | `mkdocs.yml` |
| `--clean` / `--dirty`                          | `-c` / | Remove stale files from site_dir before building | `--clean`    |
| `--strict`                                     | `-s`   | Enable strict mode (fail on warnings)            | `false`      |
| `--theme`                                      | `-t`   | Theme to use (mkdocs or readthedocs)             | From config  |
| `--use-directory-urls` / `--no-directory-urls` |        | Enable/disable directory URLs                    | From config  |
| `--site-dir`                                   | `-d`   | Output directory for documentation site          | `site`       |
| `--quiet`                                      | `-q`   | Suppress warnings                                |              |
| `--verbose`                                    | `-v`   | Enable verbose output                            |              |

**Examples:**

```bash
# Basic build
mkdocs build

# Build with clean output directory
mkdocs build --clean

# Build in strict mode (fail on warnings)
mkdocs build --strict

# Build to custom directory
mkdocs build --site-dir public

# Use specific config file
mkdocs build -f my-config.yml

# Build with specific theme
mkdocs build --theme readthedocs
```

### mkdocs serve

Run the built-in development server.

```bash
mkdocs serve [OPTIONS]
```

**Options:**

| Option                                         | Short | Description                                                  | Default          |
| ---------------------------------------------- | ----- | ------------------------------------------------------------ | ---------------- |
| `--dev-addr`                                   | `-a`  | Address and port to serve on                                 | `127.0.0.1:8000` |
| `--config-file`                                | `-f`  | Path to mkdocs.yml configuration file                        | `mkdocs.yml`     |
| `--strict`                                     | `-s`  | Enable strict mode (fail on warnings)                        | `false`          |
| `--theme`                                      | `-t`  | Theme to use                                                 | From config      |
| `--use-directory-urls` / `--no-directory-urls` |       | Enable/disable directory URLs                                | From config      |
| `--livereload`                                 |       | Enable live reloading in browser (default)                   | `true`           |
| `--no-livereload`                              |       | Disable live reloading                                       |                  |
| `--dirtyreload`                                |       | Only rebuild files that have changed                         | `false`          |
| `--watch-theme`                                |       | Watch theme files for changes                                | `false`          |
| `--watch`                                      | `-w`  | Additional directories or files to watch                     |                  |
| `--clean`                                      | `-c`  | Clean site_dir before building                               | `false`          |
| `--dirty`                                      |       | Only rebuild pages that have changed (implies --dirtyreload) |                  |

**Examples:**

```bash
# Basic serve (default: http://127.0.0.1:8000)
mkdocs serve

# Serve on different address/port
mkdocs serve --dev-addr 0.0.0.0:8080

# Serve without live reload
mkdocs serve --no-livereload

# Serve with dirty reload (faster for large sites)
mkdocs serve --dirtyreload

# Watch additional directories
mkdocs serve --watch ../my-code --watch ../tests

# Watch theme changes
mkdocs serve --watch-theme

# Clean build then serve
mkdocs serve --clean
```

### mkdocs new

Create a new MkDocs project.

```bash
mkdocs new [OPTIONS] PROJECT_DIRECTORY
```

**Arguments:**

- `PROJECT_DIRECTORY` - Path where the new project will be created

**Options:**

| Option      | Short | Description           |
| ----------- | ----- | --------------------- |
| `--quiet`   | `-q`  | Suppress output       |
| `--verbose` | `-v`  | Enable verbose output |
| `--help`    |       | Show help message     |

**Examples:**

```bash
# Create new project
mkdocs new my-project

# Create project in current directory
mkdocs new .

# Create project with path
mkdocs new docs/my-documentation
```

**Created Structure:**

<eg>
PROJECT_DIRECTORY/
├── mkdocs.yml       # Main configuration file
└── docs/
    └── index.md     # Homepage for documentation
</eg>

### mkdocs gh-deploy

Deploy documentation to GitHub Pages.

```bash
mkdocs gh-deploy [OPTIONS]
```

**Options:**

| Option             | Short | Description                                | Default               |
| ------------------ | ----- | ------------------------------------------ | --------------------- |
| `--config-file`    | `-f`  | Path to mkdocs.yml configuration file      | `mkdocs.yml`          |
| `--message`        | `-m`  | Commit message for GitHub Pages            | Site built from {sha} |
| `--remote-branch`  | `-b`  | Remote branch to deploy to                 | `gh-pages`            |
| `--remote-name`    | `-r`  | Remote name to deploy to                   | `origin`              |
| `--force`          |       | Force push to repository                   | `false`               |
| `--no-history`     |       | Replace entire git history with one commit | `false`               |
| `--ignore-version` |       | Deploy without checking MkDocs version     | `false`               |
| `--shell`          |       | Use shell when invoking Git                | `false`               |
| `--clean`          | `-c`  | Clean site_dir before building             | `true`                |
| `--dirty`          | `-d`  | Don't clean site_dir before building       |                       |
| `--strict`         | `-s`  | Enable strict mode                         | `false`               |
| `--theme`          | `-t`  | Theme to use                               | From config           |
| `--site-dir`       |       | The directory to deploy                    | `site`                |
| `--verbose`        | `-v`  | Enable verbose output                      |                       |
| `--quiet`          | `-q`  | Suppress output                            |                       |

**Examples:**

```bash
# Basic GitHub Pages deployment
mkdocs gh-deploy

# Deploy with custom commit message
mkdocs gh-deploy --message "Deploy version 2.0"

# Deploy to different branch
mkdocs gh-deploy --remote-branch docs

# Force push (careful!)
mkdocs gh-deploy --force

# Deploy without history (single commit)
mkdocs gh-deploy --no-history

# Deploy from different config
mkdocs gh-deploy -f production.yml

# Deploy to different remote
mkdocs gh-deploy --remote-name upstream
```

**Requirements:**

- Git repository must be initialized
- Remote repository must be configured
- Appropriate push permissions to remote

### mkdocs get-deps

Show required Python dependencies inferred from plugins.

```bash
mkdocs get-deps [OPTIONS]
```

**Options:**

| Option            | Short | Description                           | Default        |
| ----------------- | ----- | ------------------------------------- | -------------- |
| `--config-file`   | `-f`  | Path to mkdocs.yml configuration file | `mkdocs.yml`   |
| `--projects-file` | `-p`  | URL or path to projects file          | MkDocs catalog |

**Examples:**

```bash
# List dependencies for current project
mkdocs get-deps

# Check dependencies for specific config
mkdocs get-deps -f custom-config.yml

# Use custom projects file
mkdocs get-deps -p https://example.com/projects.yml
```

**Output Format:** Prints a list of required PyPI packages that should be installed for the plugins specified in your configuration.

## Environment Variables

MkDocs supports environment variables in configuration using the `!ENV` tag:

```yaml
site_name: !ENV [SITE_NAME, "Default Site Name"]
site_url: !ENV SITE_URL
```

### Common Environment Variables

| Variable               | Purpose            | Example                                 |
| ---------------------- | ------------------ | --------------------------------------- |
| `SITE_NAME`            | Override site name | `export SITE_NAME="My Docs"`            |
| `SITE_URL`             | Set production URL | `export SITE_URL="https://example.com"` |
| `GOOGLE_ANALYTICS_KEY` | Analytics tracking | `export GOOGLE_ANALYTICS_KEY="G-XXXXX"` |
| `GITHUB_TOKEN`         | GitHub API access  | `export GITHUB_TOKEN="ghp_xxxx"`        |

## Configuration File Detection

MkDocs searches for configuration files in this order:

1. File specified with `-f` / `--config-file`
2. `mkdocs.yml` in current directory
3. `mkdocs.yaml` in current directory

## Common Workflows

### Local Development

```bash
# 1. Create new project
mkdocs new my-docs
cd my-docs

# 2. Start development server
mkdocs serve

# 3. Open browser to http://127.0.0.1:8000

# 4. Edit docs/*.md files (auto-reload in browser)

# 5. Build final site
mkdocs build
```

### GitHub Pages Deployment

```bash
# 1. Initialize git repository
git init
git add .
git commit -m "Initial commit"

# 2. Add GitHub remote
git remote add origin https://github.com/username/repo.git

# 3. Push main branch
git push -u origin main

# 4. Deploy to GitHub Pages
mkdocs gh-deploy

# Site available at: https://username.github.io/repo/
```

### Multi-Language Documentation

```bash
# Serve different language versions
mkdocs serve -f config/en/mkdocs.yml  # English
mkdocs serve -f config/es/mkdocs.yml  # Spanish
mkdocs serve -f config/fr/mkdocs.yml  # French

# Build all languages
mkdocs build -f config/en/mkdocs.yml -d site/en
mkdocs build -f config/es/mkdocs.yml -d site/es
mkdocs build -f config/fr/mkdocs.yml -d site/fr
```

### Strict Mode Development

```bash
# Development with strict mode (recommended)
mkdocs serve --strict

# Build with strict mode for CI/CD
mkdocs build --strict

# Deploy with strict mode
mkdocs gh-deploy --strict
```

## Exit Codes

| Code | Meaning                                            |
| ---- | -------------------------------------------------- |
| `0`  | Success                                            |
| `1`  | General error (build failure, missing files, etc.) |
| `2`  | Warning in strict mode                             |
| `3`  | Keyboard interrupt (Ctrl+C)                        |

## Tips and Best Practices

1. **Always use `--strict` in CI/CD** to catch documentation errors early
2. **Use `--dirtyreload` for large documentation sites** to improve rebuild performance
3. **Watch additional directories** with `-w` when documentation depends on external files
4. **Use environment variables** for sensitive data instead of hardcoding in mkdocs.yml
5. **Test builds locally** before deploying with `mkdocs build --strict`
6. **Use `--no-history` carefully** as it removes all git history from gh-pages branch
7. **Custom domains:** Create `CNAME` file in docs/ directory for GitHub Pages custom domains

## Related Commands

- `pip install mkdocs` - Install MkDocs
- `pip install mkdocs-material` - Install Material theme
- `python -m mkdocs` - Run MkDocs as Python module
- `mkdocs --version` - Check installed version

## Official Resources

- Documentation: <https://www.mkdocs.org/>
- GitHub: <https://github.com/mkdocs/mkdocs>
- PyPI: <https://pypi.org/project/mkdocs/>
- Community: <https://github.com/mkdocs/mkdocs/discussions>
