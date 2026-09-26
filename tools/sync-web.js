#!/usr/bin/env node
/* Copies the shipped PWA files into www/, which is what the Android
   (Capacitor) build packages as its WebView assets. index.html stays the
   single source of truth; this script is the only thing that touches www/. */
const fs = require('fs');
const path = require('path');

const ROOT = path.join(__dirname, '..');
const WWW = path.join(ROOT, 'www');
const FILES = ['index.html', 'sw.js', 'manifest.json', 'icon-192.png', 'icon-512.png'];

fs.mkdirSync(WWW, { recursive: true });
for (const file of FILES) {
  fs.copyFileSync(path.join(ROOT, file), path.join(WWW, file));
}
console.log('Synced ' + FILES.length + ' files into www/');
