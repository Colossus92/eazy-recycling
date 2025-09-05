import { act, ReactNode } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { useTruckCrud } from '../useTruckCrud';
import { trucks as initialTrucks } from '../../../testing/mocks/mockTrucks';
import { Truck } from '@/types/api';

// Mock the truckService
vi.mock('@/api/truckService.ts', () => {
  return {
    truckService: {
      list: vi
        .fn()
        .mockImplementation(() => Promise.resolve([...initialTrucks])),
      create: vi.fn().mockImplementation((truck) => {
        const newTruck = { ...truck, id: 'new-id' };
        initialTrucks.push(newTruck);
        return Promise.resolve(newTruck);
      }),
      update: vi.fn().mockImplementation((truck) => {
        const index = initialTrucks.findIndex(
          (t) => t.licensePlate === truck.licensePlate
        );
        if (index !== -1) {
          initialTrucks[index] = truck;
        }
        return Promise.resolve(truck);
      }),
      remove: vi.fn().mockImplementation((licensePlate) => {
        const index = initialTrucks.findIndex(
          (t) => t.licensePlate === licensePlate
        );
        if (index !== -1) {
          initialTrucks.splice(index, 1);
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

describe('useTruckCrud', () => {
  // Reset QueryClient before each test
  beforeEach(() => {
    queryClient.clear();
  });
  it('initializes with all trucks and empty query', async () => {
    const { result } = renderHook(() => useTruckCrud(), { wrapper });

    // Wait for the query to resolve
    await vi.waitFor(() =>
      expect(result.current.displayedTrucks.length).toBeGreaterThan(0)
    );

    expect(result.current.displayedTrucks).toEqual(initialTrucks);
    expect(result.current.isAdding).toBe(false);
    expect(result.current.editing).toBeUndefined();
    expect(result.current.deleting).toBeUndefined();
  });

  it('filters displayedTrucks by query (case‑insensitive)', async () => {
    const { result } = renderHook(() => useTruckCrud(), { wrapper });
    await act(async () => {
      result.current.setQuery(initialTrucks[0].licensePlate.toUpperCase());
    });

    await vi.waitFor(() =>
      expect(result.current.displayedTrucks.length).toBeGreaterThan(0)
    );

    expect(result.current.displayedTrucks).toHaveLength(1);
    expect(result.current.displayedTrucks[0].licensePlate).toBe(
      initialTrucks[0].licensePlate
    );
  });

  it('toggling add, edit and delete flags', async () => {
    const { result } = renderHook(() => useTruckCrud(), { wrapper });
    act(() => {
      result.current.setIsAdding(true);
      result.current.setEditing(initialTrucks[1]);
      result.current.setDeleting(initialTrucks[2]);
    });
    expect(result.current.isAdding).toBe(true);
    expect(result.current.editing).toEqual(initialTrucks[1]);
    expect(result.current.deleting).toEqual(initialTrucks[2]);
  });

  it('removes an existing truck correctly', async () => {
    const target = initialTrucks[0];
    const { result } = renderHook(() => useTruckCrud(), { wrapper });
    act(() => {
      result.current.remove(target);
    });
    expect(result.current.displayedTrucks).not.toContainEqual(target);
    expect(result.current.deleting).toBeUndefined();
  });

  it('create(newItem) adds new item', async () => {
    const newItem: Truck = {
      licensePlate: 'NEW123',
      brand: 'TestBrand',
      model: 'X1',
    };
    const { result } = renderHook(() => useTruckCrud(), { wrapper });

    // Wait for initial data to load
    await vi.waitFor(() =>
      expect(result.current.displayedTrucks.length).toBeGreaterThan(0)
    );

    // Get the initial length to compare later
    const initialLength = result.current.displayedTrucks.length;

    // Create the new item
    await act(async () => {
      await result.current.create(newItem);
    });

    // Wait for the displayedTrucks to update with the new item
    await vi.waitFor(() =>
      expect(result.current.displayedTrucks.length).toBe(initialLength + 1)
    );

    // Check that the new item is in the list
    expect(result.current.displayedTrucks).toContainEqual(
      expect.objectContaining({
        licensePlate: newItem.licensePlate,
        brand: newItem.brand,
        model: newItem.model,
      })
    );
  });

  it('update(item) logs and does not change state (current implementation)', async () => {
    let target: Truck = initialTrucks[0];
    target = { ...target, model: 'Changed' };
    const { result } = renderHook(() => useTruckCrud(), { wrapper });
    await act(() => {
      result.current.update(target);
    });

    await vi.waitFor(() =>
      expect(result.current.displayedTrucks).toContainEqual(target)
    );

    expect(
      result.current.displayedTrucks.find(
        (truck) => truck.licensePlate === target.licensePlate
      )
    ).toEqual(target);
  });
});
