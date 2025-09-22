import { Page, Locator, expect } from '@playwright/test';

/**
 * Component Object Model for the Waste Transport Form
 * Handles all interactions with the waste transport form elements
 */
export class WasteTransportFormComponent {
  readonly page: Page;
  
  // Form sections
  private readonly formContainer: Locator;
  private readonly nextButton: Locator;
  
  // We don't need to store these as properties since we're using the selectFromReactSelect helper method
  
  constructor(page: Page) {
    this.page = page;
    
    // Initialize form container and navigation buttons
    this.formContainer = page.locator('form');
    this.nextButton = page.locator('button[data-testid="next-button"]');
  }
  
  /**
   * Wait for the form to be visible
   */
  async waitForFormVisible(): Promise<void> {
    await expect(this.formContainer).toBeVisible();
  }
  
  /**
   * Fill in the main section of the waste transport form
   */
  async fillMainSection(consignorName: string, classificationType: number, carrierName: string, containerOperation: string): Promise<void> {
    // Fill consignor party (sender)
    await this.selectFromReactSelect('consignor-party-select', consignorName);
    
    // Select consignor classification
    await this.page.locator(`[data-testid="consignor-classification-select-option-${classificationType}"]`).click();
    
    // Fill carrier party
    await this.selectFromReactSelect('carrier-party-select', carrierName);
    
    // Fill container operation
    await this.selectFromReactSelect('container-operation-select', containerOperation);
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
   * Navigate to the next step in the form
   */
  async goToNextStep(): Promise<void> {
    await this.nextButton.click();
  }

  /**
   * Verify that we're on the pickup info section
   */
  async verifyPickupInfoSectionVisible(): Promise<void> {
    const ophaalInfoSpan = this.page.locator('span.subtitle-2:has-text("Ophaal info")');
    await expect(ophaalInfoSpan).toBeVisible();
  }
  
  /**
   * Verify that we're on the delivery info section
   */
  async verifyDeliveryInfoSectionVisible(): Promise<void> {
    const afleverInfoSpan = this.page.locator('span.subtitle-2:has-text("Aflever info")');
    await expect(afleverInfoSpan).toBeVisible();
  }
  
  /**
   * Verify that we're on the waste details section
   */
  async verifyWasteDetailsSectionVisible(): Promise<void> {
    const afvalDetailsSpan = this.page.locator('span.subtitle-2:has-text("Afval details")');
    await expect(afvalDetailsSpan).toBeVisible();
  }
  
  /**
   * Verify that we're on the transport details section
   */
  async verifyTransportDetailsSectionVisible(): Promise<void> {
    const transportDetailsSpan = this.page.locator('span.subtitle-2:has-text("Transport details")');
    await expect(transportDetailsSpan).toBeVisible();
  }
}
