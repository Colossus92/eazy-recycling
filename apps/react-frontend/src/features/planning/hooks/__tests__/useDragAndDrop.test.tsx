import { act, ReactNode } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { DropResult } from '@hello-pangea/dnd';
import { useDragAndDrop } from '../useDragAndDrop';
import { Planning } from '../usePlanning';
import { toastService } from '@/components/ui/toast/toastService';
import { planningService } from '@/api/services/planningService';
import { DriverPlanningItemStatusEnum } from '@/api/client';
import { TransportViewTransportTypeEnum } from '@/api/client/models/transport-view';

// Mock dependencies
vi.mock('@/api/services/planningService', () => {
  return {
    planningService: {
      reorder: vi.fn(),
    },
  };
});

vi.mock('@/components/ui/toast/toastService', () => {
  return {
    toastService: {
      error: vi.fn(),
    },
  };
});

/**
 * Creates a mock planning response that matches the Planning type structure
 * @param basePlanning - The base planning object to transform
 * @returns A properly formatted Planning object for testing
 */
const createMockPlanningResponse = (basePlanning: Planning): Planning => {
  return {
    ...basePlanning,
    dates: basePlanning.dates,
    transports: basePlanning.transports.map((t) => ({
      truck: t.truck,
      transports: Object.fromEntries(
        Object.entries(t.transports).map(([date, items]) => [
          date,
          items.map((item) => ({
            id: item.id,
            pickupDate: item.pickupDate,
            deliveryDate: item.deliveryDate,
            displayNumber: item.displayNumber || '',
            originCity: item.originCity || '',
            destinationCity: item.destinationCity || '',
            driver: {
              firstName: item.driver?.firstName || '',
              lastName: item.driver?.lastName || '',
            },
            status: item.status,
            truck: item.truck
              ? {
                  licensePlate: item.truck.licensePlate,
                  brand: item.truck.brand || '',
                  model: item.truck.description || '',
                  updatedAt: item.truck.updatedAt,
                  displayName: item.truck.displayName,
                }
              : undefined,
            containerId: item.containerId,
            transportType:
              typeof item.transportType === 'string'
                ? item.transportType
                : String(item.transportType),
            sequenceNumber: item.sequenceNumber,
            consignorName: item.consignorName,
          })),
        ])
      ),
    })),
  };
};

