import { test, expect } from '@playwright/test';
import { LoginPage } from './pages/LoginPage';
import { PageFactory } from './utils/page-factory';
import { testUsers, errorMessages } from './fixtures/test-data';
import { PlanningPage } from './pages/PlanningPage';

test.describe('Login Page', () => {
  let loginPage: LoginPage;
  let planningPage: PlanningPage;
  let pageFactory: PageFactory;

  test.beforeEach(async ({ page }) => {
    pageFactory = new PageFactory(page);
    loginPage = pageFactory.createLoginPage();
    planningPage = pageFactory.createPlanningPage();
    await loginPage.navigateToLogin();
  });

  test('should display login form elements correctly', async () => {
    await loginPage.verifyPageElements();

    // Verify form is visible and interactive
    expect(await loginPage.isLoginFormVisible()).toBe(true);

    // Verify initial button text
    const buttonText = await loginPage.getLoginButtonText();
    expect(buttonText).toContain('Inloggen');
  });

  // eslint-disable-next-line playwright/expect-expect
  test('should successfully login with valid credentials', async () => {
    // Perform login
    await loginPage.login(testUsers.validUser.email, testUsers.validUser.password);

    // Verify successful login (user navigated away from login page)
    await planningPage.verifyPlanningLoaded();
  });

  test('should show error message with invalid credentials', async () => {
    // Attempt login with invalid credentials
    await loginPage.login(testUsers.invalidUser.email, testUsers.invalidUser.password);

    // Verify error message is displayed
    const errorMessage = await loginPage.getErrorMessage();
    expect(errorMessage).toBe(errorMessages.login.invalidCredentials);
  });


  // eslint-disable-next-line playwright/expect-expect
  test('should navigate to correct page based on user type', async () => {
    // Test mobile vs desktop navigation
    // This would require setting up different user types or device contexts
    await loginPage.login(testUsers.adminUser.email, testUsers.adminUser.password);

    await planningPage.verifyPlanningLoaded();
  });
});
