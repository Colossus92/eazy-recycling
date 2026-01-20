import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { format, parse } from 'date-fns';
import FilePdf from '@/assets/icons/FilePdf.svg?react';
import EyeSolid from '@/assets/icons/EyeSolid.svg?react';
import { fetchWaybillInfo, downloadWaybill } from '@/api/services/waybillService';
import { TransportStatusTag } from '@/features/planning/components/tag/TransportStatusTag';
import { Button } from '@/components/ui/button/Button';

interface TransportCardProps {
    transportId: string;
    displayNumber: string;
    pickupDate?: string;
    deliveryDate?: string;
    status: string;
}

export const TransportCard = ({
    transportId,
    displayNumber,
    pickupDate,
    deliveryDate,
    status,
}: TransportCardProps) => {
    const navigate = useNavigate();
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

    const handleViewDetails = () => {
        // Use pickupDate if available, otherwise use deliveryDate
        const dateToUse = pickupDate || deliveryDate;
        if (!dateToUse) {
            console.error('No date available for transport');
            return;
        }
        // dateToUse is already in yyyy-MM-dd format from the API
        navigate(`/?highlightTransportId=${transportId}&date=${dateToUse}`);
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
                    {pickupDate && `Ophaalmoment: ${format(parse(pickupDate, 'yyyy-MM-dd', new Date()), 'dd-MM-yyyy')}`}
                    {!pickupDate && deliveryDate && `Aflevermoment: ${format(parse(deliveryDate, 'yyyy-MM-dd', new Date()), 'dd-MM-yyyy')}`}
                </span>
            </div>
            <div className="flex items-center gap-2">
                <Button
                    variant="icon"
                    icon={FilePdf}
                    onClick={handleWaybillClick}
                    disabled={isLoadingWaybill || !waybillUrl}
                    title="Download waybill"
                    showText={false}
                />
                <Button
                    variant="icon"
                    icon={EyeSolid}
                    onClick={handleViewDetails}
                    title="Bekijk in planning"
                    showText={false}
                />
            </div>
        </div>
    );
};
