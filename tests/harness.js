/* Loads the real index.html, runs its script in a VM with a small DOM stub, and
   hands back the App object. No dependencies: node tests/run.js is the whole story.

   The DOM stub is deliberately thin. It exists so the render functions run for real
   against the real element ids, which is what catches a renderer reaching for an
   element the markup does not have. It is not a browser and does not pretend to be:
   layout, CSS and events are out of scope here and are checked by hand in Chrome. */
const fs = require('fs');
const path = require('path');
const vm = require('vm');

const ROOT = path.join(__dirname, '..');

function parseIds(html) {
  const ids = new Set();
  const re = /id="([a-zA-Z0-9_-]+)"/g;
  let m;
  while ((m = re.exec(html)) !== null) ids.add(m[1]);
  return ids;
}

class El {
  constructor(id) {
    this.id = id;
    this._html = '';
    this._text = '';
    this.value = '';
    this.style = {};
    this.dataset = {};
    this.children = [];
    this.classList = {
      _s: new Set(),
      add: (c) => this.classList._s.add(c),
      remove: (c) => this.classList._s.delete(c),
      toggle: (c, on) => (on ? this.classList._s.add(c) : this.classList._s.delete(c)),
      contains: (c) => this.classList._s.has(c),
    };
  }
  get innerHTML() { return this._html; }
  set innerHTML(v) {
    if (v === undefined || v === null) throw new Error('innerHTML set to ' + v + ' on #' + this.id);
    if (String(v).includes('undefined')) throw new Error('innerHTML on #' + this.id + ' contains the string "undefined": ' + String(v).slice(0, 200));
    this._html = String(v);
  }
  get textContent() { return this._text; }
  set textContent(v) {
    if (v === undefined || v === null) throw new Error('textContent set to ' + v + ' on #' + this.id);
    this._text = String(v);
  }
  appendChild(c) { this.children.push(c); return c; }
  remove() {}
  click() { this.clicked = true; }
  addEventListener() {}
}

function makeDocument(ids) {
  const els = new Map();
  for (const id of ids) els.set(id, new El(id));
  return {
    _els: els,
    getElementById(id) { return els.has(id) ? els.get(id) : null; },
    querySelectorAll() { return []; },
    querySelector() { return null; },
    createElement() { return new El('created'); },
    addEventListener() {},
    body: { appendChild() {} },
  };
}

function load(opts) {
  opts = opts || {};
  const html = fs.readFileSync(path.join(ROOT, 'index.html'), 'utf8');
  const script = /<script>([\s\S]*)<\/script>/.exec(html);
  if (!script) throw new Error('no inline <script> found in index.html');

  const store = Object.assign({}, opts.localStorage || {});
  const downloads = [];
  const alerts = [];

  const doc = makeDocument(parseIds(html));
  const sandbox = {
    console,
    document: doc,
    window: { addEventListener() {}, scrollTo() {} },
    scrollTo() {},
    history: { state: null, pushState() {}, replaceState() {} },
    navigator: {},
    location: { reload() { sandbox.__reloaded = true; } },
    alert: (m) => alerts.push(m),
    confirm: () => true,
    setTimeout: () => 0,
    TextEncoder, TextDecoder,
    localStorage: {
      getItem: (k) => (k in store ? store[k] : null),
      setItem: (k, v) => { store[k] = String(v); },
      removeItem: (k) => { delete store[k]; },
    },
    Blob: class { constructor(parts, o) { this.parts = parts; this.type = o && o.type; } },
    URL: { createObjectURL: (b) => { downloads.push(b); return 'blob:test'; }, revokeObjectURL() {} },
    FileReader: class {
      readAsText(f) { this.result = f._text; if (this.onload) this.onload(); }
    },
  };
  sandbox.globalThis = sandbox;

  const ctx = vm.createContext(sandbox);
  // opts.now pins "now" (a local time, e.g. '2026-09-27T10:00') so date bugs that
  // only show on some days of the year can be tested on any day. new Date(...)
  // with arguments still behaves normally.
  if (opts.now) {
    const now = JSON.stringify(opts.now);
    vm.runInContext(
      'const __RealDate = Date;' +
      'globalThis.Date = class extends __RealDate {' +
      '  constructor(...a) { if (a.length === 0) super(' + now + '); else super(...a); }' +
      '  static now() { return new __RealDate(' + now + ').getTime(); }' +
      '};', ctx);
  }
  // DOMContentLoaded never fires here; tests call App.init() when they want it.
  // Top-level `const` in a VM script does not land on the global, so publish
  // the bindings the tests need onto an explicit object at the end.
  const source = script[1].replace("window.addEventListener('DOMContentLoaded',App.init);", '') +
    '\n;globalThis.__app={App:App,MONTHS:MONTHS,SURAHS:SURAHS,AYAHS:AYAHS,WEEKS:WEEKS,' +
    'DUAS:DUAS,DUA_TEXTS:DUA_TEXTS,BISMILLAH:BISMILLAH,NAMES:NAMES,FOOD:FOOD,DUA_TODAY:DUA_TODAY};';
  vm.runInContext(source, ctx, { filename: 'index.html' });

  const A = sandbox.__app;
  if (!A || !A.App || !A.App.__test) throw new Error('index.html did not expose App.__test — is the test hook still there?');

  return {
    ctx, doc, alerts, downloads, store,
    App: A.App,
    T: A.App.__test,
    data: A,
    html,
    downloadText: () => downloads.map((b) => b.parts.join('')),
  };
}

module.exports = { load, ROOT };
