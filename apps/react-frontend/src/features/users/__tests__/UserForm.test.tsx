import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { UserForm } from '../UserForm';
import { User } from '@/api/services/userService';

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

// Mock the Avatar component
vi.mock('react-avatar', () => ({
  default: ({ name }: { name: string }) => (
    <div data-testid="avatar">{name}</div>
  ),
}));

// Create mock users for testing
const mockUsers: User[] = [
  {
    id: 'user-1',
    firstName: 'John',
    lastName: 'Doe',
    email: 'john.doe@example.com',
    roles: ['admin'],
    lastSignInAt: '2023-01-01T12:00:00Z',
  },
  {
    id: 'user-2',
    firstName: 'Jane',
    lastName: 'Smith',
    email: 'jane.smith@example.com',
    roles: ['planner'],
    lastSignInAt: '2023-01-02T12:00:00Z',
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

describe('UserForm', () => {
  const mockOnCancel = vi.fn();
  const mockOnSubmit = vi.fn();

  beforeEach(() => {
    mockOnCancel.mockReset();
    mockOnSubmit.mockReset();
    queryClient.clear();
    vi.clearAllMocks();
  });

  it('renders correctly in add mode', async () => {
    render(<UserForm onCancel={mockOnCancel} onSubmit={mockOnSubmit} />, {
      wrapper,
    });

    // Check form title
    expect(screen.getByText('Gebruiker toevoegen')).toBeInTheDocument();

    // Check form fields
    expect(screen.getByText('Voornaam')).toBeInTheDocument();
    expect(screen.getByText('Achternaam')).toBeInTheDocument();
    expect(screen.getByText('Email')).toBeInTheDocument();
    expect(screen.getByText('Wachtwoord')).toBeInTheDocument();
    expect(screen.getByText('Rol')).toBeInTheDocument();

    // Check buttons
    expect(screen.getByTestId('cancel-button')).toBeInTheDocument();
    expect(screen.getByTestId('submit-button')).toBeInTheDocument();
  });

  it('renders correctly in edit mode', async () => {
    const mockUser = mockUsers[0];

    render(
      <UserForm
        onCancel={mockOnCancel}
        onSubmit={mockOnSubmit}
        user={mockUser}
      />,
      { wrapper }
    );

    // Check form title
    expect(screen.getByText('Gebruiker bewerken')).toBeInTheDocument();

    // Check form fields have correct values
    expect(screen.getByPlaceholderText('Vul voornaam in')).toHaveValue(
      mockUser.firstName
    );
    expect(screen.getByPlaceholderText('Vul achternaam in')).toHaveValue(
      mockUser.lastName
    );
    expect(screen.getByPlaceholderText('Vul email in')).toHaveValue(
      mockUser.email
    );

    // Password field should not be present in edit mode
    expect(screen.queryByText('Wachtwoord')).not.toBeInTheDocument();
  });

  it('calls onCancel when cancel button is clicked', async () => {
    render(<UserForm onCancel={mockOnCancel} onSubmit={mockOnSubmit} />, {
      wrapper,
    });

    const cancelButton = screen.getByTestId('cancel-button');
    await userEvent.click(cancelButton);

    expect(mockOnCancel).toHaveBeenCalledTimes(1);
  });

  it('calls onCancel when close icon is clicked', async () => {
    render(<UserForm onCancel={mockOnCancel} onSubmit={mockOnSubmit} />, {
      wrapper,
    });

    const closeIcon = screen.getByTestId('close-button');
    await userEvent.click(closeIcon);

    expect(mockOnCancel).toHaveBeenCalledTimes(1);
  });

  it('shows validation errors when submitting empty form', async () => {
    render(<UserForm onCancel={mockOnCancel} onSubmit={mockOnSubmit} />, {
      wrapper,
    });

    const submitButton = screen.getByTestId('submit-button');
    await userEvent.click(submitButton);

    // Check validation errors
    await waitFor(() => {
      expect(screen.getByText('Voornaam is verplicht')).toBeInTheDocument();
      expect(screen.getByText('Achternaam is verplicht')).toBeInTheDocument();
      expect(screen.getByText('Email is verplicht')).toBeInTheDocument();
      expect(screen.getByText('Wachtwoord is verplicht')).toBeInTheDocument();
      expect(screen.getByText('Rol is verplicht')).toBeInTheDocument();
    });

    // onSubmit should not be called when validation fails
    expect(mockOnSubmit).not.toHaveBeenCalled();
  });

  it('shows validation error for invalid email format', async () => {
    render(<UserForm onCancel={mockOnCancel} onSubmit={mockOnSubmit} />, {
      wrapper,
    });

    // Fill in the form with invalid email
    await userEvent.type(
      screen.getByPlaceholderText('Vul voornaam in'),
      'Test'
    );
    await userEvent.type(
      screen.getByPlaceholderText('Vul achternaam in'),
      'User'
    );
    await userEvent.type(
      screen.getByPlaceholderText('Vul email in'),
      'invalid-email'
    );
    await userEvent.type(
      screen.getByPlaceholderText('Vul wachtwoord in'),
      'Password123!'
    );

    // Submit the form
    const submitButton = screen.getByTestId('submit-button');
    await userEvent.click(submitButton);

    // Check validation error for email
    await waitFor(() => {
      expect(
        screen.getByText(
          'Ongeldig email, gebruik een geldig email adres zoals test@test.nl'
        )
      ).toBeInTheDocument();
    });

    // onSubmit should not be called when validation fails
    expect(mockOnSubmit).not.toHaveBeenCalled();
  });

  it('shows validation error for invalid password format', async () => {
    render(<UserForm onCancel={mockOnCancel} onSubmit={mockOnSubmit} />, {
      wrapper,
    });

    // Fill in the form with invalid password
    await userEvent.type(
      screen.getByPlaceholderText('Vul voornaam in'),
      'Test'
    );
    await userEvent.type(
      screen.getByPlaceholderText('Vul achternaam in'),
      'User'
    );
    await userEvent.type(
      screen.getByPlaceholderText('Vul email in'),
      'test@example.com'
    );
    await userEvent.type(
      screen.getByPlaceholderText('Vul wachtwoord in'),
      'password'
    );

    // Submit the form
    const submitButton = screen.getByTestId('submit-button');
    await userEvent.click(submitButton);

    // Check validation error for password
    await waitFor(() => {
      expect(
        screen.getByText(
          'Wachtwoord moet een kleine letter, hoofdletter, cijfer en speciaal karakter bevatten'
        )
      ).toBeInTheDocument();
    });

    // onSubmit should not be called when validation fails
    expect(mockOnSubmit).not.toHaveBeenCalled();
  });

  it('submits form with valid data in add mode', async () => {
    render(<UserForm onCancel={mockOnCancel} onSubmit={mockOnSubmit} />, {
      wrapper,
    });

    // Fill in the form
    await userEvent.type(
      screen.getByPlaceholderText('Vul voornaam in'),
      'Test'
    );
    await userEvent.type(
      screen.getByPlaceholderText('Vul achternaam in'),
      'User'
    );
    await userEvent.type(
      screen.getByPlaceholderText('Vul email in'),
      'test.user@example.com'
    );
    await userEvent.type(
      screen.getByPlaceholderText('Vul wachtwoord in'),
      'Password123!'
    );

    // Select a role
    const roleSelect = screen.getByText('Selecteer een rol');
    await userEvent.click(roleSelect);

    // Wait for dropdown options to appear and select Admin
    await waitFor(() => {
      const options = screen.getAllByRole('option');
      if (options.length > 0) {
        userEvent.click(options[0]); // Select Admin
      }
    });

    // Submit the form
    const submitButton = screen.getByTestId('submit-button');
    await userEvent.click(submitButton);

    // Check if onSubmit was called with correct data
    await waitFor(() => {
      expect(mockOnSubmit).toHaveBeenCalledTimes(1);
      expect(mockOnSubmit).toHaveBeenCalledWith(
        expect.objectContaining({
          firstName: 'Test',
          lastName: 'User',
          email: 'test.user@example.com',
          password: 'Password123!',
          roles: ['admin'],
        })
      );
    });

    // Check if onCancel was called after successful submission
    expect(mockOnCancel).toHaveBeenCalledTimes(1);
  });

  it('submits form with valid data in edit mode', async () => {
    const mockUser = mockUsers[0];

    render(
      <UserForm
        onCancel={mockOnCancel}
        onSubmit={mockOnSubmit}
        user={mockUser}
      />,
      { wrapper }
    );

    // Wait for form to load with existing data
    await waitFor(() => {
      expect(screen.getByPlaceholderText('Vul voornaam in')).toHaveValue(
        mockUser.firstName
      );
    });

    // Clear and update fields
    const firstNameInput = screen.getByPlaceholderText('Vul voornaam in');
    const lastNameInput = screen.getByPlaceholderText('Vul achternaam in');
    const emailInput = screen.getByPlaceholderText('Vul email in');

    await userEvent.clear(firstNameInput);
    await userEvent.clear(lastNameInput);
    await userEvent.clear(emailInput);
    await userEvent.type(firstNameInput, 'Updated');
    await userEvent.type(lastNameInput, 'User');
    await userEvent.type(emailInput, 'updated.user@example.com');

    // Submit the form
    const submitButton = screen.getByTestId('submit-button');
    await userEvent.click(submitButton);

    // Check if onSubmit was called with correct data
    await waitFor(() => {
      expect(mockOnSubmit).toHaveBeenCalledTimes(1);
      expect(mockOnSubmit).toHaveBeenCalledWith(
        expect.objectContaining({
          id: mockUser.id,
          firstName: 'Updated',
          lastName: 'User',
          email: 'updated.user@example.com',
        })
      );
    });

    // Check if onCancel was called after successful submission
    expect(mockOnCancel).toHaveBeenCalledTimes(1);
  });
});
