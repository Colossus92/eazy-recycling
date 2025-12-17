import { useCallback, useState } from 'react';
import { useForm } from 'react-hook-form';
import { CreateInvoiceRequest, UpdateInvoiceRequest, InvoiceLineRequest } from '@/api/client';
import { invoiceService } from '@/api/services/invoiceService';
import { toastService } from '@/components/ui/toast/toastService';
import { format } from 'date-fns';

export interface InvoiceLineFormValue {
  id?: number;
  catalogItemId: string;
  catalogItemName: string;
  date: string;
  description: string;
  quantity: string;
  unitPrice: string;
  unitOfMeasure: string;
  vatPercentage: string;
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
  lines: [],
};

export const useInvoiceFormHook = () => {
  const [isLoading, setIsLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [invoiceStatus, setInvoiceStatus] = useState<string | null>(null);
  const [invoiceNumber, setInvoiceNumber] = useState<string | null>(null);

  const formContext = useForm<InvoiceFormValues>({
    defaultValues,
    mode: 'onChange',
  });

  const isReadOnly = invoiceStatus === 'FINAL';

  const resetForm = useCallback(() => {
    formContext.reset(defaultValues);
    setInvoiceStatus(null);
    setInvoiceNumber(null);
  }, [formContext]);

  const loadInvoice = useCallback(async (invoiceId: number) => {
    setIsLoading(true);
    try {
      const invoice = await invoiceService.getById(invoiceId);

      setInvoiceStatus(invoice.status);
      setInvoiceNumber(invoice.invoiceNumber || null);

      formContext.reset({
        customerId: invoice.customer.companyId,
        invoiceDate: invoice.invoiceDate,
        invoiceType: invoice.invoiceType,
        documentType: invoice.documentType,
        lines: invoice.lines.map((line) => ({
          id: line.id,
          catalogItemId: String(line.catalogItemId),
          catalogItemName: line.catalogItemName,
          date: line.date,
          description: undefined,
          quantity: String(line.quantity),
          unitPrice: String(line.unitPrice),
          unitOfMeasure: line.unitOfMeasure || '',
          vatPercentage: String(line.vatPercentage),
          orderReference: line.orderReference || '',
        })),
      });
    } catch (error) {
      console.error('Error loading invoice:', error);
      toastService.error('Er is een fout opgetreden bij het laden van de factuur');
    } finally {
      setIsLoading(false);
    }
  }, [formContext]);

  const buildCreateRequest = (values: InvoiceFormValues): CreateInvoiceRequest => {
    const lines: InvoiceLineRequest[] = values.lines
      .filter((line) => line.catalogItemId)
      .map((line) => ({
        id: typeof line.id === 'number' ? line.id : undefined,
        date: line.date || values.invoiceDate,
        catalogItemId: parseInt(line.catalogItemId, 10),
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

  const buildUpdateRequest = (values: InvoiceFormValues): UpdateInvoiceRequest => {
    const lines: InvoiceLineRequest[] = values.lines
      .filter((line) => line.catalogItemId)
      .map((line) => ({
        id: typeof line.id === 'number' ? line.id : undefined,
        date: line.date || values.invoiceDate,
        catalogItemId: parseInt(line.catalogItemId, 10),
        description: line.description,
        quantity: parseFloat(line.quantity) || 0,
        unitPrice: parseFloat(line.unitPrice) || 0,
        orderReference: line.orderReference || undefined,
      }));

    return {
      invoiceDate: values.invoiceDate,
      lines,
    };
  };

  const handleSubmit = async (invoiceId?: number): Promise<boolean> => {
    const isValid = await formContext.trigger();
    if (!isValid) return false;

    setIsSaving(true);
    try {
      const values = formContext.getValues();

      if (invoiceId) {
        await invoiceService.update(invoiceId, buildUpdateRequest(values));
      } else {
        await invoiceService.create(buildCreateRequest(values));
      }
      
      return true;
    } catch (error) {
      console.error('Error saving invoice:', error);
      toastService.error(
        `Er is een fout opgetreden bij het ${invoiceId ? 'bijwerken' : 'aanmaken'} van de factuur`
      );
      return false;
    } finally {
      setIsSaving(false);
    }
  };

  const handleSubmitAndFinalize = async (invoiceId?: number): Promise<boolean> => {
    const isValid = await formContext.trigger();
    if (!isValid) return false;

    setIsSaving(true);
    try {
      const values = formContext.getValues();

      if (invoiceId) {
        // Update existing invoice, then finalize
        await invoiceService.update(invoiceId, buildUpdateRequest(values));
        await invoiceService.finalize(invoiceId);
      } else {
        // Create and finalize in one call
        await invoiceService.createCompleted(buildCreateRequest(values));
      }
      
      return true;
    } catch (error) {
      console.error('Error saving and finalizing invoice:', error);
      toastService.error(
        `Er is een fout opgetreden bij het verwerken van de factuur`
      );
      return false;
    } finally {
      setIsSaving(false);
    }
  };

  return {
    formContext,
    isLoading,
    isSaving,
    isReadOnly,
    invoiceNumber,
    loadInvoice,
    resetForm,
    handleSubmit,
    handleSubmitAndFinalize,
  };
};
