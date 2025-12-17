import {
  InvoiceControllerApi,
  CreateInvoiceRequest,
  UpdateInvoiceRequest,
  InvoiceView,
  InvoiceDetailView,
  InvoiceResult,
} from '../client';
import { apiInstance } from './apiInstance';

const invoiceApi = new InvoiceControllerApi(apiInstance.config);

export const invoiceService = {
  getAll: (): Promise<InvoiceView[]> => {
    return invoiceApi.getAll().then((r) => r.data);
  },

  getById: (id: number): Promise<InvoiceDetailView> => {
    return invoiceApi.getById(id).then((r) => r.data);
  },

  create: (request: CreateInvoiceRequest): Promise<InvoiceResult> => {
    return invoiceApi.create2(request).then((r) => r.data);
  },

  createCompleted: (request: CreateInvoiceRequest): Promise<InvoiceResult> => {
    return invoiceApi.createCompleted1(request).then((r) => r.data);
  },

  update: (id: number, request: UpdateInvoiceRequest): Promise<InvoiceResult> => {
    return invoiceApi.update2(id, request).then((r) => r.data);
  },

  finalize: (id: number): Promise<InvoiceResult> => {
    return invoiceApi.finalize(id).then((r) => r.data);
  },

  delete: (id: number): Promise<void> => {
    return invoiceApi._delete(id).then((r) => r.data);
  },
};
