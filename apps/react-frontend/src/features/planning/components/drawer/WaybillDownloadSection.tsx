import { useState, useEffect } from 'react';
import FilePdf from '@/assets/icons/FilePdf.svg?react';
import {
  fetchWaybillInfo,
  downloadWaybill,
  WaybillInfo,
} from '@/api/services/waybillService';

interface WaybillDownloadSectionProps {
  transportId?: string;
}

export const WaybillDownloadSection = ({
  transportId,
}: WaybillDownloadSectionProps) => {
  const [isLoading, setIsLoading] = useState(true);
  const [isDownloading, setIsDownloading] = useState(false);
  const [waybillInfo, setWaybillInfo] = useState<WaybillInfo | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const loadWaybillInfo = async () => {
      if (!transportId) return;

      setIsLoading(true);
      setError(null);

      try {
        const info = await fetchWaybillInfo(transportId);
        setWaybillInfo(info);
      } catch (error: any) {
        setError(error.message || 'Onverwachte fout bij het ophalen van waybill informatie');
      } finally {
        setIsLoading(false);
      }
    };

    loadWaybillInfo();
  }, [transportId]);

  const handleDirectDownload = () => {
    if (!waybillInfo || isDownloading || !transportId) return;

    setIsDownloading(true);
    downloadWaybill(waybillInfo, transportId);

    // Reset downloading state after a short delay
    setTimeout(() => setIsDownloading(false), 1000);
  };

  return (
        isLoading ?
      <span className={'text-body-2 text-color-text-secondary'}>
        Laden...
      </span>
      : error ?
        <span className={'text-body-2 text-red-600'}>{error}</span>
        : !waybillInfo ? (
          <span className={'text-body-2 text-color-text-secondary'}>
            Geen begeleidingsbrief beschikbaar
          </span>)
          : (
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
          )
  );
};
