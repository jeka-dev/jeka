#!/bin/bash
# Entry point for developers, AI agents, and CI to build and run JeKa purely from source,
# with no prior JeKa or build tool installation required.
#
# It compiles JeKa from scratch using only the JDK, then hands off to the compiled JeKa
# to execute the given arguments (e.g. 'project: pack', 'base: test').
#
# Usage: ./bootstrap.sh <jeka arguments>
# Example: ./bootstrap.sh project: pack --clean --inspect --stacktrace
set -euo pipefail

BIN=".jeka-work/bootstrap"

# JARs needed to compile JeKa from scratch
COMPILE_CP=$(find core/jeka-boot core/libs/compile-only -name "*.jar" | tr '\n' ':')

echo "Preparing output directory $BIN..."
mkdir -p "$(dirname "$BIN")"
rm -rf "$BIN"
mkdir -p "$BIN"

echo "Compiling JeKa sources..."
SOURCES_FILE=$(mktemp)
find core/src/main/java core/jeka-src plugins/plugins.nexus -name "*.java" > "$SOURCES_FILE"
SOURCE_COUNT=$(wc -l < "$SOURCES_FILE")
javac -encoding UTF-8 --release 21 -d "$BIN" -cp "$COMPILE_CP" "@$SOURCES_FILE"
rm -f "$SOURCES_FILE"
echo "$SOURCE_COUNT source files compiled."

echo "Copying resources..."
find core/src/main/java -type f ! -name "*.java" | while IFS= read -r f; do
    rel="${f#core/src/main/java/}"
    mkdir -p "$BIN/$(dirname "$rel")"
    cp "$f" "$BIN/$rel"
done

echo "Launching JeKa..."
TEST_CP=$(find core/libs/test -name "*.jar" | tr '\n' ':')
java -cp "$BIN:$COMPILE_CP:$TEST_CP" dev.jeka.core.tool.Main "$@"
