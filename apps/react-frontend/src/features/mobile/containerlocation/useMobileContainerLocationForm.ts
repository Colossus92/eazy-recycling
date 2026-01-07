import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { useLocation } from 'react-router-dom';
import { useEffect } from 'react';
import { containerService } from '@/api/services/containerService';
import { WasteContainerRequest, WasteContainerView } from '@/api/client/models';
import { LocationFormValue } from '@/types/forms/LocationFormValue';
import { locationFormValueToPickupLocationRequest, pickupLocationViewToFormValue } from '@/types/forms/locationConverters';
import { ContainerLocationNavigationState } from './ContainerLocationButton';

export interface ContainerLocationFormValues {
  containerId: string;
  location: LocationFormValue;
}

function getWasteContainerLocation(container: WasteContainerView): string {
  if (!container.location) {
    return 'Geen locatie';
  }

  const location = container.location as any;
  switch (location.type) {
    case 'dutch_address':
      return `${location.streetName} ${location.buildingNumber}${location.buildingNumberAddition ? ' ' + location.buildingNumberAddition : ''}, ${location.city}`;

    case 'company':
      return `${location.company?.name || ''}${location.company?.name && location.company?.address?.city ? ', ' + location.company.address.city : ''}`;

    case 'project_location':
      return `${location.company?.name || ''} - ${location.streetName} ${location.buildingNumber}${location.buildingNumberAddition ? ' ' + location.buildingNumberAddition : ''}, ${location.city}`;

    case 'proximity':
      return `${location.description}, ${location.city}`;

    case 'no_pickup':
    default:
      return 'Geen locatie';
  }
}

export const useMobileContainerLocationForm = () => {
  const queryClient = useQueryClient();
  const { state } = useLocation();
  const navigationState = state as ContainerLocationNavigationState | undefined;

  const formContext = useForm<ContainerLocationFormValues>({
    defaultValues: {
      containerId: '',
      location: { type: 'none' },
    },
    mode: 'onChange',
  });

  const { data: containers = [] } = useQuery<WasteContainerView[]>({
    queryKey: ['containers'],
    queryFn: () => containerService.getAll(),
  });

  // Preload form with navigation state values
  useEffect(() => {
    if (navigationState?.containerId) {
      formContext.setValue('containerId', navigationState.containerId);
    }
    if (navigationState?.deliveryLocation) {
      const locationFormValue = pickupLocationViewToFormValue(navigationState.deliveryLocation);
      formContext.setValue('location', locationFormValue);
    }
  }, [navigationState, formContext]);

  const selectedContainerId = formContext.watch('containerId');
  const selectedContainer = containers.find(c => c.id === selectedContainerId);
  const currentLocation = selectedContainer ? getWasteContainerLocation(selectedContainer) : '';

  const updateMutation = useMutation({
    mutationFn: (request: WasteContainerRequest) =>
      containerService.update(request.id, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['containers'] });
    },
  });

  const handleSubmit = async (values: ContainerLocationFormValues): Promise<void> => {
    if (!values.containerId) {
      throw new Error('Container is verplicht');
    }
    
    if (!values.location || values.location.type === 'none') {
      throw new Error('Locatie is verplicht');
    }

    const request: WasteContainerRequest = {
      id: values.containerId,
      location: locationFormValueToPickupLocationRequest(values.location),
      notes: undefined,
    };

    return new Promise((resolve, reject) => {
      updateMutation.mutate(request, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };

  return {
    formContext,
    handleSubmit,
    isSubmitting: updateMutation.isPending,
    currentLocation,
  };
};
