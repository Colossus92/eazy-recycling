import { TransportControllerApi, TransportFinishedRequest } from "../client"
import { apiInstance } from "./apiInstance"
import { CreateContainerTransportRequest } from "../client/models/create-container-transport-request"
import { CreateWasteTransportRequest } from "../client/models/create-waste-transport-request"


const transportApi = new TransportControllerApi(apiInstance.config)

export const transportService = {
    deleteTransport: (id: string) => transportApi.deleteTransport(id),
    getTransportById: (id: string) => transportApi.getTransportById(id),
    updateContainerTransport: (id: string, data: CreateContainerTransportRequest) => transportApi.updateContainerTransport(id, data),
    createContainerTransport: (data: CreateContainerTransportRequest) => transportApi.createContainerTransport(data),
    createWasteTransport: (data: CreateWasteTransportRequest) => transportApi.createWasteTransport(data),
    updateWasteTransport: (id: string, data: CreateWasteTransportRequest) => transportApi.updateWasteTransport(id, data),
    reportFinished: (id: string, data: TransportFinishedRequest) => transportApi.markTransportAsFinished(id, data),
}