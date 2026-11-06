const fs = require('node:fs');
const path = require('node:path');
const { spawnSync } = require('node:child_process');

const backendRoot = __dirname;
const preferredJava = 'C:/Program Files/Java/jdk-25.0.4.1/bin/java.exe';
const java = fs.existsSync(preferredJava) ? preferredJava : 'java';
const classpath = ['bin', 'resources', 'lib/*'].join(path.delimiter);

const result = spawnSync(java, ['-cp', classpath, 'civsim.test.TestStage32InteractiveMap'], {
  cwd: backendRoot,
  stdio: 'inherit',
});

if (result.error) {
  console.error(`Could not start Java tests: ${result.error.message}`);
  process.exit(1);
}

if (result.signal) {
  console.error(`Java tests were terminated by ${result.signal}.`);
  process.exit(1);
}

process.exit(result.status ?? 1);
