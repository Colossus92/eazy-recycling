import { Page } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage';

/**
 * Page Factory for creating page object instances
 * Centralizes page object creation and provides type safety
 */
export class PageFactory {
  private page: Page;

  constructor(page: Page) {
    this.page = page;
  }

  /**
   * Create a LoginPage instance
   */
  createLoginPage(): LoginPage {
    return new LoginPage(this.page);
  }

  // Add more page creators as you build more Page Objects
  // createDashboardPage(): DashboardPage {
  //   return new DashboardPage(this.page);
  // }
  
  // createTruckPage(): TruckPage {
  //   return new TruckPage(this.page);
  // }
}
