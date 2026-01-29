#!/bin/bash

echo "================================"
echo "Testing JDK Structure Detection"
echo "================================"

# Simulate the fixed logic
test_structure() {
  local test_name="$1"
  local test_dir="$2"
  
  echo -e "\n--- $test_name ---"
  
  pushd "$test_dir" > /dev/null 2>&1
  
  # This is your fix logic
  local bin_dir
  bin_dir=$(find "." -maxdepth 3 -type d -name "bin" | head -n 1)
  
  if [ -z "$bin_dir" ]; then
    echo "❌ FAIL: Could not find bin directory"
    popd > /dev/null 2>&1
    return 1
  fi
  
  local jdk_root
  jdk_root=$(dirname "$bin_dir")
  
  if [ ! -f "$jdk_root/bin/javac" ]; then
    echo "❌ FAIL: javac not found in $jdk_root/bin"
    popd > /dev/null 2>&1
    return 1
  fi
  
  echo "✅ PASS: Found bin at $bin_dir"
  echo "   JDK root: $jdk_root"
  echo "   javac found: $jdk_root/bin/javac"
  
  popd > /dev/null 2>&1
  return 0
}

# Create test directories
TEST_BASE="/tmp/jeka_test_$$"
rm -rf "$TEST_BASE"
mkdir -p "$TEST_BASE"

echo "Creating test JDK structures..."

# Test 1: Standard structure (like Temurin)
echo "Setting up Test 1: Standard structure"
mkdir -p "$TEST_BASE/standard/jdk-21.0.1/bin"
touch "$TEST_BASE/standard/jdk-21.0.1/bin/javac"
touch "$TEST_BASE/standard/jdk-21.0.1/bin/java"
chmod +x "$TEST_BASE/standard/jdk-21.0.1/bin/"*

# Test 2: Azul 8 nested structure (THE PROBLEM CASE)
echo "Setting up Test 2: Azul 8 nested structure"
mkdir -p "$TEST_BASE/azul8/zulu8.78.0.19-ca-jdk8.0.412/zulu-8.jdk/bin"
touch "$TEST_BASE/azul8/zulu8.78.0.19-ca-jdk8.0.412/zulu-8.jdk/bin/javac"
touch "$TEST_BASE/azul8/zulu8.78.0.19-ca-jdk8.0.412/zulu-8.jdk/bin/java"
chmod +x "$TEST_BASE/azul8/zulu8.78.0.19-ca-jdk8.0.412/zulu-8.jdk/bin/"*

# Test 3: Triple nested (edge case)
echo "Setting up Test 3: Triple nested structure"
mkdir -p "$TEST_BASE/triple/level1/level2/level3/bin"
touch "$TEST_BASE/triple/level1/level2/level3/bin/javac"
touch "$TEST_BASE/triple/level1/level2/level3/bin/java"
chmod +x "$TEST_BASE/triple/level1/level2/level3/bin/"*

echo -e "\n================================"
echo "Running Tests"
echo "================================"

# Run tests
test_structure "Test 1: Standard JDK (Temurin-like)" "$TEST_BASE/standard"
TEST1=$?

test_structure "Test 2: Azul 8 Nested Structure (THE FIX)" "$TEST_BASE/azul8"
TEST2=$?

test_structure "Test 3: Triple Nested (Edge Case)" "$TEST_BASE/triple"
TEST3=$?

# Cleanup
rm -rf "$TEST_BASE"

# Summary
echo -e "\n================================"
echo "Test Summary"
echo "================================"
[ $TEST1 -eq 0 ] && echo "✅ Standard structure: PASS" || echo "❌ Standard structure: FAIL"
[ $TEST2 -eq 0 ] && echo "✅ Azul 8 structure: PASS" || echo "❌ Azul 8 structure: FAIL"
[ $TEST3 -eq 0 ] && echo "✅ Triple nested: PASS" || echo "❌ Triple nested: FAIL"

if [ $TEST1 -eq 0 ] && [ $TEST2 -eq 0 ] && [ $TEST3 -eq 0 ]; then
  echo -e "\n🎉 All tests PASSED! Your fix works!"
  exit 0
else
  echo -e "\n❌ Some tests FAILED. Review the output above."
  exit 1
fi
