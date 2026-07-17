# Real-World MkDocs Examples

A comprehensive collection of active MkDocs implementations, deployment workflows, and best practices from production projects.

## Active GitHub Repositories Using MkDocs

### Major Framework Documentation

1. **FastAPI** - <https://github.com/fastapi/fastapi>

   - Modern web API framework documentation
   - Uses Material for MkDocs theme
   - Configuration: `docs/en/mkdocs.yml`
   - Live site: <https://fastapi.tiangolo.com/>
   - Stars: 70k+

2. **Pydantic** - <https://github.com/pydantic/pydantic>

   - Data validation library documentation
   - Material for MkDocs with mkdocstrings
   - Configuration: `mkdocs.yml` at root
   - Live site: <https://docs.pydantic.dev/>
   - Stars: 20k+

3. **Django REST Framework** - <https://github.com/encode/django-rest-framework>

   - Web API toolkit for Django
   - Custom theme with MkDocs
   - Configuration: `mkdocs.yml`
   - Live site: <https://www.django-rest-framework.org/>
   - Stars: 28k+

4. **Material for MkDocs** - <https://github.com/squidfunk/mkdocs-material>
   - The most popular MkDocs theme
   - Self-documenting (uses itself)
   - Configuration: `mkdocs.yml`
   - Live site: <https://squidfunk.github.io/mkdocs-material/>
   - Stars: 25k+

### AI/ML Projects

5. **InvokeAI** - <https://github.com/invoke-ai/InvokeAI>

   - Stable Diffusion creative engine
   - Material for MkDocs theme
   - Workflow: `.github/workflows/mkdocs-material.yml`
   - Stars: 23k+

6. **XGBoostLSS** - <https://github.com/StatMixedML/XGBoostLSS>

   - XGBoost extension for probabilistic modeling
   - Material for MkDocs
   - Workflow: `.github/workflows/mkdocs.yaml`
   - Stars: 400+

7. **Docling** - <https://github.com/docling-project/docling>
   - Document preparation for Gen AI
   - MkDocs with custom configuration
   - Workflow: `.github/workflows/docs.yml`
   - Stars: 13k+

### Developer Tools

8. **Titiler** - <https://github.com/developmentseed/titiler>

   - Dynamic map tile services
   - Material for MkDocs
   - Workflow: `.github/workflows/deploy_mkdocs.yml`
   - Live site: <https://developmentseed.org/titiler/>
   - Stars: 800+

9. **Google Timesketch** - <https://github.com/google/timesketch>

   - Collaborative forensic timeline analysis
   - MkDocs Material theme
   - Workflow: `.github/workflows/documentation.yml`
   - Stars: 2.5k+

10. **APIO (FPGAwars)** - <https://github.com/FPGAwars/apio>
    - Open source ecosystem for FPGA boards
    - MkDocs documentation
    - Workflow: `.github/workflows/publish-mkdocs-docs.yaml`
    - Stars: 800+

### Educational Resources

11. **90DaysOfDevOps** - <https://github.com/MichaelCade/90DaysOfDevOps>
    - Structured DevOps learning path
    - MkDocs with Material theme
    - Workflow: `.github/workflows/web-app-deploy.yml`
    - Live site: <https://90daysofdevops.com/>
    - Stars: 26k+

### Specialized Documentation

12. **ABCI User Guide** - <https://github.com/aistabci/abci-docs>

    - Supercomputing resource documentation
    - MkDocs implementation
    - Stars: 64

13. **Skytable Docs** - <https://github.com/skytable/docs>

    - NoSQL database documentation
    - MkDocs with custom theme
    - Stars: 10

14. **Vaping** - <https://github.com/20c/vaping>

    - Network monitoring tool docs
    - MkDocs documentation
    - Workflow: `.github/workflows/ci.yml`
    - Stars: 300+

15. **Django File Form** - <https://github.com/mbraak/django-file-form>
    - Django form component docs
    - MkDocs implementation
    - Workflow: `.github/workflows/mkdocs.yml`
    - Stars: 200+

## Active GitLab Repositories Using MkDocs

1. **GitLab Pages MkDocs Example** - <https://gitlab.com/pages/mkdocs>

   - Official GitLab Pages example
   - Live site: <https://pages.gitlab.io/mkdocs>
   - Default template for GitLab MkDocs projects

2. **to-be-continuous MkDocs** - <https://to-be-continuous.gitlab.io/doc/ref/mkdocs/>

   - CI/CD pipeline templates for MkDocs
   - Provides reusable GitLab CI configurations

3. **MkDocs Material Examples** - Various GitLab projects

   - Many projects use the Material theme
   - Common pattern: Python image + pip install

