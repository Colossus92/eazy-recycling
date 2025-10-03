import Plus from '@/assets/icons/Plus.svg?react';
import { Button } from '@/components/ui/button/Button';
import { ContentTitleBar } from '@/features/crud/ContentTitleBar';
import { TabPanel } from "@headlessui/react";
import { ErrorBoundary } from 'react-error-boundary';
import { fallbackRender } from '@/utils/fallbackRender';
import { ActionMenu } from './ActionMenu';
import { PaginationRow } from './pagination/PaginationRow';
import { useState } from 'react';

export const MasterDataTab = <T,>() => {
    const [page, setPage] = useState(1);
    const [rowsPerPage, setRowsPerPage] = useState(10);

    return (
        <TabPanel className={"flex flex-col items-start self-stretch flex-1 gap-4"}>
            <ContentTitleBar setQuery={() => { }}>
                <Button
                    variant={'primary'}
                    icon={Plus}
                    label={'Voeg toe'}
                    onClick={() => { }}
                />
            </ContentTitleBar>
            <ErrorBoundary fallbackRender={fallbackRender} onReset={() => { }}>
                <div className="flex-1 items-start self-stretch border-t-solid border-t border-t-color-border-primary h-full overflow-y-auto">
                    <table className="w-full table-fixed border-collapse">
                        <colgroup>
                            <col key="code" className={'w-[calc((100%-64px)/2)]'} />
                            <col key="description" className={'w-[calc((100%-64px)/2)]'} />
                        </colgroup>
                        <thead className="sticky top-0 bg-color-surface-secondary border-solid border-b border-color-border-primary ">
                            <tr className="text-subtitle-1">
                                <th className={'px-4 py-3 text-left'} key={"Code"}>
                                    Code
                                </th>
                                <th className={'px-4 py-3 text-left'} key={"Beschrijving"}>
                                    Beschrijving
                                </th>
                                <th className="px-4 py-3"></th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr className="text-body-2 border-b border-solid border-color-border-primary">
                                <td className="p-4" key={String("code")}></td>
                                <td className="p-4" key={String("description")}></td>
                                <td className="p-4 text-center">
                                    <ActionMenu<T>
                                        onEdit={() => { }}
                                        onDelete={() => { }}
                                        item={{} as T}
                                    />
                                </td>
                            </tr>
                        </tbody>
                        <tfoot className="sticky bottom-0 bg-color-surface-primary border-solid border-y border-color-border-primary z-10">
                            <tr className="text-body-2 bg-color-surface-primary">
                                <td colSpan={3} className="p-4">
                                    <PaginationRow
                                        page={page}
                                        setPage={setPage}
                                        rowsPerPage={rowsPerPage}
                                        setRowsPerPage={setRowsPerPage}
                                        numberOfResults={0}
                                    />
                                </td>
                            </tr>
                        </tfoot>
                    </table>
                </div>
            </ErrorBoundary>
        </TabPanel>
    )
}