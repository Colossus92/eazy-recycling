import { FieldValues, useForm } from 'react-hook-form';
import { FormEvent, useState } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { TextFormField } from '@/components/ui/form/TextFormField.tsx';
import { Company } from '@/api/services/companyService';
import { FormTopBar } from '@/components/ui/form/FormTopBar.tsx';
import { FormActionButtons } from '@/components/ui/form/FormActionButtons.tsx';
import { useErrorHandling } from '@/hooks/useErrorHandling.tsx';
import { JdenticonAvatar } from '@/components/ui/icon/JdenticonAvatar.tsx';
import { fallbackRender } from '@/utils/fallbackRender';
import { PostalCodeFormField } from '@/components/ui/form/PostalCodeFormField';
import { NumberFormField } from '@/components/ui/form/NumberFormField';
import { RestoreCompanyDialog } from './RestoreCompanyDialog';
import { AxiosError } from 'axios';
import { SelectFormField } from '@/components/ui/form/selectfield/SelectFormField';
import { CompleteCompanyViewRolesEnum } from '@/api/client/models/complete-company-view';

interface CompanyFormProps {
  onCancel: () => void;
  onSubmit: (data: Company, restoreCompanyId?: string) => void;
  company?: Company;
}

interface SoftDeleteConflict {
  message: string;
  deletedCompanyId: string;
  conflictField: string;
  conflictValue: string;
}

export interface CompanyFormValues extends FieldValues {
  id?: string;
  name: string;
  street: string;
  houseNumber: string;
  houseNumberAddition: string;
  postalCode: string;
  city: string;
  chamberOfCommerceId: string;
  vihbId: string;
  processorId?: string;
  roles: CompleteCompanyViewRolesEnum[];
}

function toCompany(data: CompanyFormValues): Company {
  const company: Company = {
    id: data.id || '',
    address: {
      street: data.street,
      houseNumber: String(data.houseNumber),
      houseNumberAddition: data.houseNumberAddition,
      postalCode: data.postalCode,
      city: data.city,
      country: 'Nederland',
    },
    chamberOfCommerceId: data.chamberOfCommerceId?.trim() || undefined,
    name: data.name,
    vihbId: data.vihbId?.trim() || undefined,
    processorId: data.processorId?.trim() || undefined,
    updatedAt: new Date().toISOString(),
    roles: data.roles,
    branches: [],
  };

  return company;
}

