import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { TruckForm } from '../TruckForm';
import { Truck } from '@/types/api';

vi.mock('@/hooks/useErrorHandling.tsx', () => ({
  useErrorHandling: () => ({
    handleError: vi.fn(),
    ErrorDialogComponent: () => <div data-testid="error-dialog"></div>,
    isErrorDialogOpen: false,
    setIsErrorDialogOpen: vi.fn(),
    errorMessage: '',
    setErrorMessage: vi.fn(),
  }),
}));

describe('TruckForm', () => {
  const mockOnCancel = vi.fn();
  const mockOnSubmit = vi.fn();

  beforeEach(() => {
    mockOnCancel.mockReset();
    mockOnSubmit.mockReset();
    vi.clearAllMocks();
  });

  it('renders correctly in add mode', () => {
    render(<TruckForm onCancel={mockOnCancel} onSubmit={mockOnSubmit} />);

    // Check form title
    expect(screen.getByText('Vrachtwagen toevoegen')).toBeInTheDocument();

    // Check form fields
    expect(screen.getByText('Merk')).toBeInTheDocument();
    expect(screen.getByText('Beschrijving')).toBeInTheDocument();
    expect(screen.getByText('Kenteken')).toBeInTheDocument();

    // Check buttons
    expect(
      screen.getByRole('button', { name: /Annuleren/i })
    ).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: /Toevoegen/i })
    ).toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: /Opslaan/i })
    ).not.toBeInTheDocument();
  });

  it('renders correctly in edit mode', () => {
    const mockTruck: Truck = {
      brand: 'Volvo',
      model: 'FH16',
      licensePlate: 'AB-123-C',
    };

    render(
      <TruckForm
        onCancel={mockOnCancel}
        onSubmit={mockOnSubmit}
        truck={mockTruck}
      />
    );

    // Check form title
    expect(screen.getByText('Vrachtwagen bewerken')).toBeInTheDocument();

    // Check form fields have correct values
    expect(screen.getByPlaceholderText('Vul merk in')).toHaveValue('Volvo');
    expect(screen.getByPlaceholderText('Vul een beschrijving in')).toHaveValue(
      'FH16'
    );
    expect(screen.getByPlaceholderText('Vul kenteken in')).toHaveValue(
      'AB-123-C'
    );

    // Check license plate is disabled in edit mode
    expect(screen.getByPlaceholderText('Vul kenteken in')).toBeDisabled();

    // Check buttons
    expect(
      screen.getByRole('button', { name: /Annuleren/i })
    ).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: /Aanpassen/i })
    ).toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: /Toevoegen/i })
    ).not.toBeInTheDocument();
  });

  it('calls onCancel when cancel button is clicked', async () => {
    render(<TruckForm onCancel={mockOnCancel} onSubmit={mockOnSubmit} />);

    const cancelButton = screen.getByTestId('cancel-button');
    await userEvent.click(cancelButton);

    expect(mockOnCancel).toHaveBeenCalledTimes(1);
  });

  it('calls onCancel when close icon is clicked', async () => {
    render(<TruckForm onCancel={mockOnCancel} onSubmit={mockOnSubmit} />);

    const closeIcon = screen.getByTestId('close-button');
    await userEvent.click(closeIcon);

    expect(mockOnCancel).toHaveBeenCalledTimes(1);
  });

  it('shows validation errors when submitting empty form', async () => {
    render(<TruckForm onCancel={mockOnCancel} onSubmit={mockOnSubmit} />);

    const submitButton = screen.getByTestId('submit-button');
    await userEvent.click(submitButton);

    // Check validation errors
    await waitFor(() => {
      expect(screen.getByText('Merk is verplicht')).toBeInTheDocument();
      expect(screen.getByText('Beschrijving is verplicht')).toBeInTheDocument();
      expect(screen.getByText('Kenteken is verplicht')).toBeInTheDocument();
    });

    // onSubmit should not be called when validation fails
    expect(mockOnSubmit).not.toHaveBeenCalled();
  });

  it('submits form with valid data in add mode', async () => {
    render(<TruckForm onCancel={mockOnCancel} onSubmit={mockOnSubmit} />);

    // Fill in the form
    await userEvent.type(
      screen.getByPlaceholderText('Vul merk in'),
      'Mercedes'
    );
    await userEvent.type(
      screen.getByPlaceholderText('Vul een beschrijving in'),
      'Actros'
    );
    await userEvent.type(
      screen.getByPlaceholderText('Vul kenteken in'),
      'XY-789-Z'
    );

    // Submit the form
    const submitButton = screen.getByTestId('submit-button');
    await userEvent.click(submitButton);

    // Check if onSubmit was called with correct data
    await waitFor(() => {
      expect(mockOnSubmit).toHaveBeenCalledTimes(1);
      expect(mockOnSubmit).toHaveBeenCalledWith({
        brand: 'Mercedes',
        model: 'Actros',
        licensePlate: 'XY-789-Z',
      });
    });

    // Check if onCancel was called after successful submission
    expect(mockOnCancel).toHaveBeenCalledTimes(1);
  });

  it('submits form with valid data in edit mode', async () => {
    const mockTruck: Truck = {
      brand: 'Volvo',
      model: 'FH16',
      licensePlate: 'AB-123-C',
    };

    render(
      <TruckForm
        onCancel={mockOnCancel}
        onSubmit={mockOnSubmit}
        truck={mockTruck}
      />
    );

    // Clear and update fields
    const brandInput = screen.getByPlaceholderText('Vul merk in');
    const modelInput = screen.getByPlaceholderText('Vul een beschrijving in');

    await userEvent.clear(brandInput);
    await userEvent.clear(modelInput);
    await userEvent.type(brandInput, 'Scania');
    await userEvent.type(modelInput, 'R500');

    // Submit the form
    const submitButton = screen.getByTestId('submit-button');
    await userEvent.click(submitButton);

    // Check if onSubmit was called with correct data
    await waitFor(() => {
      expect(mockOnSubmit).toHaveBeenCalledTimes(1);
      expect(mockOnSubmit).toHaveBeenCalledWith({
        brand: 'Scania',
        model: 'R500',
        licensePlate: 'AB-123-C',
      });
    });

    // Check if onCancel was called after successful submission
    expect(mockOnCancel).toHaveBeenCalledTimes(1);
  });

  it('handles submission error correctly', async () => {
    const mockError = new Error('Submission failed');
    mockOnSubmit.mockRejectedValue(mockError);

    render(<TruckForm onCancel={mockOnCancel} onSubmit={mockOnSubmit} />);

    // Fill in the form
    await userEvent.type(
      screen.getByPlaceholderText('Vul merk in'),
      'Mercedes'
    );
    await userEvent.type(
      screen.getByPlaceholderText('Vul een beschrijving in'),
      'Actros'
    );
    await userEvent.type(
      screen.getByPlaceholderText('Vul kenteken in'),
      'XY-789-Z'
    );

    // Submit the form
    const submitButton = screen.getByTestId('submit-button');
    await userEvent.click(submitButton);

    // Wait for the submission to be processed
    await waitFor(() => {
      expect(mockOnSubmit).toHaveBeenCalled();
    });

    // onCancel should not be called when submission fails
    expect(mockOnCancel).not.toHaveBeenCalled();
  });
});
