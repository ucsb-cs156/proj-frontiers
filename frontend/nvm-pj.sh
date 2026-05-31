# ==============================================================================
# USAGE INSTRUCTIONS:
# This script modifies your current shell environment to switch Node versions.
# Because of this, running it normally (e.g., './nvm-pj.sh') WILL NOT WORK.
#
# You MUST source the script so it executes in your current terminal session:
#   source ./nvm-pj.sh   OR   . ./nvm-pj.sh
# ==============================================================================

if ! command -v python3 >/dev/null 2>&1; then
  echo "❌ Error: python3 is required to parse package.json"
  echo "   Install python3 with your system package manager and try again"
  return 1 2>/dev/null || exit 1
fi

CLEAN_VERSION=$(python3 <<'PY'
import json
import re

spec = ""
try:
    with open("package.json", encoding="utf-8") as package_json:
        spec = json.load(package_json).get("engines", {}).get("node", "")
except Exception:
    pass

match = re.search(r"(\d+\.\d+\.\d+)", str(spec))
print(match.group(1) if match else "")
PY
)

if [ -z "$CLEAN_VERSION" ]; then
  echo "❌ Error: Could not find 'node' engine in package.json in the current directory (run this from frontend/)"
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

if ! nvm use "$CLEAN_VERSION"; then
  echo "❌ Error: Node version $CLEAN_VERSION is not installed in nvm"
  echo "   Run: nvm install $CLEAN_VERSION"
  return 1 2>/dev/null || exit 1
fi
