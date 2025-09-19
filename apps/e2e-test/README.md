# E2E Test Suite - Page Object Model

This directory contains the end-to-end test suite for the Eazy Recycling application, implemented using Playwright and the Page Object Model (POM) design pattern.

## Page Object Model (POM)

The **Page Object Model** is a design pattern that creates an object-oriented class for each web page/screen in your application. It encapsulates the page elements and actions that can be performed on that page, making tests more maintainable, readable, and reusable.

### Benefits of POM

1. **Maintainability**: Changes to UI elements only require updates in one place
2. **Reusability**: Page objects can be reused across multiple tests
3. **Readability**: Tests become more readable and express business logic clearly
4. **Separation of Concerns**: Test logic is separated from page interaction logic

## Folder Structure

```
apps/e2e-test/src/
├── pages/                    # Page Object Model classes
│   ├── base/
│   │   └── BasePage.ts      # Base class with common functionality
│   ├── LoginPage.ts         # Login page POM
│   └── [OtherPage].ts       # Additional page POMs
├── fixtures/                 # Test data and fixtures
│   └── test-data.ts         # Centralized test data
├── utils/                   # Utility classes and helpers
│   └── page-factory.ts      # Factory for creating page objects
├── screenshots/             # Test screenshots (auto-generated)
└── *.spec.ts               # Test specification files
```

## Key Components

### 1. BasePage Class (`pages/base/BasePage.ts`)

The base class that all page objects extend. It provides common functionality:

- Navigation methods (`goto()`, `waitForPageLoad()`)
- Element interaction helpers (`fillFieldByTestId()`, `clickByTestId()`)
- Utility methods (`takeScreenshot()`, `waitForElement()`)

### 2. Page Object Classes (`pages/*.ts`)

Each page in your application should have a corresponding page object class:

- **LoginPage**: Handles login form interactions
- **DashboardPage**: (Future) Main dashboard interactions
- **TruckPage**: (Future) Truck management interactions

### 3. Page Factory (`utils/page-factory.ts`)

Centralizes page object creation and provides type safety. Use this to create page object instances in your tests.

### 4. Test Data (`fixtures/test-data.ts`)

Centralized location for test data, user credentials, and expected error messages.

## Usage Examples

### Basic Test Structure

```typescript
import { test, expect } from '@playwright/test';
import { LoginPage } from './pages/LoginPage';
import { PageFactory } from './utils/page-factory';
import { testUsers } from './fixtures/test-data';

test.describe('Login Tests', () => {
  let loginPage: LoginPage;

  test.beforeEach(async ({ page }) => {
    const pageFactory = new PageFactory(page);
    loginPage = pageFactory.createLoginPage();
    await loginPage.navigateToLogin();
  });

  test('should login successfully', async () => {
    await loginPage.login(testUsers.validUser.email, testUsers.validUser.password);
    await loginPage.waitForLoginResult();
    
    expect(await loginPage.isLoginSuccessful()).toBe(true);
  });
});
```

### Creating New Page Objects

1. **Create the page class** extending `BasePage`:

```typescript
import { Page, Locator } from '@playwright/test';
import { BasePage } from './base/BasePage';

export class DashboardPage extends BasePage {
  private readonly welcomeMessage: Locator;
  private readonly navigationMenu: Locator;

  constructor(page: Page) {
    super(page, '/dashboard');
    this.welcomeMessage = page.locator('[data-testid="welcome-message"]');
    this.navigationMenu = page.locator('[data-testid="nav-menu"]');
  }

  async verifyDashboardLoaded(): Promise<void> {
    await this.waitForElement(this.welcomeMessage);
    await this.waitForElement(this.navigationMenu);
  }
}
```

2. **Add to PageFactory**:

```typescript
createDashboardPage(): DashboardPage {
  return new DashboardPage(this.page);
}
```

3. **Use in tests**:

```typescript
const dashboardPage = pageFactory.createDashboardPage();
await dashboardPage.goto();
await dashboardPage.verifyDashboardLoaded();
```

## Best Practices

### 1. Element Selection Strategy

- **Prefer `data-test-id` attributes** for test-specific element identification
- Use semantic selectors (text content, roles) for stable elements
- Avoid CSS classes and complex selectors that may change

### 2. Method Naming Conventions

- **Actions**: Use verbs (`click()`, `fill()`, `select()`)
- **Assertions**: Use `verify` prefix (`verifyPageLoaded()`, `verifyErrorMessage()`)
- **Getters**: Use `get` prefix (`getTitle()`, `getErrorMessage()`)
- **Checkers**: Use `is` prefix (`isVisible()`, `isEnabled()`)

### 3. Wait Strategies

- Always wait for elements before interacting with them
- Use `waitForElement()` for visibility
- Use `waitForLoadState()` for page transitions
- Implement specific wait methods for complex interactions

### 4. Error Handling

- Create methods to check for error states
- Centralize error message expectations in test data
- Implement timeout handling for slow operations

### 5. Test Data Management

- Keep test data in separate fixture files
- Use meaningful names for test users and data sets
- Avoid hardcoding values in page objects or tests

## Data Test IDs

For reliable element selection, add `data-test-id` attributes to your React components:

```tsx
// In your React components
<input 
  data-test-id="email-field"
  type="email" 
  {...props} 
/>

<button 
  data-test-id="login-button"
  type="submit"
>
  Login
</button>
```

## Running Tests

### Standard Test Execution (with Docker Compose)

By default, the tests will spin up the required Docker containers using docker-compose:

```bash
# Run all tests (will start Docker containers)
npx playwright test

# Run tests in headed mode (with browser UI)
npx playwright test --headed

# Run specific test file
npx playwright test login.spec.ts

# Run tests with debug mode
npx playwright test --debug
```

### Development Mode (Against Running Docker)

For faster development cycles, you can run tests against an already running Docker environment:

```bash
# First, start the Docker environment (if not already running)
./scripts/start-e2e-dev.sh

# Run tests against existing Docker environment (won't tear down containers)
npm run e2e:dev

# Run tests with UI against existing Docker environment
npm run e2e:dev:ui

# Run specific test file against existing Docker environment
npm run e2e:dev -- login.spec.ts

# When you're completely done, stop the Docker environment
./scripts/stop-e2e-dev.sh
```

This approach skips the Docker container startup/teardown process, making the test cycle much faster during development. The development mode:

1. Uses a custom Playwright configuration (`playwright.config.dev.ts`) that doesn't include webServer startup
2. Uses a special global teardown that doesn't shut down Docker containers
3. Sets the `PLAYWRIGHT_TEARDOWN=false` environment variable as an extra safety measure

All these measures ensure your Docker containers remain running between test runs for faster development.

### Common Issues

1. **Element not found**: Ensure `data-test-id` attributes are added to components
2. **Timing issues**: Use proper wait strategies instead of fixed delays
3. **Test flakiness**: Implement robust wait conditions and error handling

### Debug Tips

- Use `--headed` mode to see browser interactions
- Add `await page.pause()` to stop execution and inspect
- Use `takeScreenshot()` method to capture test state
- Check browser console for JavaScript errors
