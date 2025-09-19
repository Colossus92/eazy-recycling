import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './base/BasePage';
import { WasteTransportMainSectionComponent } from '../components/WasteTransportMainSectionComponent';
import { WasteTransportPickupSectionComponent } from '../components/WasteTransportPickupSectionComponent';
import { WasteTransportDeliverySectionComponent } from '../components/WasteTransportDeliverySectionComponent';
import { WasteTransportGoodsSectionComponent } from '../components/WasteTransportGoodsSectionComponent';
import { WasteTransportDetailsSectionComponent } from '../components/WasteTransportDetailsSectionComponent';

/**
 * Page Object Model for the Waste Transport Form Page
 * This page is accessible from the Planning page
 */
export class WasteTransportFormPage extends BasePage {
  // Form components for each section
  public readonly mainSection: WasteTransportMainSectionComponent;
  public readonly pickupSection: WasteTransportPickupSectionComponent;
  public readonly deliverySection: WasteTransportDeliverySectionComponent;
  public readonly goodsSection: WasteTransportGoodsSectionComponent;
  public readonly detailsSection: WasteTransportDetailsSectionComponent;
  
  // Form navigation elements
  private readonly formContainer: Locator;
  private readonly nextButton: Locator;
  private readonly backButton: Locator;
  private readonly cancelButton: Locator;
  private readonly stepper: Locator;
  
  constructor(page: Page) {
    // The URL is the same as planning page since this is a modal form
    super(page, '/');
    
    // Initialize form navigation elements
    this.formContainer = page.locator('form');
    this.nextButton = page.locator('button[data-testid="next-button"]');
    this.backButton = page.locator('button[data-testid="back-button"]');
    this.cancelButton = page.locator('button[data-testid="cancel-button"]');
    this.stepper = page.locator('.stepper');
    
    // Initialize section components
    this.mainSection = new WasteTransportMainSectionComponent(page);
    this.pickupSection = new WasteTransportPickupSectionComponent(page);
    this.deliverySection = new WasteTransportDeliverySectionComponent(page);
    this.goodsSection = new WasteTransportGoodsSectionComponent(page);
    this.detailsSection = new WasteTransportDetailsSectionComponent(page);
  }
  
  /**
   * Verify the waste transport form is loaded correctly
   */
  async verifyFormLoaded(): Promise<void> {
    await expect(this.formContainer).toBeVisible();
  }
  
  /**
   * Fill the main section of the waste transport form
   */
  async fillMainSection(consignorName: string, classificationType: number, carrierName: string, containerOperation: string): Promise<void> {
    await this.mainSection.fillMainSection(consignorName, classificationType, carrierName, containerOperation);
  }

  /**
   * Fill the pickup section of the waste transport form
   */
  async fillPickupSection(pickupCompanyAddress: string, pickupDateTime: string): Promise<void> {
    await this.pickupSection.fillPickupSection(pickupCompanyAddress, pickupDateTime);
  }

  /**
   * Fill the delivery section of the waste transport form
   */
  async fillDeliverySection(deliveryCompanyAddress: string, deliveryDateTime: string): Promise<void> {
    await this.deliverySection.fillDeliverySection(deliveryCompanyAddress, deliveryDateTime);
  }
  
  /**
   * Navigate to the next step in the form
   */
  async goToNextStep(): Promise<void> {
    await this.nextButton.click();
  }
  
  /**
   * Navigate to the previous step in the form
   */
  async goToPreviousStep(): Promise<void> {
    await this.backButton.click();
  }
  
  /**
   * Cancel the form
   */
  async cancelForm(): Promise<void> {
    await this.cancelButton.click();
  }
  
  /**
   * Get the current step index (0-based)
   */
  async getCurrentStepIndex(): Promise<number> {
    const activeStep = await this.stepper.locator('.active').count();
    return activeStep - 1;
  }
  
  /**
   * Navigate to a specific step by clicking on the stepper
   */
  async navigateToStep(stepIndex: number): Promise<void> {
    await this.stepper.locator(`li:nth-child(${stepIndex + 1})`).click();
  }

}
