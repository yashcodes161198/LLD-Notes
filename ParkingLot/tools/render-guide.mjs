import { readFile, writeFile } from 'node:fs/promises';
import { fileURLToPath, pathToFileURL } from 'node:url';
import path from 'node:path';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const packageRoot = process.argv[2];
if (!packageRoot) throw new Error('Pass an absolute node_modules directory containing marked.');
const { marked } = await import(pathToFileURL(path.join(packageRoot, 'marked/lib/marked.esm.js')));
let markdown = await readFile(path.join(root, 'STUDY_GUIDE.rendered.md'), 'utf8');
let diagramCount = 0;
for (const match of [...markdown.matchAll(/!\[diagram\]\(([^)]+)\)/g)]) {
  const svg = await readFile(path.resolve(root, match[1]), 'utf8');
  const data = Buffer.from(svg).toString('base64');
  diagramCount++;
  markdown = markdown.replace(match[0], `<figure class="diagram"><button type="button" class="diagram-button" aria-label="Enlarge UML diagram ${diagramCount}"><img src="data:image/svg+xml;base64,${data}" alt="UML diagram ${diagramCount}; its editable Mermaid source is in STUDY_GUIDE.md"></button><figcaption>Diagram ${String(diagramCount).padStart(2, '0')} · Click to enlarge</figcaption></figure>`);
}
let body = marked.parse(markdown);
const headings = [];
body = body.replace(/<h([12])>(.*?)<\/h\1>/gs, (match, level, label) => {
  const plain = label.replace(/<[^>]*>/g, '');
  const id = plain.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
  if (level === '2') headings.push({ id, label });
  return `<h${level} id="${id}">${label}</h${level}>`;
});
body = body.replace(/<table>/g, '<div class="table-scroll"><table>').replace(/<\/table>/g, '</table></div>');
const navigation = headings.map(h => `<a href="#${h.id}">${h.label}</a>`).join('\n');
const html = `<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Parking lot LLD — a guided study</title>
<style>
:root{--ink:#1d3036;--muted:#586b70;--paper:#fbfaf7;--line:#dce3df;--accent:#176452}
*{box-sizing:border-box}html{scroll-behavior:smooth;scroll-padding-top:24px}body{margin:0;background:var(--paper);color:var(--ink);font:17px/1.75 system-ui,-apple-system,"Segoe UI",sans-serif}
a{color:var(--accent);text-underline-offset:3px}aside{position:fixed;inset:0 auto 0 0;width:250px;padding:32px 24px;border-right:1px solid var(--line);background:#f0f3ed;overflow:auto}aside .brand{display:block;font-size:12px;font-weight:750;letter-spacing:2px;margin-bottom:7px}aside .title{font-family:Georgia,serif;font-size:29px;line-height:1.2;margin:0 0 25px}nav a{display:block;font-size:13px;line-height:1.5;text-decoration:none;padding:9px 0;border-top:1px solid var(--line)}aside .meta{font-size:12px;color:var(--muted);margin-top:24px}main{margin-left:250px;max-width:1150px;padding:52px 56px 100px}h1{font:700 48px/1.12 Georgia,serif;letter-spacing:-1.5px;max-width:780px;margin:0 0 24px}h2{font:700 32px/1.2 Georgia,serif;margin:68px 0 20px;padding-top:22px;border-top:2px solid var(--ink)}h3{font-size:20px;line-height:1.4;margin:34px 0 14px}p{margin:14px 0}strong{font-weight:700}li{margin:7px 0}ul,ol{padding-left:25px}blockquote{margin:24px 0;padding:12px 24px;border-left:4px solid var(--accent);background:#edf3ed}code{font:0.86em/1.55 Consolas,"Courier New",monospace;background:#eaf0ea;padding:2px 4px;border-radius:3px}pre{background:#172b31;color:#e9f2ed;overflow:auto;border-radius:5px;padding:23px;font-size:13px;line-height:1.65}pre code{background:transparent;color:inherit;padding:0;font-size:inherit}.table-scroll{overflow:auto;margin:23px 0}table{width:100%;border-collapse:collapse;font-size:14px;line-height:1.6}th{text-align:left;color:#fff;background:#244d46;font-weight:650}td,th{padding:12px 14px;border:1px solid var(--line);vertical-align:top}tbody tr:nth-child(even){background:#f0f3ed}td code{white-space:normal;overflow-wrap:anywhere}details{margin:24px 0;border:1px solid #bdcfc3;border-radius:6px;background:#f4f7f0;padding:0 18px}summary{cursor:pointer;padding:16px 0;font-size:14px;font-weight:650;overflow-wrap:anywhere}details pre{margin:0 0 18px}.diagram{margin:28px 0;padding:18px;background:#fff;border:1px solid var(--line);border-radius:7px}.diagram-button{display:block;width:100%;border:0;padding:0;background:#fff;cursor:zoom-in}.diagram img{display:block;width:100%;height:auto;max-height:900px;object-fit:contain}figcaption{font-size:11px;letter-spacing:1px;text-transform:uppercase;color:var(--muted);padding-top:17px;text-align:center}button:focus-visible,a:focus-visible,summary:focus-visible{outline:3px solid #b38327;outline-offset:4px}dialog{width:96vw;max-width:none;height:94vh;padding:0;border:1px solid var(--line);border-radius:8px;background:white}dialog::backdrop{background:#102c35cc}.zoom-toolbar{display:flex;gap:14px;align-items:center;padding:12px 18px;background:#f0f3ed;border-bottom:1px solid var(--line)}.zoom-toolbar button{padding:7px 14px;cursor:pointer}.zoom-toolbar label{font-size:13px}.zoom-toolbar input{vertical-align:middle}.zoom-canvas{height:calc(100% - 65px);overflow:auto;padding:25px}.zoom-canvas img{display:block;max-width:none;margin:0 auto}
@media(max-width:950px){aside{position:static;width:auto;border-right:0;border-bottom:1px solid var(--line);padding:20px 24px}aside .title{margin:0}aside nav,aside .meta{display:none}main{margin:0;padding:32px 24px}h1{font-size:39px}h2{font-size:28px}}
@media print{aside,dialog{display:none}main{margin:0;max-width:none;padding:0}body{background:white;font-size:11pt}h1{font-size:28pt}h2{font-size:21pt;break-before:page}h3{break-after:avoid}pre{white-space:pre-wrap;overflow-wrap:anywhere;color:#172b31;background:#f3f5f1;font-size:8pt}.diagram{break-inside:avoid}.diagram img{max-height:650px}.diagram-button{cursor:default}figcaption{display:none}details{background:white}.table-scroll{overflow:visible}th{color:var(--ink);background:#eaf0ea}a{color:var(--ink)}}
</style></head><body><aside><span class="brand">LLD / STUDY SERIES</span><p class="title">Parking lot</p><nav aria-label="Study guide sections">${navigation}</nav><p class="meta">4 runnable checkpoints<br>${diagramCount} UML diagrams<br>Java 17+<br><br>Self-contained, offline edition</p></aside><main>${body}</main>
<dialog id="zoom" aria-label="Enlarged UML diagram"><div class="zoom-toolbar"><button id="close-zoom" type="button">Close</button><label>Diagram width <input id="zoom-scale" type="range" min="70" max="240" value="100"></label><span style="font-size:12px">Scroll to explore · Esc to close</span></div><div class="zoom-canvas"><img id="zoom-image" alt="Enlarged UML diagram"></div></dialog>
<script>
const zoom=document.getElementById('zoom'),zoomImage=document.getElementById('zoom-image'),scale=document.getElementById('zoom-scale');
function resizeDiagram(){zoomImage.style.width=(scale.value/100*(window.innerWidth-110))+'px'}
document.querySelectorAll('.diagram-button').forEach(button=>button.addEventListener('click',()=>{const image=button.querySelector('img');zoomImage.src=image.src;zoomImage.alt=image.alt;scale.value=100;resizeDiagram();zoom.showModal()}));
document.getElementById('close-zoom').addEventListener('click',()=>zoom.close());scale.addEventListener('input',resizeDiagram);
let openedForPrint=[];window.addEventListener('beforeprint',()=>{openedForPrint=[...document.querySelectorAll('details:not([open])')];openedForPrint.forEach(d=>d.open=true)});window.addEventListener('afterprint',()=>openedForPrint.forEach(d=>d.open=false));
</script></body></html>`;
await writeFile(path.join(root, 'STUDY_GUIDE.html'), html, 'utf8');
console.log(`Rendered self-contained HTML: ${diagramCount} diagrams, ${headings.length} navigation links.`);
