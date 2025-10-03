import { MasterDataTab } from "./MasterDataTab"
import { useEffect, useState } from "react";
import { euralService } from "@/api/services/euralService";
import { DataTableProps } from "./MasterDataTab";
import { Eural } from "@/api/client";
import { Column } from "./MasterDataTab";

export const EuralCodeTab = () => {
    const [items, setItems] = useState<Eural[]>([]);

    const columns: Column<Eural>[] = [
        { key: "code", label: "Code", width: "20" },
        { key: "description", label: "Beschrijving", width: "80" },
    ];

    const data: DataTableProps<Eural> = {
        columns,
        items: items,
        setQuery: () => { },
    };

    useEffect(() => {
        const fetchEuralCodes = async () => {
            const euralCodes = await euralService.getAll();
            setItems(euralCodes);
        };
        fetchEuralCodes();
    }, []);


    return (
        <MasterDataTab data={data} />
    )
}