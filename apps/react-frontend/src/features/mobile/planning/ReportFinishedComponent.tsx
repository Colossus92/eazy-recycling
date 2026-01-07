import { useNavigate } from 'react-router-dom';
import { TransportDetailView } from '@/api/client';
import { Button } from '@/components/ui/button/Button';
import BxTimeFive from '@/assets/icons/BxTimeFive.svg?react';
import { DriverNote } from '@/features/planning/components/note/DriverNote';

interface ReportFinishedComponentProps {
  transport: TransportDetailView;
}

export const ReportFinishedComponent = ({
  transport,
}: ReportFinishedComponentProps) => {
  const navigate = useNavigate();

  return transport.transportHours === null ? (
    <Button
      variant="primary"
      label="Gereed melden"
      icon={BxTimeFive}
      iconPosition="left"
      fullWidth
      onClick={() =>
        navigate(`/mobile/report-ready/${transport.id}`, {
          state: { transport },
        })
      }
    />
  ) : (
    <div className="flex flex-col items-start self-stretch gap-2">
      <div className="flex items-center self-stretch gap-2">
        <div className="flex items-center flex-1 gap-2">
          <BxTimeFive className="size-5 text-color-text-secondary" />
          <span className="text-body-2 text-color-text-secondary">Uren</span>
        </div>
        <span className="text-subtitle-2">{transport.transportHours} uur</span>
      </div>
      {transport.driverNote && <DriverNote note={transport.driverNote} />}
    </div>
  );
};