// Create a mock planning data structure
const mockPlanning: Planning = {
  dates: [
    new Date('2025-06-02'),
    new Date('2025-06-03'),
    new Date('2025-06-04'),
    new Date('2025-06-05'),
    new Date('2025-06-06'),
    new Date('2025-06-07'),
    new Date('2025-06-08'),
  ],
  transports: [
    {
      truck: '01-VBT-8',
      transports: {
        '2025-06-06': [
          {
            pickupDate: '2025-06-06',
            deliveryDate: '2025-06-06',
            id: '99b3b4d0-2949-42f9-8f4f-7e75a03ecb79',
            truck: {
              licensePlate: '01-VBT-8',
              brand: 'DAF',
              description: 'XC200',
              updatedAt: '2025-06-06',
              displayName: '01-VBT-8',
            },
            originCity: 'Bunnik',
            destinationCity: 'Bergschenhoek',
            driver: {
              firstName: 'Pieter',
              lastName: 'Posts',
            },
            status: DriverPlanningItemStatusEnum.Finished,
            displayNumber: '25-0011',
            containerId: '40M001',
            transportType: TransportViewTransportTypeEnum.Container,
            sequenceNumber: 0,
            consignorName: 'Test Consignor',
          },
        ],
      },
    },
    {
      truck: '11-DJ-12',
      transports: {
        '2025-06-04': [
          {
            pickupDate: '2025-06-04',
            deliveryDate: '2025-06-04',
            id: 'b1564678-7437-479c-be4b-d4bca422f270',
            truck: {
              licensePlate: '11-DJ-12',
              brand: 'MAN',
              description: 'TGX',
              updatedAt: '2025-06-04',
              displayName: '11-DJ-12',
            },
            driver: {
              firstName: 'Pieter',
              lastName: 'Posts',
            },
            originCity: 'Bunnik',
            destinationCity: 'Bergschenhoek',
            status: DriverPlanningItemStatusEnum.Unplanned,
            displayNumber: '25-0009',
            containerId: '40M001',
            transportType: TransportViewTransportTypeEnum.Container,
            sequenceNumber: 0,
            consignorName: 'Test Consignor',
          },
          {
            pickupDate: '2025-06-04',
            deliveryDate: '2025-06-04',
            id: '89946532-cdb2-4456-b779-8dd1b45b49c4',
            truck: {
              licensePlate: '11-DJ-12',
              brand: 'MAN',
              description: 'TGX',
              updatedAt: '2025-06-04',
              displayName: '11-DJ-12',
            },
            originCity: 'Bergschenhoek',
            destinationCity: 'Bunnik',
            driver: {
              firstName: 'Pieter',
              lastName: 'Posts',
            },
            status: DriverPlanningItemStatusEnum.Finished,
            displayNumber: '25-0010',
            containerId: undefined,
            transportType: TransportViewTransportTypeEnum.Container,
            sequenceNumber: 2,
            consignorName: 'Test Consignor',
          },
        ],
        '2025-06-05': [
          {
            pickupDate: '2025-06-05',
            deliveryDate: '2025-06-05',
            id: '176944ac-ceca-4068-a37d-3ac023c32344',
            truck: {
              licensePlate: '11-DJ-12',
              brand: 'MAN',
              description: 'TGX',
              updatedAt: '2025-06-05',
              displayName: '11-DJ-12',
            },
            originCity: 'Bergschenhoek',
            destinationCity: 'Bunnik',
            driver: {
              firstName: 'Pieter',
              lastName: 'Posts',
            },
            status: DriverPlanningItemStatusEnum.Finished,
            displayNumber: '25-0008',
            containerId: '40M001',
            transportType: TransportViewTransportTypeEnum.Waste,
            sequenceNumber: 0,
            consignorName: 'Test Consignor',
          },
        ],
      },
    },
  ],
};

