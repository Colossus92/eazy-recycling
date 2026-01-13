import { ExternalProductResponse, MaterialPriceResponse } from '@/api/client';
import { materialPriceSyncService } from '@/api/services/materialPriceSyncService';
import { FormDialog } from '@/components/ui/dialog/FormDialog';
import { FormActionButtons } from '@/components/ui/form/FormActionButtons';
import { FormTopBar } from '@/components/ui/form/FormTopBar';
import { NumberFormField } from '@/components/ui/form/NumberFormField';
import { MaterialSelectFormField } from '@/components/ui/form/selectfield/MaterialSelectFormField';
import { SelectFormField } from '@/components/ui/form/selectfield/SelectFormField';
import { TextFormField } from '@/components/ui/form/TextFormField';
import { useErrorHandling } from '@/hooks/useErrorHandling';
import { fallbackRender } from '@/utils/fallbackRender';
import { FormEvent, useEffect, useState } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { useForm } from 'react-hook-form';
import { useQuery } from '@tanstack/react-query';
import { Switch } from '@headlessui/react';

interface MaterialPriceFormProps {
  isOpen: boolean;
  onCancel: () => void;
  onSubmit: (
    catalogItemId: string,
    price: number,
    publishToPricingApp?: boolean,
    externalPricingAppId?: number,
    externalPricingAppName?: string
  ) => void;
  initialData?: MaterialPriceResponse;
}

interface MaterialPriceFormValues {
  catalogItemId: number | string;
  price: number;
  publishToPricingApp: boolean;
  externalPricingAppId: string;
  externalPricingAppName: string;
}

const CREATE_NEW_OPTION = 'create_new';

