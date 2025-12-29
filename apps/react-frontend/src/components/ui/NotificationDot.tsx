interface NotificationDotProps {
  show: boolean;
  className?: string;
}

export const NotificationDot = ({ show, className = "" }: NotificationDotProps) => {
  if (!show) return null;
  
  return (
    <div 
      className={`absolute w-2 h-2 bg-color-status-error-dark rounded-full ${className}`}
      data-testid="notification-dot"
    />
  );
};
