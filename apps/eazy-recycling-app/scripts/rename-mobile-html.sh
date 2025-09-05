#!/bin/bash

# Script to rename mobile.html to index.html after build
MOBILE_HTML="dist/mobile/mobile.html"
INDEX_HTML="dist/mobile/index.html"

if [ -f "$MOBILE_HTML" ]; then
  # Copy the content to index.html
  cp "$MOBILE_HTML" "$INDEX_HTML"
  
  # Remove the original mobile.html file
  rm "$MOBILE_HTML"
  
  echo "Successfully renamed mobile.html to index.html"
  exit 0
else
  echo "Error: mobile.html not found in dist/mobile directory"
  exit 1
fi
