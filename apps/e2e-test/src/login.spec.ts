import { test, expect } from '@playwright/test';
import { LoginPage } from './pages/LoginPage';
import { PageFactory } from './utils/page-factory';
import { testUsers, errorMessages } from './fixtures/test-data';

test.describe('Login Page', () => {
  let loginPage: LoginPage;
  let pageFactory: PageFactory;

  test.beforeEach(async ({ page }) => {
    pageFactory = new PageFactory(page);
    loginPage = pageFactory.createLoginPage();
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

  test('should successfully login with valid credentials', async () => {
    // Perform login
    await loginPage.login(testUsers.validUser.email, testUsers.validUser.password);

    // Wait for login result
    await loginPage.waitForLoginResult();

    // Verify successful login (user navigated away from login page)
    const isSuccessful = await loginPage.isLoginSuccessful();
    expect(isSuccessful).toBe(true);
  });

  test('should show error message with invalid credentials', async () => {
    // Attempt login with invalid credentials
    await loginPage.login(testUsers.invalidUser.email, testUsers.invalidUser.password);

    // Wait for login result
    await loginPage.waitForLoginResult();

    // Verify error message is displayed
    const errorMessage = await loginPage.getErrorMessage();
    expect(errorMessage).toBe(errorMessages.login.invalidCredentials);

    // Verify user is still on login page
    const isSuccessful = await loginPage.isLoginSuccessful();
    expect(isSuccessful).toBe(false);
  });


  test('should navigate to correct page based on user type', async () => {
    // Test mobile vs desktop navigation
    // This would require setting up different user types or device contexts
    await loginPage.login(testUsers.adminUser.email, testUsers.adminUser.password);
    await loginPage.waitForLoginResult();

    const isSuccessful = await loginPage.isLoginSuccessful();
    expect(isSuccessful).toBe(true);

    // You could add more specific assertions about which page the user lands on
    // based on whether it's a mobile or desktop session
  });
});
