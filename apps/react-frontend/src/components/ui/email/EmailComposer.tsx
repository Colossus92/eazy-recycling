import { useForm } from 'react-hook-form';
import { Button } from '@/components/ui/button/Button';
import { TextFormField } from '@/components/ui/form/TextFormField';
import { TextAreaFormField } from '@/components/ui/form/TextAreaFormField';
import FilePdf from '@/assets/icons/FilePdf.svg?react';
import CaretDown from '@/assets/icons/CaretDown.svg?react';
import CaretRight from '@/assets/icons/CaretRight.svg?react';
import { useState } from 'react';
import { ClipLoader } from 'react-spinners';
import { FormTopBar } from '../form/FormTopBar';
import { useInvoiceEmailFormHook } from '@/features/invoices/components/invoiceform/useInvoiceEmailFormHook';

export interface EmailComposerValues {
  to: string;
  bcc: string;
  subject: string;
  body: string;
}

interface EmailComposerProps {
  emailHook: ReturnType<typeof useInvoiceEmailFormHook>;
  onSend: (values: EmailComposerValues) => void;
  isSending?: boolean;
}

export const EmailComposer = ({
  emailHook,
  onSend,
  isSending = false,
}: EmailComposerProps) => {
  const {
    emailData,
    isPdfReady,
    getEmailDefaultValues,
    handleOpenPdfAttachment,
    cancelEmailStep,
  } = emailHook;
  const [showDetails, setShowDetails] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<EmailComposerValues>({
    defaultValues: getEmailDefaultValues(emailData),
    mode: 'onChange',
  });

  const onSubmit = (values: EmailComposerValues) => {
    onSend(values);
  };

  if (!emailData) {
    return null;
  }

  const title = `Factuur verzenden #${emailData.invoiceNumber}`;
  const attachment = {
    name: emailData.pdfFileName,
    url: emailData.pdfUrl,
    isLoading: !isPdfReady,
  };

  return (
    <div className="flex flex-col h-full w-full">
      <FormTopBar title={title} onClick={cancelEmailStep} />
      <form
        onSubmit={handleSubmit(onSubmit)}
        className="flex flex-col flex-1 min-h-0"
      >
        <div className="flex flex-col flex-1 overflow-y-auto">
          {/* Email Fields - styled like email client */}
          <div className="flex flex-col border-b border-color-border-primary">
            {/* To Field */}
            <div className="flex items-center gap-3 px-4 py-2 border-b border-color-border-secondary">

              <div className="flex-1">
                <TextFormField
                  title="Aan:"
                  placeholder="ontvanger@email.nl"
                  formHook={{
                    register,
                    name: 'to',
                    rules: {
                      required: 'Ontvanger is verplicht',
                      pattern: {
                        value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
                        message: 'Voer een geldig e-mailadres in',
                      },
                    },
                    errors,
                  }}

                />
              </div>
            </div>

            {/* BCC Toggle & Field */}
            <div className="flex flex-col">
              <button
                type="button"
                onClick={() => setShowDetails(!showDetails)}
                className="flex items-center gap-2 px-4 py-2 text-caption-2 text-color-text-secondary hover:bg-color-surface-secondary transition-colors focus:outline-none"
              >
                {showDetails ? (
                  <CaretDown className="w-3 h-3" />
                ) : (
                  <CaretRight className="w-3 h-3" />
                )}
                <span>Details</span>
              </button>

              {showDetails && (
                <div className="flex items-center gap-3 px-4 py-2 border-t border-color-border-secondary bg-color-surface-secondary">
                  <div className="flex-1">
                    <TextFormField
                      title="BCC:"
                      placeholder="bcc@email.nl"
                      formHook={{
                        register,
                        name: 'bcc',
                        rules: {
                          pattern: {
                            value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
                            message: 'Voer een geldig e-mailadres in',
                          },
                          required: 'BCC is verplicht',
                        },
                        errors,
                      }}
                      disabled
    
                    />
                  </div>
                </div>
              )}
            </div>

            {/* Subject Field */}
            <div className="flex items-center gap-3 px-4 py-2 border-t border-color-border-secondary">
              <div className="flex-1">
                <TextFormField
                  title="Onderwerp:"
                  placeholder="Onderwerp van de e-mail"
                  formHook={{
                    register,
                    name: 'subject',
                    rules: { required: 'Onderwerp is verplicht' },
                    errors,
                  }}

                />
              </div>
            </div>
          </div>

          {/* Attachment Section */}
          {attachment && (
            <div className="flex items-center gap-3 px-4 py-3 border-b border-color-border-primary bg-color-surface-secondary">
              <span className="text-caption-2">
                Bijlage(n):
              </span>
              <button
                type="button"
                onClick={() => handleOpenPdfAttachment(emailData.pdfUrl)}
                disabled={attachment.isLoading}
                className="flex items-center gap-2 px-3 py-2 bg-color-surface-secondary border border-color-border rounded-radius-sm hover:bg-color-surface-tertiary transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {attachment.isLoading ? (
                  <ClipLoader size={14} color="currentColor" />
                ) : (
                  <FilePdf className="w-4 h-4" />
                )}
                <span className="text-caption-1 text-color-text-primary">
                  {attachment.name}
                </span>
              </button>
            </div>
          )}

          {/* Email Body */}
          <div className="flex-1 p-4">
            <TextAreaFormField
              title="Bericht:"
              placeholder="Typ hier uw bericht..."
              rows={12}
              formHook={{
                register,
                name: 'body',
                rules: { required: 'Bericht is verplicht' },
                errors,
              }}
            />
          </div>
        </div>

        {/* Footer with actions */}
        <div className="flex py-3 px-4 justify-between items-center border-t border-color-border-primary">
          <div className="flex gap-2 justify-end">
            <Button
              variant="secondary"
              label="Annuleren"
              onClick={cancelEmailStep}
              type="button"
            />
            <Button
              variant="primary"
              label={isSending ? 'Verzenden...' : 'Verzenden'}
              type="submit"
              disabled={!isPdfReady || isSending}
              title={!isPdfReady ? 'PDF wordt gegenereerd...' : undefined}
            />
          </div>
        </div>
      </form>
    </div>
  );
};
