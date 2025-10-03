import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './base/BasePage';

/**
 * Page Object Model for the Dashboard/Planning Page
 * This is the main page users see after login - represents the Planning view
 */
export class MobileTransportDetailsPage extends BasePage {
  // Page-specific elements
  private readonly pageTitle: Locator;

  constructor(page: Page) {
    super(page, '/mobile');

    // Initialize page-specific locators
    this.pageTitle = page.locator('h4').first();
  }

  async verifyMobileTransportDetailsPageLoaded(): Promise<void> {
    await expect(this.pageTitle).toBeVisible();
    await expect(this.pageTitle).toHaveText('Transport Details');
  }

  async checkTransportDetails(): Promise<void> {
    const transportDetailsElement = this.page.locator('.bg-color-brand-light-hover.text-color-status-info-dark');
    await expect(transportDetailsElement).toBeVisible();
    await expect(transportDetailsElement).toHaveText('Gepland');
  }

}
