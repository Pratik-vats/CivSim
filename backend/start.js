const fs = require('node:fs');
const { spawnSync } = require('node:child_process');

const preferredJava = 'C:/Program Files/Java/jdk-25.0.4.1/bin/java.exe';
const java = fs.existsSync(preferredJava) ? preferredJava : 'java';
const result = spawnSync(java, [
  '-cp', 'bin;resources;lib/*',
  'civsim.web.CivSimWebApplication',
], { stdio: 'inherit' });

if (result.error) {
  console.error(`Could not start Java: ${result.error.message}`);
  process.exit(1);
}
process.exit(result.status ?? 1);
