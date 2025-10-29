import { useNavigate } from 'react-router-dom';
import { SignatureStatusTag } from './SignatureStatusTag';
import Pen from '@/assets/icons/Pen.svg?react';
import { JdenticonAvatar } from '@/components/ui/icon/JdenticonAvatar';
import { useSignatureStatus } from '@/features/mobile/hooks/useSignatureStatus';
import { TransportDetailView } from '@/api/client';

interface SignaturesTabProps {
  transport: TransportDetailView;
}

const SignaturesTab = ({ transport }: SignaturesTabProps) => {
  const navigate = useNavigate();
  const {
    signatureStatus,
    isLoading,
    totalSignatures,
    collectedSignatures,
    progressPercentage,
  } = useSignatureStatus({ transportId: transport.id!! });

  const navigateToSignatureFor = (
    path: 'consignor' | 'pickup' | 'carrier' | 'consignee'
  ) =>
    navigate(`/mobile/signature/${transport.id}/${path}`, {
      state: { transport: transport },
    });

  return (
    <>
      <div className="flex items-center self-stretch gap-2 py-2 pl-2 pr-3 border border-solid border-color-border-primary rounded-lg">
        <div className="flex justify-center items-center gap-3 size-10 border border-solid border-color-border-primary rounded-md bg-color-surface-secondary">
          <Pen />
        </div>
        <div className="flex flex-col items-start justify-center gap-2 flex-1">
          <div className="flex content-center items-center self-stretch gap-2">
            <span className="text-body-2 flex-1">Handtekeningen verzameld</span>
            <span className="text-subtitle-2">
              {collectedSignatures}/{totalSignatures}
            </span>
          </div>
          <div className="flex-1 min-h-1.5 bg-color-border-primary w-full rounded-[10px]">
            <div
              className="min-h-1.5 bg-color-brand-primary rounded-[10px]"
              style={{ width: `${progressPercentage}%` }}
            ></div>
          </div>
        </div>
      </div>
      <div className="flex flex-col items-start self-stretch gap-3">
        <div
          className="flex items-center self-stretch gap-3 py-2"
          onClick={() => {
            navigateToSignatureFor('consignor');
          }}
        >
          <div className="flex items-center gap-2 flex-1">
            {transport.consignorParty.name && (
              <JdenticonAvatar
                size={40}
                value={transport.consignorParty.name}
              />
            )}
            <div className="flex flex-col items-start justify-center flex-1">
              <span className="text-subtitle-2">
                {transport.consignorParty.name}
              </span>
              <span className="text-caption-1 text-color-text-secondary">
                Afzender
              </span>
            </div>
            <SignatureStatusTag
              status={
                isLoading
                  ? 'LOADING'
                  : signatureStatus?.consignorSigned
                    ? 'SIGNED'
                    : 'NOT SIGNED'
              }
            />
          </div>
        </div>
        <div
          className="flex items-center self-stretch gap-3 py-2"
          onClick={() => {
            navigateToSignatureFor('pickup');
          }}
        >
          <div className="flex items-center gap-2 flex-1">
            <JdenticonAvatar
              size={40}
              value={transport.pickupParty?.name || 'Ontdoener'}
            />
            <div className="flex flex-col items-start justify-center flex-1">
              <span className="text-subtitle-2">
                {transport.pickupParty?.name || 'Ontdoener'}
              </span>
              <span className="text-caption-1 text-color-text-secondary">
                Ontdoener
              </span>
            </div>
            <SignatureStatusTag
              status={
                isLoading
                  ? 'LOADING'
                  : signatureStatus?.pickupSigned
                    ? 'SIGNED'
                    : 'NOT SIGNED'
              }
            />
          </div>
        </div>
        <div
          className="flex items-center self-stretch gap-3 py-2"
          onClick={() => {
            navigateToSignatureFor('carrier');
          }}
        >
          <div className="flex items-center gap-2 flex-1">
            {transport.carrierParty.name && (
              <JdenticonAvatar size={40} value={transport.carrierParty.name} />
            )}
            <div className="flex flex-col items-start justify-center flex-1">
              <span className="text-subtitle-2">
                {transport.carrierParty.name}
              </span>
              <span className="text-caption-1 text-color-text-secondary">
                Transporteur
              </span>
            </div>
            <SignatureStatusTag
              status={
                isLoading
                  ? 'LOADING'
                  : signatureStatus?.carrierSigned
                    ? 'SIGNED'
                    : 'NOT SIGNED'
              }
            />
          </div>
        </div>
        <div
          className="flex items-center self-stretch gap-3 py-2"
          onClick={() => {
            navigateToSignatureFor('consignee');
          }}
        >
          <div className="flex items-center gap-2 flex-1">
            <JdenticonAvatar
              size={40}
              value={transport.consigneeParty?.name || 'Ontvanger'}
            />
            <div className="flex flex-col items-start justify-center flex-1">
              <span className="text-subtitle-2">
                {transport.consigneeParty?.name || 'Ontvanger'}
              </span>
              <span className="text-caption-1 text-color-text-secondary">
                Ontvanger
              </span>
            </div>
            <SignatureStatusTag
              status={
                isLoading
                  ? 'LOADING'
                  : signatureStatus?.consigneeSigned
                    ? 'SIGNED'
                    : 'NOT SIGNED'
              }
            />
          </div>
        </div>
      </div>
    </>
  );
};

export default SignaturesTab;
