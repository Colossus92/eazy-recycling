import { apiInstance } from "./apiInstance";
import { WeightTicketControllerApi } from "../client";
import { WeightTicketRequest } from "../client/models/weight-ticket-request";
import { CancelWeightTicketRequest } from "../client/models/cancel-weight-ticket-request";
import { SplitWeightTicketRequest } from "../client/models/split-weight-ticket-request";

const weightTicketApi = new WeightTicketControllerApi(apiInstance.config)

export const weightTicketService = {
    getAll: () => weightTicketApi.getWeightTickets().then((r) => r.data),
    getByNumber: (weightTicketNumber: number) => weightTicketApi.getWeightTicketByNumber(weightTicketNumber).then((r) => r.data),
    create: (weightTicketRequest: WeightTicketRequest) => weightTicketApi.create(weightTicketRequest).then((r) => r.data),
    update: (weightTicketNumber: number, weightTicketRequest: WeightTicketRequest) => weightTicketApi.update(weightTicketNumber, weightTicketRequest).then((r) => r.data),
    cancel: (weightTicketId: number, cancelWeightTicketRequest: CancelWeightTicketRequest) => weightTicketApi.cancel(weightTicketId, cancelWeightTicketRequest).then((r) => r.data),
    split: (weightTicketId: number, splitWeightTicketRequest: SplitWeightTicketRequest) => weightTicketApi.split(weightTicketId, splitWeightTicketRequest).then((r) => r.data),
};
