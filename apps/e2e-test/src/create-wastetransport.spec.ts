import { test, expect } from '@playwright/test';
import { LoginPage } from './pages/LoginPage';
import { PlanningPage } from './pages/PlanningPage';
import { WasteTransportFormPage } from './pages/WasteTransportFormPage';
import { PageFactory } from './utils/page-factory';
import { testData, testUsers } from './fixtures/test-data';

test.describe('Create New Waste Transport', () => {
  let loginPage: LoginPage;
  let planningPage: PlanningPage;
  let wasteTransportFormPage: WasteTransportFormPage;
  let pageFactory: PageFactory;

  test.beforeEach(async ({ page }) => {
    pageFactory = new PageFactory(page);
    loginPage = pageFactory.createLoginPage();
    planningPage = pageFactory.createPlanningPage();
    wasteTransportFormPage = pageFactory.createWasteTransportFormPage();
  });

  test('should create a new waste transport', async () => {
    // Step 1: Login as planner
    await loginPage.navigateToLogin();
    await loginPage.login(testUsers.validUser.email, testUsers.validUser.password);
    await loginPage.waitForLoginResult();
    
    // Step 2: Verify we're on the planning page
    await planningPage.verifyPlanningLoaded();
    
    // Step 3: Click on the "New Waste Transport" button
    await planningPage.openWasteTransportForm();
    
    // Step 4: Verify the form is loaded
    await wasteTransportFormPage.verifyFormLoaded();

    // Step 5: Fill in the main section of the form
    await wasteTransportFormPage.fillMainSection(
      testData.customer.name,
      1, // 'ontdoener'
      'Eazy Recycling',
      'Container wisselen'
    );

    // Step 6: Navigate to the next step
    await wasteTransportFormPage.goToNextStep();

    // Step 7: Verify we're on the pickup info section
    await wasteTransportFormPage.pickupSection.verifyPickupInfoSectionVisible();

    await wasteTransportFormPage.pickupSection.fillPickupCompanyAddress(
      testData.customer.name,
    );

    await wasteTransportFormPage.pickupSection.fillPickupDateTime(
      '2025-09-20T10:00',
    );

    await wasteTransportFormPage.goToNextStep();
  });
});

// We've moved the pickFromMultiselect function to the WasteTransportFormComponent class

