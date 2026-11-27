#!/usr/bin/env bash
# Runs the naive agent in a separate, headless Claude Code session, outside the repo,
# so that it gets neither the repo's CLAUDE.md nor the project memory: only the docs.
#
# Usage: run-naive.sh <run-dir> <scenario>
#   <run-dir>   new or empty directory, e.g. <scratch>/naive-1
#   <scenario>  the page or goal to follow, as given to the naive agent
# Env:
#   NAIVE_MODEL  model for the session (default: the user's default model)
#
# Writes <run-dir>/report.md. The repo is only read.

set -euo pipefail

RUN=${1:?run dir missing}
SCENARIO=${2:?scenario missing}
REPO=$(cd "$(dirname "$0")/../../.." && pwd)
DISTRIB="$REPO/core/jeka-output/distrib"

[ -x "$DISTRIB/bin/jeka" ] || { echo "No local JeKa build at $DISTRIB: bootstrap it first (see CLAUDE.md)." >&2; exit 1; }
mkdir -p "$RUN"
[ -z "$(ls -A "$RUN")" ] || { echo "$RUN is not empty." >&2; exit 1; }
RUN=$(cd "$RUN" && pwd)

# The docs, as a reader sees them: the generated ones when they exist.
mkdir -p "$RUN/work/site/plugins" "$RUN/work/runs" "$RUN/home"
if [ -d "$REPO/jeka-output/mkdocs/docs" ]; then
  cp -R "$REPO/jeka-output/mkdocs/docs" "$RUN/work/site/docs"
  DOCS_NOTE="generated (the KBean reference pages are filled in)"
else
  cp -R "$REPO/docs" "$RUN/work/site/docs"
  DOCS_NOTE="raw sources: the KBean reference placeholders are not filled in"
fi
rm -rf "$RUN/work/site/docs/adrs"
cp "$REPO/mkdocs.yml" "$RUN/work/site/"
for p in "$REPO"/plugins/plugins.*; do
  if [ -f "$p/README.md" ]; then
    mkdir -p "$RUN/work/site/plugins/$(basename "$p")"
    cp "$p/README.md" "$RUN/work/site/plugins/$(basename "$p")/"
  fi
done

# JeKa, copied so that no path leads back to the repo.
cp -R "$DISTRIB" "$RUN/jeka"

# Isolated JeKa home, reusing the JDKs the user already has (read only) instead of downloading them.
: > "$RUN/home/global.properties"
for d in "$HOME"/.jeka/cache/jdks/temurin-*; do
  [ -d "$d" ] || continue
  v=${d##*temurin-}
  if [ -x "$d/bin/java" ]; then echo "jeka.jdk.$v=$d" >> "$RUN/home/global.properties"
  elif [ -x "$d/Contents/Home/bin/java" ]; then echo "jeka.jdk.$v=$d/Contents/Home" >> "$RUN/home/global.properties"
  fi
done

# The agent: naive.md, with the content of RULES.md appended.
python3 -I - "$REPO/.claude/agents/naive.md" "$REPO/.claude/RULES.md" "$RUN/agents.json" <<'EOF'
import sys, json, re
src, rules, out = sys.argv[1:4]
m = re.match(r'---\n(.*?)\n---\n(.*)', open(src).read(), re.S)
meta = dict(l.split(': ', 1) for l in m.group(1).splitlines())
body = m.group(2) + "\n\n# Content of .claude/RULES.md (already given: don't look for the file)\n\n" + open(rules).read()
json.dump({"naive": {"description": meta["description"], "prompt": body,
                     "tools": [t.strip() for t in meta["tools"].split(",")]}}, open(out, "w"))
EOF

cat > "$RUN/prompt.txt" <<EOF
Scenario: $SCENARIO

Setup for this run (it replaces the paths of your instructions):
- The documentation is in ./site: pages in ./site/docs ($DOCS_NOTE), the site menu in ./site/mkdocs.yml,
  plugin docs in ./site/plugins/*/README.md.
- JeKa is installed at $RUN/jeka/bin: put it first on PATH. It is a build of the current docs' version.
- Your scratch directory is $RUN/work/runs. JEKA_USER_HOME is $RUN/home: keep it, and keep its global.properties.
- Projects you scaffold pin the latest JeKa release, run by their ./jeka script. When a step fails with it, run the
  same step again with the jeka on PATH ($RUN/jeka/bin/jeka), and report both results: the docs may describe
  something not released yet.

Write your report in English.
EOF

MODEL_ARGS=()
[ -n "${NAIVE_MODEL:-}" ] && MODEL_ARGS=(--model "$NAIVE_MODEL")

cd "$RUN/work"
# ${a[@]+...}: an empty array is an error under 'set -u' in the bash 3.2 of macOS.
JEKA_USER_HOME="$RUN/home" claude -p --agents "$RUN/agents.json" --agent naive ${MODEL_ARGS[@]+"${MODEL_ARGS[@]}"} \
  --permission-prompts none --allowedTools Bash Read Write Glob Grep \
  < "$RUN/prompt.txt" > "$RUN/report.md"
echo "Report: $RUN/report.md"
