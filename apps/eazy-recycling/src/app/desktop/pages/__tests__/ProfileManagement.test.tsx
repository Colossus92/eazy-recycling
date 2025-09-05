import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { BrowserRouter } from 'react-router-dom';
import { ProfileManagement } from '../ProfileManagement';

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

// Mock the AvatarMenu component to avoid AuthContext dependency
vi.mock('@/components/layouts/header/AvatarMenu.tsx', () => ({
  AvatarMenu: () => <div data-testid="avatar-menu">Avatar Menu</div>,
}));

// Mock the useNavigate hook
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

// Create a mock update function that we can control in tests
const mockUpdate = vi.fn().mockImplementation((user) => Promise.resolve(user));

// Mock the useUserCrud hook
vi.mock('@/features/users/useUserCrud.ts', () => ({
  useUserCrud: () => ({
    updateProfile: mockUpdate,
  }),
}));

// Mock the Supabase client
vi.mock('@/api/supabaseClient.tsx', () => {
  const mockUser = {
    id: 'user-1',
    user_metadata: {
      first_name: 'John',
      last_name: 'Doe',
      roles: ['admin'],
    },
    email: 'john.doe@example.com',
    last_sign_in_at: '2023-01-01T12:00:00Z',
  };

  return {
    supabase: {
      auth: {
        getUser: vi.fn().mockResolvedValue({
          data: { user: mockUser },
          error: null,
        }),
      },
    },
  };
});

// Create a wrapper with QueryClientProvider and BrowserRouter for the component
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
    },
  },
});

const wrapper = ({ children }: { children: React.ReactNode }) => (
  <QueryClientProvider client={queryClient}>
    <BrowserRouter>{children}</BrowserRouter>
  </QueryClientProvider>
);

describe('ProfileManagement', () => {
  beforeEach(() => {
    queryClient.clear();
    vi.clearAllMocks();
    mockNavigate.mockReset();
  });

  it('renders loading state initially', async () => {
    render(<ProfileManagement />, { wrapper });

    expect(screen.getByText('Laden...')).toBeInTheDocument();
  });

  it('renders the profile form with user data after loading', async () => {
    render(<ProfileManagement />, { wrapper });

    // Wait for the profile to load
    await waitFor(() => {
      expect(screen.queryByText('Laden...')).not.toBeInTheDocument();
    });

    // Check page title
    expect(screen.getByText('Mijn Profiel')).toBeInTheDocument();
    expect(screen.getByText('Beheer je profiel')).toBeInTheDocument();

    // Check form fields
    expect(screen.getByText('Voornaam')).toBeInTheDocument();
    expect(screen.getByText('Achternaam')).toBeInTheDocument();
    expect(screen.getByText('Email')).toBeInTheDocument();
    expect(screen.getByText('Rol')).toBeInTheDocument();

    // Check form values
    expect(screen.getByPlaceholderText('Vul voornaam in')).toHaveValue('John');
    expect(screen.getByPlaceholderText('Vul achternaam in')).toHaveValue('Doe');
    expect(screen.getByPlaceholderText('Vul email in')).toHaveValue(
      'john.doe@example.com'
    );

    // Check avatar
    expect(screen.getByTestId('avatar')).toHaveTextContent('John Doe');

    // Check buttons
    expect(screen.getByTestId('cancel-button')).toBeInTheDocument();
    expect(screen.getByTestId('submit-button')).toBeInTheDocument();
  });

  it('navigates back when cancel button is clicked', async () => {
    render(<ProfileManagement />, { wrapper });

    // Wait for the profile to load
    await waitFor(() => {
      expect(screen.queryByText('Laden...')).not.toBeInTheDocument();
    });

    const cancelButton = screen.getByTestId('cancel-button');
    await userEvent.click(cancelButton);

    expect(mockNavigate).toHaveBeenCalledWith('/');
  });

  it('shows validation errors when submitting with empty required fields', async () => {
    render(<ProfileManagement />, { wrapper });

    // Wait for the profile to load
    await waitFor(() => {
      expect(screen.queryByText('Laden...')).not.toBeInTheDocument();
    });

    // Clear the form fields
    const firstNameInput = screen.getByPlaceholderText('Vul voornaam in');
    const lastNameInput = screen.getByPlaceholderText('Vul achternaam in');
    const emailInput = screen.getByPlaceholderText('Vul email in');

    await userEvent.clear(firstNameInput);
    await userEvent.clear(lastNameInput);
    await userEvent.clear(emailInput);

    // Submit the form
    const submitButton = screen.getByTestId('submit-button');
    await userEvent.click(submitButton);

    // Check validation errors
    await waitFor(() => {
      expect(screen.getByText('Voornaam is verplicht')).toBeInTheDocument();
      expect(screen.getByText('Achternaam is verplicht')).toBeInTheDocument();
      expect(screen.getByText('Email is verplicht')).toBeInTheDocument();
    });
  });

  it('shows validation error for invalid email format', async () => {
    render(<ProfileManagement />, { wrapper });

    // Wait for the profile to load
    await waitFor(() => {
      expect(screen.queryByText('Laden...')).not.toBeInTheDocument();
    });

    // Clear and type invalid email
    const emailInput = screen.getByPlaceholderText('Vul email in');
    await userEvent.clear(emailInput);
    await userEvent.type(emailInput, 'invalid-email');

    // Submit the form
    const submitButton = screen.getByTestId('submit-button');
    await userEvent.click(submitButton);

    // Check validation error for email
    await waitFor(() => {
      expect(
        screen.getByText('Vul een geldig emailadres in')
      ).toBeInTheDocument();
    });
  });

  it('submits the form with valid data and navigates to home', async () => {
    // Reset the mock and make it resolve successfully
    mockUpdate.mockReset();
    mockUpdate.mockResolvedValueOnce({});

    render(<ProfileManagement />, { wrapper });

    // Wait for the profile to load
    await waitFor(() => {
      expect(screen.queryByText('Laden...')).not.toBeInTheDocument();
    });

    // Update form fields
    const firstNameInput = screen.getByPlaceholderText('Vul voornaam in');
    const lastNameInput = screen.getByPlaceholderText('Vul achternaam in');

    await userEvent.clear(firstNameInput);
    await userEvent.clear(lastNameInput);
    await userEvent.type(firstNameInput, 'Updated');
    await userEvent.type(lastNameInput, 'Name');

    // Submit the form
    const submitButton = screen.getByTestId('submit-button');
    await userEvent.click(submitButton);

    // Check if update was called and navigation happened
    await waitFor(() => {
      expect(mockUpdate).toHaveBeenCalledWith(
        expect.objectContaining({
          firstName: 'Updated',
          lastName: 'Name',
          email: 'john.doe@example.com',
          roles: ['admin'],
        })
      );
      expect(mockNavigate).toHaveBeenCalledWith('/');
    });
  });

  it('shows error dialog when update fails', async () => {
    // Mock the update function to reject with an error
    mockUpdate.mockRejectedValueOnce(new Error('Update failed'));

    render(<ProfileManagement />, { wrapper });

    // Wait for the profile to load
    await waitFor(() => {
      expect(screen.queryByText('Laden...')).not.toBeInTheDocument();
    });

    // Submit the form
    const submitButton = screen.getByTestId('submit-button');
    await userEvent.click(submitButton);

    // Check if error dialog is shown
    await waitFor(() => {
      expect(screen.getByTestId('error-dialog')).toBeInTheDocument();
      expect(screen.getByTestId('error-dialog')).toHaveTextContent(
        'Update failed'
      );
    });

    // Navigation should not happen on error
    expect(mockNavigate).not.toHaveBeenCalled();
  });
});
