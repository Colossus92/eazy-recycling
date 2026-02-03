import { useCallback, useRef } from 'react';

interface UseRowClickHandlerOptions<T> {
  onSingleClick: (item: T) => void;
  onDoubleClick: (item: T) => void;
  delay?: number;
}

interface UseRowClickHandlerResult<T> {
  handleClick: (item: T) => void;
  handleDoubleClick: (item: T) => void;
}

export function useRowClickHandler<T>({
  onSingleClick,
  onDoubleClick,
  delay = 400,
}: UseRowClickHandlerOptions<T>): UseRowClickHandlerResult<T> {
  const clickTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  const handleClick = useCallback(
    (item: T) => {
      if (clickTimeoutRef.current) {
        clearTimeout(clickTimeoutRef.current);
        clickTimeoutRef.current = null;
      }
      clickTimeoutRef.current = setTimeout(() => {
        onSingleClick(item);
        clickTimeoutRef.current = null;
      }, delay);
    },
    [onSingleClick, delay]
  );

  const handleDoubleClick = useCallback(
    (item: T) => {
      if (clickTimeoutRef.current) {
        clearTimeout(clickTimeoutRef.current);
        clickTimeoutRef.current = null;
      }
      onDoubleClick(item);
    },
    [onDoubleClick]
  );

  return { handleClick, handleDoubleClick };
}
