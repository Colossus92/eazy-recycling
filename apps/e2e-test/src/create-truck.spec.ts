import { test, expect } from '@playwright/test';

test('has title', async ({ page }) => {
  await page.goto('http://localhost:5173/');

  // Expect h1 to contain a substring.
  expect(await page.locator('h3').innerText()).toContain('Welkom bij Eazy Recycling');
});
