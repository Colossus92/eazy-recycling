/* eslint-disable playwright/expect-expect */
import { expect, test, devices, chromium } from '@playwright/test';
import { LoginPage } from './pages/LoginPage';
import { PageFactory } from './utils/page-factory';
import { testUsers } from './fixtures/test-data';
import { MobileHomePage } from './pages/MobileHomePage';
import { MobileTransportDetailsPage } from './pages/MobileTransportDetailsPage';

// Configure this entire test file to use iPhone 15
test.use(devices['iPhone 15']);

let apiContext;

test.describe('Driver completes transport', () => {
  let loginPage: LoginPage;
  let pageFactory: PageFactory;
  let mobileHomePage: MobileHomePage;
  let mobileTransportDetailsPage: MobileTransportDetailsPage;
  
  test.beforeAll(async ({ playwright }) => {
    // Create a desktop Chrome browser explicitly for setup (not affected by test.use)
    const desktopBrowser = await chromium.launch();
    const context = await desktopBrowser.newContext();
    const page = await context.newPage();
    const pageFactory = new PageFactory(page);
    const mobileHomePage = pageFactory.createMobileHomePage();

    // Login to get the auth token
    const tempLoginPage = new LoginPage(page);
    await tempLoginPage.navigateToLogin();
    await tempLoginPage.login(testUsers.validUser .email, testUsers.validUser.password);
    
    // Wait for navigation to complete after login
    await page.waitForURL(/.*/, { waitUntil: 'load' });
    await mobileHomePage.verifyMobileHomePageLoaded();
    
    // Get the token from local storage
    const authData = await page.evaluate(() => {
      // @ts-expect-error - localStorage is available in browser context
      console.log(JSON.stringify(localStorage));
      // @ts-expect-error - localStorage is available in browser context
      const authString = localStorage.getItem('sb-127-auth-token');
      if (!authString) return null;
      return JSON.parse(authString);
    }) as { access_token: string } | null;
    
    expect(authData).toBeTruthy();
    const accessToken = authData?.access_token;
    
    // Clean up the desktop browser
    await desktopBrowser.close();
    
    // Create API context with the bearer token
    // apiContext = await playwright.request.newContext({
    //   // All requests we send go to this API endpoint.
    //   baseURL: 'http://localhost:8080',
    //   extraHTTPHeaders: {
    //     'Authorization': `Bearer ${accessToken}`,
    //   },
    // });

    // apiContext.post('/transport/waste', {
    //   data: {
    //     "consignorPartyId": "6a683b2a-96d6-454c-8cae-4a7e2a03f249",
    //     "carrierPartyId": "6a683b2a-96d6-454c-8cae-4a7e2a03f249",
    //     "containerOperation": "EMPTY",
    //     "pickupCompanyId": "e6b151a2-adb9-43d9-8108-1a021fd2f3b7",
    //     "pickupStreet": "Test",
    //     "pickupBuildingNumber": "2a",
    //     "pickupPostalCode": "1234 AB",
    //     "pickupCity": "Plaats",
    //     "pickupDateTime": "2025-10-01T15:12",
    //     "deliveryCompanyId": "e6b151a2-adb9-43d9-8108-1a021fd2f3b7",
    //     "deliveryStreet": "Test",
    //     "deliveryBuildingNumber": "2a",
    //     "deliveryPostalCode": "1234 AB",
    //     "deliveryCity": "Plaats",
    //     "deliveryDateTime": "2025-10-01T16:12",
    //     "truckId": "86-BVB-6",
    //     "driverId": "e2d6b650-3ff9-42de-8dbb-1cda2950ccbc",
    //     "containerId": "3eb0e01e-155c-4e0a-9162-3f4d14d0f13f",
    //     "note": "Created by E2E test",
    //     "transportType": "WASTE",
    //     "consigneePartyId": "6a683b2a-96d6-454c-8cae-4a7e2a03f249",
    //     "pickupPartyId": "6a683b2a-96d6-454c-8cae-4a7e2a03f249",
    //     "consignorClassification": 1,
    //     "wasteStreamNumber": "987654321987",
    //     "weight": "2000",
    //     "unit": "kg",
    //     "quantity": "1",
    //     "goodsName": "Tesss",
    //     "euralCode": "16 01 18",
    //     "processingMethodCode": "A.02"
    // }
    // })
  });
  
  test.beforeEach(async ({ page }) => {
    pageFactory = new PageFactory(page);
    loginPage = pageFactory.createLoginPage();
    mobileHomePage = pageFactory.createMobileHomePage();
    mobileTransportDetailsPage = pageFactory.createMobileTransportDetailsPage();
  });

  test('should complete transport', async ({ page }) => {
    // Step 1: Login as driver (on iPhone 15)
    await loginPage.navigateToLogin();
    await loginPage.login(testUsers.driverUser.email, testUsers.driverUser.password);   
    
    await mobileHomePage.verifyMobileHomePageLoaded();
    await mobileHomePage.navigateToWednesdaysTransportList();
    await mobileHomePage.openTransportDetails('25-0010');
    await mobileTransportDetailsPage.verifyMobileTransportDetailsPageLoaded();
    await mobileTransportDetailsPage.checkTransportDetails();
  });
});
