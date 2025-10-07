import Plus from '@/assets/icons/Plus.svg?react';
import { Button } from '@/components/ui/button/Button';
import { ContentTitleBar } from '@/features/crud/ContentTitleBar';
import { TabPanel } from "@headlessui/react";
import { ErrorBoundary } from 'react-error-boundary';
import { fallbackRender } from '@/utils/fallbackRender';
import { ActionMenu } from '../ActionMenu';
import { PaginationRow } from '../pagination/PaginationRow';
import { useState } from 'react';

export type Column<T> = {
    key: keyof T;
    label: string;
    accessor: (value: T) => string;
    width: string;
};

export interface DataTableProps<T> {
    items: T[];
    columns: Column<T>[];
}

export interface MasterDataTabProps<T> {
    data: DataTableProps<T>;
    searchQuery: (query: string) => void;
    openAddForm: () => void;
    removeAction: (item: T) => void;
}

export const MasterDataTab = <T,>({data, searchQuery, openAddForm, removeAction}: MasterDataTabProps<T>) => {
    const [page, setPage] = useState(1);
    const [rowsPerPage, setRowsPerPage] = useState(10);

    return (
        <TabPanel className={"flex-1 flex flex-col items-start self-stretch gap-4 overflow-hidden"}>
            <ContentTitleBar setQuery={searchQuery}>
                <Button
                    variant={'primary'}
                    icon={Plus}
                    label={'Voeg toe'}
                    onClick={openAddForm}
                />
            </ContentTitleBar>
            <ErrorBoundary fallbackRender={fallbackRender} onReset={() => { }}>
                <div className="flex-1 items-start self-stretch border-t-solid border-t border-t-color-border-primary overflow-y-auto">
                    <table className="w-full table-fixed border-collapse">
                        <colgroup>
                            {data.columns.map((col) => (
                                <col key={String(col.key)} style={{ width: `${col.width}%` }} />
                            ))}
                            <col style={{ width: '63px' }} />
                        </colgroup>
                        <thead className="sticky top-0 bg-color-surface-secondary border-solid border-b border-color-border-primary">
                            <tr className="text-subtitle-1">
                                {data.columns.map((col) => (
                                    <th className={'px-4 py-3 text-left'} key={String(col.key)}>
                                        {col.label}
                                    </th>
                                ))}
                                <th className="px-4 py-3"></th>
                            </tr>
                        </thead>
                        <tbody>
                            {data.items
                            .slice((page - 1) * rowsPerPage, page * rowsPerPage)
                            .map((item, index) => (
                                <tr key={index} className="text-body-2 border-b border-solid border-color-border-primary">
                                    {data.columns.map((col) => (
                                        <td className="p-4" key={String(col.key)}>
                                            {col.accessor(item)}
                                        </td>
                                    ))}
                                    <td className="p-4 text-center">
                                        <ActionMenu<T>
                                        onEdit={() => { }}
                                        onDelete={removeAction}
                                        item={item}
                                    />
                                </td>
                            </tr>
                        ))}
                        </tbody>
                        <tfoot className="sticky bottom-0 bg-color-surface-primary border-solid border-y border-color-border-primary z-10">
                            <tr className="text-body-2 bg-color-surface-primary">
                                <td colSpan={3} className="p-4">
                                    <PaginationRow
                                        page={page}
                                        setPage={setPage}
                                        rowsPerPage={rowsPerPage}
                                        setRowsPerPage={setRowsPerPage}
                                        numberOfResults={data.items.length}
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