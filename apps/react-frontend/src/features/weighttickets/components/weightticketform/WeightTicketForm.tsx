import { Company, companyService } from '@/api/services/companyService';
import { FormDialog } from '@/components/ui/dialog/FormDialog.tsx';
import { FormActionButtons } from '@/components/ui/form/FormActionButtons';
import { FormTopBar } from '@/components/ui/form/FormTopBar.tsx';
import { SelectFormField } from '@/components/ui/form/selectfield/SelectFormField';
import { TruckSelectFormField } from '@/components/ui/form/selectfield/TruckSelectFormField';
import { TextAreaFormField } from '@/components/ui/form/TextAreaFormField';
import { TextFormField } from '@/components/ui/form/TextFormField';
import { Note } from '@/features/planning/components/note/Note';
import { fallbackRender } from '@/utils/fallbackRender';
import { useQuery } from '@tanstack/react-query';
import { ErrorBoundary } from 'react-error-boundary';
import { FormProvider } from 'react-hook-form';
import { WeightTicketStatusTag } from '../WeightTicketStatusTag';
import { useWeightTicketForm } from './useWeigtTicketFormHook';
import { WeightTicketLinesTab } from './WeightTicketLinesTab';
import { WeightTicketFormActionMenu } from './WeightTicketFormActionMenu';
import { useEffect, useState } from 'react';
import { TabGroup, TabList, TabPanel, TabPanels } from '@headlessui/react';
import { Tab } from '@/components/ui/tab/Tab';
import { RadioFormField } from '@/components/ui/form/RadioFormField';
import { AddressFormField } from '@/components/ui/form/addressformfield/AddressFormField';
import { TransportFromWeightTicketForm } from '../TransportFromWeightTicketForm';
import { createWasteTransportFromWeightTicket, transportService } from '@/api/services/transportService';
import { toastService } from '@/components/ui/toast/toastService';
import { useNavigate } from 'react-router-dom';
import { format } from 'date-fns';

interface WeightTicketFormProps {
  isOpen: boolean;
  setIsOpen: (value: boolean) => void;
  weightTicketNumber?: number;
  status?: string;
  onDelete: (id: number) => void;
  onSplit?: (id: number) => void;
  onCopy?: (id: number) => void;
  onComplete?: (id: number) => void;
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
  noDialog = false,
}: WeightTicketFormProps) => {
  const { data, isLoading, formContext, mutation, resetForm } =
    useWeightTicketForm(weightTicketNumber, () => setIsOpen(false));
  const [isDisabled, setIsDisabled] = useState(false);
  const [isTransportFormOpen, setIsTransportFormOpen] = useState(false);
  const [selectedWeightTicketId, setSelectedWeightTicketId] = useState<number | undefined>();
  const navigate = useNavigate();

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
    try {
      const response = await createWasteTransportFromWeightTicket(
        weightTicketId,
        pickupDateTime,
        deliveryDateTime
      );
      
      // Fetch transport details to get the pickup date
      const transportDetails = await transportService.getTransportById(response.transportId);
      
      // Format the date for URL params
      const pickupDate = new Date(transportDetails.pickupDateTime);
      const dateParam = format(pickupDate, 'yyyy-MM-dd');
      
      // Create clickable toast with link to planning
      const toastContent = (
        <div className="flex flex-col gap-1">
          <span>Transport aangemaakt: {response.transportId}</span>
          <button
            onClick={() => {
              navigate(`/?transportId=${response.transportId}&date=${dateParam}`);
            }}
            className="text-left underline hover:no-underline text-color-brand-primary font-semibold"
          >
            Bekijk in planning →
          </button>
        </div>
      );
      
      toastService.success(toastContent);
    } catch (error) {
      toastService.error('Fout bij aanmaken transport');
      throw error;
    }
  };

  useEffect(() => {
    setIsDisabled(
      Boolean((data?.status || status) && (data?.status || status) !== 'DRAFT')
    );
  }, [data?.status, status]);

  const { data: companies = [] } = useQuery<Company[]>({
    queryKey: ['companies'],
    queryFn: () => companyService.getAll(),
  });
  const companyOptions = companies.map((company) => ({
    value: company.id || '',
    label: company.name,
  }));

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

  const onSubmit = formContext.handleSubmit(async (formValues) => {
    // Calculate netto to validate it's not below 0
    const weging1 = formValues.lines.reduce((sum, field) => {
      const weight = parseNumber(field.weightValue as string);
      return sum + weight;
    }, 0);
    const weging2 = parseNumber(formValues.secondWeighingValue as unknown as string);
    const bruto = weging1 - weging2;
    const tarra = parseNumber(formValues.tarraWeightValue as unknown as string);
    const netto = bruto - tarra;

    if (netto < 0) {
      formContext.setError('secondWeighingValue', {
        type: 'manual',
        message: 'Netto gewicht kan niet negatief zijn',
      });
      return;
    }

    // Filter out empty lines (lines where wasteStreamNumber is empty)
    const filteredFormValues = {
      ...formValues,
      lines: formValues.lines.filter(
        (line) => line.wasteStreamNumber && line.wasteStreamNumber.trim() !== ''
      ),
    };

    await mutation.mutateAsync(filteredFormValues);
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
                  onCreateTransport={handleCreateTransport}
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
                        <SelectFormField
                          title={'Opdrachtgever'}
                          placeholder={'Selecteer een opdrachtgever'}
                          options={companyOptions}
                          testId="consignor-party-select"
                          disabled={isDisabled}
                          formHook={{
                            register: formContext.register,
                            name: 'consignorPartyId',
                            rules: { required: 'Opdrachtgever is verplicht' },
                            errors: formContext.formState.errors,
                            control: formContext.control,
                          }}
                        />
                      </div>
                      <div className="w-1/2">
                        <SelectFormField
                          title={'Vervoerder'}
                          placeholder={'Selecteer een vervoerder'}
                          options={companyOptions}
                          testId="carrier-party-select"
                          disabled={isDisabled}
                          formHook={{
                            register: formContext.register,
                            name: 'carrierPartyId',
                            errors: formContext.formState.errors,
                            control: formContext.control,
                          }}
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
                      />
                      <AddressFormField
                        name="deliveryLocation"
                        control={formContext.control}
                        label="Afleverlocatie"
                        testId="delivery-location-select"
                        required={false}
                        isNoLocationAllowed={true}
                      />
                    </TabPanel>
                  </TabPanels>
                </TabGroup>
              </div>
            )}
          </div>
          <FormActionButtons
            onClick={handleCancel}
            item={data}
            disabled={isDisabled}
          />
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
