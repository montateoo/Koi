#!/usr/bin/env bash
# Netlify build: assembles the deployable site in netlify/dist from KoiCalculator/.
# Only what the browser needs is copied; the extraction scripts, the rulebook PDF and the Java sources stay out.
set -euo pipefail
cd "$(dirname "$0")"

SRC=../KoiCalculator
rm -rf dist
mkdir dist

cp "$SRC/index.html" dist/
cp -r "$SRC/img" "$SRC/rules" dist/

# The database URL can come from a Netlify environment variable (Site configuration > Environment variables),
# so it does not have to be committed. Without it, the config.js in the repository is used as is.
if [ -n "${KOI_SYNC_URL:-}" ]; then
  printf 'window.KOI_SYNC_URL = "%s";\n' "$KOI_SYNC_URL" > dist/config.js
else
  cp "$SRC/config.js" dist/
fi

echo "Built netlify/dist: $(find dist -type f | wc -l) files, $(du -sh dist | cut -f1)"
