import { FieldValues, useForm } from 'react-hook-form';
import { FormEvent } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { TextFormField } from '@/components/ui/form/TextFormField.tsx';
import { Company } from '@/types/api.ts';
import { FormTopBar } from '@/components/ui/form/FormTopBar.tsx';
import { FormActionButtons } from '@/components/ui/form/FormActionButtons.tsx';
import { useErrorHandling } from '@/hooks/useErrorHandling.tsx';
import { JdenticonAvatar } from '@/components/ui/icon/JdenticonAvatar.tsx';
import { fallbackRender } from '@/utils/fallbackRender';
import { PostalCodeFormField } from '@/components/ui/form/PostalCodeFormField';

interface CompanyFormProps {
  onCancel: () => void;
  onSubmit: (data: Company) => void;
  company?: Company;
}

export interface CompanyFormValues extends FieldValues {
  id?: string;
  name: string;
  street: string;
  houseNumber: string;
  postalCode: string;
  city: string;
  chamberOfCommerceId: string;
  vihbId: string;
}

function toCompany(data: CompanyFormValues): Company {
  const company: Company = {
    address: {
      streetName: data.street,
      buildingNumber: data.houseNumber,
      postalCode: data.postalCode,
      city: data.city,
    },
    chamberOfCommerceId: data.chamberOfCommerceId?.trim() || null,
    name: data.name,
    vihbId: data.vihbId?.trim() || null,
  };

  if (data.id) {
    company.id = data.id;
  }

  return company;
}

export const CompanyForm = ({
  onCancel,
  onSubmit,
  company,
}: CompanyFormProps) => {
  const { handleError, ErrorDialogComponent } = useErrorHandling();

  const {
    register,
    setValue,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<CompanyFormValues>({
    defaultValues: company
      ? {
          id: company.id,
          name: company.name,
          street: company.address.streetName,
          houseNumber: company.address.buildingNumber,
          postalCode: company.address.postalCode,
          city: company.address.city,
          chamberOfCommerceId: company.chamberOfCommerceId || '',
          vihbId: company.vihbId || '',
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
                value={company?.address.streetName}
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
                value={company?.address.buildingNumber}
              />
            </div>
          </div>
          <div className={'flex items-start gap-4 self-stretch'}>
            <PostalCodeFormField
              register={register}
              setValue={setValue}
              name="postalCode"
              errors={errors}
              value={company?.address.postalCode}
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
              value={company?.address.city}
            />
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
        </div>
        <FormActionButtons onClick={onCancel} item={company} />
      </form>

      <ErrorDialogComponent />
    </ErrorBoundary>
  );
};
