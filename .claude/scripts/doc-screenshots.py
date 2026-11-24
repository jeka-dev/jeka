#!/usr/bin/env python3
"""
Screenshot the built JeKa MkDocs site, for visual review by the doc-architect agent.

Prerequisites (one-time):
    pip install playwright pyyaml
    playwright install chromium

Usage (from repo root):
    mkdocs build                                   # builds into site/
    python -m http.server -d site 8000 &           # serve it (don't open files directly)
    python .claude/scripts/doc-screenshots.py      # all pages from mkdocs.yml nav

Options:
    --base-url URL     default http://localhost:8000
    --out DIR          default /tmp/doc-shots
    --pages a.md b.md  only these pages (paths relative to docs/, as in nav)
    --viewports ...    any of: desktop mobile (default: both)
    --schemes ...      any of: light dark (default: both)
    --max-height PX    cap for full-page shots (default 4000; 0 = no full-page shot)

Output: <out>/<page-slug>__<viewport>__<scheme>__top.png   (what's visible on load)
        <out>/<page-slug>__<viewport>__<scheme>__full.png  (full page, capped)
        <out>/index.txt                                     (list of shots + any errors)
"""
import argparse
import sys
from pathlib import Path

import yaml

VIEWPORTS = {
    "desktop": {"width": 1440, "height": 900},
    "mobile": {"width": 390, "height": 844},
}
# Material for MkDocs scheme names, as configured in mkdocs.yml palette
SCHEMES = {"light": "default", "dark": "slate"}


class _IgnoreTagsLoader(yaml.SafeLoader):
    """mkdocs.yml contains python tags (!!python/name:...) that safe_load refuses."""


_IgnoreTagsLoader.add_multi_constructor("", lambda loader, suffix, node: None)
_IgnoreTagsLoader.add_multi_constructor("tag:yaml.org,2002:python/", lambda loader, suffix, node: None)


def nav_pages(mkdocs_yml: Path) -> list[str]:
    config = yaml.load(mkdocs_yml.read_text(), Loader=_IgnoreTagsLoader)
    pages: list[str] = ["index.md"]

    def walk(node):
        if isinstance(node, str):
            if node.endswith(".md"):
                pages.append(node.strip())
        elif isinstance(node, list):
            for item in node:
                walk(item)
        elif isinstance(node, dict):
            for value in node.values():
                walk(value)

    walk(config.get("nav", []))
    seen, unique = set(), []
    for p in pages:
        if p not in seen:
            seen.add(p)
            unique.append(p)
    return unique


def page_url(base_url: str, md_path: str) -> str:
    """Map docs path to URL, assuming mkdocs default use_directory_urls: true."""
    path = md_path[:-3]
    if path == "index":
        path = ""
    elif path.endswith("/index"):
        path = path[: -len("index")]
    else:
        path += "/"
    return base_url.rstrip("/") + "/" + path


def slug(md_path: str) -> str:
    return md_path[:-3].replace("/", "_") or "index"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--base-url", default="http://localhost:8000")
    parser.add_argument("--out", default="/tmp/doc-shots")
    parser.add_argument("--pages", nargs="*")
    parser.add_argument("--viewports", nargs="*", default=list(VIEWPORTS), choices=list(VIEWPORTS))
    parser.add_argument("--schemes", nargs="*", default=list(SCHEMES), choices=list(SCHEMES))
    parser.add_argument("--max-height", type=int, default=4000)
    parser.add_argument("--mkdocs-yml", default="mkdocs.yml")
    args = parser.parse_args()

    try:
        from playwright.sync_api import sync_playwright
    except ImportError:
        print("Playwright missing: pip install playwright && playwright install chromium", file=sys.stderr)
        return 2

    pages = args.pages or nav_pages(Path(args.mkdocs_yml))
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    log: list[str] = []

    with sync_playwright() as pw:
        browser = pw.chromium.launch()
        for vp_name in args.viewports:
            context = browser.new_context(viewport=VIEWPORTS[vp_name], device_scale_factor=1)
            page = context.new_page()
            for md in pages:
                url = page_url(args.base_url, md)
                try:
                    response = page.goto(url, wait_until="networkidle", timeout=30000)
                    if response is not None and response.status >= 400:
                        log.append(f"ERROR {response.status} {url} ({md})")
                        continue
                except Exception as e:  # noqa: BLE001
                    log.append(f"ERROR {url} ({md}): {e}")
                    continue
                for scheme_name in args.schemes:
                    page.evaluate(
                        "s => document.body.setAttribute('data-md-color-scheme', s)", SCHEMES[scheme_name]
                    )
                    page.evaluate("window.scrollTo(0, 0)")
                    page.wait_for_timeout(200)
                    base = out / f"{slug(md)}__{vp_name}__{scheme_name}"
                    page.screenshot(path=f"{base}__top.png")
                    log.append(f"{base}__top.png  <- {url}")
                    if args.max_height > 0:
                        height = page.evaluate("document.documentElement.scrollHeight")
                        clip_h = min(height, args.max_height)
                        page.screenshot(
                            path=f"{base}__full.png",
                            full_page=True,
                            clip={"x": 0, "y": 0, "width": VIEWPORTS[vp_name]["width"], "height": clip_h},
                        )
                        note = f" (cut at {clip_h}px of {height}px)" if height > clip_h else ""
                        log.append(f"{base}__full.png <- {url}{note}")
            context.close()
        browser.close()

    (out / "index.txt").write_text("\n".join(log) + "\n")
    errors = [line for line in log if line.startswith("ERROR")]
    print(f"{len(log) - len(errors)} screenshots in {out}, {len(errors)} errors. See {out / 'index.txt'}")
    for e in errors:
        print(e)
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
