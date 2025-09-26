import { FieldValues, useForm } from 'react-hook-form';
import { FormEvent } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { TextFormField } from '@/components/ui/form/TextFormField.tsx';
import { FormTopBar } from '@/components/ui/form/FormTopBar.tsx';
import { FormActionButtons } from '@/components/ui/form/FormActionButtons.tsx';
import { useErrorHandling } from '@/hooks/useErrorHandling.tsx';
import { PostalCodeFormField } from '@/components/ui/form/PostalCodeFormField';
import { fallbackRender } from '@/utils/fallbackRender';
import { CompanyBranch } from '@/api/services/companyService';

interface CompanyBranchFormProps {
  onCancel: () => void;
  onSubmit: (data: CompanyBranch) => void;
  companyName: string;
  companyBranch?: CompanyBranch;
}

export interface CompanyBranchFormValues extends FieldValues {
  id?: string;
  companyId: string;
  street: string;
  houseNumber: string;
  postalCode: string;
  city: string;
}

function toCompanyBranch(data: CompanyBranchFormValues): CompanyBranch {
  const companyBranch: CompanyBranch = {
    id: data.id || '',
    address: {
      streetName: data.street,
      buildingNumber: data.houseNumber,
      postalCode: data.postalCode,
      city: data.city,
    },
    companyId: data.companyId,
  };

  return companyBranch;
}

export const CompanyBranchForm = ({
  onCancel,
  onSubmit,
  companyName,
  companyBranch,
}: CompanyBranchFormProps) => {
  const { handleError, ErrorDialogComponent } = useErrorHandling();

  const {
    register,
    setValue,
    handleSubmit,
    formState: { errors },
  } = useForm<CompanyBranchFormValues>({
    defaultValues: companyBranch
      ? {
          id: companyBranch.id,
          companyId: companyBranch.companyId,
          street: companyBranch.address.streetName,
          houseNumber: companyBranch.address.buildingNumber,
          postalCode: companyBranch.address.postalCode,
          city: companyBranch.address.city,
        }
      : undefined,
  });

  const submitAndClose = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    await handleSubmit(async (data) => {
      try {
        await onSubmit(toCompanyBranch(data));
        onCancel();
      } catch (error) {
        handleError(error);
      }
    })();
  };

  return (
    <ErrorBoundary fallbackRender={fallbackRender}>
      <form
        className="flex flex-col items-center self-stretch"
        onSubmit={(e) => submitAndClose(e)}
      >
        <FormTopBar
          title={
            companyBranch
              ? `Vestiging voor ${companyName} aanpassen`
              : `Een vestiging voor ${companyName} toevoegen`
          }
          onClick={onCancel}
        />
        <div className="flex flex-col items-center self-stretch p-4 gap-4">
          <div className={'flex items-start gap-4 self-stretch'}>
            <div className={'w-[70%]'}>
              <TextFormField
                title={'Straat'}
                placeholder={'Vul straat in'}
                formHook={{
                  register,
                  name: 'street',
                  rules: {
                    required: 'Straat is verplicht',
                    validate: (value: string) => {
                      const trimmed = value?.trim() || '';
                      return trimmed !== '' || 'Straat mag niet leeg zijn';
                    },
                  },
                  errors,
                }}
                value={companyBranch?.address.streetName}
              />
            </div>
            <div className={'flex-1'}>
              <TextFormField
                title={'Huisnummer'}
                placeholder={'Nr.'}
                formHook={{
                  register,
                  name: 'houseNumber',
                  rules: {
                    required: 'Huisnummer is verplicht',
                    validate: (value: string) => {
                      const trimmed = value?.trim() || '';
                      return trimmed !== '' || 'Huisnummer mag niet leeg zijn';
                    },
                  },
                  errors,
                }}
                value={companyBranch?.address.buildingNumber}
              />
            </div>
          </div>
          <div className={'flex items-start gap-4 self-stretch'}>
            <PostalCodeFormField
              register={register}
              setValue={setValue}
              name="postalCode"
              errors={errors}
              value={companyBranch?.address.postalCode}
            />
            <TextFormField
              title={'Plaats'}
              placeholder={'Vul Plaats in'}
              formHook={{
                register,
                name: 'city',
                rules: {
                  required: 'Plaats is verplicht',
                  validate: (value: string) => {
                    const trimmed = value?.trim() || '';
                    return trimmed !== '' || 'Plaats mag niet leeg zijn';
                  },
                },
                errors,
              }}
              value={companyBranch?.address.city}
            />
          </div>
        </div>
        <FormActionButtons onClick={onCancel} item={companyBranch} />
      </form>

      <ErrorDialogComponent />
    </ErrorBoundary>
  );
};
