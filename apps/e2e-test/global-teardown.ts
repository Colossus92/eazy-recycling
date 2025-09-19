import { execSync } from 'child_process';
import { workspaceRoot } from '@nx/devkit';

async function globalTeardown() {
  // Check if we should teardown (only if tests passed)
  const shouldTeardown = process.env.PLAYWRIGHT_TEARDOWN !== 'false';
  
  if (shouldTeardown) {
    console.log('🧹 Tearing down e2e environment...');
    try {
      execSync('npx nx run eazy-recycling:e2e-down', {
        cwd: workspaceRoot,
        stdio: 'inherit'
      });
      console.log('✅ E2E environment torn down successfully');
    } catch (error) {
      console.error('❌ Failed to tear down e2e environment:', error);
    }
  } else {
    console.log('⏭️  Skipping teardown (PLAYWRIGHT_TEARDOWN=false)');
  }
}

export default globalTeardown;
