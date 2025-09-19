import { Page, expect } from '@playwright/test';
import { BaseFormComponent } from './base/BaseFormComponent';

/**
 * Component for interacting with the goods section of the waste transport form
 * Maps to WasteTransportFormGoodsSection.tsx in the React codebase
 */
export class WasteTransportGoodsSectionComponent extends BaseFormComponent {
  constructor(page: Page) {
    super(page);
  }
  
  /**
   * Verify that we're on the waste details section
   */
  async verifyWasteDetailsSectionVisible(): Promise<void> {
    const afvalDetailsSpan = this.page.locator('span.subtitle-2:has-text("Afval details")');
    await expect(afvalDetailsSpan).toBeVisible();
  }
  
  /**
   * Fill the waste details
   */
  async fillWasteDetails(wasteType: string, wasteWeight: string): Promise<void> {
    // Select waste type
    await this.selectFromReactSelect('waste-type-select', wasteType);
    
    // Fill waste weight
    await this.fillInputByTestId('waste-weight-input', wasteWeight);
  }
}
