import { DragDropContext, Droppable, Draggable } from '@hello-pangea/dnd';
import { Planning } from '@/features/planning/hooks/usePlanning';
import { CalendarGridHeader } from '@/features/planning/components/calendar/CalendarGridHeader.tsx';
import { PlanningCard } from '@/features/planning/components/calendar/PlanningCard.tsx';
import { usePopover } from '@/features/planning/hooks/usePopover.ts';
import { useDragAndDrop } from '@/features/planning/hooks/useDragAndDrop';

interface CalendarGridProps {
  planning?: Planning;
  isLoading?: boolean;
  highlightedTransportId?: string | null;
}

export const CalendarGrid = ({
  planning: initialPlanning,
  isLoading,
  highlightedTransportId,
}: CalendarGridProps) => {
  const { handleMouseEnter, handleMouseLeave, activePopoverId } = usePopover();
  const { planning, onDragEnd } = useDragAndDrop({ initialPlanning });

  if (isLoading) {
    return <div className="p-4">Planning data ophalen...</div>;
  }

  if (!planning) {
    return <div className="p-4">Geen planning data beschikbaar.</div>;
  }
  return (
    <DragDropContext onDragEnd={onDragEnd}>
      <div className="w-full flex flex-col h-full">
        <CalendarGridHeader dates={planning.dates} />

        <div className="overflow-y-scroll flex-1 -mt-px">
          <div className="grid grid-cols-8 w-full">
            {planning.transports.map((truckTransports, rowIndex) => {
              const isLastRow = rowIndex === planning.transports.length - 1;
              return [
                // Truck name cell (first column)
                <div
                  key={`truck-${rowIndex}`}
                  className={`p-2 min-h-40 border-t border-r ${isLastRow ? 'border-b' : ''} border-solid border-color-border-primary`}
                >
                  <span className="subtitle-2">{truckTransports.truck}</span>
                </div>,

                planning.dates.map((date, colIndex) => {
                  const dateKey = date.toISOString().split('T')[0]; // Convert Date to YYYY-MM-DD format
                  const transportsForDate =
                    truckTransports.transports[dateKey] || [];
                  const droppableId = `${truckTransports.truck}|${dateKey}`;

                  return (
                    <div
                      key={`truck-${truckTransports.truck}-date-${colIndex}`}
                      className={`p-2 border-t border-r ${isLastRow ? 'border-b' : ''} border-solid border-color-border-primary`}
                    >
                      <Droppable droppableId={droppableId}>
                        {(provided, snapshot) => (
                          <div
                            ref={provided.innerRef}
                            {...provided.droppableProps}
                            className={`flex flex-col gap-2 p-1 rounded transition-colors h-full min-h-[100px] ${snapshot.isDraggingOver ? 'bg-color-brand-light' : ''}`}
                          >
                            {transportsForDate.map((transport, index) => (
                              <Draggable
                                key={transport.id}
                                draggableId={transport.id}
                                index={index}
                              >
                                {(provided, snapshot) => (
                                  <div
                                    ref={provided.innerRef}
                                    {...provided.draggableProps}
                                    {...provided.dragHandleProps}
                                    style={{
                                      ...provided.draggableProps.style,
                                      opacity: snapshot.isDragging ? 0.8 : 1,
                                    }}
                                  >
                                    <PlanningCard
                                      key={transport.id}
                                      transport={transport}
                                      activePopoverId={activePopoverId}
                                      onMouseEnter={() =>
                                        handleMouseEnter(transport.id)
                                      }
                                      onMouseLeave={() =>
                                        handleMouseLeave(transport.id)
                                      }
                                      isHighlighted={highlightedTransportId === transport.id}
                                    />
                                  </div>
                                )}
                              </Draggable>
                            ))}
                            {provided.placeholder}
                          </div>
                        )}
                      </Droppable>
                    </div>
                  );
                }),
              ];
            })}
          </div>
        </div>
      </div>
    </DragDropContext>
  );
};
