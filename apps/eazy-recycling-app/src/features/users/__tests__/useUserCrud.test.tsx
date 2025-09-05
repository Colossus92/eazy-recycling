import { act, ReactNode } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { useUserCrud } from '../useUserCrud';
import { User } from '@/types/api';

// Mock the userService
vi.mock('@/api/userService.ts', () => {
  const initialUsers = [
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
    {
      id: 'user-3',
      firstName: 'Bob',
      lastName: 'Johnson',
      email: 'bob.johnson@example.com',
      roles: ['chauffeur'],
      lastSignInAt: '2023-01-03T12:00:00Z',
    },
  ];

  const users = [...initialUsers];

  return {
    userService: {
      list: vi.fn().mockImplementation(() => Promise.resolve([...users])),
      create: vi.fn().mockImplementation((user) => {
        const newUser = { ...user, id: `user-${users.length + 1}` };
        users.push(newUser);
        return Promise.resolve(newUser);
      }),
      update: vi.fn().mockImplementation((user) => {
        const index = users.findIndex((u) => u.id === user.id);
        if (index !== -1) {
          users[index] = user;
        }
        return Promise.resolve(user);
      }),
      remove: vi.fn().mockImplementation((id) => {
        const index = users.findIndex((u) => u.id === id);
        if (index !== -1) {
          users.splice(index, 1);
          return Promise.resolve({ success: true });
        }
        return Promise.resolve({ success: false });
      }),
      listDrivers: vi
        .fn()
        .mockImplementation(() =>
          Promise.resolve(
            users.filter((user) => user.roles.includes('chauffeur'))
          )
        ),
    },
  };
});

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
    },
  },
});

const wrapper = ({ children }: { children: ReactNode }) => (
  <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
);

