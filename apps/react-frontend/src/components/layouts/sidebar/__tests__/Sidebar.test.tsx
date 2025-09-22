import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import { Sidebar } from '../Sidebar.tsx';

// Mock the useAuth hook
const mockHasRole = vi.fn();

vi.mock('@/components/auth/useAuthHook.ts', () => ({
  useAuth: () => ({
    user: { firstName: 'Test', lastName: 'User' },
    signOut: vi.fn(),
    hasRole: mockHasRole,
  }),
}));

describe('Sidebar', () => {
  beforeEach(() => {
    // Reset the mock before each test
    mockHasRole.mockReset();
  });

  it('should render all nav items for admin user', () => {
    // Mock admin user that has all roles
    mockHasRole.mockImplementation(() => true);

    render(
      <MemoryRouter initialEntries={['/']}>
        <Sidebar />
      </MemoryRouter>
    );

    expect(screen.getByAltText('full-logo')).toBeInTheDocument();
    // All 5 navigation items should be visible
    expect(screen.getAllByRole('link')).toHaveLength(6);
    expect(screen.getByText('Gebruikersbeheer')).toBeInTheDocument();
  });

  it('should hide admin-only nav items for non-admin user', () => {
    // Mock non-admin user
    mockHasRole.mockImplementation((role) => role !== 'admin');

    render(
      <MemoryRouter initialEntries={['/']}>
        <Sidebar />
      </MemoryRouter>
    );

    expect(screen.getByAltText('full-logo')).toBeInTheDocument();
    // Only 4 navigation items should be visible (without Gebruikersbeheer)
    expect(screen.getAllByRole('link')).toHaveLength(5);
    expect(screen.queryByText('Gebruikersbeheer')).not.toBeInTheDocument();
  });
});
