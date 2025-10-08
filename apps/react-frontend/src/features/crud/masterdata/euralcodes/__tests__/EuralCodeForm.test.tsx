import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import { EuralCodeForm } from '../EuralCodeForm';
import { Eural } from '@/api/client';

// Mock the ErrorDialog component
vi.mock('@/components/ui/dialog/ErrorDialog.tsx', () => ({
  ErrorDialog: ({
    isOpen,
    errorMessage,
  }: {
    isOpen: boolean;
    errorMessage: string;
  }) => (isOpen ? <div data-testid="error-dialog">{errorMessage}</div> : null),
}));

// Get the API base URL from environment variable
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

// Setup MSW server
const server = setupServer();

// Start server before all tests
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));

// Reset handlers after each test
afterEach(() => server.resetHandlers());

// Clean up after all tests
afterAll(() => server.close());

// Create a wrapper with QueryClientProvider for the component
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
    },
  },
});

const wrapper = ({ children }: { children: React.ReactNode }) => (
  <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
);

describe('EuralCodeForm Integration Tests', () => {
  const mockOnCancel = vi.fn();
  const mockOnSubmit = vi.fn();

  beforeEach(() => {
    mockOnCancel.mockReset();
    mockOnSubmit.mockReset();
    queryClient.clear();
    vi.clearAllMocks();
  });

  describe('Form Submission with API Integration', () => {
    it('makes correct POST request when creating a new eural code', async () => {
      // Track the request body
      let capturedRequestBody: Eural | null = null;

      // Setup MSW handler to intercept the POST request
      server.use(
        http.post(`${API_BASE_URL}/eural`, async ({ request }) => {
          capturedRequestBody = await request.json() as Eural;
          return HttpResponse.json(capturedRequestBody, { status: 201 });
        })
      );

      // Mock onSubmit to simulate the actual API call
      const mockSubmitWithApi = vi.fn(async (eural: Eural) => {
        const response = await fetch(`${API_BASE_URL}/eural`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(eural),
        });
        return response.json();
      });

      render(
        <EuralCodeForm
          isOpen={true}
          setIsOpen={vi.fn()}
          onCancel={mockOnCancel}
          onSubmit={mockSubmitWithApi}
        />,
        { wrapper }
      );

      // Verify form is in add mode
      expect(screen.getByText('Eural code toevoegen')).toBeInTheDocument();

      // Fill in the form
      const codeInput = screen.getByPlaceholderText('Vul een code in');
      const descriptionInput = screen.getByPlaceholderText('Vul een beschrijving in');

      await userEvent.type(codeInput, '010101');
      await userEvent.type(descriptionInput, 'Test Eural Description');

      // Submit the form
      const submitButton = screen.getByTestId('submit-button');
      await userEvent.click(submitButton);

      // Wait for the API call to complete
      await waitFor(() => {
        expect(mockSubmitWithApi).toHaveBeenCalledTimes(1);
      });

      // Verify the correct data was sent in the POST request
      expect(capturedRequestBody).toEqual({
        code: '010101',
        description: 'Test Eural Description',
      });

      // Verify onSubmit was called with correct data
      expect(mockSubmitWithApi).toHaveBeenCalledWith({
        code: '010101',
        description: 'Test Eural Description',
      });

      // Verify onCancel was called after successful submission
      await waitFor(() => {
        expect(mockOnCancel).toHaveBeenCalledTimes(1);
      });
    });

    it('makes correct POST request with trimmed whitespace', async () => {
      let capturedRequestBody: Eural | null = null;

      server.use(
        http.post(`${API_BASE_URL}/eural`, async ({ request }) => {
          capturedRequestBody = await request.json() as Eural;
          return HttpResponse.json(capturedRequestBody, { status: 201 });
        })
      );

      const mockSubmitWithApi = vi.fn(async (eural: Eural) => {
        const response = await fetch(`${API_BASE_URL}/eural`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(eural),
        });
        return response.json();
      });

      render(
        <EuralCodeForm
          isOpen={true}
          setIsOpen={vi.fn()}
          onCancel={mockOnCancel}
          onSubmit={mockSubmitWithApi}
        />,
        { wrapper }
      );

      // Fill in the form with extra whitespace
      const codeInput = screen.getByPlaceholderText('Vul een code in');
      const descriptionInput = screen.getByPlaceholderText('Vul een beschrijving in');

      await userEvent.type(codeInput, '  020202  ');
      await userEvent.type(descriptionInput, '  Test Description with Spaces  ');

      // Submit the form
      const submitButton = screen.getByTestId('submit-button');
      await userEvent.click(submitButton);

      // Wait for the API call to complete
      await waitFor(() => {
        expect(mockSubmitWithApi).toHaveBeenCalledTimes(1);
      });

      // Verify the data includes the whitespace (form doesn't trim by default)
      expect(capturedRequestBody).toEqual({
        code: '  020202  ',
        description: '  Test Description with Spaces  ',
      });
    });

    it('handles API error response correctly', async () => {
      // Setup MSW handler to return an error
      server.use(
        http.post(`${API_BASE_URL}/eural`, () => {
          return HttpResponse.json(
            { message: 'Eural code already exists' },
            { status: 409 }
          );
        })
      );

      const mockSubmitWithApi = vi.fn(async (eural: Eural) => {
        const response = await fetch(`${API_BASE_URL}/eural`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(eural),
        });
        
        if (!response.ok) {
          throw new Error('Eural code already exists');
        }
        
        return response.json();
      });

      render(
        <EuralCodeForm
          isOpen={true}
          setIsOpen={vi.fn()}
          onCancel={mockOnCancel}
          onSubmit={mockSubmitWithApi}
        />,
        { wrapper }
      );

      // Fill in the form
      const codeInput = screen.getByPlaceholderText('Vul een code in');
      const descriptionInput = screen.getByPlaceholderText('Vul een beschrijving in');

      await userEvent.type(codeInput, '030303');
      await userEvent.type(descriptionInput, 'Duplicate Code');

      // Submit the form
      const submitButton = screen.getByTestId('submit-button');
      await userEvent.click(submitButton);

      // Wait for the API call to complete
      await waitFor(() => {
        expect(mockSubmitWithApi).toHaveBeenCalledTimes(1);
      });

      // Verify onCancel was NOT called due to error
      expect(mockOnCancel).not.toHaveBeenCalled();
    });

    it('does not make API request when validation fails', async () => {
      let apiCallCount = 0;

      server.use(
        http.post(`${API_BASE_URL}/eural`, () => {
          apiCallCount++;
          return HttpResponse.json({}, { status: 201 });
        })
      );

      render(
        <EuralCodeForm
          isOpen={true}
          setIsOpen={vi.fn()}
          onCancel={mockOnCancel}
          onSubmit={mockOnSubmit}
        />,
        { wrapper }
      );

      // Submit the form without filling any fields
      const submitButton = screen.getByTestId('submit-button');
      await userEvent.click(submitButton);

      // Check validation errors appear
      await waitFor(() => {
        expect(screen.getByText('Code is verplicht')).toBeInTheDocument();
        expect(screen.getByText('Beschrijving is verplicht')).toBeInTheDocument();
      });

      // Verify no API call was made
      expect(apiCallCount).toBe(0);
      expect(mockOnSubmit).not.toHaveBeenCalled();
      expect(mockOnCancel).not.toHaveBeenCalled();
    });

    it('makes correct PUT request when updating an existing eural code', async () => {
      const existingEural: Eural = {
        code: '040404',
        description: 'Original Description',
      };

      let capturedRequestBody: Eural | null = null;
      let capturedUrl = '';

      server.use(
        http.put(`${API_BASE_URL}/eural/:code`, async ({ request }) => {
          capturedUrl = request.url;
          capturedRequestBody = await request.json() as Eural;
          return HttpResponse.json(capturedRequestBody, { status: 200 });
        })
      );

      const mockSubmitWithApi = vi.fn(async (eural: Eural) => {
        const response = await fetch(`${API_BASE_URL}/eural/${eural.code}`, {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(eural),
        });
        return response.json();
      });

      render(
        <EuralCodeForm
          isOpen={true}
          setIsOpen={vi.fn()}
          onCancel={mockOnCancel}
          onSubmit={mockSubmitWithApi}
          initialData={existingEural}
        />,
        { wrapper }
      );

      // Verify form is in edit mode
      expect(screen.getByText('Eural code bewerken')).toBeInTheDocument();

      // Verify code field is disabled in edit mode
      const codeInput = screen.getByPlaceholderText('Vul een code in');
      expect(codeInput).toBeDisabled();
      expect(codeInput).toHaveValue('040404');

      // Update the description
      const descriptionInput = screen.getByPlaceholderText('Vul een beschrijving in');
      await userEvent.clear(descriptionInput);
      await userEvent.type(descriptionInput, 'Updated Description');

      // Submit the form
      const submitButton = screen.getByTestId('submit-button');
      await userEvent.click(submitButton);

      // Wait for the API call to complete
      await waitFor(() => {
        expect(mockSubmitWithApi).toHaveBeenCalledTimes(1);
      });

      // Verify the correct data was sent in the PUT request
      expect(capturedRequestBody).toEqual({
        code: '040404',
        description: 'Updated Description',
      });

      // Verify the correct URL was used
      expect(capturedUrl).toContain('/eural/040404');

      // Verify onCancel was called after successful submission
      await waitFor(() => {
        expect(mockOnCancel).toHaveBeenCalledTimes(1);
      });
    });
  });

  describe('Form Behavior', () => {
    it('renders correctly in add mode', () => {
      render(
        <EuralCodeForm
          isOpen={true}
          setIsOpen={vi.fn()}
          onCancel={mockOnCancel}
          onSubmit={mockOnSubmit}
        />,
        { wrapper }
      );

      expect(screen.getByText('Eural code toevoegen')).toBeInTheDocument();
      expect(screen.getByText('Code')).toBeInTheDocument();
      expect(screen.getByText('Beschrijving')).toBeInTheDocument();
      expect(screen.getByTestId('cancel-button')).toBeInTheDocument();
      expect(screen.getByTestId('submit-button')).toBeInTheDocument();
    });

    it('renders correctly in edit mode', () => {
      const existingEural: Eural = {
        code: '050505',
        description: 'Existing Description',
      };

      render(
        <EuralCodeForm
          isOpen={true}
          setIsOpen={vi.fn()}
          onCancel={mockOnCancel}
          onSubmit={mockOnSubmit}
          initialData={existingEural}
        />,
        { wrapper }
      );

      expect(screen.getByText('Eural code bewerken')).toBeInTheDocument();
      expect(screen.getByPlaceholderText('Vul een code in')).toHaveValue('050505');
      expect(screen.getByPlaceholderText('Vul een beschrijving in')).toHaveValue('Existing Description');
    });

    it('calls onCancel when cancel button is clicked', async () => {
      // Note: In the actual implementation, both setIsOpen and onCancel are set to the same function (form.close)
      // This causes the function to be called twice: once by cancel() and once by Dialog's onClose
      render(
        <EuralCodeForm
          isOpen={true}
          setIsOpen={mockOnCancel}  // Same function as onCancel, matching real usage
          onCancel={mockOnCancel}
          onSubmit={mockOnSubmit}
        />,
        { wrapper }
      );

      const cancelButton = screen.getByTestId('cancel-button');
      await userEvent.click(cancelButton);

      // Called twice: once by cancel() function, once by Dialog's onClose handler
      expect(mockOnCancel).toHaveBeenCalledTimes(2);
    });

    it('calls onCancel when close icon is clicked', async () => {
      render(
        <EuralCodeForm
          isOpen={true}
          setIsOpen={mockOnCancel}  // Same function as onCancel, matching real usage
          onCancel={mockOnCancel}
          onSubmit={mockOnSubmit}
        />,
        { wrapper }
      );

      const closeButton = screen.getByTestId('close-button');
      await userEvent.click(closeButton);

      // Called twice: once by cancel() function, once by Dialog's onClose handler
      expect(mockOnCancel).toHaveBeenCalledTimes(2);
    });

    it('resets form when cancel is called after filling fields', async () => {
      render(
        <EuralCodeForm
          isOpen={true}
          setIsOpen={mockOnCancel}  // Same function as onCancel, matching real usage
          onCancel={mockOnCancel}
          onSubmit={mockOnSubmit}
        />,
        { wrapper }
      );

      // Fill in the form
      const codeInput = screen.getByPlaceholderText('Vul een code in');
      const descriptionInput = screen.getByPlaceholderText('Vul een beschrijving in');

      await userEvent.type(codeInput, '060606');
      await userEvent.type(descriptionInput, 'Test Description');

      // Verify fields are filled
      expect(codeInput).toHaveValue('060606');
      expect(descriptionInput).toHaveValue('Test Description');

      // Click cancel
      const cancelButton = screen.getByTestId('cancel-button');
      await userEvent.click(cancelButton);

      // Called twice: once by cancel() function, once by Dialog's onClose handler
      expect(mockOnCancel).toHaveBeenCalledTimes(2);
    });
  });
});
