import { WeightTicketRequest } from '@/api/client';
import { Button } from '@/components/ui/button/Button';
import { SplitButton } from '@/components/ui/button/SplitButton';
import { FormDialog } from '@/components/ui/dialog/FormDialog.tsx';
import { AddressFormField } from '@/components/ui/form/addressformfield/AddressFormField';
import { AuditMetadataFooter } from '@/components/ui/form/AuditMetadataFooter';
import { CompanySelectFormField } from '@/components/ui/form/CompanySelectFormField';
import { FormTopBar } from '@/components/ui/form/FormTopBar.tsx';
import { RadioFormField } from '@/components/ui/form/RadioFormField';
import { TruckSelectFormField } from '@/components/ui/form/selectfield/TruckSelectFormField';
import { TextAreaFormField } from '@/components/ui/form/TextAreaFormField';
import { TextFormField } from '@/components/ui/form/TextFormField';
import { Tab } from '@/components/ui/tab/Tab';
import { Note } from '@/features/planning/components/note/Note';
import { fallbackRender } from '@/utils/fallbackRender';
import { TabGroup, TabList, TabPanel, TabPanels } from '@headlessui/react';
import { useEffect, useState } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { FormProvider, useWatch } from 'react-hook-form';
import { TransportFromWeightTicketForm } from '../TransportFromWeightTicketForm';
import { WeightTicketStatusTag } from '../WeightTicketStatusTag';
import {
  useWeightTicketForm,
  WeightTicketFormValues,
} from './useWeigtTicketFormHook';
import { WeightTicketFormActionMenu } from './WeightTicketFormActionMenu';
import { WeightTicketLinesTab } from './WeightTicketLinesTab';
import { WeightTicketRelatedTab } from './WeightTicketRelatedTab';

interface WeightTicketFormProps {
  isOpen: boolean;
  setIsOpen: (value: boolean) => void;
  weightTicketNumber?: number;
  status?: string;
  onDelete: (id: number) => void;
  onSplit?: (id: number) => void;
  onCopy?: (id: number) => void;
  onComplete?: (id: number) => void;
  onCreateTransport?: (
    weightTicketId: number,
    weightTicketData: WeightTicketRequest,
    pickupDateTime: string,
    deliveryDateTime?: string
  ) => Promise<void>;
  onCreateInvoice?: (id: number) => void;
  noDialog?: boolean;
}