4. **Corporate Documentation Projects**

   - Many internal corporate projects use GitLab + MkDocs
   - Private repositories but similar patterns

5. **Open Source GitLab Projects**
   - Various FOSS projects hosted on GitLab
   - Similar CI/CD patterns to GitHub

## GitHub Pages Deployment Workflows

### Basic GitHub Actions Workflow

```yaml
# .github/workflows/mkdocs.yml
name: Deploy MkDocs to GitHub Pages

on:
  push:
    branches: [main]
  workflow_dispatch:

permissions:
  contents: read
  pages: write
  id-token: write

concurrency:
  group: "pages"
  cancel-in-progress: false

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Python
        uses: actions/setup-python@v5
        with:
          python-version: "3.x"

      - name: Install dependencies
        run: |
          pip install mkdocs-material
          pip install mkdocs-minify-plugin
          pip install mkdocs-redirects

      - name: Build site
        run: mkdocs build

      - name: Upload artifact
        uses: actions/upload-pages-artifact@v3
        with:
          path: ./site

  deploy:
    environment:
      name: github-pages
      url: ${{ steps.deployment.outputs.page_url }}
    runs-on: ubuntu-latest
    needs: build
    steps:
      - name: Deploy to GitHub Pages
        id: deployment
        uses: actions/deploy-pages@v4
```

### Django REST Framework Workflow

```yaml
# From encode/django-rest-framework
name: mkdocs

on:
  push:
    branches:
      - main
    paths:
      - docs/**
      - docs_theme/**
      - requirements/requirements-documentation.txt
      - mkdocs.yml
      - .github/workflows/mkdocs-deploy.yml

jobs:
  deploy:
    runs-on: ubuntu-latest
    environment: github-pages
    permissions:
      contents: write
    concurrency:
      group: ${{ github.workflow }}-${{ github.ref }}
    steps:
      - uses: actions/checkout@v5
      - run: git fetch --no-tags --prune --depth=1 origin gh-pages
      - uses: actions/setup-python@v6
        with:
          python-version: 3.x
      - run: pip install -r requirements/requirements-documentation.txt
      - run: mkdocs gh-deploy
```

### Google Timesketch Workflow

```yaml
# From google/timesketch
name: Publish docs to GitHub Pages

on:
  push:
    branches:
      - master
    paths:
      - "docs/**"
  workflow_dispatch:

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v2
        with:
          python-version: 3.8
      - name: Install dependencies
        run: pip install mkdocs-material mkdocs-redirects
      - name: Build site
        run: mkdocs build
      - name: Deploy to GitHub Pages
        uses: peaceiris/actions-gh-pages@v3
        with:
          github_token: ${{ secrets.GITHUB_TOKEN }}
          publish_dir: ./site
          publish_branch: gh-pages
          deploy_key: ${{ secrets.DEPLOY_KEY }}
          external_repository: google/timesketch
          user_name: github-actions[bot]
          user_email: 41898282+github-actions[bot]@users.noreply.github.com
```

### Advanced Multi-Version Workflow

```yaml
# Supporting multiple documentation versions
name: Deploy Multi-Version Docs

on:
  push:
    branches: [main]
    tags: ["v*"]
  pull_request:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0 # Fetch all history for mike

      - uses: actions/setup-python@v5
        with:
          python-version: 3.x

      - name: Install dependencies
        run: |
          pip install mkdocs-material
          pip install mike
          pip install mkdocs-macros-plugin

      - name: Configure Git
        run: |
          git config user.name github-actions
          git config user.email github-actions@github.com

      - name: Deploy version
        run: |
          if [[ "${{ github.ref }}" == refs/tags/* ]]; then
            VERSION=${GITHUB_REF#refs/tags/}
            mike deploy --push --update-aliases $VERSION latest
          else
            mike deploy --push dev
          fi
```

## GitLab Pages Deployment Workflows

### Basic GitLab CI Configuration

```yaml
# .gitlab-ci.yml
image: python:3.11-alpine

before_script:
  - pip install mkdocs-material

pages:
  stage: deploy
  script:
    - mkdocs build
    - mv site public
  artifacts:
    paths:
      - public
  only:
    - main
```

### Material for MkDocs Docker Image

```yaml
# Using official Docker image
stages:
  - build
  - deploy

pages:
  stage: deploy
  image:
    name: squidfunk/mkdocs-material:latest
    entrypoint: [""]
  script:
    - mkdocs build --site-dir public
  artifacts:
    paths:
      - public
  rules:
    - if: "$CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH"
```

### Advanced GitLab Configuration with Caching

