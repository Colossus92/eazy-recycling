import { Page, Locator } from '@playwright/test';

/**
 * Base Page Object Model class that provides common functionality
 * for all page objects in the application.
 */
export abstract class BasePage {
  page: Page;
  protected url: string;

  constructor(page: Page, url: string) {
    this.page = page;
    this.url = url;
  }

  /**
   * Navigate to the page
   */
  async goto(): Promise<void> {
    await this.page.goto(this.url);
  }

  /**
   * Wait for the page to be loaded
   */
  async waitForPageLoad(): Promise<void> {
    await this.page.waitForLoadState('domcontentloaded');
  }

  /**
   * Get page title
   */
  async getTitle(): Promise<string> {
    return await this.page.title();
  }

  /**
   * Wait for an element to be visible
   */
  async waitForElement(locator: Locator): Promise<void> {
    await locator.waitFor({ state: 'visible' });
  }

  /**
   * Fill form field by data-testid
   */
  async fillFieldByTestId(testId: string, value: string): Promise<void> {
    await this.page.fill(`[data-testid="${testId}"]`, value);
  }

  /**
   * Click element by data-testid
   */
  async clickByTestId(testId: string): Promise<void> {
    await this.page.click(`[data-testid="${testId}"]`);
  }

  /**
   * Get text content by data-testid
   */
  async getTextByTestId(testId: string): Promise<string> {
    return await this.page.textContent(`[data-testid="${testId}"]`) || '';
  }

  /**
   * Check if element is visible by data-testid
   */
  async isVisibleByTestId(testId: string): Promise<boolean> {
    return await this.page.isVisible(`[data-testid="${testId}"]`);
  }

  /**
   * Wait for navigation to complete
   */
  async waitForNavigation(): Promise<void> {
    await this.page.waitForLoadState('domcontentloaded');
  }

  /**
   * Take a screenshot
   */
  async takeScreenshot(name: string): Promise<void> {
    await this.page.screenshot({ path: `screenshots/${name}.png` });
  }
}
