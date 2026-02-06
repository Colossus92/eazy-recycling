import {
  Control,
  FieldErrors,
  UseFormRegister,
  UseFormSetValue,
  UseFormWatch,
} from 'react-hook-form';
import { Company } from '@/api/services/companyService';
import { TextFormField } from '@/components/ui/form/TextFormField';
import { JdenticonAvatar } from '@/components/ui/icon/JdenticonAvatar';
import { PostalCodeFormField } from '@/components/ui/form/PostalCodeFormField';
import { NumberFormField } from '@/components/ui/form/NumberFormField';
import { PhoneNumberFormField } from '@/components/ui/form/PhoneNumberFormField';
import { EmailFormField } from '@/components/ui/form/EmailFormField';
import { SelectFormField } from '@/components/ui/form/selectfield/SelectFormField';
import { AuditMetadataFooter } from '@/components/ui/form/AuditMetadataFooter';
import { sanitizeStreetOrCity } from '@/utils/addressSanitization';
import { CompleteCompanyViewRolesEnum } from '@/api/client/models/complete-company-view';
import { CompanyFormValues } from './CompanyForm';

interface CompanyDetailsSectionProps {
  register: UseFormRegister<CompanyFormValues>;
  setValue: UseFormSetValue<CompanyFormValues>;
  watch: UseFormWatch<CompanyFormValues>;
  control: Control<CompanyFormValues>;
  errors: FieldErrors<CompanyFormValues>;
  company?: Company;
}

export const CompanyDetailsSection = ({
  register,
  setValue,
  watch,
  control,
  errors,
  company,
}: CompanyDetailsSectionProps) => {
  return (
    <>
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
      <div className="p-4 bg-color-surface-secondary rounded-md flex flex-col gap-4 self-stretch">
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
                onBlur: (e) => {
                  const sanitized = sanitizeStreetOrCity(e.target.value);
                  setValue('street', sanitized);
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
                onBlur: (e) => {
                  const sanitized = sanitizeStreetOrCity(e.target.value);
                  setValue('city', sanitized);
                },
              },
              errors,
            }}
            value={company?.address.city}
          />
        </div>
      </div>
      <div className="flex flex-col items-start self-stretch p-4 bg-color-surface-secondary rounded-md gap-4">
        <span className="text-subtitle-1">Contact</span>
        <EmailFormField
          register={register}
          errors={errors}
          name="email"
          value={company?.email}
        />
        <PhoneNumberFormField
          formHook={{
            register,
            name: 'phone',
            errors,
          }}
          value={company?.phone}
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
      <div className={'flex items-start gap-4 self-stretch'}>
        <TextFormField
          title={'Vewerkersnummer'}
          placeholder={'Vul vewerkersnummer in'}
          formHook={{
            register,
            name: 'processorId',
            rules: {
              validate: (value: string) => {
                const trimmed = value?.trim() || '';
                const selectedRoles = watch('roles') || [];
                const hasProcessorRole = selectedRoles.includes(
                  'PROCESSOR' as CompleteCompanyViewRolesEnum
                );

                if (hasProcessorRole && trimmed === '') {
                  return 'Vewerkersnummer is verplicht voor bedrijven met de rol Verwerker';
                }

                if (trimmed === '') return true;
                return (
                  trimmed.length === 5 ||
                  'Vewerkersnummer moet 5 tekens bevatten'
                );
              },
            },
            errors,
          }}
          value={company?.processorId || undefined}
        />
        <TextFormField
          title={'BTW ID'}
          placeholder={'Vul BTW ID in'}
          formHook={{
            register,
            name: 'vatNumber',
            errors,
          }}
          value={company?.vatNumber || undefined}
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
        testId="roles-select"
      />
      <AuditMetadataFooter
        createdAt={company?.createdAt}
        createdByName={company?.createdByName}
        updatedAt={company?.updatedAt}
        updatedByName={company?.updatedByName}
      />
    </>
  );
};
