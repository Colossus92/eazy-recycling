import { ContentContainer } from '@/components/layouts/ContentContainer';
import { TabGroup, TabList, TabPanel, TabPanels } from '@headlessui/react';
import { Tab } from '@/components/ui/tab/Tab';
import { MasterDataTab } from '@/features/crud/MasterDataTab';

export const MasterdataManagement = () => {

  return (
    <ContentContainer title={"Masterdata"}>
      <div className="flex-1 flex flex-col items-start self-stretch gap-4 rounded-b-radius-lg border-color-border-primary overflow-hidden">

        <TabGroup className="w-full h-full">
          <TabList>
            <Tab label="Eural Codes" />
            <Tab label="Verwerkingsmethodes" />
            <Tab label="Vrachtwagens" />
            <Tab label="Containers" disabled />
          </TabList>
          <TabPanels className="
      flex flex-col flex-1
      bg-color-surface-primary 
      border border-solid rounded-b-radius-lg rounded-tr-radius-lg border-color-border-primary
      pt-4
      gap-4
      h-full
      -mt-[2px]
      ">
            <MasterDataTab />
            <TabPanel>Content 2</TabPanel>
            <TabPanel>Content 3</TabPanel>
            <TabPanel>Content 4</TabPanel>
          </TabPanels>
        </TabGroup>
      </div>
    </ContentContainer>
  );
};

export default MasterdataManagement;