export const WeightTicketForm = ({
  isOpen,
  setIsOpen,
  weightTicketNumber,
  status,
  onDelete,
  onSplit,
  onCopy,
  onComplete,
  onCreateTransport,
  onCreateInvoice,
  noDialog = false,
}: WeightTicketFormProps) => {
  const {
    data,
    isLoading,
    formContext,
    mutation,
    createCompletedMutation,
    resetForm,
    formValuesToWeightTicketRequest,
    currentWeightTicketNumber,
  } = useWeightTicketForm(weightTicketNumber);
  const [isDisabled, setIsDisabled] = useState(false);
  const [isTransportFormOpen, setIsTransportFormOpen] = useState(false);
  const [selectedWeightTicketId, setSelectedWeightTicketId] = useState<
    number | undefined
  >();

  const handleClose = (value: boolean) => {
    if (!value) {
      resetForm();
    }
    setIsOpen(value);
  };

  const handleCancel = () => {
    resetForm();
    setIsOpen(false);
  };

  const handleCreateTransport = (id: number) => {
    setSelectedWeightTicketId(id);
    setIsTransportFormOpen(true);
  };

  const handleTransportCreation = async (
    weightTicketId: number,
    pickupDateTime: string,
    deliveryDateTime?: string
  ) => {
    if (onCreateTransport) {
      // Trigger form validation
      const isValid = await formContext.trigger();
      if (!isValid) {
        return;
      }

      // Get current form values and convert to request format
      const formValues = formContext.getValues();
      const weightTicketRequest = formValuesToWeightTicketRequest(formValues);

      await onCreateTransport(
        weightTicketId,
        weightTicketRequest,
        pickupDateTime,
        deliveryDateTime
      );
    }
  };

  useEffect(() => {
    setIsDisabled(
      Boolean((data?.status || status) && (data?.status || status) !== 'DRAFT')
    );
  }, [data?.status, status]);

  /**
   * Helper function to parse number strings that may use comma or period as decimal separator
   */
  const parseNumber = (value: string | number | undefined): number => {
    if (value === undefined || value === null || value === '') return 0;
    if (typeof value === 'number') return value;
    const normalizedValue = String(value).replace(',', '.');
    const parsed = parseFloat(normalizedValue);
    return isNaN(parsed) ? 0 : parsed;
  };

  const validateAndFilterFormValues = (
    formValues: WeightTicketFormValues
  ): WeightTicketFormValues | null => {
    // Calculate netto to validate it's not below 0
    const weging1 = formValues.lines.reduce((sum: number, field) => {
      const weight = parseNumber(field.weightValue as string);
      return sum + weight;
    }, 0);
    const weging2 = parseNumber(
      formValues.secondWeighingValue as unknown as string
    );
    const bruto = weging1 - weging2;
    const tarra = parseNumber(formValues.tarraWeightValue as unknown as string);
    const netto = bruto - tarra;

    if (netto < 0) {
      formContext.setError('secondWeighingValue', {
        type: 'manual',
        message: 'Netto gewicht kan niet negatief zijn',
      });
      return null;
    }

    // Filter out empty lines (lines where catalogItemId is empty)
    return {
      ...formValues,
      lines: formValues.lines.filter(
        (line) => line.catalogItemId && line.catalogItemId.trim() !== ''
      ),
    };
  };

  const onSubmit = formContext.handleSubmit(async (formValues) => {
    const filteredFormValues = validateAndFilterFormValues(formValues);
    if (!filteredFormValues) return;
    await mutation.mutateAsync(filteredFormValues);
  });

  const handleComplete = formContext.handleSubmit(async (formValues) => {
    const filteredFormValues = validateAndFilterFormValues(formValues);
    if (!filteredFormValues) return;

    // Save first (creates or updates)
    const response = await mutation.mutateAsync(filteredFormValues);

    // Determine the weight ticket ID to complete
    // Priority: currentWeightTicketNumber (set after first save), then data.id, then response.id (for new tickets)
    const weightTicketId =
      currentWeightTicketNumber ?? data?.id ?? (response as any)?.id;

    if (weightTicketId && onComplete) {
      onComplete(weightTicketId);
    }
  });

  const handleSubmit = isDisabled
    ? (e: React.FormEvent) => e.preventDefault()
    : onSubmit;

  // Detect errors in each tab
  const errors = formContext.formState.errors;

  const algemeneenHasError = !!(
    errors.direction ||
    errors.consignorPartyId ||
    errors.carrierPartyId ||
    errors.truckLicensePlate ||
    errors.reclamation ||
    errors.note
  );

  const sorteerweginHasError = !!(
    errors.lines &&
    Array.isArray(errors.lines) &&
    errors.lines.some((e) => e)
  );

  const routeHasError = !!(errors.pickupLocation || errors.deliveryLocation);
  const formValues = useWatch({ control: formContext.control });
  console.log('Form values:', formValues);
  const formContent = (
    <div className={'w-full h-[90vh]'}>
      <FormProvider {...formContext}>
        <form
          className="flex flex-col items-center self-stretch h-full"
          onSubmit={handleSubmit}
        >
          <FormTopBar
            title={data ? `Weegbon ${data.id}` : 'Nieuw Weegbon'}
            actions={
              data && (
                <WeightTicketFormActionMenu
                  weightTicket={data}
                  onDelete={onDelete}
                  onSplit={onSplit}
                  onCopy={onCopy}
                  onComplete={onComplete}
                  onCreateTransport={
                    onCreateTransport ? handleCreateTransport : undefined
                  }
                  onCreateInvoice={onCreateInvoice}
                />
              )
            }
            onClick={handleCancel}
          />
          <div
            className={
              'flex flex-col items-start self-stretch flex-1 gap-5 p-4 min-h-0'
            }
          >
            {data?.cancellationReason && (
              <Note note={data?.cancellationReason} />
            )}
            {isLoading ? (
              <div className="flex justify-center items-center w-full p-8">
                <p>Weegbon laden...</p>
              </div>
            ) : (
              <div
                className={
                  'flex flex-col items-start self-stretch gap-4 flex-1 min-h-0'
                }
              >
                {data?.status && (
                  <WeightTicketStatusTag
                    status={
                      data.status as
                        | 'DRAFT'
                        | 'COMPLETED'
                        | 'INVOICED'
                        | 'CANCELLED'
                    }
                  />
                )}
                <TabGroup className="w-full flex-1 flex flex-col min-h-0">
                  <TabList className="relative z-10">
                    <Tab label="Algemeen" hasError={algemeneenHasError} />
                    <Tab
                      label="Sorteerweging"
                      hasError={sorteerweginHasError}
                    />
                    <Tab label="Route" hasError={routeHasError} />
                    <Tab label="Gerelateerd" />
                  </TabList>
                  <TabPanels className="flex flex-col flex-1 bg-color-surface-primary border border-solid rounded-b-radius-lg rounded-tr-radius-lg border-color-border-primary pt-4 gap-4 min-h-0 -mt-[2px] overflow-y-auto">
                    <TabPanel className="flex flex-col items-start gap-4 px-4 pb-4">
                      <RadioFormField
                        title={'Richting'}
                        options={[
                          { value: 'INBOUND', label: 'Inkomend' },
                          { value: 'OUTBOUND', label: 'Uitgaand' },
                        ]}
                        testId="direction"
                        disabled={isDisabled}
                        formHook={{
                          name: 'direction',
                          rules: { required: 'Richting is verplicht' },
                          errors: formContext.formState.errors,
                        }}
                      />
                      <div className="w-1/2">
                        <CompanySelectFormField
                          title={'Opdrachtgever'}
                          placeholder={'Selecteer een opdrachtgever'}
                          name={'consignorPartyId'}
                          rules={{ required: 'Opdrachtgever is verplicht' }}
                          disabled={isDisabled}
                        />
                      </div>
                      <div className="w-1/2">
                        <CompanySelectFormField
                          title={'Vervoerder'}
                          placeholder={'Selecteer een vervoerder'}
                          name={'carrierPartyId'}
                          rules={{ required: 'Vervoerder is verplicht' }}
                          disabled={isDisabled}
                        />
                      </div>
                      <div className="w-1/2">
                        <TruckSelectFormField
                          disabled={isDisabled}
                          formHook={{
                            register: formContext.register,
                            name: 'truckLicensePlate',
                            errors: formContext.formState.errors,
                            control: formContext.control,
                          }}
                        />
                      </div>
                      <TextFormField
                        title={'Reclamatie'}
                        placeholder={'Vul reclamatie in'}
                        disabled={isDisabled}
                        formHook={{
                          register: formContext.register,
                          name: 'reclamation',
                          errors: formContext.formState.errors,
                        }}
                      />
                      <TextAreaFormField
                        title={'Opmerkingen'}
                        placeholder={'Plaats opmerkingen'}
                        disabled={isDisabled}
                        formHook={{
                          register: formContext.register,
                          name: 'note',
                          rules: {},
                          errors: formContext.formState.errors,
                        }}
                      />
                    </TabPanel>
                    <TabPanel className="px-4 pb-4">
                      <WeightTicketLinesTab disabled={isDisabled} />
                    </TabPanel>
                    <TabPanel className="flex flex-col items-start gap-4 px-4 pb-4">
                      <AddressFormField
                        name="pickupLocation"
                        control={formContext.control}
                        label="Ophaallocatie"
                        testId="pickup-location-select"
                        required={false}
                        isNoLocationAllowed={true}
                        disabled={isDisabled}
                      />
                      <AddressFormField
                        name="deliveryLocation"
                        control={formContext.control}
                        label="Afleverlocatie"
                        testId="delivery-location-select"
                        required={false}
                        isNoLocationAllowed={true}
                        disabled={isDisabled}
                      />
                    </TabPanel>
                    {data && (
                      <TabPanel className="flex flex-col items-start gap-4 px-4 pb-4">
                        <WeightTicketRelatedTab weightTicketId={data.id} />
                      </TabPanel>
                    )}
                  </TabPanels>
                </TabGroup>
              </div>
            )}
            <AuditMetadataFooter
              createdAt={data?.createdAt?.toString()}
              createdByName={data?.createdByName}
              updatedAt={data?.updatedAt?.toString()}
              updatedByName={data?.updatedByName}
            />
          </div>
          <div className="flex py-3 px-4 justify-end items-center self-stretch gap-4 border-t border-solid border-color-border-primary">
            <Button
              variant={'secondary'}
              label={'Annuleren'}
              onClick={handleCancel}
              fullWidth={false}
              data-testid={'cancel-button'}
            />
            <SplitButton
              primaryLabel={data ? 'Opslaan' : 'Concept opslaan'}
              secondaryLabel={data ? 'Verwerken' : 'Opslaan en verwerken'}
              onPrimaryClick={onSubmit}
              onSecondaryClick={handleComplete}
              isSubmitting={
                mutation.isPending || createCompletedMutation.isPending
              }
              disabled={isDisabled}
            />
          </div>
        </form>
      </FormProvider>
    </div>
  );

  if (noDialog) {
    return (
      <ErrorBoundary fallbackRender={fallbackRender}>
        {formContent}
      </ErrorBoundary>
    );
  }

  return (
    <ErrorBoundary fallbackRender={fallbackRender}>
      <FormDialog isOpen={isOpen} setIsOpen={handleClose} width="w-[720px]">
        {formContent}
      </FormDialog>
      <TransportFromWeightTicketForm
        isOpen={isTransportFormOpen}
        setIsOpen={setIsTransportFormOpen}
        weightTicketId={selectedWeightTicketId}
        onCreateTransport={handleTransportCreation}
      />
    </ErrorBoundary>
  );
};
