import { Page } from '@playwright/test';

/**
 * Class for handling network request interception in e2e tests
 */
export class NetworkInterceptors {
  private page: Page;
  private wasteTransportId: string | null = null;
  private wasteTransportDisplayNumber: string | null = null;

  constructor(page: Page) {
    this.page = page;
  }

  /**
   * Set up interceptors for waste transport creation and planning data
   */
  async setupWasteTransportInterceptors(): Promise<void> {
    await this.setupWastePostInterceptor();
    await this.setupPlanningGetInterceptor();
  }

  /**
   * Set up interceptor for POST requests to the waste endpoint
   */
  private async setupWastePostInterceptor(): Promise<void> {
    await this.page.route('**/waste', async (route) => {
      const request = route.request();
      if (request.method() === 'POST') {
        // Continue with the request and get the response
        const response = await route.fetch();
        const responseBody = await response.json();
        
        // Extract the id from the response
        this.wasteTransportId = responseBody.id;
        console.log(`Intercepted waste transport id: ${this.wasteTransportId}`);
        
        // Forward the response
        await route.fulfill({ response });
      } else {
        // For non-POST requests, just continue normally
        await route.continue();
      }
    });
  }

  /**
   * Set up interceptor for GET requests to the planning endpoint
   */
  private async setupPlanningGetInterceptor(): Promise<void> {
    const todayDateString = new Date().toISOString().slice(0, 10); // format: YYYY-MM-DD
    await this.page.route(`**/planning/${todayDateString}`, async (route) => {
      const request = route.request();
      if (request.method() === 'GET') {
        // Continue with the request and get the response
        const response = await route.fetch();
        const responseBody = await response.json();
        
        // Only process if we have a waste transport ID
        if (this.wasteTransportId) {
          // Search through all trucks and dates to find our transport
          for (const truck of responseBody.transports) {
            const transports = truck.transports;
            for (const date in transports) {
              for (const transport of transports[date]) {
                if (transport.id === this.wasteTransportId) {
                  this.wasteTransportDisplayNumber = transport.displayNumber;
                  console.log(`Found transport with display number: ${this.wasteTransportDisplayNumber}`);
                  break;
                }
              }
              if (this.wasteTransportDisplayNumber) break;
            }
            if (this.wasteTransportDisplayNumber) break;
          }
        }
        
        // Forward the response
        await route.fulfill({ response });
      } else {
        // For non-GET requests, just continue normally
        await route.continue();
      }
    });
  }

  /**
   * Get the waste transport ID extracted from the intercepted response
   */
  getWasteTransportId(): string | null {
    return this.wasteTransportId;
  }

  /**
   * Get the waste transport display number extracted from the planning response
   */
  getWasteTransportDisplayNumber(): string | null {
    return this.wasteTransportDisplayNumber;
  }
}
