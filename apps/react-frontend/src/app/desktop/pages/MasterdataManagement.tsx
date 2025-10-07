import { ContentContainer } from '@/components/layouts/ContentContainer';
import { Tab } from '@/components/ui/tab/Tab';
import { EuralCodesTab } from '@/features/crud/masterdata/euralcodes/EuralCodeTab';
import { ProcessingMethodsTab } from '@/features/crud/masterdata/processingmethods/ProcessingMethodsTab';
import { TrucksTab } from '@/features/crud/masterdata/trucks/EuralCodeTab';
import { WasteContainersTab } from '@/features/crud/masterdata/wastecontainers/WasteContainerTab';
import { TabGroup, TabList, TabPanels } from '@headlessui/react';
import React, { ReactNode, useState } from 'react';

export const MasterdataManagement = () => {
  const [selectedIndex, setSelectedIndex] = useState(0);
  
  const tabs: {name: string, component: () => ReactNode}[] = [
    {name: "Vrachtwagens", component: () => <TrucksTab key={`truck-${selectedIndex}`} />},
    {name: "Containers", component: () => <WasteContainersTab key={`wastecontainer-${selectedIndex}`} />},
    {name: "Eural Codes", component: () => <EuralCodesTab key={`eural-${selectedIndex}`} />},
    {name: "Verwerkingsmethodes", component: () => <ProcessingMethodsTab key={`processing-${selectedIndex}`} />},
  ]

  return (
    <ContentContainer title={"Masterdata"}>
      <div className="flex-1 flex flex-col items-start self-stretch gap-4 rounded-b-radius-lg border-color-border-primary overflow-hidden">

        <TabGroup selectedIndex={selectedIndex} onChange={setSelectedIndex} className="w-full flex-1 flex flex-col min-h-0">
          <TabList className="relative z-10">
            {
              tabs.map((tab) => (
                <Tab label={tab.name} />
              ))
            }
          </TabList>
          <TabPanels className="
      flex flex-col flex-1
      bg-color-surface-primary 
      border border-solid rounded-b-radius-lg rounded-tr-radius-lg border-color-border-primary
      pt-4
      gap-4
      min-h-0
      -mt-[2px]
      ">
            {
              tabs.map((tab, index) => <React.Fragment key={index}>{tab.component()}</React.Fragment>)
            }
          </TabPanels>
        </TabGroup>
      </div>
    </ContentContainer>
  );
};

export default MasterdataManagement;
