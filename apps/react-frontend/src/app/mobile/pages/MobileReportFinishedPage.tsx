import { useForm } from 'react-hook-form';
import { useLocation, useNavigate } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { resolveLocationAddress, transportService } from '@/api/services/transportService';
import CaretLeft from '@/assets/icons/CaretLeft.svg?react';
import { Button } from '@/components/ui/button/Button';
import { NumberFormField } from '@/components/ui/form/NumberFormField';
import { TextAreaFormField } from '@/components/ui/form/TextAreaFormField';
import { useErrorHandling } from '@/hooks/useErrorHandling';
import { TransportDetailView } from '@/api/client';

type LocationState = {
  transport: TransportDetailView;
};

export const MobileReportFinishedPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const state = location.state as LocationState;
  const { ErrorDialogComponent } = useErrorHandling();
  const navigateBack = () =>
    navigate(`/mobile/transport/${state.transport.id}`, {
      state: { activeTab: 'Algemeen' },
    });

  const queryClient = useQueryClient();

  const reportFinishedMutation = useMutation({
    mutationFn: (data: { hours: number; driverNote: string }) =>
      transportService.reportFinished(state.transport.id!, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['transport', state.transport.id],
      });
      navigateBack();
    },
    onError: (error) => {
      console.error('Error reporting transport finished:', error);
    },
  });

  const onSubmit = (data: { hours: number; driverNote: string }) => {
    reportFinishedMutation.mutate(data);
  };
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<{ hours: number; driverNote: string }>();

  return (
    <div className="flex flex-col w-full">
      <div className="flex items-center py-2 px-4 gap-3 border-b border-solid border-color-border-primary">
        <Button
          variant="icon"
          icon={CaretLeft}
          showText={false}
          onClick={navigateBack}
        />
        <h4>Rit gereed melden</h4>
      </div>
      {state.transport && (
        <form id="report-finished-form" onSubmit={handleSubmit(onSubmit)}>
          <div className="flex flex-col items-start self-stretch gap-5 p-4">
            <div className="flex flex-col items-start self-stretch gap-4">
              <h4>
                {' '}
                {resolveLocationAddress(state.transport.pickupLocation)?.city} &gt;{' '}
                {resolveLocationAddress(state.transport.deliveryLocation)?.city}
              </h4>
            </div>
            <div className="flex flex-col items-start self-stretch gap-1">
              <NumberFormField
                title="Aantal uren"
                placeholder="uren"
                step={0.1}
                formHook={{
                  register,
                  name: 'hours',
                  rules: {
                    required: {
                      value: true,
                      message: 'Aantal uren is verplicht',
                    },
                  },
                  errors,
                }}
              />
              <span className="text-caption-1">
                Het aantal uren dat het uitvoeren van het transport duurde.
              </span>
            </div>
            <div className="flex flex-col items-start self-stretch gap-1">
              <TextAreaFormField
                title="Opmerking chauffeur"
                placeholder="Voer hier eventuele opmerkingen in..."
                rows={4}
                formHook={{
                  register,
                  name: 'driverNote',
                  errors,
                }}
              />
              <span className="text-caption-1">
                Optionele opmerkingen van de chauffeur over het transport.
              </span>
            </div>
          </div>
          <div className="flex justify-end items-center py-3 px-4 gap-4 border-t border-solid border-color-border-primary">
            <div className="flex-1">
              <Button
                variant="secondary"
                size="medium"
                label="Annuleren"
                onClick={navigateBack}
                fullWidth
              />
            </div>
            <div className="flex-1">
              <Button
                variant="primary"
                size="medium"
                label="Bewaren"
                type="submit"
                disabled={isSubmitting || reportFinishedMutation.isPending}
                fullWidth
              />
            </div>
          </div>
        </form>
      )}
      <ErrorDialogComponent />
    </div>
  );
};
