/* eslint-disable playwright/expect-expect */
import { expect, test } from '@playwright/test';
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

  test('should create a new waste transport', async ({ page }) => {
    // Setup response interception for the waste POST endpoint
    let wasteTransportId: string | null = null;
    let wasteTransportDisplayNumber: string | null = null;
    
    // Intercept the waste POST request
    await page.route('**/waste', async (route) => {
      const request = route.request();
      if (request.method() === 'POST') {
        // Continue with the request and get the response
        const response = await route.fetch();
        const responseBody = await response.json();
        
        // Extract the id from the response
        wasteTransportId = responseBody.id;
        console.log(`Intercepted waste transport id: ${wasteTransportId}`);
        
        // Forward the response
        await route.fulfill({ response });
      } else {
        // For non-POST requests, just continue normally
        await route.continue();
      }
    });
    
    // Intercept the planning GET request
    const todayDateString = new Date().toISOString().slice(0, 10); // format: YYYY-MM-DD
    await page.route(`**/planning/${todayDateString}`, async (route) => {
      const request = route.request();
      if (request.method() === 'GET') {
        // Continue with the request and get the response
        const response = await route.fetch();
        const responseBody = await response.json();
        
        // Only process if we have a waste transport ID
        if (wasteTransportId) {
          // Search through all trucks and dates to find our transport
          for (const truck of responseBody.transports) {
            const transports = truck.transports;
            for (const date in transports) {
              for (const transport of transports[date]) {
                if (transport.id === wasteTransportId) {
                  wasteTransportDisplayNumber = transport.displayNumber;
                  console.log(`Found transport with display number: ${wasteTransportDisplayNumber}`);
                  break;
                }
              }
              if (wasteTransportDisplayNumber) break;
            }
            if (wasteTransportDisplayNumber) break;
          }
        }
        
        // Forward the response
        await route.fulfill({ response });
      } else {
        // For non-GET requests, just continue normally
        await route.continue();
      }
    });
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
    
    // Verify that we successfully extracted the waste transport ID and display number
    expect(wasteTransportId).not.toBeNull();
    expect(wasteTransportDisplayNumber).not.toBeNull();
    console.log(`Created waste transport with ID: ${wasteTransportId} and display number: ${wasteTransportDisplayNumber}`);
    
    // Click on the planning card with our display number
    // Wait for the planning card to be visible - locate by the display number text content
    const planningCard = page.locator(`text=${wasteTransportDisplayNumber}`).first();
    await expect(planningCard).toBeVisible({ timeout: 5000 });
    
    // Click on the planning card
    await planningCard.click();
    console.log(`Clicked on planning card for transport ${wasteTransportDisplayNumber}`);
    
    // Verify that the transport details drawer opens
    const transportDetailsDrawer = page.locator('data-testid=transport-details-drawer-content');
    await expect(transportDetailsDrawer).toBeVisible({ timeout: 5000 });
  });
});

function getWednesdayDateAt(time: string): string {
  return new Date().toLocaleDateString('en-US', { weekday: 'long' }) === 'Wednesday'
        ? new Date().toISOString().split('T')[0] + 'T' + time
        : new Date(new Date().setDate(new Date().getDate() + (3 - new Date().getDay()))).toISOString().split('T')[0] + 'T' + time;
}
