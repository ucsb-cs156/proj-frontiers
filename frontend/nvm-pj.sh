#!/bin/bash

# ==============================================================================
# USAGE INSTRUCTIONS:
# This script modifies your current shell environment to switch Node versions.
# Because of this, running it normally (e.g., './nvm-pj.sh') WILL NOT WORK.
#
# You MUST source the script so it executes in your current terminal session:
#   source ./nvm-pj.sh   OR   . ./nvm-pj.sh
# ==============================================================================

RAW_VERSION=$(node -p "require('./package.json').engines?.node || ''")
CLEAN_VERSION=$(printf '%s' "$RAW_VERSION" | sed 's/[\^~v]//g')

if [ -z "$CLEAN_VERSION" ]; then
  echo "❌ Error: Could not find 'node' engine defined in package.json"
  return 1 2>/dev/null || exit 1
fi

echo "✅ Found Node version target: $CLEAN_VERSION"

export NVM_DIR="$HOME/.nvm"
if [ -s "$NVM_DIR/nvm.sh" ]; then
  . "$NVM_DIR/nvm.sh"
else
  echo "❌ Error: NVM directory not found at $NVM_DIR"
  return 1 2>/dev/null || exit 1
fi

nvm use "$CLEAN_VERSION"
