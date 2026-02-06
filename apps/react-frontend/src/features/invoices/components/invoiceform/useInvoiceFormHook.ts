import { useCallback, useState } from 'react';
import { useForm } from 'react-hook-form';
import {
  CreateInvoiceRequest,
  InvoiceLineRequest,
  UpdateInvoiceRequest,
} from '@/api/client';
import { invoiceService } from '@/api/services/invoiceService';
import { toastService } from '@/components/ui/toast/toastService';
import { format } from 'date-fns';

export interface InvoiceLineFormValue {
  id?: string;
  catalogItemId: string;
  catalogItemName: string;
  catalogItemType?: 'MATERIAL' | 'PRODUCT' | 'WASTE_STREAM';
  date: string;
  description: string;
  quantity: string;
  unitPrice: string;
  unitOfMeasure: string;
  vatCode: string;
  vatPercentage: string;
  isReverseCharge: boolean;
  orderReference: string;
}

export interface InvoiceFormValues {
  customerId: string;
  invoiceDate: string;
  invoiceType: string;
  documentType: string;
  lines: InvoiceLineFormValue[];
}

const defaultValues: InvoiceFormValues = {
  customerId: '',
  invoiceDate: format(new Date(), 'yyyy-MM-dd'),
  invoiceType: 'SALE',
  documentType: 'INVOICE',
  lines: [
    {
      catalogItemId: '',
      catalogItemName: '',
      date: '',
      description: '',
      quantity: '1',
      unitPrice: '0',
      unitOfMeasure: '',
      vatCode: '',
      vatPercentage: '21',
      isReverseCharge: false,
      orderReference: '',
    },
  ],
};

