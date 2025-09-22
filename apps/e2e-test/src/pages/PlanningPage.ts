import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './base/BasePage';
import { SidebarComponent } from '../components/SidebarComponent';
import { TransportDetailsDrawerComponent } from '../components/TransportDetailsDrawerComponent';

/**
 * Page Object Model for the Dashboard/Planning Page
 * This is the main page users see after login - represents the Planning view
 */
export class PlanningPage extends BasePage {
  // Sidebar component (shared across desktop pages)
  public readonly sidebar: SidebarComponent;

  // Page-specific elements
  private readonly pageTitle: Locator;
  private readonly mainContent: Locator;
  private readonly newWasteTransportButton: Locator;
  
  // Components
  public readonly transportDetailsDrawer: TransportDetailsDrawerComponent;

  constructor(page: Page) {
    super(page, '/');

    // Initialize sidebar component
    this.sidebar = new SidebarComponent(page);

    // Initialize page-specific locators
    this.pageTitle = page.locator('h3').first(); // Adjust based on actual page structure
    this.mainContent = page.locator('[data-testid="calendar-container"]'); // Adjust based on actual page structure
    this.newWasteTransportButton = page.locator('[data-testid="new-waste-transport-button"]');
    
    // Initialize components
    this.transportDetailsDrawer = new TransportDetailsDrawerComponent(page);
  }

  /**
   * Navigate to planning and wait for it to load
   */
  async navigateToPlanning(): Promise<void> {
    await this.goto();
    await this.waitForPageLoad();
    await this.waitForPlanningLoad();
  }

  /**
   * Wait for planning page to be fully loaded
   */
  async waitForPlanningLoad(): Promise<void> {
    await this.sidebar.waitForSidebarLoad();
    await this.sidebar.verifySidebarVisible();
    expect(this.pageTitle).toBe('Planning');
    // Add more specific waits based on your planning content
  }

  /**
   * Verify the planning page is loaded correctly
   */
  async verifyPlanningLoaded(): Promise<void> {
    // Verify sidebar is present and visible
    await this.sidebar.verifySidebarVisible();
    await this.waitForElement(this.pageTitle);
    await expect(this.pageTitle).toHaveText('Planning');
    // Verify Planning nav item is active (since this is the planning page)
    expect(await this.sidebar.isPlanningActive()).toBe(true);

    // Verify main content area is visible
    await expect(this.mainContent).toBeVisible();
  }

  /**
   * Verify user has regular permissions (no admin nav items)
   */
  async verifyRegularUserAccess(): Promise<void> {
    await this.sidebar.verifyRegularUserNavigation();
  }

  /**
   * Verify user has admin permissions (all nav items visible)
   */
  async verifyAdminUserAccess(): Promise<void> {
    await this.sidebar.verifyAdminUserNavigation();
  }

  /**
   * Navigate to different sections using sidebar
   */
  async navigateToContainers(): Promise<void> {
    await this.sidebar.navigateToContainers();
  }

  async navigateToCRM(): Promise<void> {
    await this.sidebar.navigateToCRM();
  }

  async navigateToTrucks(): Promise<void> {
    await this.sidebar.navigateToTrucks();
  }

  async navigateToWasteStreams(): Promise<void> {
    await this.sidebar.navigateToWasteStreams();
  }

  async navigateToUsers(): Promise<void> {
    await this.sidebar.navigateToUsers();
  }

  async openWasteTransportForm(): Promise<void> {
    await this.newWasteTransportButton.click();
  }
  
  /**
   * Find and click on a planning card by its display number
   */
  async clickPlanningCardByDisplayNumber(displayNumber: string): Promise<void> {
    const planningCard = this.page.locator(`text=${displayNumber}`).first();
    await expect(planningCard).toBeVisible({ timeout: 5000 });
    await planningCard.click();
  }
  
  /**
   * Verify that a planning card with the given display number exists
   */
  async verifyPlanningCardExists(displayNumber: string): Promise<void> {
    const planningCard = this.page.locator(`text=${displayNumber}`).first();
    await expect(planningCard).toBeVisible({ timeout: 5000 });
  }

  /**
   * Open the transport details drawer for a specific transport
   */
  async openDetailsDrawerFor(displayNumber: string): Promise<void> {
    if (await this.transportDetailsDrawer.isDrawerVisible()) {
      await this.transportDetailsDrawer.closeDrawer();
    }
    await this.clickPlanningCardByDisplayNumber(displayNumber);
    await this.transportDetailsDrawer.verifyDrawerVisible();
  }

  /**
   * Download the waybill from the transport details drawer with retry logic
   * Will attempt to download up to 3 times if it fails
   */
  async downloadWaybill(displayNumber: string, maxRetries = 3): Promise<void> {
    let attempts = 0;
    let success = false;
    let lastError: Error | null = null;
    
    while (attempts < maxRetries && !success) {
      attempts++;
      try {
        console.log(`Attempt ${attempts} of ${maxRetries} to download waybill for transport ${displayNumber}`);
        
        // Open the transport details drawer
        await this.openDetailsDrawerFor(displayNumber);
        
        // Try to download the waybill
        await this.transportDetailsDrawer.downloadWaybill();
        
        // If we get here, the download was successful
        success = true;
        console.log(`Successfully downloaded waybill on attempt ${attempts}`);
      } catch (error) {
        // Cast the error to Error type safely
        lastError = error instanceof Error ? error : new Error(String(error));
        console.log(`Attempt ${attempts} failed: ${lastError.message}`);
        
        // Close the drawer if it's open before retrying
          await this.transportDetailsDrawer.closeDrawer();
          console.log('Closed drawer before retrying');
          
          // Add a small delay before retrying - using a safer approach than waitForTimeout
          // This gives the UI time to settle before the next attempt
          await new Promise(resolve => setTimeout(resolve, 100));
    }
  }
    
    // If all attempts failed, throw the last error
    if (!success) {
      throw new Error(`Failed to download waybill after ${maxRetries} attempts. Last error: ${lastError?.message}`);
    }
  }

  async deleteTransport(displayNumber: string): Promise<void> {
    await this.openDetailsDrawerFor(displayNumber);
    await this.transportDetailsDrawer.deleteTransport();
    const planningCard = this.page.locator(`text=${displayNumber}`).first();
    await expect(planningCard).not.toBeVisible({ timeout: 5000 });
  }

  /**
   * Test sidebar collapse/expand functionality
   */
  async testSidebarToggle(): Promise<void> {
    // Start expanded
    await this.sidebar.expandSidebar();
    expect(await this.sidebar.isCollapsed()).toBe(false);

    // Collapse
    await this.sidebar.collapseSidebar();
    expect(await this.sidebar.isCollapsed()).toBe(true);

    // Expand again
    await this.sidebar.expandSidebar();
    expect(await this.sidebar.isCollapsed()).toBe(false);
  }
}
