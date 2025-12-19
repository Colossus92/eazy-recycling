import { formatInstantInCET } from '@/utils/dateUtils';
import FilePdf from '@/assets/icons/FilePdf.svg?react';
import EyeSolid from '@/assets/icons/EyeSolid.svg?react';
import { Button } from '@/components/ui/button/Button';
import { WeightTicketStatusTag } from './WeightTicketStatusTag';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { downloadWeightTicketPdfDirect } from '@/api/services/weightTicketPdfService';

interface WeightTicketCardProps {
    weightTicketId: number;
    createdAt: string;
    status: 'DRAFT' | 'COMPLETED' | 'INVOICED' | 'CANCELLED';
    pdfUrl?: string | null;
}

export const WeightTicketCard = ({
    weightTicketId,
    createdAt,
    status,
    pdfUrl,
}: WeightTicketCardProps) => {
    const navigate = useNavigate();
    const [isDownloadingPdf, setIsDownloadingPdf] = useState(false);

    const handlePdfClick = async () => {
        if (!pdfUrl || isDownloadingPdf) return;

        setIsDownloadingPdf(true);
        try {
            const fileName = pdfUrl.split('/').pop() || `weegbon-${weightTicketId}.pdf`;
            await downloadWeightTicketPdfDirect(pdfUrl, fileName);
        } catch (error) {
            console.error('Error downloading weight ticket PDF:', error);
        } finally {
            setIsDownloadingPdf(false);
        }
    };

    const handleViewDetails = () => {
        navigate(`/weight-tickets?weightTicketId=${weightTicketId}`);
    };

    return (
        <div className="flex items-center gap-10 p-3 border border-solid border-color-border-primary rounded-radius-md bg-color-surface-primary w-full">
            <div className="flex flex-col justify-center items-start gap-1 flex-1">
                <WeightTicketStatusTag status={status} />
                <span className="text-subtitle-2 text-color-text-secondary">
                    #{weightTicketId}
                </span>
                <span className="text-body-2 text-color-text-secondary">
                    Aangemaakt: {formatInstantInCET(createdAt, 'dd-MM-yyyy HH:mm')}
                </span>
            </div>
            <div className="flex items-center gap-2">
                {pdfUrl && (
                    <Button
                        variant="icon"
                        icon={FilePdf}
                        onClick={handlePdfClick}
                        disabled={isDownloadingPdf}
                        title="Download weegbon PDF"
                        showText={false}
                    />
                )}
                <Button
                    variant="icon"
                    icon={EyeSolid}
                    onClick={handleViewDetails}
                    title="Bekijk weegbon"
                    showText={false}
                />
            </div>
        </div>
    );
};
