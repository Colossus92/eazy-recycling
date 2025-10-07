import { ContentContainer } from '@/components/layouts/ContentContainer';
import { TabGroup, TabList, TabPanel, TabPanels } from '@headlessui/react';
import { Tab } from '@/components/ui/tab/Tab';
import { EuralCodeTab } from '@/features/crud/masterdata/eural/EuralCodeTab';
import { ProcessingMethodsTab } from '@/features/crud/masterdata/processingmethods/ProcessingMethodsTab';
import { ReactNode } from 'react';
import React from 'react';

export const MasterdataManagement = () => {
  const tabs: {name: string, panel: ReactNode}[] = [
    {name: "Eural Codes", panel: <EuralCodeTab />},
    {name: "Verwerkingsmethodes", panel: <ProcessingMethodsTab />}
  ]


  return (
    <ContentContainer title={"Masterdata"}>
      <div className="flex-1 flex flex-col items-start self-stretch gap-4 rounded-b-radius-lg border-color-border-primary overflow-hidden">

        <TabGroup className="w-full flex-1 flex flex-col min-h-0">
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
              tabs.map((tab, index) => <React.Fragment key={index}>{tab.panel}</React.Fragment>)
            }
          </TabPanels>
        </TabGroup>
      </div>
    </ContentContainer>
  );
};

export default MasterdataManagement;
