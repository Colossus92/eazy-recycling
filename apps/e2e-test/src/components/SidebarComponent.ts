import { Page, Locator, expect } from '@playwright/test';

/**
 * Sidebar Component POM - Reusable component for pages that include the sidebar
 * This represents the navigation sidebar that appears on all desktop pages except login
 */
export class SidebarComponent {
  private page: Page;

  // Main sidebar elements
  private readonly sidebar: Locator;
  private readonly sidebarHeader: Locator;
  private readonly sidebarNav: Locator;
  private readonly expandButton: Locator;
  private readonly collapseButton: Locator;

  // Navigation items (using the data-testid from NavItem)
  private readonly planningNavItem: Locator;
  private readonly containersNavItem: Locator;
  private readonly crmNavItem: Locator;
  private readonly wasteStreamsNavItem: Locator;
  private readonly usersNavItem: Locator; // Admin only

  constructor(page: Page) {
    this.page = page;

    // Initialize main sidebar locators
    this.sidebar = page.locator('[data-testid="sidebar"]');
    this.sidebarHeader = page.locator('[data-testid="sidebar-header"]');
    this.sidebarNav = page.locator('[data-testid="sidebar-nav"]');
    this.expandButton = page.locator('[data-testid="sidebar-expand-button"]');
    this.collapseButton = page.locator('[data-testid="sidebar-collapse-button"]');

    // Initialize navigation item locators
    this.planningNavItem = page.locator('[data-testid="nav-item-planning"]');
    this.containersNavItem = page.locator('[data-testid="nav-item-containerbeheer"]');
    this.crmNavItem = page.locator('[data-testid="nav-item-crm"]');
    this.wasteStreamsNavItem = page.locator('[data-testid="nav-item-afvalstroombeheer"]');
    this.usersNavItem = page.locator('[data-testid="nav-item-gebruikersbeheer"]');
  }

  /**
   * Verify that the sidebar is visible and loaded
   */
  async verifySidebarVisible(): Promise<void> {
    await expect(this.sidebar).toBeVisible();
    await expect(this.sidebarHeader).toBeVisible();
    await expect(this.sidebarNav).toBeVisible();
  }

  /**
   * Check if the sidebar is in collapsed state
   */
  async isCollapsed(): Promise<boolean> {
    return await this.collapseButton.isHidden();
  }

  /**
   * Collapse the sidebar
   */
  async collapseSidebar(): Promise<void> {
    if (!(await this.isCollapsed())) {
      await this.collapseButton.click();
      // Wait for animation to complete
      await this.page.waitForTimeout(300);
    }
  }

  /**
   * Expand the sidebar
   */
  async expandSidebar(): Promise<void> {
    if (await this.isCollapsed()) {
      // Hover over sidebar to show expand button
      await this.sidebar.hover();
      await this.expandButton.click();
      // Wait for animation to complete
      await this.page.waitForTimeout(300);
    }
  }

  /**
   * Navigate to Planning page
   */
  async navigateToPlanning(): Promise<void> {
    await this.planningNavItem.click();
    await this.page.waitForURL('/');
    await this.waitForNavItemActive(this.planningNavItem);
  }

  /**
   * Navigate to Containers page
   */
  async navigateToContainers(): Promise<void> {
    await this.containersNavItem.click();
    await this.page.waitForURL('/containers');
    await this.waitForNavItemActive(this.containersNavItem);
  }

  /**
   * Navigate to CRM page
   */
  async navigateToCRM(): Promise<void> {
    await this.crmNavItem.click();
    await this.page.waitForURL('/relaties');
    await this.waitForNavItemActive(this.crmNavItem);
  }

  /**
   * Navigate to Waste Streams page
   */
  async navigateToWasteStreams(): Promise<void> {
    await this.wasteStreamsNavItem.click();
    await this.page.waitForURL('/waste-streams');
    await this.waitForNavItemActive(this.wasteStreamsNavItem);
  }

