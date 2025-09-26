import { act, ReactNode } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { Company } from '@/api/services/companyService';
import { useCompanyCrud } from '../useCompanyCrud';

// Mock the containerService
vi.mock('@/api/services/companyService.ts', () => {
  const initialCompanies: Company[] = [
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
      updatedAt: '2025-01-01T00:00:00.000Z',
      branches: [],
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
      updatedAt: '2025-01-01T00:00:00.000Z',
      branches: [],
    },
    {
      id: 'comp-3',
      name: 'Circular Economy BV',
      address: {
        streetName: 'Sustainability Road',
        buildingNumber: '78',
        postalCode: '9012 EF',
        city: 'Utrecht',
      },
      chamberOfCommerceId: 'KVK789012',
      vihbId: 'VIHB345',
      updatedAt: '2025-01-01T00:00:00.000Z',
      branches: [],
    },
  ];

  const companies = [...initialCompanies];

  return {
    companyService: {
      getAll: vi.fn().mockImplementation(() => Promise.resolve([...companies])),
      create: vi.fn().mockImplementation((company) => {
        const newCompany = { ...company, id: `comp-${companies.length + 1}` };
        companies.push(newCompany);
        return Promise.resolve(newCompany);
      }),
      update: vi.fn().mockImplementation((company) => {
        const index = companies.findIndex((c) => c.id === company.id);
        if (index !== -1) {
          companies[index] = company;
        }
        return Promise.resolve(company);
      }),
      delete: vi.fn().mockImplementation((id) => {
        const index = companies.findIndex((c) => c.id === id);
        if (index !== -1) {
          companies.splice(index, 1);
          return Promise.resolve();
        }
        return Promise.reject(new Error('Company not found'));
      }),
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

describe('useCompanyCrud', () => {
  // Reset QueryClient before each test
  beforeEach(() => {
    queryClient.clear();
  });

  it('initializes with all companies and empty query', async () => {
    const { result } = renderHook(() => useCompanyCrud(), { wrapper });

    // Wait for the query to resolve
    await vi.waitFor(() =>
      expect(result.current.displayedCompanies.length).toBeGreaterThan(0)
    );

    expect(result.current.displayedCompanies.length).toBeGreaterThan(0);
    expect(result.current.isAdding).toBe(false);
    expect(result.current.editing).toBeUndefined();
    expect(result.current.deleting).toBeUndefined();
  });

  it('filters displayedCompanies by query (case-insensitive)', async () => {
    const { result } = renderHook(() => useCompanyCrud(), { wrapper });

    // Wait for initial data to load
    await vi.waitFor(() =>
      expect(result.current.displayedCompanies.length).toBeGreaterThan(0)
    );

    // Search by company name
    await act(async () => {
      result.current.setQuery('acme');
    });

    expect(result.current.displayedCompanies.length).toBe(1);
    expect(result.current.displayedCompanies[0].name.toLowerCase()).toContain(
      'acme'
    );

    // Search by street name
    await act(async () => {
      result.current.setQuery('eco street');
    });

    expect(result.current.displayedCompanies.length).toBe(1);
    expect(
      result.current.displayedCompanies[0]?.address?.streetName?.toLowerCase()
    ).toContain('eco');

    // Search by chamber of commerce ID
    await act(async () => {
      result.current.setQuery('KVK789');
    });

    expect(result.current.displayedCompanies.length).toBe(1);
    expect(result.current.displayedCompanies[0].chamberOfCommerceId).toContain(
      'KVK789'
    );

    // Search by VIHB ID
    await act(async () => {
      result.current.setQuery('VIHB987');
    });

    expect(result.current.displayedCompanies.length).toBe(1);
    expect(result.current.displayedCompanies[0].vihbId).toContain('VIHB987');

    // No results
    await act(async () => {
      result.current.setQuery('nonexistent');
    });

    expect(result.current.displayedCompanies.length).toBe(0);
  });

  it('toggles add, edit and delete flags', async () => {
    const { result } = renderHook(() => useCompanyCrud(), { wrapper });

    // Wait for initial data to load
    await vi.waitFor(() =>
      expect(result.current.displayedCompanies.length).toBeGreaterThan(0)
    );

    const company1 = result.current.displayedCompanies[0];
    const company2 = result.current.displayedCompanies[1];

    act(() => {
      result.current.setIsAdding(true);
    });
    expect(result.current.isAdding).toBe(true);

    act(() => {
      result.current.setEditing(company1);
    });
    expect(result.current.editing).toEqual(company1);

    act(() => {
      result.current.setDeleting(company2);
    });
    expect(result.current.deleting).toEqual(company2);
  });

  it('creates a new company correctly', async () => {
    const { result } = renderHook(() => useCompanyCrud(), { wrapper });

    // Wait for initial data to load
    await vi.waitFor(() =>
      expect(result.current.displayedCompanies.length).toBeGreaterThan(0)
    );

    const initialLength = result.current.displayedCompanies.length;

    const newCompany = {
      name: 'Test Company',
      address: {
        streetName: 'Test Street',
        buildingNumber: '42',
        postalCode: '1234 ZZ',
        city: 'Testville',
      },
      chamberOfCommerceId: 'KVK999999',
      vihbId: 'VIHB999',
    } as Omit<Company, 'id'>;

    await act(async () => {
      await result.current.create(newCompany);
    });

    // Wait for the displayedCompanies to update
    await vi.waitFor(() =>
      expect(result.current.displayedCompanies.length).toBe(initialLength + 1)
    );

    // Check that the new company is in the list
    const addedCompany = result.current.displayedCompanies.find(
      (c) => c.name === newCompany.name
    );
    expect(addedCompany).toBeDefined();
    expect(addedCompany?.address.city).toBe(newCompany.address.city);
    expect(addedCompany?.chamberOfCommerceId).toBe(
      newCompany.chamberOfCommerceId
    );

    // Check that isAdding is reset to false
    expect(result.current.isAdding).toBe(false);
  });

  it('updates an existing company correctly', async () => {
    const { result } = renderHook(() => useCompanyCrud(), { wrapper });

    // Wait for initial data to load
    await vi.waitFor(() =>
      expect(result.current.displayedCompanies.length).toBeGreaterThan(0)
    );

    const companyToUpdate = {
      ...result.current.displayedCompanies[0],
      name: 'Updated Company Name',
    };

    await act(async () => {
      await result.current.update(companyToUpdate);
    });

    // Wait for the displayedCompanies to update
    await vi.waitFor(() => {
      const found = result.current.displayedCompanies.find(
        (c) => c.id === companyToUpdate.id
      );
      return found && found.name === 'Updated Company Name';
    });

    // Check that the company was updated
    const updatedCompany = result.current.displayedCompanies.find(
      (c) => c.id === companyToUpdate.id
    );
    expect(updatedCompany).toBeDefined();
    expect(updatedCompany?.name).toBe('Updated Company Name');

    // Check that editing is reset to undefined
    expect(result.current.editing).toBeUndefined();
  });

  it('removes an existing company correctly', async () => {
    const { result } = renderHook(() => useCompanyCrud(), { wrapper });

    // Wait for initial data to load
    await vi.waitFor(() =>
      expect(result.current.displayedCompanies.length).toBeGreaterThan(0)
    );

    const companyToRemove = result.current.displayedCompanies[0];
    const initialLength = result.current.displayedCompanies.length;

    await act(async () => {
      await result.current.remove(companyToRemove);
    });

    // Wait for the displayedCompanies to update
    await vi.waitFor(() =>
      expect(result.current.displayedCompanies.length).toBe(initialLength - 1)
    );

    // Check that the company was removed
    const removedCompany = result.current.displayedCompanies.find(
      (c) => c.id === companyToRemove.id
    );
    expect(removedCompany).toBeUndefined();

    // Check that deleting is reset to undefined
    expect(result.current.deleting).toBeUndefined();
  });

  it('handles create operation failure', async () => {
    const { result } = renderHook(() => useCompanyCrud(), { wrapper });

    // Wait for initial data to load
    await vi.waitFor(() =>
      expect(result.current.displayedCompanies.length).toBeGreaterThan(0)
    );

    // Mock the create function to reject
    const mockError = new Error('Create failed');
    const createSpy = vi
      .spyOn(result.current, 'create')
      .mockRejectedValueOnce(mockError);

    const newCompany = {
      name: 'Test Company',
      address: {
        streetName: 'Test Street',
        buildingNumber: '42',
        postalCode: '1234 ZZ',
        city: 'Testville',
      },
      chamberOfCommerceId: 'KVK999999',
      vihbId: 'VIHB999',
    } as Omit<Company, 'id'>;

    // Attempt to create and expect it to fail
    await expect(result.current.create(newCompany)).rejects.toThrow(
      'Create failed'
    );

    // Clean up
    createSpy.mockRestore();
  });

  it('handles update operation failure', async () => {
    const { result } = renderHook(() => useCompanyCrud(), { wrapper });

    // Wait for initial data to load
    await vi.waitFor(() =>
      expect(result.current.displayedCompanies.length).toBeGreaterThan(0)
    );

    // Mock the update function to reject
    const mockError = new Error('Update failed');
    const updateSpy = vi
      .spyOn(result.current, 'update')
      .mockRejectedValueOnce(mockError);

    const companyToUpdate = result.current.displayedCompanies[0];

    // Attempt to update and expect it to fail
    await expect(result.current.update(companyToUpdate)).rejects.toThrow(
      'Update failed'
    );

    // Clean up
    updateSpy.mockRestore();
  });

  it('handles remove operation failure', async () => {
    const { result } = renderHook(() => useCompanyCrud(), { wrapper });

    // Wait for initial data to load
    await vi.waitFor(() =>
      expect(result.current.displayedCompanies.length).toBeGreaterThan(0)
    );

    // Mock the remove function to reject
    const mockError = new Error('Remove failed');
    const removeSpy = vi
      .spyOn(result.current, 'remove')
      .mockRejectedValueOnce(mockError);

    const companyToRemove = result.current.displayedCompanies[0];

    // Attempt to remove and expect it to fail
    await expect(result.current.remove(companyToRemove)).rejects.toThrow(
      'Remove failed'
    );

    // Clean up
    removeSpy.mockRestore();
  });
});
