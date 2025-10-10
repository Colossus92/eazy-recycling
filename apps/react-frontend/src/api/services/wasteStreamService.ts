import { apiInstance } from "./apiInstance";
import { WasteStreamControllerApi } from "../client";
import { WasteStreamDto } from "../client/models/waste-stream-dto";
import { CreateWasteStreamRequest } from "../client/models/create-waste-stream-request";

const wasteStreamApi = new WasteStreamControllerApi(apiInstance.config)
export type WasteStream = WasteStreamDto;

export const wasteStreamService = {
    getAll: () => wasteStreamApi.getWasteStreams().then((r) => r.data),
    create: (wasteStreamRequest: CreateWasteStreamRequest) => wasteStreamApi.create1(wasteStreamRequest).then((r) => r.data),
    update: (wasteStream: WasteStream) => {}, //TODO implement
    delete: (id: string) => {}, //TODO implement
};
