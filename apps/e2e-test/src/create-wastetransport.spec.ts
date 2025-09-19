import { test, expect, Page } from '@playwright/test';
import { LoginPage } from './pages/LoginPage';
import { PlanningPage } from './pages/PlanningPage';
import { PageFactory } from './utils/page-factory';
import { testData, testUsers } from './fixtures/test-data';

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
    await planningPage.openWasteTransportForm();
    await expect(page.locator('form')).toBeVisible();

    // Step 4: Fill in the form
    // Fill consignor party (sender)
    await pickFromMultiselect(page, 'consignor-party-select', testData.customer.name);
    // Select consignor classification (default is already selected, but let's explicitly select one)
    const classificationType = 1; // 'ontdoener'
    await page.locator(`[data-testid="consignor-classification-select-option-${classificationType}"]`).click();
    
    await pickFromMultiselect(page, 'carrier-party-select', 'Eazy Recycling');

    await pickFromMultiselect(page, 'container-operation-select', 'Container wisselen');

    // Step 5: Click on the "Volgende" button
    await page.locator('button[data-testid="next-button"]').click();

    const ophaalInfoSpan = page.locator('span.subtitle-2:has-text("Ophaal info")');
    await expect(ophaalInfoSpan).toBeVisible();
    

  });
});

async function pickFromMultiselect(page: Page, selectId: string, companyName: string) {
  const consignorSelect = page.locator(`#${selectId}`);
  await consignorSelect.click();
  await page.keyboard.type(companyName.substring(0, 4));
  // Select the option by its text content
  const consignorOption = page.locator('.react-select__option').filter({ hasText: companyName });
  await expect(consignorOption).toBeVisible();
  await consignorOption.click();
}

