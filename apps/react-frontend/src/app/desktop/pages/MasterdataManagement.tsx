import { ContentContainer } from '@/components/layouts/ContentContainer';
import { TabGroup, TabList, TabPanel, TabPanels } from '@headlessui/react';
import { Tab } from '@/components/ui/tab/Tab';

export const MasterdataManagement = () => {

  return (
    <ContentContainer title={"Masterdata"}>
      <div className="flex-1 flex flex-col items-start self-stretch pt-4 gap-4 border border-solid rounded-radius-xl border-color-border-primary bg-color-surface-primary overflow-hidden">

      <TabGroup>
      <TabList>
        <Tab label="Eural Codes" />
        <Tab label="Verwerkingsmethodes" />
        <Tab label="Vrachtwagens" />
        <Tab label="Containers" disabled/>
      </TabList>
      <TabPanels>
        <TabPanel>Content 1</TabPanel>
        <TabPanel>Content 2</TabPanel>
        <TabPanel>Content 3</TabPanel>
      </TabPanels>
    </TabGroup>
      </div>
    </ContentContainer>
  );
};

export default MasterdataManagement;
