#!/bin/bash

# Create test structures
TEST_DIR="/tmp/jeka_jdk_test"
rm -rf "$TEST_DIR"

echo "Testing JDK structure detection..."

# Test 1: Standard structure
echo -e "\n--- Test 1: Standard JDK structure ---"
mkdir -p "$TEST_DIR/standard/jdk-21/bin"
touch "$TEST_DIR/standard/jdk-21/bin/javac"
touch "$TEST_DIR/standard/jdk-21/bin/java"
chmod +x "$TEST_DIR/standard/jdk-21/bin/"*

pushd "$TEST_DIR/standard" > /dev/null
bin_dir=$(find "." -maxdepth 3 -type d -name "bin" | head -n 1)
jdk_root=$(dirname "$bin_dir")
echo "Found bin at: $bin_dir"
echo "JDK root: $jdk_root"
[ -f "$jdk_root/bin/javac" ] && echo "✅ PASS: javac found" || echo "❌ FAIL: javac not found"
popd > /dev/null

# Test 2: Azul 8 nested structure
echo -e "\n--- Test 2: Azul 8 nested structure ---"
mkdir -p "$TEST_DIR/azul8/zulu8.12.0.1/zulu-8.jdk/bin"
touch "$TEST_DIR/azul8/zulu8.12.0.1/zulu-8.jdk/bin/javac"
touch "$TEST_DIR/azul8/zulu8.12.0.1/zulu-8.jdk/bin/java"
chmod +x "$TEST_DIR/azul8/zulu8.12.0.1/zulu-8.jdk/bin/"*

pushd "$TEST_DIR/azul8" > /dev/null
bin_dir=$(find "." -maxdepth 3 -type d -name "bin" | head -n 1)
jdk_root=$(dirname "$bin_dir")
echo "Found bin at: $bin_dir"
echo "JDK root: $jdk_root"
[ -f "$jdk_root/bin/javac" ] && echo "✅ PASS: javac found" || echo "❌ FAIL: javac not found"
popd > /dev/null

# Test 3: macOS structure
echo -e "\n--- Test 3: macOS structure ---"
mkdir -p "$TEST_DIR/macos/jdk-21/Contents/Home/bin"
touch "$TEST_DIR/macos/jdk-21/Contents/Home/bin/javac"
touch "$TEST_DIR/macos/jdk-21/Contents/Home/bin/java"
chmod +x "$TEST_DIR/macos/jdk-21/Contents/Home/bin/"*

pushd "$TEST_DIR/macos" > /dev/null
bin_dir=$(find "." -type d -path "*/Contents/Home/bin" | head -n 1)
jdk_root=$(dirname "$bin_dir")
echo "Found bin at: $bin_dir"
echo "JDK root: $jdk_root"
[ -f "$jdk_root/bin/javac" ] && echo "✅ PASS: javac found" || echo "❌ FAIL: javac not found"
popd > /dev/null

# Cleanup
rm -rf "$TEST_DIR"
echo -e "\n✅ All tests complete!"
