import { FieldValues, Path, useFormContext } from 'react-hook-form';
import {
  AsyncPaginateSelectFormField,
  LoadOptionsParams,
  Option,
  PaginatedResult,
} from './selectfield/AsyncPaginateSelectFormField';
import { companyService } from '@/api/services/companyService';
import { useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';

interface CompanySelectFormFieldProps<TFieldValues extends FieldValues> {
  title: string;
  placeholder: string;
  name: Path<TFieldValues>;
  rules: any;
  disabled?: boolean;
  role?: string;
  pageSize?: number;
}

export const CompanySelectFormField = <TFieldValues extends FieldValues>({
  title,
  placeholder,
  name,
  rules,
  disabled = false,
  role = undefined,
  pageSize = 20,
}: CompanySelectFormFieldProps<TFieldValues>) => {
  const formContext = useFormContext<TFieldValues>();
  const queryClient = useQueryClient();

  const loadOptions = useCallback(
    async ({
      inputValue,
      page,
    }: LoadOptionsParams): Promise<PaginatedResult> => {
      const response = await companyService.getAll({
        query: inputValue || undefined,
        page,
        size: pageSize,
        role,
      });
      return {
        options: response.content.map((company) => ({
          value: company.id || '',
          label: company.name,
        })),
        hasMore: page < response.totalPages - 1,
      };
    },
    [role, pageSize]
  );

  // Load a single company by ID for initial form population (with caching)
  const loadOptionByValue = useCallback(
    async (companyId: string): Promise<Option | null> => {
      try {
        // Use React Query's fetchQuery for caching - returns cached data if available
        const company = await queryClient.fetchQuery({
          queryKey: ['company', companyId],
          queryFn: () => companyService.getById(companyId),
          staleTime: 5 * 60 * 1000, // Cache for 5 minutes
        });
        return {
          value: company.id || '',
          label: company.name,
        };
      } catch {
        return null;
      }
    },
    [queryClient]
  );

  return (
    <AsyncPaginateSelectFormField
      title={title}
      placeholder={placeholder}
      loadOptions={loadOptions}
      loadOptionByValue={loadOptionByValue}
      testId="company-select"
      formHook={{
        register: formContext.register,
        name,
        rules,
        errors: formContext.formState.errors,
        control: formContext.control,
      }}
      disabled={disabled}
    />
  );
};
