import { apiInstance } from "./apiInstance";
import { WeightTicketControllerApi } from "../client";
import { CreateWeightTicketRequest } from "../client/models/create-weight-ticket-request";

const weightTicketApi = new WeightTicketControllerApi(apiInstance.config)

export const weightTicketService = {
    getAll: () => weightTicketApi.getWeightTickets().then((r) => r.data),
    create: (weightTicketRequest: CreateWeightTicketRequest) => weightTicketApi.create(weightTicketRequest).then((r) => r.data),
};
