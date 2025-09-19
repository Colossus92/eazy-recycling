import { Page, expect } from '@playwright/test';
import { BaseFormComponent } from './base/BaseFormComponent';

/**
 * Component for interacting with the pickup section of the waste transport form
 * Maps to WasteTransportFormPickupSection.tsx in the React codebase
 */
export class WasteTransportPickupSectionComponent extends BaseFormComponent {
  constructor(page: Page) {
    super(page);
  }
  
  /**
   * Verify that we're on the pickup info section
   */
  async verifyPickupInfoSectionVisible(): Promise<void> {
    const ophaalInfoSpan = this.page.locator('span.subtitle-2:has-text("Ophaal info")');
    await expect(ophaalInfoSpan).toBeVisible();
  }
  
  /**
   * Fill the pickup company address
   */
  async fillPickupCompanyAddress(companyName: string): Promise<void> {
    // Select company
    await this.selectFromReactSelect('pickup-company-address', companyName); 
  }
  
  /**
   * Fill the pickup date and time
   */
  async fillPickupDateTime(dateTime: string): Promise<void> {
    await this.fillDateTimeByTestId('pickup-date-time', dateTime);
  }
}
