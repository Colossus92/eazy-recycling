import { apiInstance } from "./apiInstance";
import { WeightTicketControllerApi } from "../client";
import { WeightTicketRequest } from "../client/models/weight-ticket-request";

const weightTicketApi = new WeightTicketControllerApi(apiInstance.config)

export const weightTicketService = {
    getAll: () => weightTicketApi.getWeightTickets().then((r) => r.data),
    getByNumber: (weightTicketNumber: number) => weightTicketApi.getWeightTicketByNumber(weightTicketNumber).then((r) => r.data),
    create: (weightTicketRequest: WeightTicketRequest) => weightTicketApi.create(weightTicketRequest).then((r) => r.data),
    update: (weightTicketNumber: number, weightTicketRequest: WeightTicketRequest) => weightTicketApi.update(weightTicketNumber, weightTicketRequest).then((r) => r.data),
    delete: (weightTicketId: number) => weightTicketApi._delete(weightTicketId),
};
