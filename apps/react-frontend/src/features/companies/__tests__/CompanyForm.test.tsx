import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { CompanyForm } from '../CompanyForm';
import { Company } from '@/api/services/companyService';
import { CompleteCompanyViewRolesEnum } from '@/api/client/models/complete-company-view';

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

// Create mock companies for testing
const mockCompanies: Company[] = [
  {
    id: 'comp-1',
    name: 'Acme Recycling',
    address: {
      street: 'Recycling Lane',
      houseNumber: '123',
      houseNumberAddition: undefined,
      postalCode: '1234 AB',
      city: 'Amsterdam',
      country: 'Nederland',
    },
    chamberOfCommerceId: '12345678',
    vihbId: '123456VIHB',
    updatedAt: new Date().toISOString(),
    branches: [],
    roles: [CompleteCompanyViewRolesEnum.Processor],
  },
  {
    id: 'comp-2',
    name: 'Green Solutions',
    address: {
      street: 'Eco Street',
      houseNumber: '45',
      houseNumberAddition: undefined,
      postalCode: '5678 CD',
      city: 'Rotterdam',
      country: 'Nederland',
    },
    chamberOfCommerceId: '87654321',
    vihbId: '654321VIHB',
    updatedAt: new Date().toISOString(),
    branches: [],
    roles: [CompleteCompanyViewRolesEnum.Carrier],
  },
];

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

