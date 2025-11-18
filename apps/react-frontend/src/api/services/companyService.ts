import { CompanyControllerApi } from '../client';
import { apiInstance } from './apiInstance';
import {
  AddressRequest,
  CompanyBranchResponse,
  CompanyRequest,
  CompleteCompanyView,
} from '../client/models';

const companyApi = new CompanyControllerApi(apiInstance.config);
export type Company = CompleteCompanyView;
export type CompanyBranch = CompanyBranchResponse;

const mapCompanyToCompanyRequest = (
  company: Omit<Company, 'id'>
): CompanyRequest => {
  return {
    ...company,
    address: {
      ...company.address,
      streetName: company.address.street,
      buildingNumber: company.address.houseNumber,
      buildingNumberAddition: company.address.houseNumberAddition,
    },
  };
};

export const companyService = {
  getAll: (includeBranches: boolean = false, role?: string) =>
    companyApi.getCompanies(includeBranches, role).then((r) => r.data),
  create: (c: Omit<Company, 'id'>, restoreCompanyId?: string) => {
    return companyApi
      .createCompany(mapCompanyToCompanyRequest(c), restoreCompanyId)
      .then((r) => r.data);
  },
  update: (company: Company) => {
    if (!company.id) {
      throw new Error('Ongeldige bedrijfsgegevens');
    }

    return companyApi
      .updateCompany(company.id, mapCompanyToCompanyRequest(company))
      .then((r) => r.data);
  },
  delete: (id: string) => companyApi.deleteCompany(id),
  createBranch: (companyId: string, companyBranch: CompanyBranch) => {
    const companyBranchRequest: AddressRequest =
      mapToCompanyBranchRequest(companyBranch);
    return companyApi
      .createBranch(companyId, companyBranchRequest)
      .then((r) => r.data);
  },
  updateBranch: (
    companyId: string,
    branchId: string,
    companyBranch: CompanyBranch
  ) => {
    const companyBranchRequest: AddressRequest =
      mapToCompanyBranchRequest(companyBranch);
    return companyApi
      .updateBranch(companyId, branchId, companyBranchRequest)
      .then((r) => r.data);
  },
  removeBranch: (companyId: string, branchId: string) =>
    companyApi.deleteBranch(companyId, branchId).then((r) => r.data),
};

function mapToCompanyBranchRequest(
  companyBranch: CompanyBranchResponse
): AddressRequest {
  if (
    !companyBranch.address ||
    !companyBranch.address.street ||
    !companyBranch.address.houseNumber ||
    !companyBranch.address.postalCode ||
    !companyBranch.address.city
  ) {
    throw new Error('Ongeldige vestigingsgegevens');
  }

  return {
    streetName: companyBranch.address.street,
    buildingNumber: companyBranch.address.houseNumber,
    buildingNumberAddition: companyBranch.address.houseNumberAddition,
    postalCode: companyBranch.address.postalCode,
    city: companyBranch.address.city,
    country: 'Nederland',
  };
}
