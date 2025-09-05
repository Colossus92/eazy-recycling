import { useCallback, useRef, useState } from 'react';

export const usePopover = () => {
  const [activePopoverId, setActivePopoverId] = useState<string | undefined>(
    undefined
  );
  const timeoutRef = useRef<NodeJS.Timeout | null>(null);
  const handleMouseEnter = useCallback((id: string) => {
    // Clear any existing timeout to prevent race conditions
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
      timeoutRef.current = null;
    }
    // Set the active popover immediately
    setActivePopoverId(id);
  }, []);

  const handleMouseLeave = useCallback(
    (id: string) => {
      // Clear any existing timeout
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }
      // Set a new timeout to hide the popover after a delay
      timeoutRef.current = setTimeout(() => {
        if (activePopoverId === id) {
          setActivePopoverId(undefined);
        }
      }, 300); // Small delay to allow moving to the popover
    },
    [activePopoverId]
  );

  return {
    activePopoverId,
    handleMouseEnter,
    handleMouseLeave,
  };
};
