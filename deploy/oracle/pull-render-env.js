// Pull Render env vars into a local .env file.
// Values are written to disk and NEVER printed — only key names and masked lengths.
// Usage: node pullenv.js <outputPath>
import { writeFileSync, chmodSync } from 'node:fs';

const OUT = process.argv[2];
if (!OUT) { console.error('usage: node pullenv.js <outputPath>'); process.exit(1); }

const targets = await (await fetch('http://127.0.0.1:9222/json/list')).json();
const page = targets.filter(t => t.type === 'page')
  .find(t => (t.url || '').includes('dashboard.render.com'));
if (!page) { console.error('Render tab not found'); process.exit(1); }

const ws = new WebSocket(page.webSocketDebuggerUrl);
let id = 0; const pending = new Map();
await new Promise((res, rej) => { ws.onopen = res; ws.onerror = () => rej(new Error('ws fail')); });
ws.onmessage = (ev) => { const m = JSON.parse(ev.data); if (m.id && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id); } };
const send = (method, params = {}) => new Promise((res, rej) => {
  const myId = ++id;
  const timer = setTimeout(() => rej(new Error(method + ' timeout')), 60000);
  pending.set(myId, (m) => { clearTimeout(timer); m.error ? rej(new Error(JSON.stringify(m.error))) : res(m.result); });
  ws.send(JSON.stringify({ id: myId, method, params }));
});
const evaluate = async (expr) => {
  const r = await send('Runtime.evaluate', { expression: expr, returnByValue: true, awaitPromise: true });
  if (r.exceptionDetails) throw new Error(r.exceptionDetails.text + ' ' + (r.exceptionDetails.exception?.description || ''));
  return r.result?.value;
};

const script = `(async () => {
  const sleep = ms => new Promise(r => setTimeout(r, ms));
  const rows = [...document.querySelectorAll('input')]
    .filter(i => /envVarKey/.test(i.name || ''))
    .map(i => i.closest('tr'))
    .filter(Boolean);

  // Reveal every masked value first.
  for (const row of rows) {
    const btn = [...row.querySelectorAll('button')]
      .find(b => /show secret/i.test(b.getAttribute('aria-label') || ''));
    if (btn) { btn.click(); await sleep(60); }
  }
  await sleep(1200);

  const out = [];
  for (const row of rows) {
    const keyEl = row.querySelector('input[name*=envVarKey]');
    const ta = row.querySelector('textarea');
    if (!keyEl) continue;
    out.push({
      key: keyEl.value,
      value: ta ? ta.value : '',
      state: ta ? (ta.getAttribute('data-state') || '') : 'no-textarea'
    });
  }
  return JSON.stringify(out);
})()`;

try {
  const raw = await evaluate(script);
  const rows = JSON.parse(raw);

  const got = rows.filter(r => r.key && r.value);
  const empty = rows.filter(r => r.key && !r.value);

  const header = `# Pulled from Render ${new Date().toISOString()}
# service srv-d9l2vj0u01pc73ep668g  (koderhyre)
# Values are live production secrets. This file is gitignored - keep it that way.
`;
  const body = got.map(r => `${r.key}=${r.value}`).join('\n');
  const extra = `
# --- not present on Render; required for the Oracle deployment ---
APP_BASE_URL=https://koderhyre.com
ADMIN_USERNAME=admin
`;
  writeFileSync(OUT, header + body + '\n' + extra, { mode: 0o600 });
  chmodSync(OUT, 0o600);

  console.log(`wrote ${got.length} variables to ${OUT} (mode 600)\n`);
  console.log('key                          chars');
  console.log('---------------------------- -----');
  for (const r of got) console.log(r.key.padEnd(28), String(r.value.length).padStart(5));
  if (empty.length) {
    console.log('\nEMPTY / NOT REVEALED (' + empty.length + '):');
    for (const r of empty) console.log('  ' + r.key + '  [state=' + r.state + ']');
  }
} catch (e) { console.error('FAILED:', e.message); process.exitCode = 1; }
ws.close();
