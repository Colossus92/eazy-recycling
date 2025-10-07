import { test, expect } from '@playwright/test';
import { LoginPage } from './pages/LoginPage';
import { PlanningPage } from './pages/PlanningPage';
import { PageFactory } from './utils/page-factory';
import { testUsers } from './fixtures/test-data';

test.describe('Dashboard with Sidebar Navigation', () => {
  let loginPage: LoginPage;
  let planningPage: PlanningPage;
  let pageFactory: PageFactory;

  test.beforeEach(async ({ page }) => {
    pageFactory = new PageFactory(page);
    loginPage = pageFactory.createLoginPage();
    planningPage = pageFactory.createPlanningPage();

    // Login first to access dashboard
    await loginPage.navigateToLogin();
    await loginPage.login(testUsers.validUser.email, testUsers.validUser.password);
    await planningPage.verifyPlanningLoaded();
  });

  test('should display sidebar and navigate to dashboard', async () => {
    // Verify sidebar is visible and functional
    await planningPage.sidebar.verifySidebarVisible();

    // Verify Planning is the active nav item (since we're on dashboard/planning page)
    expect(await planningPage.sidebar.isPlanningActive()).toBe(true);
  });

  test('should show correct navigation items for regular user', async () => {
    // Verify regular user navigation (no admin items)
    await planningPage.verifyRegularUserAccess();

    // Verify Users nav item is not visible for regular user
    expect(await planningPage.sidebar.isUsersNavVisible()).toBe(false);
  });

  test('should navigate between different sections using sidebar', async () => {
    // Navigate to Containers - navigation method now includes waiting for active state
    await planningPage.navigateToContainers();
    // The active state is already verified in the navigation method, but we can still check URL
    expect(planningPage.page.url()).toContain('/containers');

    // Navigate to CRM
    await planningPage.navigateToCRM();
    expect(planningPage.page.url()).toContain('/crm');

    // Navigate to Trucks
    await planningPage.navigateToTrucks();
    expect(planningPage.page.url()).toContain('/trucks');

    // Navigate to Waste Streams
    await planningPage.navigateToWasteStreams();
    expect(planningPage.page.url()).toContain('/waste-streams');

    // Navigate back to Planning
    await planningPage.sidebar.navigateToPlanning();
    await expect(planningPage.page).toHaveURL('/');
  });

  // eslint-disable-next-line playwright/expect-expect
  test('should collapse and expand sidebar correctly', async () => {
    // Test sidebar toggle functionality
    await planningPage.testSidebarToggle();
  });

  test('should maintain sidebar state across navigation', async () => {
    // Collapse sidebar
    await planningPage.sidebar.collapseSidebar();
    expect(await planningPage.sidebar.isCollapsed()).toBe(true);

    // Navigate to different page
    await planningPage.navigateToContainers();

    // Verify sidebar remains collapsed after navigation
    expect(await planningPage.sidebar.isCollapsed()).toBe(true);

    // Expand sidebar
    await planningPage.sidebar.expandSidebar();
    expect(await planningPage.sidebar.isCollapsed()).toBe(false);
  });

  test('should display correct visible navigation items', async () => {
    // Get all visible nav items
    const visibleItems = await planningPage.sidebar.getVisibleNavItems();

    // For regular user, should not include admin items
    expect(visibleItems).toContain('Planning');
    expect(visibleItems).toContain('Containerbeheer');
    expect(visibleItems).toContain('CRM');
    expect(visibleItems).toContain('Afvalstroombeheer');
    expect(visibleItems).not.toContain('Gebruikersbeheer');
    expect(visibleItems).not.toContain('Masterdata');
  });
});
