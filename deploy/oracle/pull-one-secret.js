// Reveal ONE stubborn Render env var and merge it into an existing .env.
// The bulk puller used a flat delay; this polls until the value actually
// arrives, which is what a lazily-fetched secret needs.
// Values are written to disk and never printed.
//
// Usage: node pull-one-secret.js <KEY_NAME> <envFilePath>
import { readFileSync, writeFileSync, chmodSync, existsSync } from 'node:fs';

const [KEY, OUT] = process.argv.slice(2);
if (!KEY || !OUT) { console.error('usage: node pull-one-secret.js <KEY_NAME> <envFilePath>'); process.exit(1); }

const targets = await (await fetch('http://127.0.0.1:9222/json/list')).json();
const page = targets.filter(t => t.type === 'page')
  .find(t => (t.url || '').includes('dashboard.render.com'));
if (!page) { console.error('Render tab not found - is the debug Chrome still open on the env page?'); process.exit(1); }

const ws = new WebSocket(page.webSocketDebuggerUrl);
let id = 0; const pending = new Map();
await new Promise((res, rej) => { ws.onopen = res; ws.onerror = () => rej(new Error('ws fail')); });
ws.onmessage = (ev) => { const m = JSON.parse(ev.data); if (m.id && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id); } };
const send = (method, params = {}) => new Promise((res, rej) => {
  const myId = ++id;
  const timer = setTimeout(() => rej(new Error(method + ' timeout')), 90000);
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
  const keyEl = [...document.querySelectorAll('input')]
    .find(i => /envVarKey/.test(i.name || '') && i.value === ${JSON.stringify(KEY)});
  if (!keyEl) return JSON.stringify({ error: 'key row not found' });

  const row = keyEl.closest('tr');
  const ta  = row.querySelector('textarea');
  if (!ta) return JSON.stringify({ error: 'no textarea in row' });

  row.scrollIntoView({ block: 'center' });
  await sleep(400);

  // Match show OR hide: if the row somehow got toggled already, a blind
  // "show" click would have hidden it again.
  const btn = [...row.querySelectorAll('button')]
    .find(b => /secret/i.test(b.getAttribute('aria-label') || ''));
  if (!btn) return JSON.stringify({ error: 'no secret toggle button' });

  for (let attempt = 0; attempt < 3; attempt++) {
    if (ta.value) break;
    btn.click();
    // Poll up to 8s for the lazily-fetched value to land.
    for (let i = 0; i < 40; i++) {
      await sleep(200);
      if (ta.value) break;
    }
  }

  return JSON.stringify({
    key: keyEl.value,
    value: ta.value || '',
    state: ta.getAttribute('data-state') || '',
    aria: btn.getAttribute('aria-label') || ''
  });
})()`;

try {
  const res = JSON.parse(await evaluate(script));
  if (res.error) { console.error('FAILED:', res.error); process.exitCode = 1; }
  else if (!res.value) {
    console.error(`still empty (state=${res.state}, button="${res.aria}")`);
    console.error('Fall back to the "Copy value" button on that row and paste it in by hand.');
    process.exitCode = 1;
  } else {
    let text = existsSync(OUT) ? readFileSync(OUT, 'utf8') : '';
    const line = `${res.key}=${res.value}`;
    const re = new RegExp(`^${res.key}=.*$`, 'm');
    text = re.test(text) ? text.replace(re, line) : text.trimEnd() + '\n' + line + '\n';
    writeFileSync(OUT, text, { mode: 0o600 });
    chmodSync(OUT, 0o600);
    console.log(`${res.key}: ${res.value.length} chars -> merged into ${OUT}`);
  }
} catch (e) { console.error('FAILED:', e.message); process.exitCode = 1; }
ws.close();
