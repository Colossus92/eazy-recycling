import { apiInstance } from "./apiInstance";
import { WasteStreamControllerApi, WasteStreamRequest } from "../client";
import { WasteStreamDto } from "../client/models/waste-stream-dto";

const wasteStreamApi = new WasteStreamControllerApi(apiInstance.config)
export type WasteStream = WasteStreamDto;

export const wasteStreamService = {
    getAll: () => wasteStreamApi.getWasteStreams().then((r) => r.data),
    getByNumber: (number: string) => wasteStreamApi.getWasteStreamByNumber(number).then((r) => r.data),
    create: (wasteStreamRequest: WasteStreamRequest) => wasteStreamApi.create1(wasteStreamRequest).then((r) => r.data),
    update: (wasteStream: WasteStreamRequest) => wasteStreamApi.update(wasteStream.wasteStreamNumber, wasteStream),
    delete: (id: string) => wasteStreamApi._delete(id),
};
