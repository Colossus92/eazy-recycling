import { useFormContext } from 'react-hook-form';
import { SelectFormField } from '../selectfield/SelectFormField';
import { useQuery } from '@tanstack/react-query';
import { Company } from '@/api/services/companyService';
import { companyService } from '@/api/services/companyService.ts';
import { Path } from 'react-hook-form';
import { TFieldValues } from './types';

interface CompanyLocationInputProps {
    /**
     * Name of the parent location field (e.g., "pickupLocation")
     */
    name: Path<TFieldValues>;
}

export const CompanyLocationInput = ({ name }: CompanyLocationInputProps) => {
    const { data: companies = [] } = useQuery<Company[]>({
        queryKey: ['companies'],
        queryFn: () => companyService.getAll(),
    });

    const companyOptions = companies.map((company) => ({
        value: company.id || '',
        label: company.name,
    }));

    const formContext = useFormContext<TFieldValues>();

    return (
        <div className="w-full flex flex-col gap-3">
            <div className="flex items-start self-stretch gap-4">
                <SelectFormField
                    title="Bedrijf"
                    placeholder="Selecteer een bedrijf"
                    options={companyOptions}
                    testId="company-location-select"
                    formHook={{
                        register: formContext.register,
                        name: `${String(name)}.companyId` as Path<TFieldValues>,
                        rules: { required: 'Bedrijf is verplicht' },
                        errors: formContext.formState.errors,
                        control: formContext.control,
                    }}
                />
            </div>
        </div>
    );
};
