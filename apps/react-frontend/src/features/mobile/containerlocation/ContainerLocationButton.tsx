import { Button } from "@/components/ui/button/Button"
import MapPinArea from '@/assets/icons/MapPinArea.svg?react';
import { useNavigate } from 'react-router-dom';
import { WasteContainerViewLocation } from '@/api/client/models';

export interface ContainerLocationNavigationState {
  containerId?: string;
  deliveryLocation?: WasteContainerViewLocation;
}

interface ContainerLocationButtonProps {
  containerId?: string;
  deliveryLocation?: WasteContainerViewLocation;
}

export const ContainerLocationButton = ({ containerId, deliveryLocation }: ContainerLocationButtonProps) => {
    const navigate = useNavigate();

    const handleClick = () => {
      const state: ContainerLocationNavigationState = {};
      if (containerId) {
        state.containerId = containerId;
      }
      if (deliveryLocation) {
        state.deliveryLocation = deliveryLocation;
      }
      navigate("/mobile/container-location", { state });
    };
 
    return (
        <Button
        icon={MapPinArea}
        label={"Containerlocatie doorgeven"}
        onClick={handleClick}
        fullWidth={true}
        variant="secondary"
        size='medium'
      />
    )
}