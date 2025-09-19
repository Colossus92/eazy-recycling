import { Page, expect } from '@playwright/test';

/**
 * Base component class for form components
 * Provides common functionality for all form components
 */
export abstract class BaseFormComponent {
  readonly page: Page;
  
  constructor(page: Page) {
    this.page = page;
  }
  
  /**
   * Helper method to select an option from a React Select component
   */
  async selectFromReactSelect(selectId: string, optionText: string): Promise<void> {
    const selectElement = this.page.locator(`#${selectId}`);
    await selectElement.click();
    
    // Type part of the option text to filter
    await this.page.keyboard.type(optionText.substring(0, 4));
    
    // Wait for the option to be visible and click it
    const option = this.page.locator('.react-select__option').filter({ hasText: optionText });
    await expect(option).toBeVisible();
    await option.click();
  }
  
  /**
   * Fill a text input field by test ID
   */
  async fillInputByTestId(testId: string, value: string): Promise<void> {
    await this.page.locator(`[data-testid="${testId}"]`).fill(value);
  }
  
  /**
   * Fill a date-time input field by test ID
   */
  async fillDateTimeByTestId(testId: string, dateTimeValue: string): Promise<void> {
    await this.page.locator(`[data-testid="${testId}"]`).fill(dateTimeValue);
  }
  
  /**
   * Check if an element is visible by test ID
   */
  async isElementVisibleByTestId(testId: string): Promise<boolean> {
    return await this.page.locator(`[data-testid="${testId}"]`).isVisible();
  }
  
  /**
   * Wait for an element to be visible by test ID
   */
  async waitForElementVisibleByTestId(testId: string): Promise<void> {
    await expect(this.page.locator(`[data-testid="${testId}"]`)).toBeVisible();
  }
}
