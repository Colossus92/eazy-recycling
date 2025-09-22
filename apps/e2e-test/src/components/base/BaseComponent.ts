import { Page, Locator, expect } from '@playwright/test';

/**
 * Base component class for all page components
 */
export class BaseComponent {
  protected page: Page;

  constructor(page: Page) {
    this.page = page;
  }

  /**
   * Wait for an element to be visible
   */
  protected async waitForElement(locator: Locator, timeout = 5000): Promise<void> {
    await expect(locator).toBeVisible({ timeout });
  }

  /**
   * Wait for an element to be hidden
   */
  protected async waitForElementHidden(locator: Locator, timeout = 5000): Promise<void> {
    await expect(locator).toBeHidden({ timeout });
  }
}
