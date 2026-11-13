// FR-017 and SC-008: every interface text extracted from the source has a non-empty translation
// in each translation file. Node only, no dependency.
import { spawnSync } from 'node:child_process';
import { mkdtempSync, readFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const translations = ['pt-BR', 'es-ES'].map((locale) => ({
  locale,
  file: join(root, 'src', 'locale', `messages.${locale}.xlf`),
}));

const outputDir = mkdtempSync(join(tmpdir(), 'veredito-i18n-'));
try {
  const ng = join(root, 'node_modules', '@angular', 'cli', 'bin', 'ng.js');
  const extraction = spawnSync(
    process.execPath,
    [ng, 'extract-i18n', '--format=xlf2', `--output-path=${outputDir}`],
    { cwd: root, encoding: 'utf8' },
  );
  if (extraction.status !== 0) {
    process.stderr.write(extraction.stdout + extraction.stderr);
    console.error('i18n:check: ng extract-i18n failed');
    process.exit(1);
  }

  const source = readUnits(join(outputDir, 'messages.xlf'));
  const problems = [];
  const warnings = [];

  for (const { locale, file } of translations) {
    const translated = readUnits(file);
    for (const [id, unit] of source) {
      const target = translated.get(id)?.target;
      if (target === undefined) {
        problems.push(`${locale}: missing unit "${id}" (source: ${unit.source})`);
      } else if (target.trim() === '') {
        problems.push(`${locale}: empty target in unit "${id}" (source: ${unit.source})`);
      }
    }
    for (const id of translated.keys()) {
      if (!source.has(id)) {
        warnings.push(`${locale}: unit "${id}" is not in the built application (obsolete or not used yet)`);
      }
    }
  }

  warnings.forEach((warning) => console.warn(`warning: ${warning}`));
  if (problems.length > 0) {
    problems.forEach((problem) => console.error(`error: ${problem}`));
    console.error(`i18n:check: ${problems.length} problem(s) in the translation files`);
    process.exit(1);
  }
  console.log(`i18n:check: ${source.size} units translated in ${translations.map((t) => t.locale).join(', ')}`);
} finally {
  rmSync(outputDir, { recursive: true, force: true });
}

/** Units of an XLIFF 2.0 file by id, with the raw source and target (undefined when absent). */
function readUnits(file) {
  const xml = readFileSync(file, 'utf8');
  const units = new Map();
  for (const match of xml.matchAll(/<unit\s+id="([^"]+)"[^>]*>([\s\S]*?)<\/unit>/g)) {
    const body = match[2];
    const source = /<source>([\s\S]*?)<\/source>/.exec(body)?.[1].trim() ?? '';
    const target = /<target\s*\/>/.test(body) ? '' : /<target>([\s\S]*?)<\/target>/.exec(body)?.[1];
    units.set(match[1], { source, target });
  }
  return units;
}
