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

const PAGE_SIZE = 20;

interface ProcessorPartySelectFormFieldProps<TFieldValues extends FieldValues> {
  name: Path<TFieldValues>;
  rules: any;
  disabled?: boolean;
}

export const ProcessorPartySelectFormField = <
  TFieldValues extends FieldValues,
>({
  name,
  rules,
  disabled = false,
}: ProcessorPartySelectFormFieldProps<TFieldValues>) => {
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
        size: PAGE_SIZE,
        role: 'PROCESSOR',
      });
      return {
        options: response.content.map((company) => ({
          value: company.processorId!,
          label: company.name,
        })),
        hasMore: page < response.totalPages - 1,
      };
    },
    []
  );

  // Load a processor by processorId for initial form population (with caching)
  const loadOptionByValue = useCallback(
    async (processorId: string): Promise<Option | null> => {
      try {
        // Use React Query's fetchQuery for caching
        const response = await queryClient.fetchQuery({
          queryKey: ['processor', processorId],
          queryFn: () =>
            companyService.getAll({
              role: 'PROCESSOR',
              page: 0,
              size: 100, // Search through processors to find by processorId
            }),
          staleTime: 5 * 60 * 1000, // Cache for 5 minutes
        });
        const company = response.content.find(
          (c) => c.processorId === processorId
        );
        if (company) {
          return {
            value: company.processorId!,
            label: company.name,
          };
        }
        return null;
      } catch {
        return null;
      }
    },
    [queryClient]
  );

  return (
    <AsyncPaginateSelectFormField
      title="Verwerker (bestemming)"
      placeholder="Selecteer een verwerker"
      loadOptions={loadOptions}
      loadOptionByValue={loadOptionByValue}
      testId="processor-party-select"
      disabled={disabled}
      formHook={{
        register: formContext.register,
        name,
        rules,
        errors: formContext.formState.errors,
        control: formContext.control,
      }}
    />
  );
};
