import { useForm, UseFormReturn } from 'react-hook-form';
import { toast } from 'react-toastify';
import { DriverSelectFormField } from '@/components/ui/form/selectfield/DriverSelectFormField.tsx';
import { TruckSelectFormField } from '@/components/ui/form/selectfield/TruckSelectFormField.tsx';
import { CheckboxField } from '@/components/ui/form/CheckboxField.tsx';
import { TransportStatusTag } from '@/features/planning/components/tag/TransportStatusTag';
import { Button } from '@/components/ui/button/Button.tsx';
import { DriverPlanningItemStatusEnum } from '@/api/client/models/driver-planning-item';

export interface TransportPlanningFormValues {
  driverId: string;
  truckId: string;
  isPlanned: boolean;
  isFinished: boolean;
  isUnplanned: boolean;
  isInvoiced: boolean;
}

interface StatusFilterOptionProps {
  formContext: UseFormReturn<TransportPlanningFormValues>;
  status: DriverPlanningItemStatusEnum;
  fieldName: keyof TransportPlanningFormValues;
}

const StatusFilterOption = ({
  fieldName,
  formContext,
  status,
}: StatusFilterOptionProps) => {
  return (
    <div className="flex items-center self-stretch gap-2">
      <CheckboxField
        formHook={{
          register: formContext.register,
          name: fieldName,
          errors: formContext.formState.errors,
          control: formContext.control,
        }}
      >
        <TransportStatusTag status={status}></TransportStatusTag>
      </CheckboxField>
    </div>
  );
};

interface PlanningFilterFormProps {
  closeDialog: () => void;
  onSubmit: (values: TransportPlanningFormValues) => void;
  initialValues?: TransportPlanningFormValues;
}

const defaultFormValues: TransportPlanningFormValues = {
  driverId: '',
  truckId: '',
  isPlanned: false,
  isFinished: false,
  isUnplanned: false,
  isInvoiced: false,
};

export const PlanningFilterForm = ({
  closeDialog,
  onSubmit,
  initialValues,
}: PlanningFilterFormProps) => {
  const formContext = useForm<TransportPlanningFormValues>({
    defaultValues: initialValues ?? defaultFormValues,
  });

  const handleApplyFilter = formContext.handleSubmit((data) => {
    onSubmit(data);
    closeDialog();
    toast.success('Filters toegepast', {
      className: 'bg-color-status-success-light text-color-text-secondary',
      progressClassName: 'bg-color-status-success-primary',
    });
  });

  return (
    <>
      <div className="relative mt-6 flex-1 px-4 sm:px-6">
        <div className={'flex flex-col flex-1 items-start self-stretch gap-4'}>
          <DriverSelectFormField
            formHook={{
              register: formContext.register,
              name: 'driverId',
              errors: formContext.formState.errors,
              control: formContext.control,
            }}
          />
          <TruckSelectFormField
            formHook={{
              register: formContext.register,
              name: 'truckId',
              errors: formContext.formState.errors,
              control: formContext.control,
            }}
          />
          <div className="flex flex-col items-start self-stretch gap-2">
            <span className="text-caption-2">Status</span>
            <StatusFilterOption
              fieldName={'isUnplanned'}
              formContext={formContext}
              status={DriverPlanningItemStatusEnum.Unplanned}
            />
            <StatusFilterOption
              fieldName={'isPlanned'}
              formContext={formContext}
              status={DriverPlanningItemStatusEnum.Planned}
            />
            <StatusFilterOption
              fieldName={'isFinished'}
              formContext={formContext}
              status={DriverPlanningItemStatusEnum.Finished}
            />
            <StatusFilterOption
              fieldName={'isInvoiced'}
              formContext={formContext}
              status={DriverPlanningItemStatusEnum.Invoiced}
            />
          </div>
        </div>
      </div>
      <div
        className={
          'flex justify-end items-center self-stretch py-3 px-4 gap-4 border-t border-solid border-color-border-primary'
        }
      >
        <div className={'flex-1'}>
          <Button
            variant={'secondary'}
            label={'Reset'}
            fullWidth={true}
            onClick={() => formContext.reset(defaultFormValues)}
          />
        </div>
        <div className={'flex-1'}>
          <Button
            variant={'primary'}
            label={'Toepassen'}
            fullWidth={true}
            onClick={handleApplyFilter}
          />
        </div>
      </div>
    </>
  );
};
