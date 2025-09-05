import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { wasteContainers } from '../../../testing/mocks/mockWasteContainers';
import { WasteContainerForm } from '../WasteContainerForm';
import { Company } from '@/types/api';

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

// Mock the companyService
vi.mock('@/api/companyService.ts', () => {
  const mockCompanies: Company[] = [
    {
      id: 'comp-1',
      name: 'Acme Recycling',
      address: {
        streetName: 'Recycling Lane',
        buildingNumber: '123',
        postalCode: '1234 AB',
        city: 'Amsterdam',
      },
      chamberOfCommerceId: 'KVK123456',
      vihbId: 'VIHB789',
    },
    {
      id: 'comp-2',
      name: 'Green Solutions',
      address: {
        streetName: 'Eco Street',
        buildingNumber: '45',
        postalCode: '5678 CD',
        city: 'Rotterdam',
      },
      chamberOfCommerceId: 'KVK654321',
      vihbId: 'VIHB987',
    },
  ];

  return {
    companyService: {
      list: vi.fn().mockResolvedValue(mockCompanies),
    },
  };
});

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

describe('WasteContainerForm', () => {
  const mockOnCancel = vi.fn();
  const mockOnSubmit = vi.fn();

  beforeEach(() => {
    mockOnCancel.mockReset();
    mockOnSubmit.mockReset();
    queryClient.clear();
    vi.clearAllMocks();
  });

  it('renders correctly in add mode', async () => {
    render(
      <WasteContainerForm onCancel={mockOnCancel} onSubmit={mockOnSubmit} />,
      { wrapper }
    );

    // Check form title
    expect(screen.getByText('Container toevoegen')).toBeInTheDocument();

    // Check form fields
    expect(screen.getByText('Containerkenmerk')).toBeInTheDocument();
    expect(screen.getByText('Kies bedrijf (optioneel)')).toBeInTheDocument();
    expect(screen.getByText('Straat')).toBeInTheDocument();
    expect(screen.getByText('Nummer')).toBeInTheDocument();
    expect(screen.getByText('Postcode')).toBeInTheDocument();
    expect(screen.getByText('Plaats')).toBeInTheDocument();
    expect(screen.getByText('Opmerkingen')).toBeInTheDocument();

    // Check buttons
    expect(screen.getByTestId('cancel-button')).toBeInTheDocument();
    expect(screen.getByTestId('submit-button')).toBeInTheDocument();

    // Wait for companies to load
    await waitFor(() => {
      expect(
        screen.getByText('Selecteer een bedrijf of vul zelf een adres in')
      ).toBeInTheDocument();
    });
  });

  it('renders correctly in edit mode', async () => {
    const mockContainer = wasteContainers[0];

    render(
      <WasteContainerForm
        onCancel={mockOnCancel}
        onSubmit={mockOnSubmit}
        wasteContainer={mockContainer}
      />,
      { wrapper }
    );

    // Check form title
    expect(screen.getByText('Container bewerken')).toBeInTheDocument();

    // Check form fields have correct values
    expect(screen.getByPlaceholderText('Vul kenmerk in')).toHaveValue(
      mockContainer.id
    );

    if (mockContainer.location.address) {
      expect(screen.getByPlaceholderText('Vul straatnaam in')).toHaveValue(
        mockContainer.location.address.streetName
      );
      expect(screen.getByPlaceholderText('Vul huisnummer in')).toHaveValue(
        mockContainer.location.address.buildingNumber
      );
      expect(screen.getByPlaceholderText('Vul postcode in')).toHaveValue(
        mockContainer.location.address.postalCode
      );
      expect(screen.getByPlaceholderText('Vul Plaats in')).toHaveValue(
        mockContainer.location.address.city
      );
    }

    if (mockContainer.notes) {
      expect(screen.getByPlaceholderText('Plaats opmerkingen')).toHaveValue(
        mockContainer.notes
      );
    }

    // Wait for companies to load
    await waitFor(() => {
      expect(
        screen.getByText('Selecteer een bedrijf of vul zelf een adres in')
      ).toBeInTheDocument();
    });
  });

  it('calls onCancel when cancel button is clicked', async () => {
    render(
      <WasteContainerForm onCancel={mockOnCancel} onSubmit={mockOnSubmit} />,
      { wrapper }
    );

    const cancelButton = screen.getByTestId('cancel-button');
    await userEvent.click(cancelButton);

    expect(mockOnCancel).toHaveBeenCalledTimes(1);
  });

  it('calls onCancel when close icon is clicked', async () => {
    render(
      <WasteContainerForm onCancel={mockOnCancel} onSubmit={mockOnSubmit} />,
      { wrapper }
    );

    const closeIcon = screen.getByTestId('close-button');
    await userEvent.click(closeIcon);

    expect(mockOnCancel).toHaveBeenCalledTimes(1);
  });

  it('shows validation errors when submitting empty form', async () => {
    render(
      <WasteContainerForm onCancel={mockOnCancel} onSubmit={mockOnSubmit} />,
      { wrapper }
    );

    const submitButton = screen.getByTestId('submit-button');
    await userEvent.click(submitButton);

    // Check validation errors
    await waitFor(() => {
      expect(screen.getByText('Kenmerk is verplicht')).toBeInTheDocument();
    });

    // onSubmit should not be called when validation fails
    expect(mockOnSubmit).not.toHaveBeenCalled();
  });

  it('submits form with valid data in add mode', async () => {
    render(
      <WasteContainerForm onCancel={mockOnCancel} onSubmit={mockOnSubmit} />,
      { wrapper }
    );

    // Fill in the form
    await userEvent.type(
      screen.getByPlaceholderText('Vul kenmerk in'),
      'CONT-TEST'
    );
    await userEvent.type(
      screen.getByPlaceholderText('Vul straatnaam in'),
      'Test Street'
    );
    await userEvent.type(
      screen.getByPlaceholderText('Vul huisnummer in'),
      '42'
    );
    await userEvent.type(
      screen.getByPlaceholderText('Vul postcode in'),
      '1234 ZZ'
    );
    await userEvent.type(
      screen.getByPlaceholderText('Vul Plaats in'),
      'Test City'
    );
    await userEvent.type(
      screen.getByPlaceholderText('Plaats opmerkingen'),
      'Test notes'
    );

    // Submit the form
    const submitButton = screen.getByTestId('submit-button');
    await userEvent.click(submitButton);

    // Check if onSubmit was called with correct data
    await waitFor(() => {
      expect(mockOnSubmit).toHaveBeenCalledTimes(1);
      expect(mockOnSubmit).toHaveBeenCalledWith(
        expect.objectContaining({
          id: 'CONT-TEST',
          location: expect.objectContaining({
            address: expect.objectContaining({
              streetName: 'Test Street',
              buildingNumber: '42',
              postalCode: '1234 ZZ',
              city: 'Test City',
            }),
          }),
          notes: 'Test notes',
        })
      );
    });

    // Check if onCancel was called after successful submission
    expect(mockOnCancel).toHaveBeenCalledTimes(1);
  });

  it('submits form with valid data in edit mode', async () => {
    const mockContainer = wasteContainers[0];

    render(
      <WasteContainerForm
        onCancel={mockOnCancel}
        onSubmit={mockOnSubmit}
        wasteContainer={mockContainer}
      />,
      { wrapper }
    );

    // Wait for form to load with existing data
    await waitFor(() => {
      expect(screen.getByPlaceholderText('Vul kenmerk in')).toHaveValue(
        mockContainer.id
      );
    });

    // Clear and update fields
    const idInput = screen.getByPlaceholderText('Vul kenmerk in');
    const notesInput = screen.getByPlaceholderText('Plaats opmerkingen');

    await userEvent.clear(idInput);
    await userEvent.clear(notesInput);
    await userEvent.type(idInput, 'CONT-UPDATED');
    await userEvent.type(notesInput, 'Updated notes');

    // Submit the form
    const submitButton = screen.getByTestId('submit-button');
    await userEvent.click(submitButton);

    // Check if onSubmit was called with correct data
    await waitFor(() => {
      expect(mockOnSubmit).toHaveBeenCalledTimes(1);
      expect(mockOnSubmit).toHaveBeenCalledWith(
        expect.objectContaining({
          uuid: mockContainer.uuid,
          id: 'CONT-UPDATED',
          notes: 'Updated notes',
        })
      );
    });

    // Check if onCancel was called after successful submission
    expect(mockOnCancel).toHaveBeenCalledTimes(1);
  });

  it('auto-fills address when company is selected', async () => {
    render(
      <WasteContainerForm onCancel={mockOnCancel} onSubmit={mockOnSubmit} />,
      { wrapper }
    );

    // Wait for companies to load
    await waitFor(() => {
      expect(
        screen.getByText('Selecteer een bedrijf of vul zelf een adres in')
      ).toBeInTheDocument();
    });

    // Open the company dropdown and select the first company
    const companySelect = screen.getByText(
      'Selecteer een bedrijf of vul zelf een adres in'
    );
    await userEvent.click(companySelect);

    // Wait for dropdown options to appear and select the first one
    await waitFor(() => {
      const options = screen.getAllByRole('option');
      if (options.length > 0) {
        userEvent.click(options[0]);
      }
    });

    // Check if address fields are auto-filled and disabled
    await waitFor(() => {
      expect(
        screen.getByText(
          'Adresgegevens worden automatisch ingevuld op basis van het geselecteerde bedrijf'
        )
      ).toBeInTheDocument();

      const streetInput = screen.getByPlaceholderText('Vul straatnaam in');
      const numberInput = screen.getByPlaceholderText('Vul huisnummer in');
      const postalCodeInput = screen.getByPlaceholderText('Vul postcode in');
      const cityInput = screen.getByPlaceholderText('Vul Plaats in');

      expect(streetInput).toBeDisabled();
      expect(numberInput).toBeDisabled();
      expect(postalCodeInput).toBeDisabled();
      expect(cityInput).toBeDisabled();

      expect(streetInput).toHaveValue('Recycling Lane');
      expect(numberInput).toHaveValue('123');
      expect(postalCodeInput).toHaveValue('1234 AB');
      expect(cityInput).toHaveValue('Amsterdam');
    });
  });

  it('handles submission error correctly', async () => {
    const mockError = new Error('Submission failed');
    mockOnSubmit.mockRejectedValue(mockError);

    render(
      <WasteContainerForm onCancel={mockOnCancel} onSubmit={mockOnSubmit} />,
      { wrapper }
    );

    // Fill in the form with minimal required fields
    await userEvent.type(
      screen.getByPlaceholderText('Vul kenmerk in'),
      'CONT-TEST'
    );
    await userEvent.type(
      screen.getByPlaceholderText('Vul straatnaam in'),
      'Test Street'
    );
    await userEvent.type(
      screen.getByPlaceholderText('Vul huisnummer in'),
      '42'
    );
    await userEvent.type(
      screen.getByPlaceholderText('Vul postcode in'),
      '1234 ZZ'
    );
    await userEvent.type(
      screen.getByPlaceholderText('Vul Plaats in'),
      'Test City'
    );

    // Submit the form
    const submitButton = screen.getByTestId('submit-button');
    await userEvent.click(submitButton);

    // Check if error dialog is shown
    await waitFor(() => {
      expect(screen.getByTestId('error-dialog')).toBeInTheDocument();
      expect(screen.getByTestId('error-dialog')).toHaveTextContent(
        'Submission failed'
      );
    });

    // onCancel should not be called when submission fails
    expect(mockOnCancel).not.toHaveBeenCalled();
  });
});