export const CompanyForm = ({
  onCancel,
  onSubmit,
  company,
}: CompanyFormProps) => {
  const { handleError, ErrorDialogComponent } = useErrorHandling();
  const [softDeleteConflict, setSoftDeleteConflict] =
    useState<SoftDeleteConflict | null>(null);
  const [pendingFormData, setPendingFormData] =
    useState<CompanyFormValues | null>(null);

  const {
    register,
    setValue,
    handleSubmit,
    watch,
    control,
    formState: { errors },
  } = useForm<CompanyFormValues>({
    defaultValues: company
      ? {
          id: company.id,
          name: company.name,
          street: company.address.street,
          houseNumber: company.address.houseNumber,
          houseNumberAddition: company.address.houseNumberAddition,
          postalCode: company.address.postalCode,
          city: company.address.city,
          chamberOfCommerceId: company.chamberOfCommerceId || '',
          vihbId: company.vihbId || '',
          processorId: company.processorId || '',
          roles: company.roles || [],
        }
      : undefined,
  });

  const submitAndClose = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    await handleSubmit(async (data) => {
      try {
        await onSubmit(toCompany(data));
        onCancel();
      } catch (error) {
        // Check if this is a soft-delete conflict
        if (error instanceof AxiosError && error.response?.status === 409) {
          const conflictData = error.response.data as SoftDeleteConflict;
          if (conflictData.deletedCompanyId) {
            // This is a soft-delete conflict
            setSoftDeleteConflict(conflictData);
            setPendingFormData(data);
            return; // Don't show error dialog, show restore dialog instead
          }
        }
        handleError(error);
      }
    })();
  };

  const handleRestoreConfirm = async () => {
    if (!softDeleteConflict || !pendingFormData) return;

    try {
      await onSubmit(
        toCompany(pendingFormData),
        softDeleteConflict.deletedCompanyId
      );
      setSoftDeleteConflict(null);
      setPendingFormData(null);
      onCancel();
    } catch (error) {
      setSoftDeleteConflict(null);
      setPendingFormData(null);
      handleError(error);
    }
  };

  const handleRestoreCancel = () => {
    setSoftDeleteConflict(null);
    setPendingFormData(null);
  };

  return (
    <ErrorBoundary fallbackRender={fallbackRender}>
      <form
        className="flex flex-col items-center self-stretch"
        onSubmit={(e) => submitAndClose(e)}
      >
        <FormTopBar
          title={company ? 'Bedrijf aanpassen' : 'Een bedrijf toevoegen'}
          onClick={onCancel}
        />
        <div className="flex flex-col items-center self-stretch p-4 gap-4">
          <JdenticonAvatar value={watch('name')} />
          <TextFormField
            title={'Bedrijfsnaam'}
            placeholder={'Vul bedrijfsnaam in'}
            formHook={{
              register,
              name: 'name',
              rules: {
                required: 'Bedrijfsnaam is verplicht',
                validate: (value: string) => {
                  const trimmed = value?.trim() || '';
                  return trimmed !== '' || 'Bedrijfsnaam mag niet leeg zijn';
                },
              },
              errors,
            }}
            value={company?.name}
          />
          <div className="p-4 bg-color-surface-secondary rounded-md flex flex-col gap-4">
            <span className="text-subtitle-1">Adres</span>
            <div className={'flex items-start gap-4 self-stretch'}>
              <div className={'flex items-start flex-grow w-1/2'}>
                <PostalCodeFormField
                  register={register}
                  setValue={setValue}
                  name="postalCode"
                  errors={errors}
                  value={company?.address.postalCode}
                />
              </div>
              <div className={'flex items-start gap-4 w-1/2'}>
                <NumberFormField
                  title={'Nummer'}
                  placeholder={''}
                  step={1}
                  formHook={{
                    register,
                    name: 'houseNumber',
                    rules: {
                      required: 'Huisnummer is verplicht',
                      min: {
                        value: 1,
                        message: 'Ongeldig',
                      },
                      maxLength: {
                        value: 10,
                        message: 'Huisnummer mag maximaal 10 tekens bevatten',
                      },
                    },
                    errors,
                  }}
                  value={company?.address.houseNumber}
                />
                <TextFormField
                  title={'Toevoeging'}
                  placeholder={''}
                  formHook={{
                    register,
                    name: 'houseNumberAddition',
                    rules: {
                      maxLength: {
                        value: 6,
                        message: 'Toevoeging mag maximaal 6 tekens bevatten',
                      },
                    },
                    errors,
                  }}
                  value={company?.address.houseNumberAddition}
                />
              </div>
            </div>
            <div className={'flex items-start gap-4 self-stretch'}>
              <TextFormField
                title={'Straat'}
                placeholder={'Vul straatnaam in'}
                formHook={{
                  register,
                  name: 'street',
                  rules: {
                    required: 'Straat is verplicht',
                    maxLength: {
                      value: 43,
                      message: 'Straatnaam mag maximaal 43 tekens bevatten',
                    },
                  },
                  errors,
                }}
                value={company?.address.street}
              />
            </div>
            <div className={'flex items-start gap-4 self-stretch'}>
              <TextFormField
                title={'Plaats'}
                placeholder={'Vul Plaats in'}
                formHook={{
                  register,
                  name: 'city',
                  rules: {
                    required: 'Plaats is verplicht',
                    maxLength: {
                      value: 24,
                      message: 'Plaats mag maximaal 24 tekens bevatten',
                    },
                  },
                  errors,
                }}
                value={company?.address.city}
              />
            </div>
          </div>
          <div className={'flex items-start gap-4 self-stretch'}>
            <TextFormField
              title={'KvK nummer'}
              placeholder={'Vul Kvk nummer in'}
              formHook={{
                register,
                name: 'chamberOfCommerceId',
                rules: {
                  validate: (value: string) => {
                    const trimmed = value?.trim() || '';
                    if (trimmed === '') return true;
                    return (
                      /^\d{8}$/.test(trimmed) ||
                      'KvK nummer moet 8 cijfers bevatten of leeg zijn'
                    );
                  },
                },
                errors,
              }}
              value={company?.chamberOfCommerceId || undefined}
            />
            <TextFormField
              title={'VIHB-nummer'}
              placeholder={'Vul VIHB-nummer in'}
              formHook={{
                register,
                name: 'vihbId',
                rules: {
                  validate: (value: string) => {
                    const trimmed = value?.trim() || '';
                    if (trimmed === '') return true;
                    return (
                      /^\d{6}[VIHBX]{4}$/i.test(trimmed) ||
                      'VIHB-nummer moet 6 cijfers en 4 letters (VIHB of X) bevatten of leeg zijn'
                    );
                  },
                },
                errors,
              }}
              value={company?.vihbId || undefined}
            />
          </div>
          <div className={'flex items-start gap-4 self-stretch w-1/2'}>
            <TextFormField
              title={'Vewerkersnummer'}
              placeholder={'Vul vewerkersnummer in'}
              formHook={{
                register,
                name: 'processorId',
                rules: {
                  validate: (value: string) => {
                    const trimmed = value?.trim() || '';
                    if (trimmed === '') return true;
                    return (
                      /^\d{5}$/.test(trimmed) ||
                      'Vewerkersnummer moet 5 cijfers bevatten of leeg zijn'
                    );
                  },
                },
                errors,
              }}
              value={company?.processorId || undefined}
            />
          </div>
          <SelectFormField
            title={'Rol(len)'}
            placeholder={'Selecteer rol(len)'}
            options={[
              {
                value: 'PROCESSOR',
                label: 'Verwerker',
              },
              {
                value: 'CARRIER',
                label: 'Transporteur',
              },
            ]}
            formHook={{
              register,
              name: 'roles',
              errors,
              control,
            }}
            value={company?.roles}
            isMulti={true}
          />
        </div>
        <FormActionButtons onClick={onCancel} item={company} />
      </form>

      <RestoreCompanyDialog
        isOpen={softDeleteConflict !== null}
        onClose={handleRestoreCancel}
        onConfirm={handleRestoreConfirm}
        conflictMessage={softDeleteConflict?.message || ''}
      />

      <ErrorDialogComponent />
    </ErrorBoundary>
  );
};
