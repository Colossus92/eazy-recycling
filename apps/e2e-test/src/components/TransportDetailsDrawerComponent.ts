import { Page, Locator, expect } from '@playwright/test';
import { BaseComponent } from '../components/base/BaseComponent';

/**
 * Component for interacting with the Transport Details Drawer
 */
export class TransportDetailsDrawerComponent extends BaseComponent {
  private readonly drawerContent: Locator;
  
  constructor(page: Page) {
    super(page);
    this.drawerContent = page.locator('[data-testid="transport-details-drawer-content"]');
  }

  /**
   * Verify the transport details drawer is visible
   */
  async verifyDrawerVisible(): Promise<void> {
    await expect(this.drawerContent).toBeVisible({ timeout: 5000 });
  }
}
