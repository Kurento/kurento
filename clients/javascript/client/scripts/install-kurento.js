#!/usr/bin/env node

// scripts/install-kurento.js
const {
  execSync,
  execFileSync
} = require('child_process');
const fs = require('fs');
const path = require('path');

const tempDir = '.temp-kurento';
const targetDir = 'node_modules/kurento-jsonrpc';
const repoUrl = 'https://github.com/Kurento/kurento.git';

console.log('Installing kurento-jsonrpc from folder...');

// Validate gitRef to prevent command injection.
// Allow only hex commit SHAs or refs that pass git check-ref-format.
function validateGitRef(ref) {
  // Allow hex commit SHA (full 40-char or abbreviated >=7 chars)
  if (/^[0-9a-f]{7,40}$/.test(ref)) {
    return true;
  }
  // Allow refs that pass git check-ref-format
  try {
    execFileSync('git', ['check-ref-format', '--branch', ref], { stdio: 'ignore' });
    return true;
  } catch (e) {
    return false;
  }
}

try {
  // Detect current branch/commit in project
  let gitRef;
  try {
    // Try to get current branch name
    gitRef = execFileSync('git', ['rev-parse', '--abbrev-ref', 'HEAD'], {
      encoding: 'utf8'
    }).trim();

    // If detached HEAD, get commit hash
    if (gitRef === 'HEAD') {
      gitRef = execFileSync('git', ['rev-parse', 'HEAD'], {
        encoding: 'utf8'
      }).trim();
      console.log(`Using commit: ${gitRef}`);
    } else {
      console.log(`Using branch: ${gitRef}`);
    }
  } catch (error) {
    console.warn('Cannot retrieve current branch/commit, using main as default');
    gitRef = 'main';
  }

  if (!validateGitRef(gitRef)) {
    console.error(`Invalid git ref: ${gitRef}`);
    process.exit(1);
  }

  // Clean if it exists
  if (fs.existsSync(tempDir)) fs.rmSync(tempDir, { recursive: true });
  if (fs.existsSync(targetDir)) fs.rmSync(targetDir, { recursive: true });

  // Clone same branch/commit
  console.log(`Cloning from ${repoUrl} (ref: ${gitRef})...`);
  // Clone the main branch first, then checkout the specific ref
  execFileSync('git', ['clone', '--depth', '1', '--filter=blob:none', '--sparse', '--branch', 'main', repoUrl, tempDir], {
    stdio: 'inherit'
  });

  try {
    // Checkout the specific branch or commit
    execFileSync('git', ['fetch', '--depth', '1', 'origin', gitRef], {
      cwd: tempDir,
      stdio: 'inherit'
    });
    execFileSync('git', ['checkout', gitRef], {
      cwd: tempDir,
      stdio: 'inherit'
    });
    execFileSync('git', ['sparse-checkout', 'set', 'clients/javascript/jsonrpc'], {
      cwd: tempDir,
      stdio: 'inherit'
    });

    // Copy to node_modules
    const sourceDir = path.join(tempDir, 'clients/javascript/jsonrpc');
    fs.mkdirSync(path.dirname(targetDir), { recursive: true });
    fs.cpSync(sourceDir, targetDir, { recursive: true });

    // Install packet dependencies
    if (fs.existsSync(path.join(targetDir, 'package.json'))) {
      console.log('Installing kurento-jsonrpc dependencies...');
      execSync('npm install', {
        cwd: targetDir,
        stdio: 'inherit'
      });
    }
  } finally {
    // Always clean up tempDir, even on failure
    if (fs.existsSync(tempDir)) {
      fs.rmSync(tempDir, { recursive: true });
    }
  }

  console.log('✓ kurento-jsonrpc correctly installed');
} catch (error) {
  console.error('✗ Error installing kurento-jsonrpc:', error.message);
  process.exit(1);
}