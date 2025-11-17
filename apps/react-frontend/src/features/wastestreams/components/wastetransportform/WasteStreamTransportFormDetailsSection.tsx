import { useFormContext } from 'react-hook-form';
import { WasteStreamTransportFormValues } from '@/features/wastestreams/hooks/useWasteStreamTransportForm';
import { TruckSelectFormField } from '@/components/ui/form/selectfield/TruckSelectFormField';
import { DriverSelectFormField } from '@/components/ui/form/selectfield/DriverSelectFormField';
import { ContainerSelectFormField } from '@/components/ui/form/selectfield/ContainerSelectFormField';
import { ContainerOperationSelectFormField } from '@/components/ui/form/selectfield/ContainerOperationSelectFormField';
import { TextAreaFormField } from '@/components/ui/form/TextAreaFormField';
import { DateTimeInput } from '@/components/ui/form/DateTimeInput';
import MapPin from '@/assets/icons/MapPin.svg?react';
import MapPinArea from '@/assets/icons/MapPinArea.svg?react';
import Hash from '@/assets/icons/Hash.svg?react';
import BuildingOffice from '@/assets/icons/BuildingOffice.svg?react';

export const WasteStreamTransportFormDetailsSection = () => {
  const formContext = useFormContext<WasteStreamTransportFormValues>();

  const wasteStreamData = formContext.watch('wasteStreamData');
  const wasteStreamNumber = formContext.watch('wasteStreamNumber');

  return (
    <div className="flex flex-col items-start self-stretch gap-4">
      {/* Summary section with icon-based layout */}
      <div className="flex flex-col items-start self-stretch gap-2">
        {/* Afvalstroomnummer */}
        <div className="flex items-center gap-2 self-stretch">
          <div className="flex items-center flex-1 gap-2">
            <Hash className="w-5 h-5 text-color-text-secondary" />
            <span className="text-body-2 text-color-text-secondary">
              Afvalstroomnummer
            </span>
          </div>
          <span className="text-body-2 truncate">
            {wasteStreamNumber} - {wasteStreamData?.wasteName || ''}
          </span>
        </div>

        {/* Opdrachtgever */}
        <div className="flex items-center gap-2 self-stretch">
          <div className="flex items-center flex-1 gap-2">
            <BuildingOffice className="w-5 h-5 text-color-text-secondary" />
            <span className="text-body-2 text-color-text-secondary">
              Opdrachtgever
            </span>
          </div>
          <span className="text-body-2 truncate">
            {wasteStreamData?.consignorPartyName || ''}
          </span>
        </div>

        {/* Herkomst */}
        <div className="flex items-center gap-2 self-stretch">
          <div className="flex items-center flex-1 gap-2">
            <MapPin className="w-5 h-5 text-color-text-secondary" />
            <span className="text-body-2 text-color-text-secondary">
              Herkomst
            </span>
          </div>
          <span className="text-body-2 truncate">
            {wasteStreamData?.pickupLocation || ''}
          </span>
        </div>

        {/* Bestemming */}
        <div className="flex items-center gap-2 self-stretch">
          <div className="flex items-center flex-1 gap-2">
            <MapPinArea className="w-5 h-5 text-color-text-secondary" />
            <span className="text-body-2 text-color-text-secondary">
              Bestemming
            </span>
          </div>
          <span className="text-body-2 truncate">
            {wasteStreamData?.deliveryLocation || ''}
          </span>
        </div>
      </div>

      {/* Transport Details Section */}
      <div className="w-full rounded-lg bg-color-surface-secondary p-4">
        <h3 className="text-subtitle-1 text-color-text-primary mb-4">
          Transport Details
        </h3>

        <div className="flex flex-col gap-4">
          {/* Truck and Driver row */}
          <div className="flex gap-4 items-start w-full">
            <div className="flex-1">
              <TruckSelectFormField
                formHook={{
                  register: formContext.register,
                  name: 'truckId',
                  errors: formContext.formState.errors,
                  control: formContext.control,
                }}
              />
            </div>
            <div className="flex-1">
              <DriverSelectFormField
                formHook={{
                  register: formContext.register,
                  name: 'driverId',
                  errors: formContext.formState.errors,
                  control: formContext.control,
                }}
              />
            </div>
          </div>
          <div className="flex gap-4 items-start w-full">
            <div className="flex-1">
              <ContainerSelectFormField
                formHook={{
                  register: formContext.register,
                  name: 'containerId',
                  errors: formContext.formState.errors,
                  control: formContext.control,
                }}
              />
            </div>
            <div className='flex-1'>
              <ContainerOperationSelectFormField
                formHook={{
                  register: formContext.register,
                  name: 'containerOperation',
                  rules: { required: 'Type transport is verplicht' },
                  errors: formContext.formState.errors,
                  control: formContext.control,
                }}
              />
            </div>
          </div>

          {/* Pickup and Delivery date row */}
          <div className="flex gap-4 items-start w-full">
            <div className="flex-1">
              <DateTimeInput
                title="Ophaalmoment"
                formHook={{
                  register: formContext.register,
                  name: 'pickupDate',
                  rules: { required: 'Pickup date is required' },
                  errors: formContext.formState.errors,
                }}
                testId="pickup-date"
              />
            </div>
            <div className="flex-1">
              <DateTimeInput
                title="Aflevermoment"
                formHook={{
                  register: formContext.register,
                  name: 'deliveryDate',
                  errors: formContext.formState.errors,
                }}
                testId="delivery-date"
              />
            </div>
          </div>

          {/* Comments field */}
          <div className="w-full">
            <TextAreaFormField
              title="Opmerkingen"
              placeholder="Voeg eventuele opmerkingen toe..."
              rows={6}
              formHook={{
                register: formContext.register,
                name: 'comments',
                errors: formContext.formState.errors,
              }}
              testId="comments"
            />
          </div>
        </div>
      </div>
    </div>
  );
};
