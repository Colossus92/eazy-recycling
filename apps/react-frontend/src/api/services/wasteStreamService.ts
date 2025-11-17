import { apiInstance } from "./apiInstance";
import { WasteStreamControllerApi, WasteStreamRequest } from "../client";
import { WasteStreamDto } from "../client/models/waste-stream-dto";

const wasteStreamApi = new WasteStreamControllerApi(apiInstance.config)
export type WasteStream = WasteStreamDto;

export const wasteStreamService = {
    getAll: (consignor?: string, status?: string) => wasteStreamApi.getWasteStreams(consignor, status).then((r) => r.data),
    getByNumber: (number: string) => wasteStreamApi.getWasteStreamByNumber(number).then((r) => r.data),
    delete: (id: string) => wasteStreamApi._delete(id),
    
    // Draft endpoints
    createDraft: (wasteStreamRequest: WasteStreamRequest) => wasteStreamApi.create1(wasteStreamRequest).then((r) => r.data),
    updateDraft: (wasteStreamNumber: string, wasteStreamRequest: WasteStreamRequest) => wasteStreamApi.update1(wasteStreamNumber, wasteStreamRequest).then((r) => r.data),
    
    // Active/Validated endpoints
    createAndValidate: (wasteStreamRequest: WasteStreamRequest) => wasteStreamApi.createAndValidate(wasteStreamRequest).then((r) => r.data),
    updateAndValidate: (wasteStreamNumber: string, wasteStreamRequest: WasteStreamRequest) => wasteStreamApi.updateAndValidate(wasteStreamNumber, wasteStreamRequest).then((r) => r.data),
};
