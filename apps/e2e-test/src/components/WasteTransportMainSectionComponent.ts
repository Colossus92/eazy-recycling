import { Page } from '@playwright/test';
import { BaseFormComponent } from './base/BaseFormComponent';

/**
 * Component for interacting with the main section of the waste transport form
 * Maps to WasteTransportMainSection.tsx in the React codebase
 */
export class WasteTransportMainSectionComponent extends BaseFormComponent {
  constructor(page: Page) {
    super(page);
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
}
