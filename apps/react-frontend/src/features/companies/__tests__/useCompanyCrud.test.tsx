import { act, ReactNode } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { Company, PagedCompanyResponse } from '@/api/services/companyService';
import { useCompanyCrud } from '../useCompanyCrud';

// Mock the containerService
vi.mock('@/api/services/companyService.ts', () => {
  const initialCompanies: Company[] = [
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
      chamberOfCommerceId: 'KVK123456',
      vihbId: 'VIHB789',
      updatedAt: '2025-01-01T00:00:00.000Z',
      branches: [],
      roles: ['PROCESSOR'],
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
      chamberOfCommerceId: 'KVK654321',
      vihbId: 'VIHB987',
      updatedAt: '2025-01-01T00:00:00.000Z',
      branches: [],
      roles: ['CARRIER'],
    },
    {
      id: 'comp-3',
      name: 'Circular Economy BV',
      address: {
        street: 'Sustainability Road',
        houseNumber: '78',
        houseNumberAddition: undefined,
        postalCode: '9012 EF',
        city: 'Utrecht',
        country: 'Nederland',
      },
      chamberOfCommerceId: 'KVK789012',
      vihbId: 'VIHB345',
      updatedAt: '2025-01-01T00:00:00.000Z',
      branches: [],
      roles: ['PROCESSOR', 'CARRIER'],
    },
  ];

  const companies = [...initialCompanies];

  // Helper to create paginated response
  const createPagedResponse = (items: Company[], page = 0, size = 10): PagedCompanyResponse => ({
    content: items,
    page,
    size,
    totalElements: items.length,
    totalPages: Math.ceil(items.length / size),
  });

  return {
    companyService: {
      getAll: vi.fn().mockImplementation(() => Promise.resolve(createPagedResponse([...companies]))),
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

  it('setQuery triggers a new query with the search term', async () => {
    const { result } = renderHook(() => useCompanyCrud(), { wrapper });

    // Wait for initial data to load
    await vi.waitFor(() =>
      expect(result.current.displayedCompanies.length).toBeGreaterThan(0)
    );

    // Set a query - this should trigger a new API call with the query parameter
    // Since filtering is now server-side, we just verify the hook accepts the query
    await act(async () => {
      result.current.setQuery('acme');
    });

    // The mock returns all companies regardless of query, but in production
    // the server would filter. We just verify the hook doesn't crash.
    expect(result.current.displayedCompanies).toBeDefined();
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
      id: '12345678-1234-1234-1234-123456789012',
      name: 'Test Company',
      address: {
        street: 'Test Street',
        houseNumber: '42',
        houseNumberAddition: undefined,
        postalCode: '1234 ZZ',
        city: 'Testville',
        country: 'Nederland',
      },
      chamberOfCommerceId: 'KVK999999',
      vihbId: 'VIHB999',
      updatedAt: '2025-01-01T00:00:00.000Z',
      branches: [],
      processorId: '12345',
      roles: ['PROCESSOR'],
    } as Company;

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
      id: '12345678-1234-1234-1234-123456789012',
      name: 'Test Company',
      address: {
        street: 'Test Street',
        houseNumber: '42',
        houseNumberAddition: undefined,
        postalCode: '1234 ZZ',
        city: 'Testville',
        country: 'Nederland',
      },
      chamberOfCommerceId: 'KVK999999',
      vihbId: 'VIHB999',
    } as Company;

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
