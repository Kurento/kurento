#!/usr/bin/env node

// scripts/install-kurento.js
const {
  execSync
} = require('child_process');
const fs = require('fs');
const path = require('path');

const tempDir = '.temp-kurento';
const targetDir = 'node_modules/kurento-jsonrpc';
const repoUrl = 'https://github.com/Kurento/kurento.git';

console.log('Installing kurento-jsonrpc from folder...');

try {
  // Detect curren branch/commit in project
  let gitRef;
  try {
    // Try to get current branch name
    gitRef = execSync('git rev-parse --abbrev-ref HEAD', {
      encoding: 'utf8'
    }).trim();

    // If detached HEAD, get commit hash
    if (gitRef === 'HEAD') {
      gitRef = execSync('git rev-parse HEAD', {
        encoding: 'utf8'
      }).trim();
      console.log(`Using commit: ${gitRef}`);
    } else {
      console.log(`Using rama: ${gitRef}`);
    }
  } catch (error) {
    console.warn('Cannot retrieve current branch/commit, using main as default');
    gitRef = 'main';
  }

  // Clean if it exists
  if (fs.existsSync(tempDir)) fs.rmSync(tempDir, {
    recursive: true
  });
  if (fs.existsSync(targetDir)) fs.rmSync(targetDir, {
    recursive: true
  });

  // Clone same branch/commit
  console.log(`Cloning from ${repoUrl} (ref: ${gitRef})...`);
  execSync(`git clone --depth 1 --filter=blob:none --sparse --branch ${gitRef} ${repoUrl} ${tempDir}`, {
    stdio: 'inherit'
  });
  execSync(`cd ${tempDir} && git sparse-checkout set clients/javascript/jsonrpc`, {
    stdio: 'inherit'
  });

  // Copy to node_modules
  const sourceDir = path.join(tempDir, 'clients/javascript/jsonrpc');
  fs.mkdirSync(path.dirname(targetDir), {
    recursive: true
  });
  fs.cpSync(sourceDir, targetDir, {
    recursive: true
  });

  // Install packet dependencies
  if (fs.existsSync(path.join(targetDir, 'package.json'))) {
    console.log('Installing kurento-jsonrpc dependencies...');
    execSync(`cd ${targetDir} && npm install`, {
      stdio: 'inherit'
    });
  }

  // Clean
  fs.rmSync(tempDir, {
    recursive: true
  });

  console.log('✓ kurento-jsonrpc correctly installed');
} catch (error) {
  console.error('✗ Error installing kurento-jsonrpc:', error.message);
  process.exit(1);
}