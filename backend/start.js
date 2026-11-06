const fs = require('node:fs');
const path = require('node:path');
const { spawnSync } = require('node:child_process');

const backendRoot = __dirname;
const applicationClass = path.join(backendRoot, 'bin', 'civsim', 'web', 'CivSimWebApplication.class');

if (!fs.existsSync(applicationClass)) {
  console.log('Backend classes not found. Building CivSim before launch...');
  const build = spawnSync(process.execPath, ['build.js'], { cwd: backendRoot, stdio: 'inherit' });
  if (build.error) {
    console.error(`Could not start the backend build: ${build.error.message}`);
    process.exit(1);
  }
  if (build.status !== 0) process.exit(build.status ?? 1);
}

const preferredJava = 'C:/Program Files/Java/jdk-25.0.4.1/bin/java.exe';
const java = fs.existsSync(preferredJava) ? preferredJava : 'java';
const result = spawnSync(java, [
  '-cp', ['bin', 'resources', 'lib/*'].join(path.delimiter),
  'civsim.web.CivSimWebApplication',
], { cwd: backendRoot, stdio: 'inherit' });

if (result.error) {
  console.error(`Could not start Java: ${result.error.message}`);
  process.exit(1);
}
process.exit(result.status ?? 1);
