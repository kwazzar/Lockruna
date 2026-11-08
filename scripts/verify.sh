#!/usr/bin/env bash
# Runs the checks listed in AGENTS.md. Pass --ios to add the iOS simulator tests.
set -euo pipefail

cd "$(dirname "$0")/.."

if [ -z "${JAVA_HOME:-}" ] && [ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]; then
  export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
fi

GRADLE="./gradlew"

echo "==> Tests"
$GRADLE \
  :api-contract:jvmTest \
  :shared:domain:jvmTest \
  :shared:application:jvmTest \
  :shared:presentation:jvmTest \
  :shared:data:jvmTest \
  :architecture-tests:test \
  :server:test

echo "==> Lint"
$GRADLE ktlintCheck detekt

echo "==> Android"
$GRADLE :androidApp:assembleDebug

if [ "${1:-}" = "--ios" ]; then
  if [ "$(uname)" != "Darwin" ]; then
    echo "The iOS simulator tests need macOS." >&2
    exit 1
  fi

  echo "==> iOS"
  $GRADLE :shared:umbrella:assemble :shared:data:iosSimulatorArm64Test
fi

echo "==> All checks passed"
