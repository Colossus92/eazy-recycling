import { FieldValues, useForm } from 'react-hook-form';
import { FormEvent, useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { ErrorBoundary } from 'react-error-boundary';
import { TextFormField } from '@/components/ui/form/TextFormField.tsx';
import { Company } from '@/types/api.ts';
import { WasteContainer } from '@/api/client';
import { FormTopBar } from '@/components/ui/form/FormTopBar.tsx';
import { FormActionButtons } from '@/components/ui/form/FormActionButtons.tsx';
import { ErrorDialog } from '@/components/ui/dialog/ErrorDialog.tsx';
import { SelectFormField } from '@/components/ui/form/selectfield/SelectFormField.tsx';
import { TextAreaFormField } from '@/components/ui/form/TextAreaFormField.tsx';
import { companyService } from '@/api/companyService.ts';
import { fallbackRender } from '@/utils/fallbackRender';
import { PostalCodeFormField } from '@/components/ui/form/PostalCodeFormField';

interface WasteContainerFormProps {
  onCancel: () => void;
  onSubmit: (data: WasteContainer) => void;
  wasteContainer?: WasteContainer;
}

export interface WasteContainerFormValues extends FieldValues {
  uuid?: string;
  id: string;
  containerType: string;
  companyId?: string;
  street: string;
  houseNumber: string;
  postalCode?: string;
  city: string;
  notes?: string;
}

function toWasteContainer(
  data: WasteContainerFormValues,
  companies: Company[]
): WasteContainer {
  const container: WasteContainer = {
    uuid: data.uuid || crypto.randomUUID(),
    id: data.id,
    location: {
      address: {
        streetName: data.street,
        buildingNumber: data.houseNumber,
        postalCode: data.postalCode || '',
        city: data.city,
      }
    },
    notes: data.notes,
  };

  if (data.companyId) {
    const company = companies.find((c) => c.id === data.companyId);
    if (company) {
      container.location = {
        companyId: company.id,
        companyName: company.name,
        address: {
          streetName: data.street,
          buildingNumber: data.houseNumber,
          postalCode: data.postalCode || '',
          city: data.city,
        }
      };
    }
  }

  return container;
}

export const WasteContainerForm = ({
  onCancel,
  onSubmit,
  wasteContainer,
}: WasteContainerFormProps) => {
  const [isErrorDialogOpen, setIsErrorDialogOpen] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const { data: companies = [] } = useQuery<Company[]>({
    queryKey: ['companies'],
    queryFn: () => companyService.list(),
  });

  const companyOptions = companies.map((company) => ({
    value: company.id || '',
    label: company.name,
  }));

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    control,
    formState: { errors },
  } = useForm<WasteContainerFormValues>({
    defaultValues: wasteContainer
      ? {
          uuid: wasteContainer.uuid,
          id: wasteContainer.id,
          companyId: wasteContainer?.location?.companyId,
          street: wasteContainer?.location?.address?.streetName,
          houseNumber: wasteContainer?.location?.address?.buildingNumber,
          postalCode: wasteContainer?.location?.address?.postalCode,
          city: wasteContainer?.location?.address?.city,
          notes: wasteContainer.notes,
        }
      : {
          useCompany: false,
          companyId: '',
        },
  });

  const watchCompanyId = watch('companyId');
  const hasCompanySelected =
    watchCompanyId !== undefined &&
    watchCompanyId !== null &&
    watchCompanyId.length > 0;

  useEffect(() => {
    if (hasCompanySelected) {
      const company = companies.find((c) => c.id === watchCompanyId);
      if (company) {
        setValue('street', company.address.streetName);
        setValue('houseNumber', company.address.buildingNumber);
        setValue('postalCode', company.address.postalCode);
        setValue('city', company.address.city);
      }
    } else {
      if (!wasteContainer) {
        // Only clear fields if creating a new container
        setValue('street', '');
        setValue('houseNumber', '');
        setValue('postalCode', '');
        setValue('city', '');
      }
    }
  }, [watchCompanyId, companies, setValue, wasteContainer, hasCompanySelected]);

  const submitForm = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    await handleSubmit(async (data) => {
      try {
        await onSubmit(toWasteContainer(data, companies));
        onCancel();
      } catch (error) {
        setErrorMessage(
          error instanceof Error ? error.message : 'Er is iets misgegaan'
        );
        setIsErrorDialogOpen(true);
      }
    })();
  };

  const isEditing = !!wasteContainer;
  const formTitle = isEditing ? 'Container bewerken' : 'Container toevoegen';

  return (
    <ErrorBoundary fallbackRender={fallbackRender}>
      <form onSubmit={(e) => submitForm(e)}>
        <FormTopBar title={formTitle} onClick={onCancel} />
        <div className="flex flex-col items-center self-stretch p-4 gap-4">
          <TextFormField
            title={'Containerkenmerk'}
            placeholder={'Vul kenmerk in'}
            formHook={{
              register,
              name: 'id',
              rules: { 
                required: 'Kenmerk is verplicht',
                validate: (value: string) => {
                  const trimmed = value?.trim() || '';
                  return trimmed !== '' || 'Kenmerk mag niet leeg zijn';
                },
              },
              errors,
            }}
            value={wasteContainer?.id}
          />

          <div className="flex flex-col items-start self-stretch gap-4 p-4 bg-color-surface-secondary rounded-radius-md">
            <span className="text-subtitle-1">Huidige locatie</span>

            <div className="flex-grow-0 flex flex-col items-start self-stretch gap-4">
              <SelectFormField
                title={'Kies bedrijf (optioneel)'}
                placeholder={'Selecteer een bedrijf of vul zelf een adres in'}
                options={companyOptions}
                formHook={{
                  register,
                  name: 'companyId',
                  rules: {},
                  errors,
                  control,
                }}
                value={wasteContainer?.location?.companyId}
              />

              {hasCompanySelected && (
                <div className="flex items-center max-w-96">
                  <span className="text-body-2 whitespace-normal break-words">
                    Adresgegevens worden automatisch ingevuld op basis van het
                    geselecteerde bedrijf
                  </span>
                </div>
              )}
            </div>

            <div className="flex items-start self-stretch gap-4 flex-grow">
              <TextFormField
                title={'Straat'}
                placeholder={'Vul straatnaam in'}
                formHook={{
                  register,
                  name: 'street',
                  errors,
                }}
                value={wasteContainer?.location?.address?.streetName}
                disabled={hasCompanySelected}
              />

              <TextFormField
                title={'Nummer'}
                placeholder={'Vul huisnummer in'}
                formHook={{
                  register,
                  name: 'houseNumber',
                  errors,
                }}
                value={wasteContainer?.location?.address?.buildingNumber}
                disabled={hasCompanySelected}
              />
            </div>

            <div className="flex items-start self-stretch gap-4">
              <PostalCodeFormField
                register={register}
                setValue={setValue}
                errors={errors}
                name="postalCode"
                value={wasteContainer?.location?.address?.postalCode}
                required={false}
                disabled={hasCompanySelected}
              />

              <TextFormField
                title={'Plaats'}
                placeholder={'Vul Plaats in'}
                formHook={{
                  register,
                  name: 'city',
                  errors,
                }}
                value={wasteContainer?.location?.address?.city}
                disabled={hasCompanySelected}
              />
            </div>
          </div>

          <TextAreaFormField
            title={'Opmerkingen'}
            placeholder={'Plaats opmerkingen'}
            formHook={{
              register,
              name: 'notes',
              rules: {},
              errors,
            }}
            value={wasteContainer?.notes}
          />
        </div>

        <FormActionButtons onClick={onCancel} item={wasteContainer} />
      </form>
      <ErrorDialog
        isOpen={isErrorDialogOpen}
        setIsOpen={setIsErrorDialogOpen}
        errorMessage={errorMessage}
      />
      ;
    </ErrorBoundary>
  );
};
