import { Page, Locator, expect } from '@playwright/test';
import { BaseComponent } from '../components/base/BaseComponent';
import { Readable } from 'stream';



const expectSizeGreaterThan100Kb = async (stream: Readable) => {
  let totalSize = 0;
  for await (const chunk of stream) {
    totalSize += chunk.length;
  }
  expect(totalSize / 1024).toBeGreaterThan(100);
}

/**
 * Component for interacting with the Transport Details Drawer
 */
export class TransportDetailsDrawerComponent extends BaseComponent {
  private readonly drawerContent: Locator;
  private readonly waybillDownloadButton: Locator;
  private readonly closeButton: Locator;
  private readonly deleteButton: Locator;
  
  constructor(page: Page) {
    super(page);
    this.drawerContent = page.locator('[data-testid="transport-details-drawer-content"]');
    this.waybillDownloadButton = page.locator('[data-testid="waybill-download-button"]');
    this.closeButton = page.locator('button[data-testid="close-drawer-button"]');
    this.deleteButton = page.locator('button[data-testid="drawer-delete-button"]');
  }

  /**
   * Verify the transport details drawer is visible
   */
  async verifyDrawerVisible(): Promise<void> {
    await expect(this.drawerContent).toBeVisible({ timeout: 5000 });
  }

  async isDrawerVisible(): Promise<boolean> {
    return await this.drawerContent.isVisible();
  }

  /**
   * Close the transport details drawer
   */
  async closeDrawer(): Promise<void> {
    // Check if the drawer is visible before trying to close it
    const isVisible = await this.drawerContent.isVisible();
    if (isVisible) {
      await this.closeButton.click();
      await expect(this.drawerContent).toBeHidden({ timeout: 5000 });
      console.log('Transport details drawer closed');
    } else {
      console.log('Drawer already closed, no action needed');
    }
  }

  async deleteTransport(): Promise<void> {
    await this.deleteButton.click();
    await expect(this.page.locator('h4:has-text("Transport verwijderen")')).toBeVisible();
    await this.page.locator('button:has-text("Verwijderen")').click();
  }

  /**
   * Click the waybill download button and verify the downloaded file
   * Note: This method only clicks the download button. The caller should handle
   * waiting for network requests if needed.
   */
  async downloadWaybill(): Promise<void> {
    // Set up download handling
    this.page.on('download', download => download.path().then(path => {
      console.log(`Waybill downloaded to: ${path}`);
    }));
    
    // Wait for the download event
    const downloadPromise = this.page.waitForEvent('download');
    
    // Click the download button
    await expect(this.waybillDownloadButton).toBeVisible();
    await this.waybillDownloadButton.click();
    console.log('Clicked on waybill download button');
    
    // Wait for the download to start
    const download = await downloadPromise;
    console.log(`Download started: ${download.suggestedFilename()}`);
    
    // Verify the downloaded file size
    const stream = await download.createReadStream();
    await expectSizeGreaterThan100Kb(stream);
    console.log('Waybill file size verification passed');
  }
}
