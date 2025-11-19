import { Dialog, DialogPanel } from '@headlessui/react';
import { useEffect, useState } from 'react';
import { WeightTicketForm } from './weightticketform/WeightTicketForm';

interface WeightTicketDualFormProps {
  isOpen: boolean;
  setIsOpen: (value: boolean) => void;
  originalWeightTicketId: number;
  newWeightTicketId: number;
  onDelete: (id: number) => void;
  onSplit?: (id: number) => void;
  onClose: () => void;
  onComplete?: (id: number) => void;
}

export const WeightTicketDualForm = ({
    isOpen,
    setIsOpen,
    originalWeightTicketId,
    newWeightTicketId,
    onDelete,
    onSplit,
    onClose,
    onComplete,
}: WeightTicketDualFormProps) => {
    const [showSecond, setShowSecond] = useState(false);
    const [originalFormOpen, setOriginalFormOpen] = useState(false);
    const [newFormOpen, setNewFormOpen] = useState(false);
    const [hasBeenOpened, setHasBeenOpened] = useState(false);

    useEffect(() => {
        if (isOpen) {
            setHasBeenOpened(true);
            // Open original form immediately
            setOriginalFormOpen(true);
            // Delay showing second form for animation effect
            const timer = setTimeout(() => {
                setShowSecond(true);
                setNewFormOpen(true);
            }, 400);
            return () => clearTimeout(timer);
        } else {
            setOriginalFormOpen(false);
            setNewFormOpen(false);
            setShowSecond(false);
        }
    }, [isOpen, originalWeightTicketId, newWeightTicketId]);

    // Auto-close dialog when both forms are closed (after they've been opened)
    useEffect(() => {
        if (hasBeenOpened && !originalFormOpen && !newFormOpen && isOpen) {
            setIsOpen(false);
            setHasBeenOpened(false);
            onClose();
        }
    }, [originalFormOpen, newFormOpen, isOpen, hasBeenOpened, setIsOpen, onClose]);

    return (
        <Dialog
            open={isOpen}
            onClose={() => {}} // Prevent closing by clicking outside
            className="relative z-50"
        >
            <div className="fixed inset-0 bg-black bg-opacity-30 backdrop-blur-xs flex w-screen items-center justify-center p-4">
                <DialogPanel className="grid grid-cols-2 gap-4 w-full max-w-[95vw] h-[90vh]">
                    {/* Original Weight Ticket Form - Always left side */}
                    <div className="col-start-1">
                        {originalFormOpen && (
                            <div
                                className="w-full transition-all duration-300 ease-out bg-color-surface-primary rounded-radius-lg overflow-hidden shadow-xl flex flex-col h-full"
                            >
                                <WeightTicketForm
                                    isOpen={true}
                                    setIsOpen={setOriginalFormOpen}
                                    weightTicketNumber={originalWeightTicketId}
                                    onDelete={onDelete}
                                    onSplit={onSplit}
                                    noDialog={true}
                                    onComplete={onComplete}
                                />
                            </div>
                        )}
                    </div>

                    {/* New Weight Ticket Form - Always right side */}
                    <div className="col-start-2">
                        {newFormOpen && (
                            <div
                                className="w-full transition-all duration-1000 ease-out bg-color-surface-primary rounded-radius-lg overflow-hidden shadow-xl flex flex-col h-full"
                                style={{
                                    transform: showSecond ? 'scaleX(1)' : 'scaleX(0)',
                                    transformOrigin: 'center',
                                }}
                            >
                                <WeightTicketForm
                                    isOpen={true}
                                    setIsOpen={setNewFormOpen}
                                    weightTicketNumber={newWeightTicketId}
                                    onDelete={onDelete}
                                    onSplit={onSplit}
                                    noDialog={true}
                                    onComplete={onComplete}
                                />
                            </div>
                        )}
                    </div>
                </DialogPanel>
            </div>
        </Dialog>
    );
};
