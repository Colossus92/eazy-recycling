import { Page, expect } from '@playwright/test';
import { BaseFormComponent } from './base/BaseFormComponent';

/**
 * Component for interacting with the delivery section of the waste transport form
 * Maps to WasteTransportFormDeliverySection.tsx in the React codebase
 */
export class WasteTransportDeliverySectionComponent extends BaseFormComponent {
  constructor(page: Page) {
    super(page);
  }
  
  /**
   * Verify that we're on the delivery info section
   */
  async verifyDeliveryInfoSectionVisible(): Promise<void> {
    const afleverInfoSpan = this.page.locator('span.subtitle-2:has-text("Aflever info")');
    await expect(afleverInfoSpan).toBeVisible();
  }
  
  /**
   * Fill the delivery company address
   */
  async fillDeliveryCompanyAddress(companyName: string): Promise<void> {
    // Select company
    await this.selectFromReactSelect('delivery-company-address', companyName);
  }
  
  /**
   * Fill the delivery date and time
   */
  async fillDeliveryDateTime(dateTime: string): Promise<void> {
    await this.fillDateTimeByTestId('delivery-date-time', dateTime);
  }
}
