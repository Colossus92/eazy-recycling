import { defineConfig, devices } from '@playwright/test';
import { nxE2EPreset } from '@nx/playwright/preset';

// For local development, we assume the application is already running
const baseURL = process.env['BASE_URL'] || 'http://localhost:5173';

/**
 * Development configuration for Playwright tests
 * This config assumes that the application is already running locally
 * and does not attempt to start the Docker containers
 */
export default defineConfig({
  ...nxE2EPreset(__filename, { testDir: './src' }),
  /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
  use: {
    baseURL,
    /* Collect trace when retrying the failed test. See https://playwright.dev/docs/trace-viewer */
    trace: 'on-first-retry',
  },
  /* Explicitly disable webServer to prevent Docker from starting */
  webServer: {
        // command: 'npx run react-frontend:dev',
        url: 'http://localhost:5173',
        reuseExistingServer: true,
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
