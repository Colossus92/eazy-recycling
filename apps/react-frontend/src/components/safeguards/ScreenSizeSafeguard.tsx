import { useEffect, useState } from 'react';

const MIN_WIDTH = 900;

export const ScreenSizeSafeguard = ({ children }: { children: React.ReactNode }) => {
  const [isScreenTooSmall, setIsScreenTooSmall] = useState(false);

  useEffect(() => {
    const handleResize = () => {
      setIsScreenTooSmall(window.innerWidth < MIN_WIDTH);
    };

    // Check on mount
    handleResize();

    // Add resize listener
    window.addEventListener('resize', handleResize);

    return () => {
      window.removeEventListener('resize', handleResize);
    };
  }, []);

  return (
    <>
      {children}
      {isScreenTooSmall && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-lg p-8 max-w-md w-full mx-4">
            <div className="flex items-start justify-between mb-4">
              <h2 className="text-xl font-semibold text-color-text-primary">
                Schermgrootte waarschuwing
              </h2>
            </div>
            <p className="text-color-text-secondary mb-6">
              Deze applicatie is geoptimaliseerd voor schermen met een minimale breedte van {MIN_WIDTH}px. 
              Uw huidige schermgrootte is te klein. Vergroot alstublieft uw browservenster.
            </p>
          </div>
        </div>
      )}
    </>
  );
};
