import { useCallback, useState } from 'react';
import { useForm } from 'react-hook-form';
import { InvoiceControllerApi, CreateInvoiceRequest, UpdateInvoiceRequest, InvoiceLineRequest } from '@/api/client';
import { apiInstance } from '@/api/services/apiInstance';
import { format } from 'date-fns';

const invoiceApi = new InvoiceControllerApi(apiInstance.config);

export interface InvoiceLineFormValue {
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

  const formContext = useForm<InvoiceFormValues>({
    defaultValues,
    mode: 'onChange',
  });

  const resetForm = useCallback(() => {
    formContext.reset(defaultValues);
  }, [formContext]);

  const loadInvoice = useCallback(async (invoiceId: number) => {
    setIsLoading(true);
    try {
      const response = await invoiceApi.getById(invoiceId);
      const invoice = response.data;

      formContext.reset({
        customerId: invoice.customer.companyId,
        invoiceDate: invoice.invoiceDate,
        invoiceType: invoice.invoiceType,
        documentType: invoice.documentType,
        lines: invoice.lines.map((line) => ({
          catalogItemId: String(line.catalogItemId),
          catalogItemName: line.catalogItemName,
          date: line.date,
          description: line.description,
          quantity: String(line.quantity),
          unitPrice: String(line.unitPrice),
          unitOfMeasure: line.unitOfMeasure || '',
          vatPercentage: String(line.vatPercentage),
          orderReference: line.orderReference || '',
        })),
      });
    } catch (error) {
      console.error('Error loading invoice:', error);
    } finally {
      setIsLoading(false);
    }
  }, [formContext]);

  const handleSubmit = async (invoiceId?: number): Promise<boolean> => {
    const isValid = await formContext.trigger();
    if (!isValid) return false;

    setIsSaving(true);
    try {
      const values = formContext.getValues();
      
      const lines: InvoiceLineRequest[] = values.lines
        .filter((line) => line.catalogItemId)
        .map((line) => ({
          date: line.date || values.invoiceDate,
          catalogItemId: parseInt(line.catalogItemId, 10),
          description: line.description,
          quantity: parseFloat(line.quantity) || 0,
          unitPrice: parseFloat(line.unitPrice) || 0,
          orderReference: line.orderReference || undefined,
        }));

      if (invoiceId) {
        const updateRequest: UpdateInvoiceRequest = {
          invoiceDate: values.invoiceDate,
          lines,
        };
        await invoiceApi.update2(invoiceId, updateRequest);
      } else {
        const createRequest: CreateInvoiceRequest = {
          invoiceType: values.invoiceType,
          documentType: values.documentType,
          customerId: values.customerId,
          invoiceDate: values.invoiceDate,
          lines,
        };
        await invoiceApi.create2(createRequest);
      }
      
      return true;
    } catch (error) {
      console.error('Error saving invoice:', error);
      return false;
    } finally {
      setIsSaving(false);
    }
  };

  return {
    formContext,
    isLoading,
    isSaving,
    loadInvoice,
    resetForm,
    handleSubmit,
  };
};
