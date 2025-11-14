import { apiInstance } from "./apiInstance";
import { AmiceControllerApi, Pageable } from "../client";

const lmaDeclarationApi = new AmiceControllerApi(apiInstance.config);

export const lmaDeclarationService = {
    getAll: (pageable: Pageable = { page: 0, size: 1000 }) => 
        lmaDeclarationApi.getDeclarations(pageable).then((r) => r.data),
};
