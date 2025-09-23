import { act, ReactNode } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { useWasteContainerCrud } from '../useWasteContainerCrud';
import { wasteContainers as initialContainers } from '../../../testing/mocks/mockWasteContainers';
import { WasteContainer, CreateContainerRequest } from '@/api/client';

// Mock the containerService
vi.mock('@/api/services/containerService', () => {
  return {
    containerService: {
      getAll: vi
        .fn()
        .mockImplementation(() => Promise.resolve([...initialContainers])),
      create: vi.fn().mockImplementation((container) => {
        const newContainer = { ...container, uuid: 'new-uuid', id: 'CONT-NEW' };
        initialContainers.push(newContainer);
        return Promise.resolve(newContainer);
      }),
      update: vi.fn().mockImplementation((container) => {
        const index = initialContainers.findIndex(
          (c) => c.uuid === container.uuid
        );
        if (index !== -1) {
          initialContainers[index] = container;
        }
        return Promise.resolve(container);
      }),
      delete: vi.fn().mockImplementation((uuid) => {
        const index = initialContainers.findIndex(
          (c) => c.uuid === uuid
        );
        if (index !== -1) {
          initialContainers.splice(index, 1);
          return Promise.resolve({ success: true });
        }
        return Promise.resolve({ success: false });
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

describe('useWasteContainerCrud', () => {
  // Reset QueryClient before each test
  beforeEach(() => {
    queryClient.clear();
  });

  it('initializes with all containers and empty query', async () => {
    const { result } = renderHook(() => useWasteContainerCrud(), { wrapper });

    // Wait for the query to resolve
    await vi.waitFor(() =>
      expect(result.current.displayedContainers.length).toBeGreaterThan(0)
    );

    expect(result.current.displayedContainers).toEqual(initialContainers);
    expect(result.current.isAdding).toBe(false);
    expect(result.current.editing).toBeUndefined();
    expect(result.current.deleting).toBeUndefined();
  });

  it('filters displayedContainers by query (case‑insensitive)', async () => {
    const { result } = renderHook(() => useWasteContainerCrud(), { wrapper });

    // Wait for initial data to load
    await vi.waitFor(() =>
      expect(result.current.displayedContainers.length).toBeGreaterThan(0)
    );

    // Search by container ID
    await act(async () => {
      result.current.setQuery(initialContainers[0].id.toUpperCase());
    });

    expect(result.current.displayedContainers).toHaveLength(1);
    expect(result.current.displayedContainers[0].id).toBe(
      initialContainers[0].id
    );

    // Search by company name
    await act(async () => {
      result.current.setQuery('green');
    });

    expect(result.current.displayedContainers.length).toBeGreaterThan(0);
    result.current.displayedContainers.forEach((container) => {
      expect(
        container.location?.companyName?.toLowerCase().includes('green') ||
          container.notes?.toLowerCase().includes('green') ||
          container.location?.address?.streetName
            ?.toLowerCase()
            .includes('green') ||
          container.location?.address?.city?.toLowerCase().includes('green')
      ).toBeTruthy();
    });

    // Search by city
    await act(async () => {
      result.current.setQuery('amsterdam');
    });

    expect(result.current.displayedContainers.length).toBeGreaterThan(0);
    result.current.displayedContainers.forEach((container) => {
      expect(container.location?.address?.city?.toLowerCase()).toBe('amsterdam');
    });

    // Search by notes
    await act(async () => {
      result.current.setQuery('entrance');
    });

    expect(result.current.displayedContainers.length).toBeGreaterThan(0);
    result.current.displayedContainers.forEach((container) => {
      expect(container.notes?.toLowerCase().includes('entrance')).toBeTruthy();
    });
  });

  it('toggling add, edit and delete flags', async () => {
    const { result } = renderHook(() => useWasteContainerCrud(), { wrapper });
    act(() => {
      result.current.setIsAdding(true);
      result.current.setEditing(initialContainers[1]);
      result.current.setDeleting(initialContainers[2]);
    });
    expect(result.current.isAdding).toBe(true);
    expect(result.current.editing).toEqual(initialContainers[1]);
    expect(result.current.deleting).toEqual(initialContainers[2]);
  });

  it('removes an existing container correctly', async () => {
    const target = initialContainers[0];
    const { result } = renderHook(() => useWasteContainerCrud(), { wrapper });

    // Wait for initial data to load
    await vi.waitFor(() =>
      expect(result.current.displayedContainers.length).toBeGreaterThan(0)
    );

    await act(async () => {
      await result.current.remove(target);
    });

    await vi.waitFor(() => {
      expect(result.current.displayedContainers).not.toContainEqual(target);
    });

    expect(result.current.deleting).toBeUndefined();
  });

  it('create(newItem) adds new container', async () => {
    const newItem: CreateContainerRequest = {
      id: 'CONT-NEW',
      location: {
        companyName: 'New Company',
        address: {
          streetName: 'Test Street',
          buildingNumber: '42',
          postalCode: '1234 ZZ',
          city: 'Eindhoven',
        },
      },
      notes: 'Test container',
    };

    const { result } = renderHook(() => useWasteContainerCrud(), { wrapper });

    // Wait for initial data to load
    await vi.waitFor(() =>
      expect(result.current.displayedContainers.length).toBeGreaterThan(0)
    );

    // Get the initial length to compare later
    const initialLength = result.current.displayedContainers.length;

    // Create the new item
    await act(async () => {
      await result.current.create(newItem);
    });

    // Wait for the displayedContainers to update with the new item
    await vi.waitFor(() =>
      expect(result.current.displayedContainers.length).toBe(initialLength + 1)
    );

    // Check that the new item is in the list
    expect(result.current.displayedContainers).toContainEqual(
      expect.objectContaining({
        location: expect.objectContaining({
          companyName: newItem.location?.companyName,
          address: expect.objectContaining({
            city: newItem.location?.address?.city,
          }),
        }),
        notes: newItem.notes,
      })
    );
  });

  it('update(item) updates an existing container', async () => {
    const target: WasteContainer = { ...initialContainers[0], notes: 'Updated notes' };
    const { result } = renderHook(() => useWasteContainerCrud(), { wrapper });

    // Wait for initial data to load
    await vi.waitFor(() =>
      expect(result.current.displayedContainers.length).toBeGreaterThan(0)
    );

    await act(async () => {
      await result.current.update(target);
    });

    await vi.waitFor(() => {
      expect(result.current.displayedContainers).toContainEqual(target);
    });

    const updatedContainer = result.current.displayedContainers.find(
      (container) => container.uuid === target.uuid
    );

    expect(updatedContainer).toEqual(target);
    expect(updatedContainer?.notes).toBe('Updated notes');
    expect(result.current.editing).toBeUndefined();
  });
});
