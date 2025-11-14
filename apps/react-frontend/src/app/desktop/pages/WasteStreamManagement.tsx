import { ContentContainer } from '@/components/layouts/ContentContainer';
import { Tab } from '@/components/ui/tab/Tab';
import { AfvalstroomnummersTab } from '@/features/wastestreams/components/AfvalstroomnummersTab';
import { LMATab } from '@/features/wastestreams/components/LMATab';
import { TabGroup, TabList, TabPanels, TabPanel } from '@headlessui/react';
import { ReactNode, useState } from 'react';

export const WasteStreamManagement = () => {
  const [selectedIndex, setSelectedIndex] = useState(0);
  
  const tabs: {name: string, component: () => ReactNode}[] = [
    {name: "Afvalstroomnummers", component: () => <AfvalstroomnummersTab key={`afvalstroomnummers-${selectedIndex}`} />},
    {name: "LMA", component: () => <LMATab key={`lma-${selectedIndex}`} />},
  ];

  return (
    <ContentContainer title={"Afvalstroomnummers"}>
      <div className="flex-1 flex flex-col items-start self-stretch gap-4 rounded-b-radius-lg border-color-border-primary overflow-hidden">

        <TabGroup selectedIndex={selectedIndex} onChange={setSelectedIndex} className="w-full flex-1 flex flex-col min-h-0">
          <TabList className="relative z-10">
            {
              tabs.map((tab) => (
                <Tab label={tab.name} key={tab.name} />
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
              tabs.map((tab, index) => (
                <TabPanel key={index} className="flex-1 flex flex-col items-start self-stretch gap-4 overflow-hidden">
                  {tab.component()}
                </TabPanel>
              ))
            }
          </TabPanels>
        </TabGroup>
      </div>
    </ContentContainer>
  );
};

export default WasteStreamManagement;
