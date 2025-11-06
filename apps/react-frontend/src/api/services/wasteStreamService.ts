import { apiInstance } from "./apiInstance";
import { WasteStreamControllerApi, WasteStreamRequest } from "../client";
import { WasteStreamDto } from "../client/models/waste-stream-dto";
import { WasteStreamValidationResponse } from "../types/validation";

const wasteStreamApi = new WasteStreamControllerApi(apiInstance.config)
export type WasteStream = WasteStreamDto;

export const wasteStreamService = {
    getAll: () => wasteStreamApi.getWasteStreams().then((r) => r.data),
    getByNumber: (number: string) => wasteStreamApi.getWasteStreamByNumber(number).then((r) => r.data),
    create: (wasteStreamRequest: WasteStreamRequest) => wasteStreamApi.create1(wasteStreamRequest).then((r) => r.data),
    update: (wasteStreamNumber: string, wasteStreamRequest: WasteStreamRequest) => wasteStreamApi.update1(wasteStreamNumber, wasteStreamRequest).then((r) => r.data),
    delete: (id: string) => wasteStreamApi._delete(id),
    
    // Draft endpoints
    createDraft: (wasteStreamRequest: WasteStreamRequest) => 
        apiInstance.axios.post<{ wasteStreamNumber: string }>('/waste-streams/concept', wasteStreamRequest)
            .then((r) => r.data),
    updateDraft: (wasteStreamNumber: string, wasteStreamRequest: WasteStreamRequest) => 
        apiInstance.axios.put<void>(`/waste-streams/${wasteStreamNumber}/concept`, wasteStreamRequest)
            .then((r) => r.data),
    
    // Active/Validated endpoints
    createAndValidate: (wasteStreamRequest: WasteStreamRequest) => wasteStreamApi.
        apiInstance.axios.post<WasteStreamValidationResponse>('/waste-streams/active', wasteStreamRequest)
            .then((r) => r.data),
    updateAndValidate: (wasteStreamNumber: string, wasteStreamRequest: WasteStreamRequest) => 
        apiInstance.axios.put<WasteStreamValidationResponse>(`/waste-streams/${wasteStreamNumber}/active`, wasteStreamRequest)
            .then((r) => r.data),
};
