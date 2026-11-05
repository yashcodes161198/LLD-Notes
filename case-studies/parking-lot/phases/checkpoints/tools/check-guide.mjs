import { fileURLToPath, pathToFileURL } from 'node:url';
import path from 'node:path';
import { mkdir } from 'node:fs/promises';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const packageRoot = process.argv[2];
const { chromium } = await import(pathToFileURL(path.join(packageRoot, 'playwright/index.mjs')));
const browser = await chromium.launch({ executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true });
try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 1000 }, deviceScaleFactor: 1 });
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  await page.goto(pathToFileURL(path.join(root, 'STUDY_GUIDE.html')).href);
  await page.locator('.diagram img').last().waitFor();
  await page.evaluate(() => Promise.all([...document.images].filter(i => i.src).map(i => i.decode())));
  const result = await page.evaluate(() => ({
    diagrams: document.querySelectorAll('.diagram img').length,
    loadedDiagrams: [...document.querySelectorAll('.diagram img')].filter(i => i.naturalWidth > 0).length,
    javaBlocks: document.querySelectorAll('details pre code.language-java').length,
    brokenAnchors: [...document.querySelectorAll('nav a')].filter(a => !document.getElementById(a.hash.slice(1))).length,
    documentOverflow: document.documentElement.scrollWidth > innerWidth
  }));
  if (result.diagrams !== 12 || result.loadedDiagrams !== 12 || result.javaBlocks !== 4 || result.brokenAnchors || result.documentOverflow)
    throw new Error(JSON.stringify(result));
  await mkdir(path.join(root, 'verification'), { recursive: true });
  await page.screenshot({ path: path.join(root, 'verification/cover.png') });
  await page.locator('.diagram').nth(4).scrollIntoViewIfNeeded();
  await page.locator('.diagram').nth(4).screenshot({ path: path.join(root, 'verification/checkpoint3-uml.png') });
  await page.locator('.diagram').nth(6).screenshot({ path: path.join(root, 'verification/checkpoint4-uml.png') });
  await page.locator('.diagram-button').nth(6).click();
  if (!(await page.locator('dialog').evaluate(d => d.open))) throw new Error('Diagram zoom did not open');
  await page.locator('#zoom-scale').fill('170');
  await page.locator('#zoom-scale').dispatchEvent('input');
  await page.screenshot({ path: path.join(root, 'verification/zoom.png') });
  await page.keyboard.press('Escape');
  await page.locator('details summary').first().click();
  if (!(await page.locator('details').first().evaluate(d => d.open))) throw new Error('Code did not expand');
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto(pathToFileURL(path.join(root, 'STUDY_GUIDE.html')).href);
  if (await page.evaluate(() => document.documentElement.scrollWidth > innerWidth)) throw new Error('Mobile page overflows');
  await page.screenshot({ path: path.join(root, 'verification/mobile.png') });
  if (errors.length) throw new Error(errors.join('\n'));
  console.log(JSON.stringify({ ...result, zoom: 'passed', codeExpansion: 'passed', mobile: 'passed', pageErrors: errors.length }));
} finally { await browser.close(); }
