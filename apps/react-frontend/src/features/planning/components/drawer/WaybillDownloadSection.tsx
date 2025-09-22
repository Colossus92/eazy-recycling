import { format } from 'date-fns';
import { useState, useEffect } from 'react';
import { supabase } from '@/api/supabaseClient';
import FilePdf from '@/assets/icons/FilePdf.svg?react';

interface WaybillDownloadSectionProps {
  transportId: string;
}

interface WaybillInfo {
  fileName: string;
  filePath: string;
  downloadUrl: string;
  timestamp: string;
  fileSizeKb: number;
}

export const WaybillDownloadSection = ({
  transportId,
}: WaybillDownloadSectionProps) => {
  const [isLoading, setIsLoading] = useState(true);
  const [isDownloading, setIsDownloading] = useState(false);
  const [waybillInfo, setWaybillInfo] = useState<WaybillInfo | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchWaybillInfo = async () => {
      if (!transportId) return;

      setIsLoading(true);
      setError(null);

      try {
        const { data: files, error: listError } = await supabase.storage
          .from('waybills')
          .list(`waybills/${transportId}`, {
            sortBy: { column: 'created_at', order: 'desc' },
          });

        if (listError) {
          console.error('Error listing waybill files:', listError);
          setError('Fout bij het ophalen van waybill bestanden');
          return;
        }

        if (!files || files.length === 0) {
          setWaybillInfo(null);
          return;
        }

        const latestFile = files[0];
        const filePath = `waybills/${transportId}/${latestFile.name}`;

        const { data: signedUrlData, error: urlError } = await supabase.storage
          .from('waybills')
          .createSignedUrl(filePath, 3600); // 1 hour expiry

        if (urlError) {
          console.error('Error creating signed URL:', urlError);
          setError('Fout bij het genereren van download link');
          return;
        }

        setWaybillInfo({
          timestamp: format(
            new Date(latestFile.updated_at),
            'dd-MM-yyyy hh:mm'
          ),
          fileSizeKb: latestFile.metadata?.size
            ? Math.round(latestFile.metadata.size / 1024)
            : 0,
          fileName: latestFile.name,
          filePath,
          downloadUrl: signedUrlData.signedUrl,
        });
      } catch (error) {
        console.error('Unexpected error fetching waybill info:', error);
        setError('Onverwachte fout bij het ophalen van waybill informatie');
      } finally {
        setIsLoading(false);
      }
    };

    fetchWaybillInfo();
  }, [transportId]);

  const handleDirectDownload = () => {
    if (!waybillInfo || isDownloading) return;

    setIsDownloading(true);

    const link = document.createElement('a');
    link.href = waybillInfo.downloadUrl;
    link.download = `waybill-${transportId}-${waybillInfo.fileName}`;
    link.target = '_blank';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    // Reset downloading state after a short delay
    setTimeout(() => setIsDownloading(false), 1000);
  };

  const renderDownloadContent = () => {
    if (isLoading) {
      return (
        <span className={'text-body-2 text-color-text-secondary'}>
          Laden...
        </span>
      );
    }

    if (error) {
      return <span className={'text-body-2 text-red-600'}>{error}</span>;
    }

    if (!waybillInfo) {
      return (
        <span className={'text-body-2 text-color-text-secondary'}>
          Geen document beschikbaar
        </span>
      );
    }

    return (
      <button
        onClick={handleDirectDownload}
        disabled={isDownloading}
        className="flex items-center self-stretch flex-1 gap-2 py-2 pl-2 pr-3 border border-solid border-color-border-primary rounded-radius-md bg-color-surface-primary hover:bg-color-surface-secondary disabled:cursor-not-allowed transition-colors"
        data-testid="waybill-download-button"
      >
        <div className="flex size-10 justify-center items-center border border-solid border-color-border-primary rounded-radius-sm bg-color-surface-secondary">
          <FilePdf className={'size-[25px] text-color-text-secondary'} />
        </div>
        <div className="flex flex-col items-start gap-1 flex-1">
          <div className="flex items-center justify-between self-stretch">
            <span className={'text-subtitle-2 text-color-text-primary'}>
              Begeleidingsbrief
            </span>
            <span className={'text-body-2 text-color-text-secondary'}>
              {waybillInfo.timestamp}
            </span>
          </div>
          <div className="flex items-center justify-between self-stretch">
            <span className={'text-body-2 text-color-text-secondary'}>
              {isDownloading ? 'Downloaden...' : `${waybillInfo.fileSizeKb} KB`}
            </span>
          </div>
        </div>
      </button>
    );
  };

  return (
    <div className={'flex flex-col items-start self-stretch gap-3'}>
      <span className={'text-subtitle-1'}>Documenten</span>
      <div className={'flex items-center gap-2 self-stretch'}>
        {renderDownloadContent()}
      </div>
    </div>
  );
};
