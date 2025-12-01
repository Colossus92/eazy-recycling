interface AuditMetadataFooterProps {
    createdAt?: string;
    createdByName?: string;
    updatedAt?: string;
    updatedByName?: string;
}

function formatDateTime(isoString?: string): string {
    if (!isoString) return '';
    const date = new Date(isoString);
    return date.toLocaleString('nl-NL', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
    });
}

export const AuditMetadataFooter = ({
    createdAt,
    createdByName,
    updatedAt,
    updatedByName,
}: AuditMetadataFooterProps) => {
    const hasCreatedInfo = createdAt || createdByName;
    const hasUpdatedInfo = updatedAt || updatedByName;

    if (!hasCreatedInfo && !hasUpdatedInfo) {
        return null;
    }

    return (
        <div className="flex flex-col items-start w-full mt-6 pt-4 border-t border-color-border-secondary text-xs text-color-text-secondary italic">
            {hasUpdatedInfo && (
                <span>
                    Laatst aangepast{updatedByName ? ` door ${updatedByName}` : ''}{updatedAt ? ` op ${formatDateTime(updatedAt)}` : ''}
                </span>
            )}
            {hasCreatedInfo && (
                <span>
                    Aangemaakt{createdByName ? ` door ${createdByName}` : ''}{createdAt ? ` op ${formatDateTime(createdAt)}` : ''}
                </span>
            )}
        </div>
    );
};