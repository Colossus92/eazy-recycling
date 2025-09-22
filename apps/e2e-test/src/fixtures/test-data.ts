/**
 * Test data fixtures for e2e tests
 */
export const testUsers = {
  validUser: {
    email: 'planner@eazyrecycling.nl',
    password: 'password123'
  },
  invalidUser: {
    email: 'invalid@example.com',
    password: 'wrongpassword'
  },
  adminUser: {
    email: 'admin@eazyrecycling.nl',
    password: 'password123'
  },
};

export const testData = {
  truck: {
    displayName: 'Scania Haakarm 86-BVB-6',
  },
  tenant: {
    name: 'Eazy Recycling',
  },
  customer: {
    name: 'King Customer'
  },
  driver: {
    name: 'Sjaak Chauffeur',
  },
  container: {
    displayName: '40M001',
  }
};

export const errorMessages = {
  login: {
    invalidCredentials: 'Inloggen mislukt',
    networkError: 'Er is een probleem opgetreden bij het inloggen.',
    requiredEmail: 'Email is verplicht',
    requiredPassword: 'Wachtwoord is verplicht'
  }
};
