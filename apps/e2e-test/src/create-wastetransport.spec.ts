import { test, expect } from '@playwright/test';
import { LoginPage } from './pages/LoginPage';
import { PlanningPage } from './pages/PlanningPage';
import { PageFactory } from './utils/page-factory';
import { testUsers } from './fixtures/test-data';

test.describe('Create New Waste Transport', () => {
  let loginPage: LoginPage;
  let planningPage: PlanningPage;
  let pageFactory: PageFactory;

  test.beforeEach(async ({ page }) => {
    pageFactory = new PageFactory(page);
    loginPage = pageFactory.createLoginPage();
    planningPage = pageFactory.createPlanningPage();
  });

  test('should create a new waste transport', async ({ page }) => {
    // Step 1: Login as planner
    await loginPage.navigateToLogin();
    await loginPage.login(testUsers.validUser.email, testUsers.validUser.password);
    await loginPage.waitForLoginResult();
    
    // Step 2: Verify we're on the planning page
    await planningPage.verifyPlanningLoaded();
    
    // Step 3: Click on the "New Waste Transport" button
    await page.locator('[data-testid="new-waste-transport-button"]').click();
    
    // Verify the waste transport form is opened
    await expect(page.locator('form')).toBeVisible();
  });
});
