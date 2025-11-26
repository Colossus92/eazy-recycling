type ConflictType = 'KVK_COLLISION' | 'ADDRESS_COLLISION' | 'ADDRESS_MATCH';

interface ConflictTypeTagProps {
  type: string;
}

const typeConfig: Record<
  ConflictType,
  { label: string; description: string }
> = {
  KVK_COLLISION: {
    label: 'KVK Conflict',
    description: 'Bedrijf met dit KVK-nummer is al gekoppeld aan een ander Exact account',
  },
  ADDRESS_COLLISION: {
    label: 'Adres Conflict',
    description: 'Bedrijf op dit adres is al gekoppeld aan een ander Exact account',
  },
  ADDRESS_MATCH: {
    label: 'Mogelijke Match',
    description: 'Bedrijf gevonden op basis van adres - bevestiging vereist',
  },
};

export const ConflictTypeTag = ({ type }: ConflictTypeTagProps) => {
  const config = typeConfig[type as ConflictType];

  if (!config) {
    return <span className="text-body-2">Onbekend type</span>;
  }

  return (
    <div className="flex flex-col gap-1">
      <span className="text-subtitle-2 text-color-text-primary">
        {config.label}
      </span>
      <span className="text-caption text-color-text-secondary">
        {config.description}
      </span>
    </div>
  );
};
