import { Page, expect } from '@playwright/test';
import { BaseFormComponent } from './base/BaseFormComponent';

/**
 * Component for interacting with the details section of the waste transport form
 * Maps to TransportFormDetailsSection.tsx in the React codebase
 */
export class WasteTransportDetailsSectionComponent extends BaseFormComponent {
  constructor(page: Page) {
    super(page);
  }
  
  /**
   * Verify that we're on the transport details section
   */
  async verifyTransportDetailsSectionVisible(): Promise<void> {
    const transportDetailsSpan = this.page.locator('span.subtitle-2:has-text("Transport details")');
    await expect(transportDetailsSpan).toBeVisible();
  }
  
  /**
   * Fill the transport details
   */
  async fillTransportDetails(notes: string): Promise<void> {
    // Fill notes
    await this.fillInputByTestId('transport-notes', notes);
  }
}
