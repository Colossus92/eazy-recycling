import { useState, useEffect } from 'react';
import { DropResult } from '@hello-pangea/dnd';
import { useQueryClient } from '@tanstack/react-query';
import { Planning } from '@/features/planning/hooks/usePlanning';
import { planningService } from '@/api/planningService';
import { toastService } from '@/components/ui/toast/toastService';

interface UseDragAndDropProps {
  initialPlanning?: Planning;
}

interface UseDragAndDropReturn {
  planning: Planning | undefined;
  onDragEnd: (result: DropResult) => Promise<void>;
}

export const useDragAndDrop = ({
  initialPlanning,
}: UseDragAndDropProps): UseDragAndDropReturn => {
  const [planning, setPlanning] = useState<Planning | undefined>(
    initialPlanning
  );
  const queryClient = useQueryClient();

  // Update local state when props change
  useEffect(() => {
    setPlanning(initialPlanning);
  }, [initialPlanning]);

  const onDragEnd = async (result: DropResult) => {
    const { source, destination } = result;

    // If there's no destination or the item was dropped in the same position, do nothing
    if (
      !destination ||
      (source.droppableId === destination.droppableId &&
        source.index === destination.index)
    ) {
      return;
    }

    if (!planning) return;
    const newPlanning = structuredClone(planning);

    // Parse source and destination IDs
    const [sourceTruck, sourceDateKey] = source.droppableId.split('|');
    const [destTruck, destDateKey] = destination.droppableId.split('|');

    // Find the source truck in the transports array
    const sourceTruckIndex = newPlanning.transports.findIndex(
      (t) => t.truck === sourceTruck
    );
    if (sourceTruckIndex === -1) return;

    const sourceTruckTransports = newPlanning.transports[sourceTruckIndex];
    if (!sourceTruckTransports.transports[sourceDateKey]) return;

    // Get the transport that was moved
    const sourceItems = [...sourceTruckTransports.transports[sourceDateKey]];
    const [movedItem] = sourceItems.splice(source.index, 1);

    // Find the destination truck (for all scenarios)
    const destTruckIndex = newPlanning.transports.findIndex(
      (t) => t.truck === destTruck
    );
    if (destTruckIndex === -1) return;

    const destTruckTransports = newPlanning.transports[destTruckIndex];

    // Initialize the destination date array if it doesn't exist
    if (!destTruckTransports.transports[destDateKey]) {
      destTruckTransports.transports[destDateKey] = [];
    }

    // Handle different scenarios for source and destination
    let destItems;

    if (source.droppableId === destination.droppableId) {
      // If reordering within the same cell, use the sourceItems we already modified
      destItems = sourceItems;
      // Insert the moved item at the new position
      destItems.splice(destination.index, 0, movedItem);
    } else {
      // If moving between different cells, create a new destination array
      destItems = [...destTruckTransports.transports[destDateKey]];
      // Insert the moved item at the new position
      destItems.splice(destination.index, 0, movedItem);
    }

    // If we're moving to a different date, update the transport's dates
    if (sourceDateKey !== destDateKey) {
      // Update the item's date information
      movedItem.pickupDate = destDateKey;
    }

    // Update sequence numbers for source
    const updatedSourceItems = sourceItems.map((item, index) => ({
      ...item,
      sequenceNumber: index + 1,
    }));

    // Update sequence numbers for destination
    const updatedDestItems = destItems.map((item, index) => ({
      ...item,
      sequenceNumber: index + 1,
    }));

    // Update the planning data
    sourceTruckTransports.transports[sourceDateKey] = updatedSourceItems;
    destTruckTransports.transports[destDateKey] = updatedDestItems;

    // Update state with the new planning data (optimistic update)
    setPlanning(newPlanning);

    try {
      // For the destination truck/date
      const updatedPlanning = await planningService.reorder(
        destDateKey,
        destTruck,
        updatedDestItems.map((item) => item.id)
      );

      // Use the returned planning data directly
      if (updatedPlanning) {
        // Transform string dates to Date objects to match the expected format
        const transformedPlanning = {
          ...updatedPlanning,
          dates: updatedPlanning.dates.map((dateStr) =>
            typeof dateStr === 'string' ? new Date(dateStr) : dateStr
          ),
        };

        // Update state with the server-returned data
        setPlanning(transformedPlanning);

        // Update the query cache with the latest data
        queryClient.setQueryData(['planning'], transformedPlanning);
      }
    } catch (error) {
      console.error('Failed to persist reordering:', error);
      setPlanning(initialPlanning);
      toastService.error('Verplaatsen van het transport mislukt');
    }
  };

  return {
    planning,
    onDragEnd,
  };
};
