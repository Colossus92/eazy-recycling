import { WasteContainer } from '@/api/client';

export const wasteContainers: WasteContainer[] = [
  {
    uuid: 'uuid-1',
    id: 'CONT-001',
    location: {
      companyId: 'comp-1',
      companyName: 'Acme Recycling',
      address: {
        streetName: 'Recycling Lane',
        buildingNumber: '123',
        postalCode: '1234 AB',
        city: 'Amsterdam',
      },
    },
    notes: 'Near the main entrance',
  },
  {
    uuid: 'uuid-2',
    id: 'CONT-002',
    location: {
      companyId: 'comp-2',
      companyName: 'Green Solutions',
      address: {
        streetName: 'Eco Street',
        buildingNumber: '45',
        postalCode: '5678 CD',
        city: 'Rotterdam',
      },
    },
    notes: 'Behind the warehouse',
  },
  {
    uuid: 'uuid-3',
    id: 'CONT-003',
    location: {
      companyId: 'comp-3',
      companyName: 'Sustainable Materials',
      address: {
        streetName: 'Green Avenue',
        buildingNumber: '78',
        postalCode: '9012 EF',
        city: 'Utrecht',
      },
    },
    notes: 'Side entrance, accessible 24/7',
  },
];