describe('CompanyForm', () => {
  const mockOnCancel = vi.fn();
  const mockOnSubmit = vi.fn();

  beforeEach(() => {
    mockOnCancel.mockReset();
    mockOnSubmit.mockReset();
    queryClient.clear();
    vi.clearAllMocks();
  });

  it('renders correctly in add mode', async () => {
    render(<CompanyForm onCancel={mockOnCancel} onSubmit={mockOnSubmit} />, {
      wrapper,
    });

    // Check form title
    expect(screen.getByText('Een bedrijf toevoegen')).toBeInTheDocument();

    // Check form fields
    expect(screen.getByText('Bedrijfsnaam')).toBeInTheDocument();
    expect(screen.getByText('Straat')).toBeInTheDocument();
    expect(screen.getByTestId('houseNumber')).toBeInTheDocument();
    expect(screen.getByText('Postcode')).toBeInTheDocument();
    expect(screen.getByText('Plaats')).toBeInTheDocument();
    expect(screen.getByText('KvK nummer')).toBeInTheDocument();
    expect(screen.getByText('VIHB-nummer')).toBeInTheDocument();

    // Check buttons
    expect(screen.getByTestId('cancel-button')).toBeInTheDocument();
    expect(screen.getByTestId('submit-button')).toBeInTheDocument();
  });

  it('renders correctly in edit mode', async () => {
    const mockCompany = mockCompanies[0];

    render(
      <CompanyForm
        onCancel={mockOnCancel}
        onSubmit={mockOnSubmit}
        company={mockCompany}
      />,
      { wrapper }
    );

    // Check form title
    expect(screen.getByText('Bedrijf aanpassen')).toBeInTheDocument();

    // Check form fields have correct values
    expect(screen.getByPlaceholderText('Vul bedrijfsnaam in')).toHaveValue(
      mockCompany.name
    );
    expect(screen.getByPlaceholderText('Vul straatnaam in')).toHaveValue(
      mockCompany.address.street
    );
    expect(screen.getByTestId('houseNumber')).toHaveValue(
      Number(mockCompany.address.houseNumber)
    );
    expect(screen.getByPlaceholderText('Vul postcode in')).toHaveValue(
      mockCompany.address.postalCode
    );
    expect(screen.getByPlaceholderText('Vul Plaats in')).toHaveValue(
      mockCompany.address.city
    );
    expect(screen.getByPlaceholderText('Vul Kvk nummer in')).toHaveValue(
      mockCompany.chamberOfCommerceId
    );
    expect(screen.getByPlaceholderText('Vul VIHB-nummer in')).toHaveValue(
      mockCompany.vihbId
    );
  });

  it('calls onCancel when cancel button is clicked', async () => {
    render(<CompanyForm onCancel={mockOnCancel} onSubmit={mockOnSubmit} />, {
      wrapper,
    });

    const cancelButton = screen.getByTestId('cancel-button');
    await userEvent.click(cancelButton);

    expect(mockOnCancel).toHaveBeenCalledTimes(1);
  });

  it('calls onCancel when close icon is clicked', async () => {
    render(<CompanyForm onCancel={mockOnCancel} onSubmit={mockOnSubmit} />, {
      wrapper,
    });

    const closeIcon = screen.getByTestId('close-button');
    await userEvent.click(closeIcon);

    expect(mockOnCancel).toHaveBeenCalledTimes(1);
  });

  it('shows validation errors when submitting empty form', async () => {
    render(<CompanyForm onCancel={mockOnCancel} onSubmit={mockOnSubmit} />, {
      wrapper,
    });

    const submitButton = screen.getByTestId('submit-button');
    await userEvent.click(submitButton);

    // Check validation errors
    await waitFor(() => {
      expect(screen.getByText('Bedrijfsnaam is verplicht')).toBeInTheDocument();
      expect(screen.getByText('Straat is verplicht')).toBeInTheDocument();
      expect(screen.getByText('Huisnummer is verplicht')).toBeInTheDocument();
      expect(screen.getByText('Postcode is verplicht')).toBeInTheDocument();
      expect(screen.getByText('Plaats is verplicht')).toBeInTheDocument();
    });

    // onSubmit should not be called when validation fails
    expect(mockOnSubmit).not.toHaveBeenCalled();
  });

  it('submits form with valid data in add mode', async () => {
    render(<CompanyForm onCancel={mockOnCancel} onSubmit={mockOnSubmit} />, {
      wrapper,
    });

    // Fill in the form
    await userEvent.type(
      screen.getByPlaceholderText('Vul bedrijfsnaam in'),
      'Test Company'
    );
    await userEvent.type(
      screen.getByPlaceholderText('Vul straatnaam in'),
      'Test Street'
    );
    await userEvent.type(screen.getByTestId('houseNumber'), '42');
    await userEvent.type(
      screen.getByPlaceholderText('Vul postcode in'),
      '1234 ZZ'
    );
    await userEvent.type(
      screen.getByPlaceholderText('Vul Plaats in'),
      'Test City'
    );
    await userEvent.type(
      screen.getByPlaceholderText('Vul Kvk nummer in'),
      '12345678'
    );
    await userEvent.type(
      screen.getByPlaceholderText('Vul VIHB-nummer in'),
      '123456VIHB'
    );

    // Submit the form
    const submitButton = screen.getByTestId('submit-button');
    await userEvent.click(submitButton);

    // Check if onSubmit was called with correct data
    await waitFor(() => {
      expect(mockOnSubmit).toHaveBeenCalledTimes(1);
      expect(mockOnSubmit).toHaveBeenCalledWith(
        expect.objectContaining({
          name: 'Test Company',
          address: expect.objectContaining({
            street: 'Test Street',
            houseNumber: '42',
            postalCode: '1234 ZZ',
            city: 'Test City',
          }),
          chamberOfCommerceId: '12345678',
          vihbId: '123456VIHB',
        })
      );
    });

    // Check if onCancel was called after successful submission
    expect(mockOnCancel).toHaveBeenCalledTimes(1);
  });

  it('submits form with valid data in edit mode', async () => {
    const mockCompany = mockCompanies[0];

    render(
      <CompanyForm
        onCancel={mockOnCancel}
        onSubmit={mockOnSubmit}
        company={mockCompany}
      />,
      { wrapper }
    );

    // Wait for form to load with existing data
    await waitFor(() => {
      expect(screen.getByPlaceholderText('Vul bedrijfsnaam in')).toHaveValue(
        mockCompany.name
      );
    });

    // Clear and update fields
    const nameInput = screen.getByPlaceholderText('Vul bedrijfsnaam in');
    const kvkInput = screen.getByPlaceholderText('Vul Kvk nummer in');

    await userEvent.clear(nameInput);
    await userEvent.clear(kvkInput);
    await userEvent.type(nameInput, 'Updated Company Name');
    await userEvent.type(kvkInput, '87654321');

    // Submit the form
    const submitButton = screen.getByTestId('submit-button');
    await userEvent.click(submitButton);

    // Check if onSubmit was called with correct data
    await waitFor(() => {
      expect(mockOnSubmit).toHaveBeenCalledTimes(1);
      expect(mockOnSubmit).toHaveBeenCalledWith(
        expect.objectContaining({
          id: mockCompany.id,
          name: 'Updated Company Name',
          chamberOfCommerceId: '87654321',
        })
      );
    });

    // Check if onCancel was called after successful submission
    expect(mockOnCancel).toHaveBeenCalledTimes(1);
  });

  it('submits form with empty optional fields as undefined', async () => {
    render(<CompanyForm onCancel={mockOnCancel} onSubmit={mockOnSubmit} />, {
      wrapper,
    });

    // Fill in only required fields, leave optional fields empty
    await userEvent.type(
      screen.getByPlaceholderText('Vul bedrijfsnaam in'),
      'Test Company'
    );
    await userEvent.type(
      screen.getByPlaceholderText('Vul straatnaam in'),
      'Test Street'
    );
    await userEvent.type(screen.getByTestId('houseNumber'), '42');
    await userEvent.type(
      screen.getByPlaceholderText('Vul postcode in'),
      '1234 ZZ'
    );
    await userEvent.type(
      screen.getByPlaceholderText('Vul Plaats in'),
      'Test City'
    );
    // Leave chamberOfCommerceId and vihbId empty

    // Submit the form
    const submitButton = screen.getByTestId('submit-button');
    await userEvent.click(submitButton);

    // Check if onSubmit was called with undefined values for optional fields
    await waitFor(() => {
      expect(mockOnSubmit).toHaveBeenCalledTimes(1);
      expect(mockOnSubmit).toHaveBeenCalledWith(
        expect.objectContaining({
          name: 'Test Company',
          address: expect.objectContaining({
            street: 'Test Street',
            houseNumber: '42',
            postalCode: '1234 ZZ',
            city: 'Test City',
          }),
          chamberOfCommerceId: undefined,
          vihbId: undefined,
          updatedAt: expect.any(String),
          branches: expect.any(Array),
        })
      );
    });

    // Check if onCancel was called after successful submission
    expect(mockOnCancel).toHaveBeenCalledTimes(1);
  });

  it('submits form with whitespace-only optional fields as undefined', async () => {
    render(<CompanyForm onCancel={mockOnCancel} onSubmit={mockOnSubmit} />, {
      wrapper,
    });

    // Fill in required fields and whitespace-only optional fields
    await userEvent.type(
      screen.getByPlaceholderText('Vul bedrijfsnaam in'),
      'Test Company'
    );
    await userEvent.type(
      screen.getByPlaceholderText('Vul straatnaam in'),
      'Test Street'
    );
    await userEvent.type(screen.getByTestId('houseNumber'), '42');
    await userEvent.type(
      screen.getByPlaceholderText('Vul postcode in'),
      '1234 ZZ'
    );
    await userEvent.type(
      screen.getByPlaceholderText('Vul Plaats in'),
      'Test City'
    );
    // Fill optional fields with whitespace only
    await userEvent.type(
      screen.getByPlaceholderText('Vul Kvk nummer in'),
      '   '
    );
    await userEvent.type(
      screen.getByPlaceholderText('Vul VIHB-nummer in'),
      '\t\n  '
    );

    // Submit the form
    const submitButton = screen.getByTestId('submit-button');
    await userEvent.click(submitButton);

    // Check if onSubmit was called with undefined values for whitespace-only fields
    await waitFor(() => {
      expect(mockOnSubmit).toHaveBeenCalledTimes(1);
      expect(mockOnSubmit).toHaveBeenCalledWith(
        expect.objectContaining({
          name: 'Test Company',
          chamberOfCommerceId: undefined,
          vihbId: undefined,
          updatedAt: expect.any(String),
          branches: expect.any(Array),
        })
      );
    });

    // Check if onCancel was called after successful submission
    expect(mockOnCancel).toHaveBeenCalledTimes(1);
  });

  it('handles edit mode with existing undefined values', async () => {
    const companyWithUndefined = {
      ...mockCompanies[0],
      chamberOfCommerceId: undefined,
      vihbId: undefined,
    };

    render(
      <CompanyForm
        onCancel={mockOnCancel}
        onSubmit={mockOnSubmit}
        company={companyWithUndefined}
      />,
      { wrapper }
    );

    // Wait for form to load with existing data
    await waitFor(() => {
      expect(screen.getByPlaceholderText('Vul bedrijfsnaam in')).toHaveValue(
        companyWithUndefined.name
      );
    });

    // Check that optional fields are empty when undefined
    expect(screen.getByPlaceholderText('Vul Kvk nummer in')).toHaveValue('');
    expect(screen.getByPlaceholderText('Vul VIHB-nummer in')).toHaveValue('');

    // Submit without changing anything
    const submitButton = screen.getByTestId('submit-button');
    await userEvent.click(submitButton);

    // Check if onSubmit was called with undefined values preserved
    await waitFor(() => {
      expect(mockOnSubmit).toHaveBeenCalledTimes(1);
      expect(mockOnSubmit).toHaveBeenCalledWith(
        expect.objectContaining({
          id: companyWithUndefined.id,
          name: companyWithUndefined.name,
          chamberOfCommerceId: undefined,
          vihbId: undefined,
          updatedAt: expect.any(String),
          branches: expect.any(Array),
        })
      );
    });

    // Check if onCancel was called after successful submission
    expect(mockOnCancel).toHaveBeenCalledTimes(1);
  });

  it('validates KvK number format when provided', async () => {
    render(<CompanyForm onCancel={mockOnCancel} onSubmit={mockOnSubmit} />, {
      wrapper,
    });

    // Fill in required fields
    await userEvent.type(
      screen.getByPlaceholderText('Vul bedrijfsnaam in'),
      'Test Company'
    );
    await userEvent.type(
      screen.getByPlaceholderText('Vul straatnaam in'),
      'Test Street'
    );
    await userEvent.type(screen.getByTestId('houseNumber'), '42');
    await userEvent.type(
      screen.getByPlaceholderText('Vul postcode in'),
      '1234 ZZ'
    );
    await userEvent.type(
      screen.getByPlaceholderText('Vul Plaats in'),
      'Test City'
    );
    // Fill KvK with invalid format
    await userEvent.type(
      screen.getByPlaceholderText('Vul Kvk nummer in'),
      'INVALID123'
    );

    // Submit the form
    const submitButton = screen.getByTestId('submit-button');
    await userEvent.click(submitButton);

    // Check that validation error appears and form is not submitted
    await waitFor(() => {
      expect(
        screen.getByText('KvK nummer moet 8 cijfers bevatten of leeg zijn')
      ).toBeInTheDocument();
    });

    // onSubmit should not have been called due to validation error
    expect(mockOnSubmit).not.toHaveBeenCalled();
  });

  it('validates VIHB number format when provided', async () => {
    render(<CompanyForm onCancel={mockOnCancel} onSubmit={mockOnSubmit} />, {
      wrapper,
    });

    // Fill in required fields
    await userEvent.type(
      screen.getByPlaceholderText('Vul bedrijfsnaam in'),
      'Test Company'
    );
    await userEvent.type(
      screen.getByPlaceholderText('Vul straatnaam in'),
      'Test Street'
    );
    await userEvent.type(screen.getByTestId('houseNumber'), '42');
    await userEvent.type(
      screen.getByPlaceholderText('Vul postcode in'),
      '1234 ZZ'
    );
    await userEvent.type(
      screen.getByPlaceholderText('Vul Plaats in'),
      'Test City'
    );
    // Fill VIHB with invalid format
    await userEvent.type(
      screen.getByPlaceholderText('Vul VIHB-nummer in'),
      'INVALID'
    );

    // Submit the form
    const submitButton = screen.getByTestId('submit-button');
    await userEvent.click(submitButton);

    // Check that validation error appears and form is not submitted
    await waitFor(() => {
      expect(
        screen.getByText(
          'VIHB-nummer moet 6 cijfers en 4 letters (VIHB of X) bevatten of leeg zijn'
        )
      ).toBeInTheDocument();
    });

    // onSubmit should not have been called due to validation error
    expect(mockOnSubmit).not.toHaveBeenCalled();
  });
});