  /**
   * Navigate to Users page (admin only)
   */
  async navigateToUsers(): Promise<void> {
    await this.usersNavItem.click();
    await this.page.waitForURL('/users');
    await this.waitForNavItemActive(this.usersNavItem);
  }

  /**
   * Check if a navigation item is active/selected
   */
  async isNavItemActive(navItem: Locator): Promise<boolean> {
    const classes = await navItem.getAttribute('class');
    return classes?.includes('bg-color-brand-primary') || false;
  }

  /**
   * Wait for a navigation item to become active with retry
   */
  async waitForNavItemActive(navItem: Locator, timeout = 5000): Promise<void> {
    await expect(navItem).toHaveClass(/bg-color-brand-primary/, { timeout });
  }

  /**
   * Check if Planning nav item is active
   */
  async isPlanningActive(): Promise<boolean> {
    return await this.isNavItemActive(this.planningNavItem);
  }

  /**
   * Check if Containers nav item is active
   */
  async isContainersActive(): Promise<boolean> {
    return await this.isNavItemActive(this.containersNavItem);
  }

  /**
   * Check if CRM nav item is active
   */
  async isCRMActive(): Promise<boolean> {
    return await this.isNavItemActive(this.crmNavItem);
  }

  /**
   * Check if Trucks nav item is active
   */
  async isTrucksActive(): Promise<boolean> {
    return await this.isNavItemActive(this.trucksNavItem);
  }

  /**
   * Check if Waste Streams nav item is active
   */
  async isWasteStreamsActive(): Promise<boolean> {
    return await this.isNavItemActive(this.wasteStreamsNavItem);
  }

  /**
   * Check if Users nav item is active
   */
  async isUsersActive(): Promise<boolean> {
    return await this.isNavItemActive(this.usersNavItem);
  }

  /**
   * Check if Users nav item is visible (admin only)
   */
  async isUsersNavVisible(): Promise<boolean> {
    return await this.usersNavItem.isVisible();
  }

  /**
   * Get all visible navigation items
   */
  async getVisibleNavItems(): Promise<string[]> {
    const navItems = [
      { locator: this.planningNavItem, name: 'Planning' },
      { locator: this.containersNavItem, name: 'Containerbeheer' },
      { locator: this.crmNavItem, name: 'CRM' },
      { locator: this.trucksNavItem, name: 'Vrachtwagenbeheer' },
      { locator: this.wasteStreamsNavItem, name: 'Afvalstroombeheer' },
      { locator: this.usersNavItem, name: 'Gebruikersbeheer' },
    ];

    const visibleItems: string[] = [];
    for (const item of navItems) {
      if (await item.locator.isVisible()) {
        visibleItems.push(item.name);
      }
    }
    return visibleItems;
  }

  /**
   * Verify navigation items for regular user (no admin items)
   */
  async verifyRegularUserNavigation(): Promise<void> {
    await expect(this.planningNavItem).toBeVisible();
    await expect(this.containersNavItem).toBeVisible();
    await expect(this.crmNavItem).toBeVisible();
    await expect(this.trucksNavItem).toBeVisible();
    await expect(this.wasteStreamsNavItem).toBeVisible();
    await expect(this.usersNavItem).toBeHidden();
  }

  /**
   * Verify navigation items for admin user (all items visible)
   */
  async verifyAdminUserNavigation(): Promise<void> {
    await this.verifyRegularUserNavigation();
    await expect(this.usersNavItem).toBeVisible();
  }

  /**
   * Wait for sidebar to be fully loaded
   */
  async waitForSidebarLoad(): Promise<void> {
    await this.sidebar.waitFor({ state: 'visible' });
    await this.sidebarNav.waitFor({ state: 'visible' });
    // Wait for navigation items to be loaded
    await this.planningNavItem.waitFor({ state: 'visible' });
  }
}
