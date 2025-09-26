import { apiInstance } from "./apiInstance";
import { WasteStreamControllerApi } from "../client";
import { WasteStreamDto } from "../client/models/waste-stream-dto";


const wasteStreamApi = new WasteStreamControllerApi(apiInstance.config)
export type WasteStream = WasteStreamDto;

export const wasteStreamService = {
    getAll: () => wasteStreamApi.getWasteStreams().then((r) => r.data),
    create: (wasteStream: Omit<WasteStream, 'id'>) => wasteStreamApi.createWasteStream(wasteStream).then((r) => r.data),
    update: (wasteStream: WasteStream) => wasteStreamApi.updateWasteStream(wasteStream.number, wasteStream).then((r) => r.data),
    delete: (id: string) => wasteStreamApi.deleteWasteStream(id).then((r) => r.data),
};
