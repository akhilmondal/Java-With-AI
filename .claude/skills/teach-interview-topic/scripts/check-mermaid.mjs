// Checks every ```mermaid block in the given .md files (or folders) with Mermaid's real parser.
//
// Setup (once per session), in a scratch folder:
//   npm install mermaid jsdom
//   copy this file into that folder
// Run:
//   node --no-warnings check-mermaid.mjs <file-or-folder> [...]
// Exit code 1 if any diagram fails, with the parser's error message.
import { JSDOM } from 'jsdom';
import fs from 'node:fs';
import path from 'node:path';

const dom = new JSDOM('<!doctype html><html><body></body></html>', { pretendToBeVisual: true });
for (const key of ['window', 'document', 'navigator', 'DOMParser', 'Element', 'HTMLElement', 'Node', 'SVGElement', 'MutationObserver']) {
  if (dom.window[key] !== undefined && globalThis[key] === undefined) {
    try { globalThis[key] = dom.window[key]; } catch { /* some globals are read-only */ }
  }
}

const { default: mermaid } = await import('mermaid');
mermaid.initialize({ startOnLoad: false });

function collectFiles(target) {
  const stat = fs.statSync(target);
  if (stat.isFile()) return target.endsWith('.md') ? [target] : [];
  return fs.readdirSync(target)
    .filter(name => name !== 'node_modules' && !name.startsWith('.'))
    .flatMap(name => collectFiles(path.join(target, name)));
}

const files = process.argv.slice(2).flatMap(collectFiles);
let total = 0;
let failed = 0;
for (const file of files) {
  const text = fs.readFileSync(file, 'utf8');
  const blocks = [...text.matchAll(/```mermaid\r?\n([\s\S]*?)```/g)].map(m => m[1]);
  for (let i = 0; i < blocks.length; i++) {
    total++;
    const firstLine = blocks[i].trim().split('\n')[0];
    try {
      await mermaid.parse(blocks[i]);
      console.log(`OK    ${path.basename(file)} #${i + 1} (${firstLine})`);
    } catch (e) {
      failed++;
      console.log(`FAIL  ${path.basename(file)} #${i + 1} (${firstLine})\n      ${String(e.message || e).split('\n').slice(0, 4).join('\n      ')}`);
    }
  }
}
console.log(`\n${total} diagrams checked, ${failed} failed`);
process.exit(failed ? 1 : 0);