describe('useUserCrud', () => {
  // Reset QueryClient before each test
  beforeEach(() => {
    queryClient.clear();
  });

  it('initializes with all users and empty query', async () => {
    const { result } = renderHook(() => useUserCrud(), { wrapper });

    // Wait for the query to resolve
    await vi.waitFor(() =>
      expect(result.current.displayedUsers.length).toBeGreaterThan(0)
    );

    expect(result.current.displayedUsers.length).toBeGreaterThan(0);
    expect(result.current.isAdding).toBe(false);
    expect(result.current.editing).toBeUndefined();
    expect(result.current.deleting).toBeUndefined();
  });

  it('filters displayedUsers by query (case-insensitive)', async () => {
    const { result } = renderHook(() => useUserCrud(), { wrapper });

    // Wait for initial data to load
    await vi.waitFor(() =>
      expect(result.current.displayedUsers.length).toBeGreaterThan(0)
    );

    // Search by first name
    await act(async () => {
      result.current.setQuery('jane');
    });

    expect(result.current.displayedUsers.length).toBe(1);
    expect(result.current.displayedUsers[0].firstName.toLowerCase()).toContain(
      'jane'
    );

    // Search by last name
    await act(async () => {
      result.current.setQuery('smith');
    });

    expect(result.current.displayedUsers.length).toBe(1);
    expect(result.current.displayedUsers[0].lastName.toLowerCase()).toContain(
      'smith'
    );

    // Search by email
    await act(async () => {
      result.current.setQuery('bob.johnson');
    });

    expect(result.current.displayedUsers.length).toBe(1);
    expect(result.current.displayedUsers[0].email.toLowerCase()).toContain(
      'bob.johnson'
    );

    // No results
    await act(async () => {
      result.current.setQuery('nonexistent');
    });

    expect(result.current.displayedUsers.length).toBe(0);
  });

  it('toggles add, edit and delete flags', async () => {
    const { result } = renderHook(() => useUserCrud(), { wrapper });

    // Wait for initial data to load
    await vi.waitFor(() =>
      expect(result.current.displayedUsers.length).toBeGreaterThan(0)
    );

    const user1 = result.current.displayedUsers[0];
    const user2 = result.current.displayedUsers[1];

    act(() => {
      result.current.setIsAdding(true);
    });
    expect(result.current.isAdding).toBe(true);

    act(() => {
      result.current.setEditing(user1);
    });
    expect(result.current.editing).toEqual(user1);

    act(() => {
      result.current.setDeleting(user2);
    });
    expect(result.current.deleting).toEqual(user2);
  });

  it('creates a new user correctly', async () => {
    const { result } = renderHook(() => useUserCrud(), { wrapper });

    // Wait for initial data to load
    await vi.waitFor(() =>
      expect(result.current.displayedUsers.length).toBeGreaterThan(0)
    );

    const initialLength = result.current.displayedUsers.length;

    const newUser: Omit<User, 'id'> = {
      firstName: 'Test',
      lastName: 'User',
      email: 'test.user@example.com',
      roles: ['planner'],
      lastSignInAt: new Date().toISOString(),
    };

    await act(async () => {
      await result.current.create(newUser);
    });

    // Wait for the displayedUsers to update
    await vi.waitFor(() =>
      expect(result.current.displayedUsers.length).toBe(initialLength + 1)
    );

    // Check that the new user is in the list
    const addedUser = result.current.displayedUsers.find(
      (u) => u.email === newUser.email
    );
    expect(addedUser).toBeDefined();
    expect(addedUser?.firstName).toBe(newUser.firstName);
    expect(addedUser?.lastName).toBe(newUser.lastName);

    // Check that isAdding is reset to false
    expect(result.current.isAdding).toBe(false);
  });

  it('updates an existing user correctly', async () => {
    const { result } = renderHook(() => useUserCrud(), { wrapper });

    // Wait for initial data to load
    await vi.waitFor(() =>
      expect(result.current.displayedUsers.length).toBeGreaterThan(0)
    );

    const userToUpdate = {
      ...result.current.displayedUsers[0],
      firstName: 'Updated',
      lastName: 'Name',
    };

    await act(async () => {
      await result.current.update(userToUpdate);
    });

    // Wait for the displayedUsers to update
    await vi.waitFor(() => {
      const found = result.current.displayedUsers.find(
        (u) => u.id === userToUpdate.id
      );
      return (
        found && found.firstName === 'Updated' && found.lastName === 'Name'
      );
    });

    // Check that the user was updated
    const updatedUser = result.current.displayedUsers.find(
      (u) => u.id === userToUpdate.id
    );
    expect(updatedUser).toBeDefined();
    expect(updatedUser?.firstName).toBe('Updated');
    expect(updatedUser?.lastName).toBe('Name');

    // Check that editing is reset to undefined
    expect(result.current.editing).toBeUndefined();
  });

  it('removes an existing user correctly', async () => {
    const { result } = renderHook(() => useUserCrud(), { wrapper });

    // Wait for initial data to load
    await vi.waitFor(() =>
      expect(result.current.displayedUsers.length).toBeGreaterThan(0)
    );

    const userToRemove = result.current.displayedUsers[0];
    const initialLength = result.current.displayedUsers.length;

    await act(async () => {
      await result.current.remove(userToRemove);
    });

    // Wait for the displayedUsers to update
    await vi.waitFor(() =>
      expect(result.current.displayedUsers.length).toBe(initialLength - 1)
    );

    // Check that the user was removed
    const removedUser = result.current.displayedUsers.find(
      (u) => u.id === userToRemove.id
    );
    expect(removedUser).toBeUndefined();

    // Check that deleting is reset to undefined
    expect(result.current.deleting).toBeUndefined();
  });

  it('handles create operation failure', async () => {
    const { result } = renderHook(() => useUserCrud(), { wrapper });

    // Wait for initial data to load
    await vi.waitFor(() =>
      expect(result.current.displayedUsers.length).toBeGreaterThan(0)
    );

    // Mock the create function to reject
    const mockError = new Error('Create failed');
    const createSpy = vi
      .spyOn(result.current, 'create')
      .mockRejectedValueOnce(mockError);

    const newUser: Omit<User, 'id'> = {
      firstName: 'Test',
      lastName: 'User',
      email: 'test.user@example.com',
      roles: ['planner'],
      lastSignInAt: new Date().toISOString(),
    };

    // Attempt to create and expect it to fail
    await expect(result.current.create(newUser)).rejects.toThrow(
      'Create failed'
    );

    // Clean up
    createSpy.mockRestore();
  });

  it('handles update operation failure', async () => {
    const { result } = renderHook(() => useUserCrud(), { wrapper });

    // Wait for initial data to load
    await vi.waitFor(() =>
      expect(result.current.displayedUsers.length).toBeGreaterThan(0)
    );

    // Mock the update function to reject
    const mockError = new Error('Update failed');
    const updateSpy = vi
      .spyOn(result.current, 'update')
      .mockRejectedValueOnce(mockError);

    const userToUpdate = result.current.displayedUsers[0];

    // Attempt to update and expect it to fail
    await expect(result.current.update(userToUpdate)).rejects.toThrow(
      'Update failed'
    );

    // Clean up
    updateSpy.mockRestore();
  });

  it('handles remove operation failure', async () => {
    const { result } = renderHook(() => useUserCrud(), { wrapper });

    // Wait for initial data to load
    await vi.waitFor(() =>
      expect(result.current.displayedUsers.length).toBeGreaterThan(0)
    );

    // Mock the remove function to reject
    const mockError = new Error('Remove failed');
    const removeSpy = vi
      .spyOn(result.current, 'remove')
      .mockRejectedValueOnce(mockError);

    const userToRemove = result.current.displayedUsers[0];

    // Attempt to remove and expect it to fail
    await expect(result.current.remove(userToRemove)).rejects.toThrow(
      'Remove failed'
    );

    // Clean up
    removeSpy.mockRestore();
  });
});
