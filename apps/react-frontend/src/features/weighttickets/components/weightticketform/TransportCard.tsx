import { useState, useEffect } from 'react';
import { formatInstantInCET } from '@/utils/dateUtils';
import FilePdf from '@/assets/icons/FilePdf.svg?react';
import EyeSolid from '@/assets/icons/EyeSolid.svg?react';
import { fetchWaybillInfo, downloadWaybill } from '@/api/services/waybillService';
import { TransportStatusTag } from '@/features/planning/components/tag/TransportStatusTag';

interface TransportCardProps {
    transportId: string;
    displayNumber: string;
    pickupDateTime: string;
    status: string;
    onViewDetails: (transportId: string) => void;
}

export const TransportCard = ({
    transportId,
    displayNumber,
    pickupDateTime,
    status,
    onViewDetails,
}: TransportCardProps) => {
    const [waybillUrl, setWaybillUrl] = useState<string | null>(null);
    const [isLoadingWaybill, setIsLoadingWaybill] = useState(false);

    useEffect(() => {
        const loadWaybill = async () => {
            setIsLoadingWaybill(true);
            try {
                const waybillInfo = await fetchWaybillInfo(transportId);
                if (waybillInfo) {
                    setWaybillUrl(waybillInfo.downloadUrl);
                }
            } catch (error) {
                console.error('Error loading waybill:', error);
            } finally {
                setIsLoadingWaybill(false);
            }
        };

        loadWaybill();
    }, [transportId]);

    const handleWaybillClick = async () => {
        if (isLoadingWaybill) return;

        try {
            const waybillInfo = await fetchWaybillInfo(transportId);
            if (waybillInfo) {
                downloadWaybill(waybillInfo, transportId);
            }
        } catch (error) {
            console.error('Error downloading waybill:', error);
        }
    };

    return (
        <div className="flex items-center gap-10 p-3 border border-solid border-color-border-primary rounded-radius-md bg-color-surface-primary w-full">
            <div className="flex flex-col justify-center items-start gap-1 flex-1">
                <TransportStatusTag
                    status={status as 'UNPLANNED' | 'PLANNED' | 'FINISHED' | 'INVOICED' | 'ERROR'}
                />
                <span className="text-subtitle-2 text-color-text-secondary">
                    #{displayNumber}
                </span>
                <span className="text-body-2 text-color-text-secondary">
                    {formatInstantInCET(pickupDateTime, 'dd-MM-yyyy HH:mm')}
                </span>
            </div>
            <div className="flex items-center gap-2">
                    <button
                        onClick={handleWaybillClick}
                        disabled={isLoadingWaybill || !waybillUrl}
                        className="flex size-10 justify-center items-center border border-solid border-color-border-primary rounded-radius-sm bg-color-surface-secondary hover:bg-color-surface-tertiary disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                        title="Download waybill"
                    >
                        <FilePdf className="size-5 text-color-text-secondary" />
                    </button>
                    <button
                        onClick={() => onViewDetails(transportId)}
                        className="flex size-10 justify-center items-center border border-solid border-color-border-primary rounded-radius-sm bg-color-surface-secondary hover:bg-color-surface-tertiary transition-colors"
                        title="Bekijk details"
                    >
                        <EyeSolid className="size-5 text-color-text-secondary" />
                    </button>
            </div>
        </div>
    );
};
