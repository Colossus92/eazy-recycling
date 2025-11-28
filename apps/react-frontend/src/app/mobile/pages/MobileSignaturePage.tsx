import { useRef } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import SignatureCanvas from 'react-signature-canvas';
import CaretLeft from '@/assets/icons/CaretLeft.svg?react';
import { Button } from '@/components/ui/button/Button';
import { SignatureFormField } from '@/components/ui/form/SignatureFormField';
import { TextFormField } from '@/components/ui/form/TextFormField';
import { JdenticonAvatar } from '@/components/ui/icon/JdenticonAvatar';
import { SignatureStatusTag } from '@/features/mobile/planning/SignatureStatusTag';
import { useSignatureForm } from '@/features/mobile/hooks/useSignatureForm';
import { useSignatureStatus } from '@/features/mobile/hooks/useSignatureStatus';
import { useErrorHandling } from '@/hooks/useErrorHandling';
import { TransportDetailView } from '@/api/client';

type LocationState = {
  transport: TransportDetailView;
};

export const MobileSignaturePage = () => {
  const { id, type } = useParams<{
    id: string;
    type: 'consignor' | 'pickup' | 'carrier' | 'consignee';
  }>();
  const navigate = useNavigate();
  const signatureCanvasRef = useRef<SignatureCanvas>(null);
  const location = useLocation();
  const state = location.state as LocationState;
  const { ErrorDialogComponent } = useErrorHandling();
  const navigateBack = () =>
    navigate(`/mobile/transport/${id}`, {
      state: { activeTab: 'Handtekeningen' },
    });
  const party =
    type === 'consignor'
      ? state.transport.consignorParty
      : type === 'pickup'
        ? state.transport.pickupParty
        : type === 'carrier'
          ? state.transport.carrierParty
          : type === 'consignee'
            ? state.transport.consigneeParty
            : undefined;
  const { signatureStatus, isLoading: isLoadingStatus } = useSignatureStatus({
    transportId: id || '',
  });
  const safeParty = party && party.name ? { name: party.name } : undefined;

  const getSignatureStatus = () => {
    if (isLoadingStatus) return 'LOADING';
    if (!signatureStatus) return 'NOT SIGNED';

    const statusMap = {
      consignor: signatureStatus.consignorSigned,
      pickup: signatureStatus.pickupSigned,
      carrier: signatureStatus.carrierSigned,
      consignee: signatureStatus.consigneeSigned,
    };

    return statusMap[type as keyof typeof statusMap] ? 'SIGNED' : 'NOT SIGNED';
  };

  const {
    register,
    errors,
    isSubmitting,
    isLoadingSignature,
    signatureError,
    submitForm,
  } = useSignatureForm(signatureCanvasRef, {
    id,
    type,
    party: safeParty,
    onSuccess: navigateBack,
  });

  return (
    <div className="flex flex-col w-full">
      <div className="flex items-center py-2 px-4 gap-3 border-b border-solid border-color-border-primary">
        <Button
          variant="icon"
          icon={CaretLeft}
          showText={false}
          onClick={navigateBack}
        />
        <h4>Handtekening {party?.name || 'Onbekend'}</h4>
      </div>
      {state.transport && party && (
        <form id="signatureForm" onSubmit={submitForm}>
          <div className="flex flex-col items-start self-stretch gap-5 p-4">
            <div className="flex flex-col items-start self-stretch gap-4">
              <h4>Begeleidingsbrief</h4>
              <div className="flex items-center self-stretch gap-3 py-2">
                <div className="flex items-center gap-2 flex-1 py-2 pl-2 pr-3 border border-solid border-color-border-primary rounded-radius-md">
                  {party.name && (
                    <JdenticonAvatar size={40} value={party.name} />
                  )}
                  <div className="flex flex-col items-start justify-center flex-1">
                    <span className="text-subtitle-2">{party.name}</span>
                    <span className="text-caption-1 text-color-text-secondary">
                      {type === 'consignor'
                        ? 'Afzender'
                        : type === 'pickup'
                          ? 'Ontdoener'
                          : type === 'carrier'
                            ? 'Transporteur'
                            : 'Ontvanger'}
                    </span>
                  </div>
                  <SignatureStatusTag status={getSignatureStatus()} />
                </div>
              </div>
            </div>
            <SignatureFormField
              title="Handtekening"
              canvasRef={signatureCanvasRef}
              errorMessage={signatureError}
              isLoading={isLoadingSignature}
            />
            <div className="flex flex-col items-start self-stretch gap-1">
              <TextFormField
                title="Emailadres"
                placeholder="Emailadres"
                value={party.email}
                formHook={{
                  register,
                  name: 'email',
                  rules: {
                    pattern: {
                      value: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i,
                      message: 'Ongeldig emailadres',
                    },
                  },
                  errors,
                }}
              />
              <span className="text-caption-1">
                Het emailadres van de persoon die een kopie van het document
                ontvangt.
              </span>
            </div>
          </div>
          <div className="flex justify-end items-center py-3 px-4 gap-4 border-t border-solid border-color-border-primary">
            <div className="flex-1">
              <Button
                variant="secondary"
                size="medium"
                label="Annuleren"
                type="button"
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
                disabled={isSubmitting}
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
