import { CompanyControllerApi } from '../client';
import { apiInstance } from './apiInstance';
import {
  AddressRequest,
  CompanyBranchResponse,
  CompanyRequest,
  CompleteCompanyView,
  PagedCompanyResponse,
} from '../client/models';

const companyApi = new CompanyControllerApi(apiInstance.config);
export type Company = CompleteCompanyView;
export type CompanyBranch = CompanyBranchResponse;
export type { PagedCompanyResponse };

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

export interface GetCompaniesParams {
  includeBranches?: boolean;
  role?: string;
  query?: string;
  page?: number;
  size?: number;
}

export const companyService = {
  /**
   * Get paginated companies with optional search and filtering.
   * Use this for the main company list with pagination.
   */
  getAll: (params: GetCompaniesParams = {}): Promise<PagedCompanyResponse> => {
    const {
      includeBranches = false,
      role,
      query,
      page = 0,
      size = 10,
    } = params;
    return companyApi
      .getCompanies(includeBranches, role, query, page, size)
      .then((r) => r.data);
  },

  /**
   * Get a single company by ID.
   */
  getById: (id: string): Promise<Company> => {
    return companyApi.getById1(id).then((r) => r.data);
  },

  /**
   * Get all companies as a simple list (no pagination).
   * Use this for dropdowns and selects where you need all companies.
   * Fetches a large page size to get all companies in one request.
   */
  getAllAsList: (
    includeBranches: boolean = false,
    role?: string,
    query?: string
  ): Promise<Company[]> => {
    // Fetch a large page to get all companies
    return companyApi
      .getCompanies(includeBranches, role, query, 0, 1000)
      .then((r) => r.data.content);
  },
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

  /**
   * Get the tenant company (the company with isTenantCompany = true).
   * Returns the first company found with the tenant flag.
   */
  getTenantCompany: async (): Promise<Company | null> => {
    const response = await companyApi.getCompanies(false, undefined, undefined, 0, 1000);
    const tenantCompany = response.data.content.find((c) => c.isTenantCompany);
    return tenantCompany || null;
  },
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
