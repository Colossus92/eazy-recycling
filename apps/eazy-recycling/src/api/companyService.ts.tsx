import { Company, CompanyBranch, DeleteResponse } from '@/types/api.ts';
import { http } from '@/api/http.ts';

export type CompanyResponse = Company;

export const companyService = {
  list: (includeBranches: boolean = false) =>
    http
      .get<Company[]>('/companies', { params: { includeBranches } })
      .then((r) => r.data),
  create: (c: Omit<Company, 'id'>) =>
    http.post<CompanyResponse>('/companies', c).then((r) => r.data),
  update: (c: Company) =>
    http.put<CompanyResponse>(`/companies/${c.id}`, c).then((r) => r.data),
  remove: (c: Company) =>
    http
      .delete<DeleteResponse>(`/companies/${c.id}`)
      .then(() => ({ success: true }) as DeleteResponse)
      .catch(() => ({ success: false }) as DeleteResponse),
  createBranch: (companyId: string, branch: CompanyBranch) =>
    http
      .post<CompanyBranch>(`/companies/${companyId}/branches`, branch.address)
      .then((r) => r.data),
  updateBranch: (companyId: string, branchId: string, branch: CompanyBranch) =>
    http
      .put<CompanyBranch>(
        `/companies/${companyId}/branches/${branchId}`,
        branch.address
      )
      .then((r) => r.data),
  removeBranch: (companyId: string, branchId: string) =>
    http
      .delete<DeleteResponse>(`/companies/${companyId}/branches/${branchId}`)
      .then(() => ({ success: true }) as DeleteResponse)
      .catch(() => ({ success: false }) as DeleteResponse),
};