export const MaterialPriceForm = ({
  isOpen,
  onCancel,
  onSubmit,
  initialData,
}: MaterialPriceFormProps) => {
  const { handleError, ErrorDialogComponent } = useErrorHandling();
  const [syncEnabled, setSyncEnabled] = useState(false);

  const { data: externalProducts = [] } = useQuery<ExternalProductResponse[]>({
    queryKey: ['externalProducts'],
    queryFn: () => materialPriceSyncService.getExternalProducts(),
    enabled: isOpen,
  });

  const {
    register,
    handleSubmit,
    reset,
    control,
    watch,
    setValue,
    formState: { errors },
  } = useForm<MaterialPriceFormValues>({
    defaultValues: {
      catalogItemId: '',
      price: 0,
      publishToPricingApp: false,
      externalPricingAppId: CREATE_NEW_OPTION,
      externalPricingAppName: '',
    },
  });

  const selectedExternalId = watch('externalPricingAppId');

  // Find the selected material's name to use as default external name
  const selectedMaterialName = initialData?.name ?? '';

  // Update external name when selecting an existing product
  useEffect(() => {
    if (selectedExternalId && selectedExternalId !== CREATE_NEW_OPTION) {
      const selectedProduct = externalProducts.find(
        (p) => p.id.toString() === selectedExternalId
      );
      if (selectedProduct) {
        setValue('externalPricingAppName', selectedProduct.name);
      }
    }
  }, [selectedExternalId, externalProducts, setValue]);

  useEffect(() => {
    if (initialData) {
      const hasSync = initialData.publishToPricingApp;
      setSyncEnabled(hasSync);
      reset({
        catalogItemId: initialData.id.toString(),
        price: initialData.defaultPrice ?? 0,
        publishToPricingApp: hasSync,
        externalPricingAppId:
          initialData.externalPricingAppId?.toString() ?? CREATE_NEW_OPTION,
        externalPricingAppName:
          initialData.externalPricingAppName ?? initialData.name,
      });
    } else {
      setSyncEnabled(false);
      reset({
        catalogItemId: '',
        price: 0,
        publishToPricingApp: false,
        externalPricingAppId: CREATE_NEW_OPTION,
        externalPricingAppName: '',
      });
    }
  }, [initialData, reset]);

  const cancel = () => {
    setSyncEnabled(false);
    reset({
      catalogItemId: '',
      price: 0,
      publishToPricingApp: false,
      externalPricingAppId: CREATE_NEW_OPTION,
      externalPricingAppName: '',
    });
    onCancel();
  };

  const handleSyncToggle = () => {
    const newValue = !syncEnabled;
    setSyncEnabled(newValue);
    setValue('publishToPricingApp', newValue);
    if (!newValue) {
      setValue('externalPricingAppId', CREATE_NEW_OPTION);
      setValue('externalPricingAppName', '');
    } else {
      // Pre-populate with material name when enabling sync for new products
      if (selectedExternalId === CREATE_NEW_OPTION) {
        setValue('externalPricingAppName', selectedMaterialName);
      }
    }
  };

  const submitForm = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    await handleSubmit(async (data) => {
      try {
        const catalogItemId =
          typeof data.catalogItemId === 'string'
            ? data.catalogItemId
            : String(data.catalogItemId);

        const externalId =
          syncEnabled && data.externalPricingAppId !== CREATE_NEW_OPTION
            ? parseInt(data.externalPricingAppId, 10)
            : undefined;

        await onSubmit(
          catalogItemId,
          data.price,
          syncEnabled,
          externalId,
          syncEnabled ? data.externalPricingAppName : undefined
        );
        cancel();
      } catch (error) {
        handleError(error);
      }
    })();
  };

  return (
    <FormDialog isOpen={isOpen} setIsOpen={cancel}>
      <ErrorBoundary fallbackRender={fallbackRender}>
        <form
          className="flex flex-col items-center self-stretch"
          onSubmit={(e) => submitForm(e)}
        >
          <FormTopBar
            title={
              initialData
                ? 'Materiaalprijs bewerken'
                : 'Materiaalprijs toevoegen'
            }
            onClick={cancel}
          />
          <div className="flex flex-col items-center self-stretch p-4 gap-4">
            <div className="flex items-start self-stretch gap-4">
              <MaterialSelectFormField
                formHook={{
                  register,
                  name: 'catalogItemId',
                  rules: { required: 'Materiaal is verplicht' },
                  errors,
                  control,
                }}
                disabled={!!initialData}
              />
            </div>
            <div className="flex items-start self-stretch gap-4">
              <NumberFormField
                title={'Prijs'}
                placeholder={'Bijv. 10.50'}
                formHook={{
                  register,
                  name: 'price',
                  rules: { required: 'Prijs is verplicht' },
                  errors,
                }}
                step={0.01}
              />
              <TextFormField
                title={'Valuta'}
                placeholder={'EUR'}
                disabled={true}
              />
            </div>

            {/* Sync to external app section */}
            <div className="flex flex-col items-start self-stretch gap-4 pt-4 border-t border-color-border">
              <div className="flex items-center justify-start gap-2 self-stretch">
                <Switch
                  checked={syncEnabled}
                  onChange={handleSyncToggle}
                  className={`${
                    syncEnabled ? 'bg-color-brand-primary' : 'bg-gray-300'
                  } relative inline-flex h-6 w-11 items-center rounded-full`}
                >
                  <span className="sr-only">Enable notifications</span>
                  <span
                    className={`${
                      syncEnabled ? 'translate-x-6' : 'translate-x-1'
                    } inline-block h-4 w-4 transform rounded-full bg-white transition`}
                  />
                </Switch>
                <span className="text-caption-2">Sync naar app</span>
              </div>

              {syncEnabled && (
                <>
                  <div className="flex items-start self-stretch gap-4">
                    <SelectFormField
                      title="Koppel aan extern product"
                      placeholder="Selecteer een product"
                      options={[
                        {
                          value: CREATE_NEW_OPTION,
                          label: 'Nieuwe prijs aanmaken',
                        },
                        ...externalProducts.map((product) => ({
                          value: product.id.toString(),
                          label: `${product.id} (${product.name})`,
                        })),
                      ]}
                      formHook={{
                        register,
                        name: 'externalPricingAppId',
                        errors,
                        control,
                      }}
                    />
                  </div>

                  <div className="flex items-start self-stretch gap-4">
                    <TextFormField
                      title="Naam in externe app"
                      placeholder="Bijv. koper"
                      formHook={{
                        register,
                        name: 'externalPricingAppName',
                        errors,
                      }}
                    />
                  </div>

                  {selectedExternalId === CREATE_NEW_OPTION && (
                    <div className="flex items-center self-stretch px-3 py-2 bg-color-surface-secondary rounded-radius-md">
                      <span className="text-body-2 text-color-text-secondary">
                        Er wordt een nieuwe prijs aangemaakt in de externe app
                        met de naam van dit materiaal.
                      </span>
                    </div>
                  )}
                </>
              )}
            </div>
          </div>
          <FormActionButtons onClick={cancel} item={initialData} />
        </form>
        <ErrorDialogComponent />
      </ErrorBoundary>
    </FormDialog>
  );
};
