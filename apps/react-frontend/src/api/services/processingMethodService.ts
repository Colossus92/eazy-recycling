import { ProcessingMethod, ProcessingMethodControllerApi } from "../client";
import { apiInstance } from "./apiInstance";


const processingMethodApi = new ProcessingMethodControllerApi(apiInstance.config);

export const processingMethodService = {
    getAll: () => processingMethodApi.getProcessingMethods().then((r) => r.data),
    create: (processingMethod: ProcessingMethod) => processingMethodApi.createProcessingMethod(processingMethod).then((r) => r.data),
    update: (processingMethod: ProcessingMethod) => processingMethodApi.updateProcessingMethod(processingMethod.code, processingMethod).then((r) => r.data),
    delete: (processingMethod: ProcessingMethod) => processingMethodApi.deleteProcessingMethod(processingMethod.code)
}