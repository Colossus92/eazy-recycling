import { Page } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage';
import { PlanningPage } from '../pages/PlanningPage';
import { WasteTransportFormPage } from '../pages/WasteTransportFormPage';
import { MobileHomePage } from '../pages/MobileHomePage';

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

  /**
   * Create a PlanningPage instance
   */
  createPlanningPage(): PlanningPage {
    return new PlanningPage(this.page);
  }

  /**
   * Create a WasteTransportFormPage instance
   */
  createWasteTransportFormPage(): WasteTransportFormPage {
    return new WasteTransportFormPage(this.page);
  }
  
  /**
   * Create a MobileHomePage instance
   */
  createMobileHomePage(): MobileHomePage {
    return new MobileHomePage(this.page);
  }
}
