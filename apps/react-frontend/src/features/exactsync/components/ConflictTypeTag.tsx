type ConflictType = 'KVK_COLLISION' | 'ADDRESS_COLLISION' | 'ADDRESS_MATCH' | 'DOMAIN_VALIDATION_ERROR';

interface ConflictTypeTagProps {
  type: string;
  description?: string;
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
  DOMAIN_VALIDATION_ERROR: {
    label: 'Foutieve gegevens',
    description: 'Ontbrekende of foutieve data in exact - maak relatie compleet',
  },
};

export const ConflictTypeTag = ({ type, description }: ConflictTypeTagProps) => {
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
        {description || config.description}
      </span>
    </div>
  );
};
