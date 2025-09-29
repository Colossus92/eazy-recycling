import { SignatureControllerApi } from "../client"
import { apiInstance } from "./apiInstance"
import { CreateSignatureRequest } from "../client/models/create-signature-request"

const signatureApi = new SignatureControllerApi(apiInstance.config)

export const signatureService = {
    getSignatureStatus: (id: string) => signatureApi.getSignatureStatuses(id).then(response => response.data),
    saveSignature: (id: string, data: CreateSignatureRequest) => signatureApi.saveSignature(id, data),
}

