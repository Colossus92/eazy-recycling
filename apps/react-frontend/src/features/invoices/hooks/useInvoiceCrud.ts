import { InvoiceView } from '@/api/client';
import { useCallback, useEffect, useState } from 'react';
import { invoiceService } from '@/api/services/invoiceService';

export const useInvoiceCrud = () => {
  const [items, setItems] = useState<InvoiceView[]>([]);
  const [isFetching, setIsFetching] = useState(true);
  const [error, setError] = useState<Error | null>(null);
  const [query, setQuery] = useState('');

  // Form state
  const [formIsOpen, setFormIsOpen] = useState(false);
  const [formItem, setFormItem] = useState<InvoiceView | undefined>(undefined);

  // Deletion state
  const [deletionItem, setDeletionItem] = useState<string | undefined>(
    undefined
  );

  const fetchInvoices = useCallback(async () => {
    setIsFetching(true);
    try {
      const data = await invoiceService.getAll();
      let filteredItems = data;

      if (query) {
        const lowerQuery = query.toLowerCase();
        filteredItems = filteredItems.filter(
          (item) =>
            item.invoiceNumber?.toLowerCase().includes(lowerQuery) ||
            item.customerName.toLowerCase().includes(lowerQuery)
        );
      }

      setItems(filteredItems);
      setError(null);
    } catch (err) {
      setError(err as Error);
    } finally {
      setIsFetching(false);
    }
  }, [query]);

  useEffect(() => {
    fetchInvoices();
  }, [fetchInvoices]);

  const openForCreate = () => {
    setFormItem(undefined);
    setFormIsOpen(true);
  };

  const openForEdit = (item: InvoiceView) => {
    setFormItem(item);
    setFormIsOpen(true);
  };

  const closeForm = () => {
    setFormIsOpen(false);
    setFormItem(undefined);
  };

  const completeForm = () => {
    fetchInvoices();
  };

  const initiateDelete = (id: string) => {
    setDeletionItem(id);
  };

  const cancelDelete = () => {
    setDeletionItem(undefined);
  };

  const confirmDelete = async () => {
    if (deletionItem) {
      await invoiceService.delete(deletionItem);
      setDeletionItem(undefined);
      fetchInvoices();
    }
  };

  return {
    read: {
      items,
      isFetching,
      setQuery,
      errorHandling: {
        error,
        reset: () => setError(null),
      },
    },
    form: {
      isOpen: formIsOpen,
      item: formItem,
      openForCreate,
      openForEdit,
      close: closeForm,
      complete: completeForm,
    },
    deletion: {
      item: deletionItem,
      initiate: initiateDelete,
      cancel: cancelDelete,
      confirm: confirmDelete,
    },
  };
};