```yaml
# With dependency caching
image: python:3.11

variables:
  PIP_CACHE_DIR: "$CI_PROJECT_DIR/.cache/pip"

cache:
  paths:
    - .cache/pip
    - venv/

before_script:
  - python -m venv venv
  - source venv/bin/activate
  - pip install --upgrade pip
  - pip install mkdocs-material mkdocs-minify-plugin

pages:
  stage: deploy
  script:
    - source venv/bin/activate
    - mkdocs build --strict --verbose --site-dir public
  artifacts:
    paths:
      - public
  only:
    - main
```

### Multi-Environment Deployment

```yaml
# Deploy to staging and production
stages:
  - build
  - staging
  - production

.mkdocs_template: &mkdocs_definition
  image: python:3.11-alpine
  before_script:
    - pip install -r requirements-docs.txt
  script:
    - mkdocs build --site-dir public

staging:
  <<: *mkdocs_definition
  stage: staging
  artifacts:
    paths:
      - public
  environment:
    name: staging
    url: https://$CI_PROJECT_NAME-staging.gitlab.io
  except:
    - main

pages:
  <<: *mkdocs_definition
  stage: production
  artifacts:
    paths:
      - public
  environment:
    name: production
    url: https://$CI_PROJECT_NAME.gitlab.io
  only:
    - main
```

## CI/CD Patterns and Best Practices

### 1. Path-Based Triggers

Only rebuild docs when documentation files change:

```yaml
on:
  push:
    paths:
      - "docs/**"
      - "mkdocs.yml"
      - ".github/workflows/docs.yml"
```

### 2. Dependency Management

**requirements-docs.txt:**

```txt
mkdocs==1.6.0
mkdocs-material==9.5.18
mkdocs-minify-plugin==0.8.0
mkdocs-redirects==1.2.1
mkdocstrings[python]==0.25.0
mkdocs-macros-plugin==1.0.5
```

### 3. Build Optimization

- Use caching for Python packages
- Pin dependency versions
- Use Alpine Linux images for smaller footprint
- Enable strict mode for validation

### 4. Security Best Practices

```yaml
permissions:
  contents: read
  pages: write
  id-token: write
```

### 5. Concurrency Control

```yaml
concurrency:
  group: "pages-${{ github.ref }}"
  cancel-in-progress: true
```

### 6. Multi-Version Documentation

Using mike for versioned docs:

```bash
mike deploy --push --update-aliases 1.0 latest
mike set-default --push latest
```

### 7. Preview Deployments

Deploy PR previews to separate URLs:

```yaml
- name: Deploy PR Preview
  if: github.event_name == 'pull_request'
  run: |
    mkdocs build --site-dir "pr-${{ github.event.number }}"
    # Deploy to pr-123.example.com
```

## Production Site Examples

### Framework Documentation

1. **FastAPI** - <https://fastapi.tiangolo.com/>

   - Multi-language support
   - API documentation integration
   - Interactive examples

2. **Pydantic** - <https://docs.pydantic.dev/>

   - Versioned documentation
   - Auto-generated API docs
   - Rich examples

3. **Material for MkDocs** - <https://squidfunk.github.io/mkdocs-material/>
   - Feature showcase
   - Insiders program docs
   - Blog integration

### Corporate Documentation

4. **Datadog** - Uses Material for MkDocs internally
5. **Netflix** - Internal documentation systems
6. **Google** - Various project docs (Timesketch, etc.)
7. **Microsoft** - Some Azure documentation

### Open Source Projects

8. **90 Days of DevOps** - <https://90daysofdevops.com/>

   - Educational content
   - Multi-language translations
   - Community contributions

9. **Hummingbot** - <https://docs.hummingbot.org/>
   - Trading bot documentation
   - Complex navigation structure
   - API references

### Notable Features in Production

- **Search**: Algolia, lunr.js integration
- **Analytics**: Google Analytics, Plausible
- **Comments**: Giscus, Disqus integration
- **Versioning**: mike for multiple versions
- **i18n**: Multi-language support
- **Social**: Open Graph cards generation
- **PDF Export**: Print-friendly versions

## Key Takeaways

1. **Material for MkDocs** is the dominant theme choice
2. **GitHub Actions** and **GitLab CI** have well-established patterns
3. **Path-based triggers** optimize CI/CD resources
4. **Versioning** is crucial for API documentation
5. **Caching** significantly speeds up builds
6. **Docker images** provide consistent environments
7. **Security permissions** should be explicit and minimal

## Resources

- [MkDocs Official Documentation](https://www.mkdocs.org/)
- [Material for MkDocs](https://squidfunk.github.io/mkdocs-material/)
- [MkDocs Catalog](https://github.com/mkdocs/catalog)
- [GitLab Pages Examples](https://gitlab.com/pages)
- [GitHub Pages Documentation](https://docs.github.com/en/pages)
