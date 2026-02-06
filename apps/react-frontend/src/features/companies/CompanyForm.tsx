import { FieldValues, useForm } from 'react-hook-form';
import { FormEvent, useState } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { Company } from '@/api/services/companyService';
import { FormTopBar } from '@/components/ui/form/FormTopBar.tsx';
import { FormActionButtons } from '@/components/ui/form/FormActionButtons.tsx';
import { useErrorHandling } from '@/hooks/useErrorHandling.tsx';
import { fallbackRender } from '@/utils/fallbackRender';
import { RestoreCompanyDialog } from './RestoreCompanyDialog';
import { AxiosError } from 'axios';
import { CompleteCompanyViewRolesEnum } from '@/api/client/models/complete-company-view';
import { Tab } from '@/components/ui/tab/Tab';
import { TabGroup, TabList, TabPanel, TabPanels } from '@headlessui/react';
import {
  CompanyInvoicesTab,
  CompanyTransportsTab,
  CompanyWasteStreamsTab,
  CompanyWeightTicketsTab,
} from './tabs';
import { CompanyDetailsSection } from './CompanyDetailsSection';

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
  phone?: string;
  email?: string;
  vatNumber?: string;
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
    phone: data.phone?.trim() || undefined,
    email: data.email?.trim() || undefined,
    vatNumber: data.vatNumber?.trim() || undefined,
    updatedAt: new Date().toISOString(),
    roles: data.roles,
    branches: [],
    isTenantCompany: false,
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
  const [isSubmitting, setIsSubmitting] = useState(false);

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
          phone: company.phone || '',
          email: company.email || '',
          vatNumber: company.vatNumber || '',
          roles: company.roles || [],
        }
      : undefined,
  });

  const submitAndClose = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (isSubmitting) return;
    await handleSubmit(async (data) => {
      setIsSubmitting(true);
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
            setIsSubmitting(false);
            return; // Don't show error dialog, show restore dialog instead
          }
        }
        handleError(error);
        setIsSubmitting(false);
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
      <div className={'w-full h-[90vh]'}>
        <form
          className="flex flex-col items-center self-stretch h-full"
          onSubmit={(e) => submitAndClose(e)}
        >
          <FormTopBar
            title={company ? 'Bedrijf aanpassen' : 'Een bedrijf toevoegen'}
            onClick={onCancel}
          />
          <div className="flex flex-col items-start self-stretch flex-1 p-4 gap-4 min-h-0">
            {company ? (
              <TabGroup className="w-full flex-1 flex flex-col min-h-0">
                <TabList className="relative z-10">
                  <Tab label="Gegevens" />
                  <Tab label="Afvalstromen" />
                  <Tab label="Weegbonnen" />
                  <Tab label="Transporten" />
                  <Tab label="Facturen" />
                </TabList>
                <TabPanels className="flex flex-col flex-1 bg-color-surface-primary border border-solid rounded-b-radius-lg rounded-tr-radius-lg border-color-border-primary pt-4 gap-4 min-h-0 -mt-[2px] overflow-y-auto">
                  <TabPanel
                    unmount={false}
                    className="flex flex-col items-start gap-4 px-4 pb-4"
                  >
                    <CompanyDetailsSection
                      register={register}
                      setValue={setValue}
                      watch={watch}
                      control={control}
                      errors={errors}
                      company={company}
                    />
                  </TabPanel>
                  <TabPanel className="flex flex-col items-start gap-4 min-h-0">
                    <CompanyWasteStreamsTab companyId={company.id} />
                  </TabPanel>
                  <TabPanel className="flex flex-col items-start gap-4 min-h-0">
                    <CompanyWeightTicketsTab companyId={company.id} />
                  </TabPanel>
                  <TabPanel className="flex flex-col items-start gap-4 min-h-0">
                    <CompanyTransportsTab companyId={company.id} />
                  </TabPanel>
                  <TabPanel className="flex flex-col items-start gap-4 min-h-0">
                    <CompanyInvoicesTab companyId={company.id} />
                  </TabPanel>
                </TabPanels>
              </TabGroup>
            ) : (
              <div className="w-full overflow-y-auto flex flex-col items-center gap-4">
                <CompanyDetailsSection
                  register={register}
                  setValue={setValue}
                  watch={watch}
                  control={control}
                  errors={errors}
                  company={company}
                />
              </div>
            )}
          </div>
          <FormActionButtons
            onClick={onCancel}
            item={company}
            disabled={isSubmitting}
          />
        </form>
      </div>
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
