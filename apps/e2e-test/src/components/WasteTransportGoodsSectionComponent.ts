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

  async fillWasteSection(pickupPartyName: string) {
    await this.verifyWasteDetailsSectionVisible();
    await this.selectFromReactSelect('pickup-party-select', pickupPartyName);
    await this.selectFromCombobox('waste-stream-number-combobox', '087970000135 - ijzer en staal');
    await this.selectFromReactSelect('eural-code-select', '16 01 17 - ferrometalen');
    await this.selectFromReactSelect('processing-method-select', 'A.02 - Overslag / Opbulken');
    await this.fillInputByLocator('[name="weight"]', '100');
    await this.fillInputByLocator('[name="quantity"]', '2');
    await expect(this.page.locator('[data-testid="goodsName"]')).toHaveValue('ijzer en staal');
    
  }
}
