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
  
  constructor(page: Page) {
    super(page);
    this.drawerContent = page.locator('[data-testid="transport-details-drawer-content"]');
    this.waybillDownloadButton = page.locator('[data-testid="waybill-download-button"]');
  }

  /**
   * Verify the transport details drawer is visible
   */
  async verifyDrawerVisible(): Promise<void> {
    await expect(this.drawerContent).toBeVisible({ timeout: 5000 });
  }

  async downloadWaybill(): Promise<void> {
    this.page.on('download', download => download.path().then(console.log));
    const downloadPromise = this.page.waitForEvent('download');
    await this.waybillDownloadButton.click();
    const download = await downloadPromise;
    const stream = await download.createReadStream();
    
    await expectSizeGreaterThan100Kb(stream);
  }
}
