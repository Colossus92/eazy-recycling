import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './base/BasePage';
import { SidebarComponent } from '../components/SidebarComponent';

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

  constructor(page: Page) {
    super(page, '/');

    // Initialize sidebar component
    this.sidebar = new SidebarComponent(page);

    // Initialize page-specific locators
    this.pageTitle = page.locator('h3').first(); // Adjust based on actual page structure
    this.mainContent = page.locator('[data-testid="calendar-container"]'); // Adjust based on actual page structure
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
    await this.page.locator('[data-testid="new-waste-transport-button"]').click();
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
