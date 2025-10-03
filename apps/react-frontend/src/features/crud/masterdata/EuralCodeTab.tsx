import { MasterDataTab } from "./MasterDataTab"
import { useEffect, useState } from "react";
import { euralService } from "@/api/services/euralService";
import { DataTableProps } from "./MasterDataTab";
import { Eural } from "@/api/client";
import { Column } from "./MasterDataTab";
import { useMemo } from "react";

export const EuralCodeTab = () => {
    const [items, setItems] = useState<Eural[]>([]);
    const [searchQuery, setSearchQuery] = useState('');
    const displayedEurals = useMemo(
        () => items.filter((item) => {
            return (
                item.code.toLowerCase().includes(searchQuery.toLowerCase()) ||
                item.description.toLowerCase().includes(searchQuery.toLowerCase())
            )
        }
        ),
        [items, searchQuery]
    );

    const columns: Column<Eural>[] = [
        { key: "code", label: "Code", width: "20", accessor: (item) => item.code },
        { key: "description", label: "Beschrijving", width: "80", accessor: (item) => item.description },
    ];

    const data: DataTableProps<Eural> = {
        columns,
        items: displayedEurals,
    };

    useEffect(() => {
        const fetchEuralCodes = async () => {
            const euralCodes = await euralService.getAll();
            setItems(euralCodes);
        };
        fetchEuralCodes();
    }, []);

    return (
        <MasterDataTab data={data} searchQuery={(query) => setSearchQuery(query)} />
    )
}