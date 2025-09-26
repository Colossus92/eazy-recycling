import { CompanyControllerApi } from "../client";
import { apiInstance } from "./apiInstance";
import { CompanyRequest, CompanyResponse, CompanyBranchResponse, AddressRequest } from "../client/models";

const companyApi = new CompanyControllerApi(apiInstance.config)
export type Company = CompanyResponse;
export type CompanyBranch = CompanyBranchResponse

export const companyService = {
    getAll: (includeBranches: boolean = false) => companyApi.getCompanies(includeBranches).then((r) => r.data),
    create: (c: Omit<Company, 'id'>) => {
        if (!c.name || !c.address || !c.address.streetName || !c.address.buildingNumber || !c.address.postalCode || !c.address.city) {
            throw new Error('Ongeldige bedrijfsgegevens');
        }

        const companyRequest: CompanyRequest = {
            name: c.name,
            address: {
                streetName: c.address.streetName,
                buildingNumber: c.address.buildingNumber,
                postalCode: c.address.postalCode,
                city: c.address.city,
                country: 'Nederland',
            },
            chamberOfCommerceId: c.chamberOfCommerceId,
            vihbId: c.vihbId,
        }

        return companyApi.createCompany(companyRequest).then((r) => r.data)
    },
    update: (company: Company) => {
        if (!company.id) {
            throw new Error('Ongeldige bedrijfsgegevens');
        }


        return companyApi.updateCompany(company.id, company).then((r) => r.data)
    },
    delete: (id: string) => companyApi.deleteCompany(id),
    createBranch: (companyId: string, companyBranch: CompanyBranch) => {
        const companyBranchRequest: AddressRequest = mapToCompanyBranchRequest(companyBranch)
        return companyApi.createBranch(companyId, companyBranchRequest).then((r) => r.data)
    },
    updateBranch: (companyId: string, branchId: string, companyBranch: CompanyBranch) => {
        const companyBranchRequest: AddressRequest = mapToCompanyBranchRequest(companyBranch)
        return companyApi.updateBranch(companyId, branchId, companyBranchRequest).then((r) => r.data)
    },
    removeBranch: (companyId: string, branchId: string) => companyApi.deleteBranch(companyId, branchId).then((r) => r.data),
};

function mapToCompanyBranchRequest(companyBranch: CompanyBranchResponse): AddressRequest {
    if (!companyBranch.address 
        || !companyBranch.address.streetName 
        || !companyBranch.address.buildingNumber 
        || !companyBranch.address.postalCode 
        || !companyBranch.address.city ) {
        throw new Error('Ongeldige vestigingsgegevens');
    }
    
    return {
        streetName: companyBranch.address.streetName,
        buildingNumber: companyBranch.address.buildingNumber,
        postalCode: companyBranch.address.postalCode,
        city: companyBranch.address.city,
        country: 'Nederland',
    };
}

