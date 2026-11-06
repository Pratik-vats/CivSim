const fs = require('node:fs');
const path = require('node:path');
const { spawnSync } = require('node:child_process');

const root = __dirname;
const sourceRoot = path.join(root, 'src');

function javaSources(directory) {
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const entryPath = path.join(directory, entry.name);
    return entry.isDirectory() ? javaSources(entryPath) : entry.name.endsWith('.java') ? [entryPath] : [];
  });
}

const sources = javaSources(sourceRoot).sort();
if (sources.length === 0) {
  console.error(`No Java sources found under ${sourceRoot}`);
  process.exit(1);
}

const sourceList = path.join(root, 'compile_sources.txt');
fs.writeFileSync(sourceList, `${sources.map((source) => path.relative(root, source).replace(/\\/g, '/')).join('\n')}\n`);
const outputDirectory = path.join(root, 'bin');
fs.rmSync(outputDirectory, { recursive: true, force: true });
fs.mkdirSync(outputDirectory, { recursive: true });

const preferredCompiler = 'C:/Program Files/Java/jdk-25.0.4.1/bin/javac.exe';
const compiler = fs.existsSync(preferredCompiler) ? preferredCompiler : 'javac';
const result = spawnSync(compiler, [
  '-cp', 'lib/*',
  '-d', 'bin',
  '--release', '21',
  '-parameters',
  '@compile_sources.txt',
], { cwd: root, stdio: 'inherit' });

if (result.error) {
  console.error(`Could not start javac: ${result.error.message}`);
  process.exit(1);
}
process.exit(result.status ?? 1);