describe('useDragAndDrop', () => {
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

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient.clear();
  });

  it('should initialize with provided planning', () => {
    const { result } = renderHook(
      () => useDragAndDrop({ initialPlanning: mockPlanning }),
      { wrapper }
    );
    expect(result.current.planning).toEqual(mockPlanning);
  });

  it('should handle drag and drop within the same date/truck', async () => {
    // Mock successful API response with a properly typed Planning object
    vi.mocked(planningService.reorder).mockResolvedValue(
      createMockPlanningResponse(mockPlanning)
    );

    const { result } = renderHook(
      () => useDragAndDrop({ initialPlanning: mockPlanning }),
      { wrapper }
    );

    // Add a second transport to the 11-DJ-12 truck on 2025-06-04 for testing reordering
    const secondTransportId = '89946532-cdb2-4456-b779-8dd1b45b49c4';
    const firstTransportId = 'b1564678-7437-479c-be4b-d4bca422f270';

    // Simulate drag and drop within the same date/truck (reordering)
    await act(async () => {
      await result.current.onDragEnd({
        source: {
          droppableId: '11-DJ-12|2025-06-04',
          index: 0,
        },
        destination: {
          droppableId: '11-DJ-12|2025-06-04',
          index: 1,
        },
        draggableId: firstTransportId,
        type: 'DEFAULT',
        mode: 'FLUID',
      } as DropResult);
    });

    // Check if API was called with correct parameters
    expect(planningService.reorder).toHaveBeenCalledWith(
      '2025-06-04',
      '11-DJ-12',
      [secondTransportId, firstTransportId] // Reordered IDs
    );
  });

  it('should handle drag and drop between different trucks on same date', async () => {
    // Mock successful API response with a properly typed Planning object
    vi.mocked(planningService.reorder).mockResolvedValue(
      createMockPlanningResponse(mockPlanning)
    );

    const { result } = renderHook(
      () => useDragAndDrop({ initialPlanning: mockPlanning }),
      { wrapper }
    );

    // We need to test moving between trucks on the same date
    // For this test, we'll use 2025-06-04 and move a transport from 11-DJ-12 to 01-VBT-8
    // First, let's add a transport to 01-VBT-8 on 2025-06-04 for the test
    const transportId = 'b1564678-7437-479c-be4b-d4bca422f270';

    // Simulate drag and drop between different trucks on the same date
    await act(async () => {
      await result.current.onDragEnd({
        source: {
          droppableId: '11-DJ-12|2025-06-04',
          index: 0,
        },
        destination: {
          droppableId: '01-VBT-8|2025-06-04',
          index: 0,
        },
        draggableId: transportId,
        type: 'DEFAULT',
        mode: 'FLUID',
      } as DropResult);
    });

    // Check if API was called with correct parameters
    expect(planningService.reorder).toHaveBeenCalledWith(
      '2025-06-04',
      '01-VBT-8',
      [transportId] // Transport moved to other truck
    );
  });

  it('should handle drag and drop between different dates', async () => {
    // Mock successful API response with a properly typed Planning object
    vi.mocked(planningService.reorder).mockResolvedValue(
      createMockPlanningResponse(mockPlanning)
    );

    const { result } = renderHook(
      () => useDragAndDrop({ initialPlanning: mockPlanning }),
      { wrapper }
    );

    // We'll move a transport from 11-DJ-12 on 2025-06-04 to 01-VBT-8 on 2025-06-06
    const transportId = 'b1564678-7437-479c-be4b-d4bca422f270';
    const existingTransportId = '99b3b4d0-2949-42f9-8f4f-7e75a03ecb79';

    // Simulate drag and drop between different dates
    await act(async () => {
      await result.current.onDragEnd({
        source: {
          droppableId: '11-DJ-12|2025-06-04',
          index: 0,
        },
        destination: {
          droppableId: '01-VBT-8|2025-06-06',
          index: 1,
        },
        draggableId: transportId,
        type: 'DEFAULT',
        mode: 'FLUID',
      } as DropResult);
    });

    // Check if API was called with correct parameters
    expect(planningService.reorder).toHaveBeenCalledWith(
      '2025-06-06',
      '01-VBT-8',
      [existingTransportId, transportId] // Transport moved to next day
    );
  });

  it('should handle API error', async () => {
    // Mock API error
    const error = new Error('API error');
    vi.mocked(planningService.reorder).mockRejectedValue(error);

    const { result } = renderHook(
      () => useDragAndDrop({ initialPlanning: mockPlanning }),
      { wrapper }
    );

    const transportId = 'b1564678-7437-479c-be4b-d4bca422f270';

    // Simulate drag and drop
    await act(async () => {
      await result.current.onDragEnd({
        source: {
          droppableId: '11-DJ-12|2025-06-04',
          index: 0,
        },
        destination: {
          droppableId: '01-VBT-8|2025-06-06',
          index: 0,
        },
        draggableId: transportId,
        type: 'DEFAULT',
        mode: 'FLUID',
      } as DropResult);
    });

    // Check if error toast was shown
    expect(toastService.error).toHaveBeenCalledWith(
      'Verplaatsen van het transport mislukt'
    );

    // Check if planning was reset to initial state
    expect(result.current.planning).toEqual(mockPlanning);
  });

  it('should do nothing when there is no destination', async () => {
    const { result } = renderHook(
      () => useDragAndDrop({ initialPlanning: mockPlanning }),
      { wrapper }
    );

    // Initial state
    const initialPlanning = result.current.planning;
    const transportId = 'b1564678-7437-479c-be4b-d4bca422f270';

    // Simulate drag with no destination
    await act(async () => {
      await result.current.onDragEnd({
        source: {
          droppableId: '11-DJ-12|2025-06-04',
          index: 0,
        },
        destination: null,
        draggableId: transportId,
        type: 'DEFAULT',
        mode: 'FLUID',
      } as DropResult);
    });

    // Planning should remain unchanged
    expect(result.current.planning).toEqual(initialPlanning);
    expect(planningService.reorder).not.toHaveBeenCalled();
  });
});
