import { apiInstance } from './apiInstance';
import {
  WasteStreamControllerApi,
  WasteStreamListView,
  WasteStreamRequest,
} from '../client';

const wasteStreamApi = new WasteStreamControllerApi(apiInstance.config);
export type WasteStream = WasteStreamListView;

export const wasteStreamService = {
  getAll: (consignor?: string, status?: string) =>
    wasteStreamApi.getWasteStreams(consignor, status).then((r) => r.data),
  getByNumber: (number: string) =>
    wasteStreamApi.getWasteStreamByNumber(number).then((r) => r.data),
  getCompatible: (wasteStreamNumber: string) =>
    wasteStreamApi
      .findCompatibleWasteStreams(wasteStreamNumber)
      .then((r) => r.data),
  delete: (id: string) => wasteStreamApi.delete1(id),

  // Draft endpoints
  createDraft: (wasteStreamRequest: WasteStreamRequest) =>
    wasteStreamApi.create1(wasteStreamRequest).then((r) => r.data),
  updateDraft: (
    wasteStreamNumber: string,
    wasteStreamRequest: WasteStreamRequest
  ) =>
    wasteStreamApi
      .update1(wasteStreamNumber, wasteStreamRequest)
      .then((r) => r.data),

  // Active/Validated endpoints
  createAndValidate: (wasteStreamRequest: WasteStreamRequest) =>
    wasteStreamApi.createAndValidate(wasteStreamRequest).then((r) => r.data),
  updateAndValidate: (
    wasteStreamNumber: string,
    wasteStreamRequest: WasteStreamRequest
  ) =>
    wasteStreamApi
      .updateAndValidate(wasteStreamNumber, wasteStreamRequest)
      .then((r) => r.data),

  // Weight tickets by waste stream
  getWeightTicketsByWasteStream: (wasteStreamNumber: string) =>
    wasteStreamApi
      .getWeightTicketsByWasteStream(wasteStreamNumber)
      .then((r) => r.data),
};
