import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './base/BasePage';

/**
 * Page Object Model for the Dashboard/Planning Page
 * This is the main page users see after login - represents the Planning view
 */
export class MobileHomePage extends BasePage {
  // Page-specific elements
  private readonly pageTitle: Locator;

  constructor(page: Page) {
    super(page, '/mobile');

    // Initialize page-specific locators
    this.pageTitle = page.locator('h4').first(); // Adjust based on actual page structure
  }

  async verifyMobileHomePageLoaded(): Promise<void> {
    await expect(this.pageTitle).toBeVisible();
    await expect(this.pageTitle).toHaveText('Planning');
  }
}
