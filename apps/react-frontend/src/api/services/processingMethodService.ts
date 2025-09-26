import { ProcessingMethodControllerApi } from "../client";
import { apiInstance } from "./apiInstance";


const processingMethodApi = new ProcessingMethodControllerApi(apiInstance.config);

export const processingMethodService = {
    getAll: () => processingMethodApi.getProcessingMethods().then((r) => r.data),
}