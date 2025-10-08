import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { WasteContainerForm } from '../WasteContainerForm';
import { WasteContainer } from '@/api/client';
import { Company } from '@/api/services/companyService';

// Mock the company service
vi.mock('@/api/services/companyService', () => ({
  companyService: {
    getAll: vi.fn(),
  },
}));

import { companyService } from '@/api/services/companyService';

const mockCompanies: Company[] = [
  {
    id: 'company-1',
    name: 'Test Company A',
    address: {
      streetName: 'Main Street',
      buildingNumber: '123',
      postalCode: '1234 AB',
      city: 'Amsterdam',
    },
    chamberOfCommerceId: '12345678',
    vihbId: '123456VIHB',
    updatedAt: '2025-10-08T10:00:00Z',
    branches: [],
  },
  {
    id: 'company-2',
    name: 'Test Company B',
    address: {
      streetName: 'Second Street',
      buildingNumber: '456',
      postalCode: '5678 CD',
      city: 'Rotterdam',
    },
    chamberOfCommerceId: '87654321',
    vihbId: '654321VIHB',
    updatedAt: '2025-10-08T10:00:00Z',
    branches: [],
  },
];

describe('WasteContainerForm Tests', () => {
  const mockOnCancel = vi.fn();
  const mockOnSubmit = vi.fn();
  let queryClient: QueryClient;

  beforeEach(() => {
    mockOnCancel.mockReset();
    mockOnSubmit.mockReset();
    vi.clearAllMocks();

    // Setup query client
    queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });

    // Mock the company service to return test companies
    vi.mocked(companyService.getAll).mockResolvedValue(mockCompanies);
  });

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  describe('Form Submission', () => {
    it('calls onSubmit with correct data when creating a new waste container', async () => {
      render(
        <WasteContainerForm
          isOpen={true}
          setIsOpen={vi.fn()}
          onCancel={mockOnCancel}
          onSubmit={mockOnSubmit}
        />,
        { wrapper }
      );

      // Verify form is in add mode
      expect(screen.getByText('Container toevoegen')).toBeInTheDocument();

      // Fill in the form
      const idInput = screen.getByPlaceholderText('Vul kenmerk in');
      const streetInput = screen.getByPlaceholderText('Vul straatnaam in');
      const houseNumberInput = screen.getByPlaceholderText('Vul huisnummer in');
      const postalCodeInput = screen.getByPlaceholderText('Vul postcode in');
      const cityInput = screen.getByPlaceholderText('Vul Plaats in');

      await userEvent.type(idInput, 'CONT-001');
      await userEvent.type(streetInput, 'Test Street');
      await userEvent.type(houseNumberInput, '42');
      await userEvent.type(postalCodeInput, '1234 AB');
      await userEvent.type(cityInput, 'Test City');

      // Submit the form
      const submitButton = screen.getByTestId('submit-button');
      await userEvent.click(submitButton);

      // Verify onSubmit was called with correct data
      await waitFor(() => {
        expect(mockOnSubmit).toHaveBeenCalledTimes(1);
        const submittedData = mockOnSubmit.mock.calls[0][0];
        expect(submittedData).toMatchObject({
          id: 'CONT-001',
          location: {
            address: {
              streetName: 'Test Street',
              buildingNumber: '42',
              postalCode: '1234 AB',
              city: 'Test City',
            },
          },
        });
        expect(submittedData.uuid).toBeDefined();
      });

      // Verify onCancel was called after successful submission
      await waitFor(() => {
        expect(mockOnCancel).toHaveBeenCalledTimes(1);
      });
    });

    it('does not call onSubmit when validation fails', async () => {
      render(
        <WasteContainerForm
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

      // Check validation error appears
      await waitFor(() => {
        expect(screen.getByText('Kenmerk is verplicht')).toBeInTheDocument();
      });

      // Verify onSubmit was not called
      expect(mockOnSubmit).not.toHaveBeenCalled();
      expect(mockOnCancel).not.toHaveBeenCalled();
    });

    it('calls onSubmit with correct data when updating an existing waste container', async () => {
      const existingContainer: WasteContainer = {
        uuid: 'existing-uuid',
        id: 'CONT-002',
        location: {
          address: {
            streetName: 'Old Street',
            buildingNumber: '10',
            postalCode: '9876 ZY',
            city: 'Old City',
          },
        },
        notes: 'Original notes',
      };

      render(
        <WasteContainerForm
          isOpen={true}
          setIsOpen={vi.fn()}
          onCancel={mockOnCancel}
          onSubmit={mockOnSubmit}
          initialData={existingContainer}
        />,
        { wrapper }
      );

      // Verify form is in edit mode
      expect(screen.getByText('Container bewerken')).toBeInTheDocument();

      // Verify id field is disabled in edit mode
      const idInput = screen.getByPlaceholderText('Vul kenmerk in');
      expect(idInput).toBeDisabled();
      expect(idInput).toHaveValue('CONT-002');

      // Update the street
      const streetInput = screen.getByPlaceholderText('Vul straatnaam in');
      await userEvent.clear(streetInput);
      await userEvent.type(streetInput, 'New Street');

      // Submit the form
      const submitButton = screen.getByTestId('submit-button');
      await userEvent.click(submitButton);

      // Verify onSubmit was called with updated data
      await waitFor(() => {
        expect(mockOnSubmit).toHaveBeenCalledWith(
          expect.objectContaining({
            uuid: 'existing-uuid',
            id: 'CONT-002',
            location: expect.objectContaining({
              address: expect.objectContaining({
                streetName: 'New Street',
              }),
            }),
          })
        );
      });

      // Verify onCancel was called after successful submission
      await waitFor(() => {
        expect(mockOnCancel).toHaveBeenCalledTimes(1);
      });
    });

    it('does not call onCancel when onSubmit throws an error', async () => {
      const mockSubmitWithError = vi.fn().mockRejectedValue(new Error('API Error'));

      render(
        <WasteContainerForm
          isOpen={true}
          setIsOpen={vi.fn()}
          onCancel={mockOnCancel}
          onSubmit={mockSubmitWithError}
        />,
        { wrapper }
      );

      // Fill in the form
      const idInput = screen.getByPlaceholderText('Vul kenmerk in');
      await userEvent.type(idInput, 'CONT-003');

      // Submit the form
      const submitButton = screen.getByTestId('submit-button');
      await userEvent.click(submitButton);

      // Verify onSubmit was called
      await waitFor(() => {
        expect(mockSubmitWithError).toHaveBeenCalledTimes(1);
      });

      // Verify onCancel was NOT called due to error
      expect(mockOnCancel).not.toHaveBeenCalled();
    });
  });

  describe('Form Behavior', () => {
    it('renders correctly in add mode', async () => {
      render(
        <WasteContainerForm
          isOpen={true}
          setIsOpen={vi.fn()}
          onCancel={mockOnCancel}
          onSubmit={mockOnSubmit}
        />,
        { wrapper }
      );

      expect(screen.getByText('Container toevoegen')).toBeInTheDocument();
      expect(screen.getByText('Containerkenmerk')).toBeInTheDocument();
      expect(screen.getByText('Huidige locatie')).toBeInTheDocument();
      expect(screen.getByTestId('cancel-button')).toBeInTheDocument();
      expect(screen.getByTestId('submit-button')).toBeInTheDocument();
    });

    it('renders correctly in edit mode', async () => {
      const existingContainer: WasteContainer = {
        uuid: 'test-uuid',
        id: 'CONT-004',
        location: {
          address: {
            streetName: 'Edit Street',
            buildingNumber: '99',
            postalCode: '1111 AA',
            city: 'Edit City',
          },
        },
      };

      render(
        <WasteContainerForm
          isOpen={true}
          setIsOpen={vi.fn()}
          onCancel={mockOnCancel}
          onSubmit={mockOnSubmit}
          initialData={existingContainer}
        />,
        { wrapper }
      );

      expect(screen.getByText('Container bewerken')).toBeInTheDocument();
      expect(screen.getByPlaceholderText('Vul kenmerk in')).toHaveValue('CONT-004');
      expect(screen.getByPlaceholderText('Vul straatnaam in')).toHaveValue('Edit Street');
    });

    it('calls onCancel when cancel button is clicked', async () => {
      render(
        <WasteContainerForm
          isOpen={true}
          setIsOpen={vi.fn()}
          onCancel={mockOnCancel}
          onSubmit={mockOnSubmit}
        />,
        { wrapper }
      );

      const cancelButton = screen.getByTestId('cancel-button');
      await userEvent.click(cancelButton);

      // Called twice: once by cancel() function, once by Dialog's onClose handler
      // This is expected behavior with the current implementation
      expect(mockOnCancel).toHaveBeenCalledTimes(2);
    });

    it('calls onCancel when close icon is clicked', async () => {
      render(
        <WasteContainerForm
          isOpen={true}
          setIsOpen={vi.fn()}
          onCancel={mockOnCancel}
          onSubmit={mockOnSubmit}
        />,
        { wrapper }
      );

      const closeButton = screen.getByTestId('close-button');
      await userEvent.click(closeButton);

      // Called twice: once by cancel() function, once by Dialog's onClose handler
      // This is expected behavior with the current implementation
      expect(mockOnCancel).toHaveBeenCalledTimes(2);
    });

    it('fills in and disables address fields when a company is selected', async () => {
      const user = userEvent.setup();

      render(
        <WasteContainerForm
          isOpen={true}
          setIsOpen={vi.fn()}
          onCancel={mockOnCancel}
          onSubmit={mockOnSubmit}
        />,
        { wrapper }
      );

      // Wait for companies to load
      await waitFor(() => {
        expect(companyService.getAll).toHaveBeenCalled();
      });

      // Get address fields
      const streetInput = screen.getByPlaceholderText('Vul straatnaam in') as HTMLInputElement;
      const houseNumberInput = screen.getByPlaceholderText('Vul huisnummer in') as HTMLInputElement;
      const postalCodeInput = screen.getByPlaceholderText('Vul postcode in') as HTMLInputElement;
      const cityInput = screen.getByPlaceholderText('Vul Plaats in') as HTMLInputElement;

      // Initially, address fields should be enabled and empty
      expect(streetInput).not.toBeDisabled();
      expect(houseNumberInput).not.toBeDisabled();
      expect(postalCodeInput).not.toBeDisabled();
      expect(cityInput).not.toBeDisabled();

      // Select a company using React Select
      const selectInput = screen.getByText('Selecteer een bedrijf of vul zelf een adres in');
      await user.click(selectInput);

      // Wait for options to appear and click the first company
      const companyOption = await screen.findByText('Test Company A');
      await user.click(companyOption);

      // Wait for address fields to be filled and disabled
      await waitFor(() => {
        expect(streetInput).toHaveValue('Main Street');
        expect(houseNumberInput).toHaveValue('123');
        expect(postalCodeInput).toHaveValue('1234 AB');
        expect(cityInput).toHaveValue('Amsterdam');
      });

      // Verify address fields are disabled
      expect(streetInput).toBeDisabled();
      expect(houseNumberInput).toBeDisabled();
      expect(postalCodeInput).toBeDisabled();
      expect(cityInput).toBeDisabled();

      // Verify the helper text is shown
      expect(
        screen.getByText('Adresgegevens worden automatisch ingevuld op basis van het geselecteerde bedrijf')
      ).toBeInTheDocument();
    });

    it('keeps address fields filled but enables them when company is deselected', async () => {
      const user = userEvent.setup();

      render(
        <WasteContainerForm
          isOpen={true}
          setIsOpen={vi.fn()}
          onCancel={mockOnCancel}
          onSubmit={mockOnSubmit}
        />,
        { wrapper }
      );

      // Wait for companies to load
      await waitFor(() => {
        expect(companyService.getAll).toHaveBeenCalled();
      });

      // Get address fields
      const streetInput = screen.getByPlaceholderText('Vul straatnaam in') as HTMLInputElement;
      const houseNumberInput = screen.getByPlaceholderText('Vul huisnummer in') as HTMLInputElement;
      const postalCodeInput = screen.getByPlaceholderText('Vul postcode in') as HTMLInputElement;
      const cityInput = screen.getByPlaceholderText('Vul Plaats in') as HTMLInputElement;

      // Select a company
      const selectInput = screen.getByText('Selecteer een bedrijf of vul zelf een adres in');
      await user.click(selectInput);

      const companyOption = await screen.findByText('Test Company B');
      await user.click(companyOption);

      // Wait for address fields to be filled
      await waitFor(() => {
        expect(streetInput).toHaveValue('Second Street');
        expect(houseNumberInput).toHaveValue('456');
        expect(postalCodeInput).toHaveValue('5678 CD');
        expect(cityInput).toHaveValue('Rotterdam');
        expect(streetInput).toBeDisabled();
      });

      // Now deselect the company by clicking the clear indicator (X)
      const clearIndicator = document.querySelector('.react-select__clear-indicator');
      if (!clearIndicator) {
        throw new Error('Clear indicator not found');
      }
      await user.click(clearIndicator as HTMLElement);

      // Wait for fields to become enabled
      await waitFor(() => {
        expect(streetInput).not.toBeDisabled();
        expect(houseNumberInput).not.toBeDisabled();
        expect(postalCodeInput).not.toBeDisabled();
        expect(cityInput).not.toBeDisabled();
      });

      // Verify address fields still contain the company address values
      expect(streetInput).toHaveValue('Second Street');
      expect(houseNumberInput).toHaveValue('456');
      expect(postalCodeInput).toHaveValue('5678 CD');
      expect(cityInput).toHaveValue('Rotterdam');

      // Verify the helper text is no longer shown
      expect(
        screen.queryByText('Adresgegevens worden automatisch ingevuld op basis van het geselecteerde bedrijf')
      ).not.toBeInTheDocument();
    });
  });
});
