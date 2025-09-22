import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './base/BasePage';

/**
 * Page Object Model for the Login Page
 * Encapsulates all elements and actions available on the login page
 */
export class LoginPage extends BasePage {
  // Page elements
  private readonly welcomeTitle: Locator;
  private readonly welcomeSubtitle: Locator;
  private readonly emailField: Locator;
  private readonly passwordField: Locator;
  private readonly loginButton: Locator;
  private readonly formError: Locator;

  constructor(page: Page) {
    super(page, '/login');

    // Initialize locators
    this.welcomeTitle = page.locator('h3:has-text("Welkom bij Eazy Recycling")');
    this.welcomeSubtitle = page.locator('text=Voer uw inloggegevens in');
    this.emailField = page.locator('[data-testid="email"]');
    this.passwordField = page.locator('[data-testid="password"]');
    this.loginButton = page.locator('button[type="submit"]');
    this.formError = page.locator('.text-color-status-error-dark');
  }

  /**
   * Navigate to login page and wait for it to load
   */
  async navigateToLogin(): Promise<void> {
    await this.goto();
    await this.waitForPageLoad();
    await this.waitForElement(this.welcomeTitle);
  }

  /**
   * Fill in the email field
   */
  async fillEmail(email: string): Promise<void> {
    await this.emailField.fill(email);
  }

  /**
   * Fill in the password field
   */
  async fillPassword(password: string): Promise<void> {
    await this.passwordField.fill(password);
  }

  /**
   * Click the login button
   */
  async clickLogin(): Promise<void> {
    await this.loginButton.click();
  }

  /**
   * Complete login flow with email and password
   */
  async login(email: string, password: string): Promise<void> {
    await this.fillEmail(email);
    await this.fillPassword(password);
    await this.clickLogin();
  }
  
  /**
   * Check if login was successful (user navigated away from login page)
   */
  async isLoginSuccessful(): Promise<boolean> {
    const currentUrl = this.page.url();
    return !currentUrl.includes('/login');
  }

  /**
   * Get the error message displayed on the form
   */
  async getErrorMessage(): Promise<string> {
    if (await this.formError.isVisible()) {
      return await this.formError.textContent() || '';
    }
    return '';
  }

  /**
   * Check if the login form is visible
   */
  async isLoginFormVisible(): Promise<boolean> {
    return await this.emailField.isVisible() &&
           await this.passwordField.isVisible() &&
           await this.loginButton.isVisible();
  }

  /**
   * Verify the page elements are displayed correctly
   */
  async verifyPageElements(): Promise<void> {
    await expect(this.welcomeTitle).toBeVisible();
    await expect(this.welcomeSubtitle).toBeVisible();
    await expect(this.emailField).toBeVisible();
    await expect(this.passwordField).toBeVisible();
    await expect(this.loginButton).toBeVisible();
  }

  /**
   * Get the login button text
   */
  async getLoginButtonText(): Promise<string> {
    return await this.loginButton.textContent() || '';
  }
}
