import React, { useState, useRef, useEffect } from 'react';

export interface TooltipProps {
  children: React.ReactNode;
  content: string | null | undefined;
  position?: 'top' | 'bottom' | 'left' | 'right';
  disabled?: boolean;
}

export const Tooltip: React.FC<TooltipProps> = ({
  children,
  content,
  position = 'top',
  disabled = false
}) => {
  const [isVisible, setIsVisible] = useState(false);
  const [tooltipPosition, setTooltipPosition] = useState({ top: 0, left: 0 });
  const triggerRef = useRef<HTMLDivElement>(null);
  const tooltipRef = useRef<HTMLDivElement>(null);

  // Don't render tooltip if content is empty/null/undefined or disabled
  if (!content || disabled) {
    return <>{children}</>;
  }

  const updatePosition = () => {
    if (!triggerRef.current || !tooltipRef.current) return;

    const triggerRect = triggerRef.current.getBoundingClientRect();
    const tooltipRect = tooltipRef.current.getBoundingClientRect();
    const viewportWidth = window.innerWidth;
    const viewportHeight = window.innerHeight;

    let top = 0;
    let left = 0;

    switch (position) {
      case 'top':
        top = triggerRect.top - tooltipRect.height - 8;
        left = triggerRect.left + (triggerRect.width - tooltipRect.width) / 2;
        break;
      case 'bottom':
        top = triggerRect.bottom + 8;
        left = triggerRect.left + (triggerRect.width - tooltipRect.width) / 2;
        break;
      case 'left':
        top = triggerRect.top + (triggerRect.height - tooltipRect.height) / 2;
        left = triggerRect.left - tooltipRect.width - 8;
        break;
      case 'right':
        top = triggerRect.top + (triggerRect.height - tooltipRect.height) / 2;
        left = triggerRect.right + 8;
        break;
    }

    // Keep tooltip within viewport bounds
    if (left < 8) left = 8;
    if (left + tooltipRect.width > viewportWidth - 8) {
      left = viewportWidth - tooltipRect.width - 8;
    }
    if (top < 8) top = 8;
    if (top + tooltipRect.height > viewportHeight - 8) {
      top = viewportHeight - tooltipRect.height - 8;
    }

    setTooltipPosition({ top, left });
  };

  const handleMouseEnter = () => {
    setIsVisible(true);
  };

  const handleMouseLeave = () => {
    setIsVisible(false);
  };

  useEffect(() => {
    if (isVisible) {
      updatePosition();
    }
  }, [isVisible, position]);

  useEffect(() => {
    if (isVisible) {
      const handleScroll = () => updatePosition();
      const handleResize = () => updatePosition();
      
      window.addEventListener('scroll', handleScroll, true);
      window.addEventListener('resize', handleResize);
      
      return () => {
        window.removeEventListener('scroll', handleScroll, true);
        window.removeEventListener('resize', handleResize);
      };
    }
  }, [isVisible]);

  return (
    <>
      <div
        ref={triggerRef}
        onMouseEnter={handleMouseEnter}
        onMouseLeave={handleMouseLeave}
        className="inline-block"
      >
        {children}
      </div>
      
      {isVisible && (
        <div
          ref={tooltipRef}
          className="fixed z-50 pointer-events-none"
          style={{
            top: tooltipPosition.top,
            left: tooltipPosition.left,
          }}
        >
          <div className="bg-color-surface-primary border border-color-border-primary rounded-radius-sm px-2 py-1 shadow-lg max-w-xs">
            <p className="text-caption-subtitle text-color-text-primary whitespace-nowrap">
              {content}
            </p>
          </div>
        </div>
      )}
    </>
  );
};
