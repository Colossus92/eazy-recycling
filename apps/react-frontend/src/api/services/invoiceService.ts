import {
  CreateInvoiceRequest,
  InvoiceControllerApi,
  InvoiceDetailView,
  InvoiceResult,
  InvoiceView,
  SendInvoiceRequest,
  UpdateInvoiceRequest,
} from '../client';
import { apiInstance } from './apiInstance';

const invoiceApi = new InvoiceControllerApi(apiInstance.config);

export const invoiceService = {
  getAll: (): Promise<InvoiceView[]> => {
    return invoiceApi.getAll().then((r) => r.data);
  },

  getById: (id: string): Promise<InvoiceDetailView> => {
    return invoiceApi.getById(id).then((r) => r.data);
  },

  create: (request: CreateInvoiceRequest): Promise<InvoiceResult> => {
    return invoiceApi.create2(request).then((r) => r.data);
  },

  createCompleted: (request: CreateInvoiceRequest): Promise<InvoiceResult> => {
    return invoiceApi.createCompleted1(request).then((r) => r.data);
  },

  update: (
    id: string,
    request: UpdateInvoiceRequest
  ): Promise<InvoiceResult> => {
    return invoiceApi.update2(id, request).then((r) => r.data);
  },

  finalize: (id: string): Promise<InvoiceResult> => {
    return invoiceApi.finalize(id).then((r) => r.data);
  },

  send: (id: string, request: SendInvoiceRequest): Promise<InvoiceResult> => {
    return invoiceApi.send(id, request).then((r) => r.data);
  },

  delete: (id: string): Promise<void> => {
    return invoiceApi._delete(id).then((r) => r.data);
  },
};
