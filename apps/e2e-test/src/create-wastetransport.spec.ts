import { expect, test } from '@playwright/test';
import { LoginPage } from './pages/LoginPage';
import { PlanningPage } from './pages/PlanningPage';
import { WasteTransportFormPage } from './pages/WasteTransportFormPage';
import { PageFactory } from './utils/page-factory';
import { NetworkInterceptors } from './utils/network-interceptors';
import { testData, testUsers } from './fixtures/test-data';

test.describe('Create New Waste Transport', () => {
  let loginPage: LoginPage;
  let planningPage: PlanningPage;
  let wasteTransportFormPage: WasteTransportFormPage;
  let pageFactory: PageFactory;
  let networkInterceptors: NetworkInterceptors;

  test.beforeEach(async ({ page }) => {
    pageFactory = new PageFactory(page);
    loginPage = pageFactory.createLoginPage();
    planningPage = pageFactory.createPlanningPage();
    wasteTransportFormPage = pageFactory.createWasteTransportFormPage();
  });

  test('should create a new waste transport', async ({ page }) => {
    // Setup network interceptors
    networkInterceptors = new NetworkInterceptors(page);
    await networkInterceptors.setupWasteTransportInterceptors();
    // Step 1: Login as planner
    await loginPage.navigateToLogin();
    await loginPage.login(testUsers.validUser.email, testUsers.validUser.password);
    
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
      testData.tenant.name,
      'Container wisselen'
    );
    await wasteTransportFormPage.goToNextStep();

    // Step 6: Fill in the pickup section
    await wasteTransportFormPage.fillPickupSection(
      testData.customer.name,
      getWednesdayDateAt('10:00'),
    );
    await wasteTransportFormPage.goToNextStep();

    // Step 7: Fill in the delivery section
    await wasteTransportFormPage.fillDeliverySection(
      testData.tenant.name,
      getWednesdayDateAt('14:00'),
    );
    await wasteTransportFormPage.goToNextStep();

    // Step 8: Fill in the waste section
    await wasteTransportFormPage.fillGoodsSection(testData.customer.name);
    await wasteTransportFormPage.goToNextStep();

    await wasteTransportFormPage.detailsSection.fillTransportDetails();
    await wasteTransportFormPage.submitForm();

    await planningPage.verifyPlanningLoaded();
    
    // Get the waste transport ID and display number from the interceptors
    const wasteTransportId = networkInterceptors.getWasteTransportId();
    const wasteTransportDisplayNumber = networkInterceptors.getWasteTransportDisplayNumber();
    
    // Verify that we successfully extracted the waste transport ID and display number
    expect(wasteTransportId).not.toBeNull();
    expect(wasteTransportDisplayNumber).not.toBeNull();
    console.log(`Created waste transport with ID: ${wasteTransportId} and display number: ${wasteTransportDisplayNumber}`);
    
    // Verify that we have a display number before proceeding
    expect(wasteTransportDisplayNumber, 'Waste transport display number should be extracted').toBeTruthy();

    // Start the waybill download
    await planningPage.downloadWaybill(wasteTransportDisplayNumber as string);
  });
});

function getWednesdayDateAt(time: string): string {
  return new Date().toLocaleDateString('en-US', { weekday: 'long' }) === 'Wednesday'
        ? new Date().toISOString().split('T')[0] + 'T' + time
        : new Date(new Date().setDate(new Date().getDate() + (3 - new Date().getDay()))).toISOString().split('T')[0] + 'T' + time;
}
