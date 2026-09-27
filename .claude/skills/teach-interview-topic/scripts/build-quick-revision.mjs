// Builds QUICK-REVISION.md in the project root: every lesson's Quick Revision, in lesson order.
//
// Sources:
//   .md files          -> the "## ⚡ Quick Revision" section, up to the end of the file
//   .java / .sql files -> the lines between "QUICK REVISION START" and "QUICK REVISION END".
//                         The block's first line is its title, e.g. "D01 Two Sum: ..." gives "D01 Two Sum".
//
// Run from the project root (no npm packages needed):
//   node .claude/skills/teach-interview-topic/scripts/build-quick-revision.mjs
import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const sectionFolders = fs.readdirSync(root)
  .filter(name => /^\d\d-/.test(name) && fs.statSync(path.join(root, name)).isDirectory())
  .sort();

function fromMarkdown(text) {
  const start = text.search(/^## ⚡ Quick Revision.*$/m);
  if (start < 0) return null;
  const title = (text.match(/^# (.+)$/m) || [null, null])[1];
  const body = text.slice(start).split('\n').slice(1).join('\n').trim();   // drop the heading line itself
  return { title, body };
}

function fromCode(text) {
  const match = text.match(/QUICK REVISION START[^\n]*\n([\s\S]*?)\n[^\n]*QUICK REVISION END/);
  if (!match) return null;
  const lines = match[1].split('\n').map(line => line
    .replace(/\r$/, '')
    .replace(/^\s*\/?\*\s?/, '')     // " * text" in Java block comments
    .replace(/^\s*--\s?/, '')        // "-- text" in SQL
    .replace(/^\s*\/\/\s?/, '')      // "// text"
    .replace(/\s+$/, ''));
  const first = lines.find(l => l.trim() !== '') || '';
  const title = first.split(':')[0].trim();
  return { title, body: '```text\n' + lines.join('\n').trim() + '\n```' };
}

const entries = [];
for (const folder of sectionFolders) {
  for (const name of fs.readdirSync(path.join(root, folder)).sort()) {
    const text = fs.readFileSync(path.join(root, folder, name), 'utf8');
    let found = null;
    if (name.endsWith('.md')) found = fromMarkdown(text);
    else if (name.endsWith('.java') || name.endsWith('.sql')) found = fromCode(text);
    if (!found) continue;
    const id = name.replace(/\.(md|java|sql)$/, '').toLowerCase().replace(/[^a-z0-9]+/g, '-');
    entries.push({
      id,
      file: `${folder}/${name}`,
      title: found.title || name,
      body: found.body,
    });
  }
}

let out = '# ⚡ Quick Revision: every lesson on one page\n\n';
out += '> Read this in the last 2 hours before the interview. For each topic, look at the diagram, read the facts, then **cover the screen and answer the "say it aloud" questions**. Recalling beats rereading.\n>\n';
out += '> This file is generated from the lessons. Run `node .claude/skills/teach-interview-topic/scripts/build-quick-revision.mjs` after changing one.\n\n';
out += '## Contents\n\n';
for (const e of entries) out += `- [${e.title}](#${e.id})\n`;
out += '\n---\n';
for (const e of entries) {
  out += `\n<a id="${e.id}"></a>\n\n## ${e.title}\n\n[Full lesson](${e.file}) · [Back to contents](#contents)\n\n${e.body}\n\n---\n`;
}
fs.writeFileSync(path.join(root, 'QUICK-REVISION.md'), out);
console.log(`QUICK-REVISION.md written with ${entries.length} topics:`);
for (const e of entries) console.log(`  #${e.id.padEnd(45)} ${e.title}`);