export const useInvoiceFormHook = () => {
  const [isLoading, setIsLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [invoiceStatus, setInvoiceStatus] = useState<string | null>(null);
  const [invoiceNumber, setInvoiceNumber] = useState<string | null>(null);
  const [currentInvoiceId, setCurrentInvoiceId] = useState<string | null>(null);

  const formContext = useForm<InvoiceFormValues>({
    defaultValues,
    mode: 'onChange',
  });

  const isFinal = invoiceStatus === 'FINAL';
  const isReadOnly = invoiceStatus === 'SENT' || isFinal;

  const resetForm = useCallback(() => {
    formContext.reset(defaultValues);
    setInvoiceStatus(null);
    setInvoiceNumber(null);
    setCurrentInvoiceId(null);
  }, [formContext]);

  const loadInvoice = useCallback(
    async (invoiceId: string) => {
      setIsLoading(true);
      try {
        const invoice = await invoiceService.getById(invoiceId);

        setCurrentInvoiceId(invoiceId);
        setInvoiceStatus(invoice.status);
        setInvoiceNumber(invoice.invoiceNumber || null);

        formContext.reset({
          customerId: invoice.customer.companyId,
          invoiceDate: invoice.invoiceDate,
          invoiceType: invoice.invoiceType,
          documentType: invoice.documentType,
          lines: invoice.lines.map((line) => ({
            id: line.id,
            catalogItemId: line.catalogItemId,
            catalogItemName: line.catalogItemName,
            date: line.date,
            description: undefined,
            quantity: String(line.quantity),
            unitPrice: String(line.unitPrice),
            unitOfMeasure: line.unitOfMeasure || '',
            vatCode: line.vatCode || '',
            vatPercentage: String(line.vatPercentage),
            isReverseCharge: line.isReverseCharge ?? false,
            orderReference: line.orderReference || '',
          })),
        });
      } catch (error) {
        console.error('Error loading invoice:', error);
        toastService.error(
          'Er is een fout opgetreden bij het laden van de factuur'
        );
      } finally {
        setIsLoading(false);
      }
    },
    [formContext]
  );

  const buildCreateRequest = (
    values: InvoiceFormValues
  ): CreateInvoiceRequest => {
    const lines: InvoiceLineRequest[] = values.lines
      .filter((line) => line.catalogItemId)
      .map((line) => ({
        id: line.id || undefined,
        date: line.date || values.invoiceDate,
        catalogItemId: line.catalogItemId,
        description: line.description,
        quantity: parseFloat(line.quantity) || 0,
        unitPrice: parseFloat(line.unitPrice) || 0,
        orderReference: line.orderReference || undefined,
      }));

    return {
      invoiceType: values.invoiceType,
      documentType: values.documentType,
      customerId: values.customerId,
      invoiceDate: values.invoiceDate,
      lines,
    };
  };

  const buildUpdateRequest = (
    values: InvoiceFormValues
  ): UpdateInvoiceRequest => {
    const lines: InvoiceLineRequest[] = values.lines
      .filter((line) => line.catalogItemId)
      .map((line) => ({
        id: line.id || undefined,
        date: line.date || values.invoiceDate,
        catalogItemId: line.catalogItemId,
        description: line.description,
        quantity: parseFloat(line.quantity) || 0,
        unitPrice: parseFloat(line.unitPrice) || 0,
        orderReference: line.orderReference || undefined,
      }));

    return {
      customerId: values.customerId,
      invoiceType: values.invoiceType,
      invoiceDate: values.invoiceDate,
      lines,
    };
  };

  const validateInvoiceLines = (values: InvoiceFormValues): boolean => {
    const validLines = values.lines.filter((line) => line.catalogItemId);
    if (validLines.length === 0) {
      toastService.error('Minimaal één factuurregel is verplicht');
      return false;
    }
    return true;
  };

  const handleSubmit = async (): Promise<string | null> => {
    const isValid = await formContext.trigger();
    if (!isValid) return null;

    const values = formContext.getValues();
    if (!validateInvoiceLines(values)) return null;

    setIsSaving(true);
    try {
      const values = formContext.getValues();
      let savedInvoiceId: string;

      if (currentInvoiceId) {
        await invoiceService.update(
          currentInvoiceId,
          buildUpdateRequest(values)
        );
        savedInvoiceId = currentInvoiceId;
      } else {
        const result = await invoiceService.create(buildCreateRequest(values));
        savedInvoiceId = result.invoiceId;
      }

      // Reload the invoice to get the updated state
      await loadInvoice(savedInvoiceId);
      toastService.success('Factuur succesvol opgeslagen');

      return savedInvoiceId;
    } catch (error) {
      console.error('Error saving invoice:', error);
      toastService.error(
        `Er is een fout opgetreden bij het ${currentInvoiceId ? 'bijwerken' : 'aanmaken'} van de factuur`
      );
      return null;
    } finally {
      setIsSaving(false);
    }
  };

  const handleSubmitAndFinalize = async (): Promise<string | null> => {
    const isValid = await formContext.trigger();
    if (!isValid) return null;

    const values = formContext.getValues();
    if (!validateInvoiceLines(values)) return null;

    setIsSaving(true);
    try {
      const values = formContext.getValues();
      let finalizedInvoiceId: string;

      if (currentInvoiceId) {
        // Update existing invoice, then finalize
        await invoiceService.update(
          currentInvoiceId,
          buildUpdateRequest(values)
        );
        await invoiceService.finalize(currentInvoiceId);
        finalizedInvoiceId = currentInvoiceId;
      } else {
        // Create and finalize in one call
        const result = await invoiceService.createCompleted(
          buildCreateRequest(values)
        );
        finalizedInvoiceId = result.invoiceId;
      }

      // Reload the invoice to get the updated state
      await loadInvoice(finalizedInvoiceId);
      toastService.success('Factuur succesvol verwerkt');

      return finalizedInvoiceId;
    } catch (error) {
      console.error('Error saving and finalizing invoice:', error);
      toastService.error(
        `Er is een fout opgetreden bij het verwerken van de factuur`
      );
      return null;
    } finally {
      setIsSaving(false);
    }
  };

  return {
    formContext,
    isLoading,
    isSaving,
    isReadOnly,
    isFinal,
    invoiceNumber,
    invoiceStatus,
    currentInvoiceId,
    loadInvoice,
    resetForm,
    handleSubmit,
    handleSubmitAndFinalize,
  };
};
