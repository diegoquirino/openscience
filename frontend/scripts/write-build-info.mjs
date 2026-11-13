// Writes src/app/build-info.ts with the deployed version (VEREDITO_VERSION, default 0.4.0+dev).
import { writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const version = process.env.VEREDITO_VERSION?.trim() || '0.4.0+dev';
if (!/^[0-9A-Za-z.+-]+$/.test(version)) {
  console.error(`Invalid VEREDITO_VERSION: ${version}`);
  process.exit(1);
}

const target = join(dirname(fileURLToPath(import.meta.url)), '..', 'src', 'app', 'build-info.ts');
writeFileSync(target, `export const BUILD_VERSION = '${version}';\n`, 'utf8');
console.log(`build-info.ts: ${version}`);
