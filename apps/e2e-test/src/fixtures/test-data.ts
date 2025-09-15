/**
 * Test data fixtures for e2e tests
 */
export const testUsers = {
  validUser: {
    email: 'peter@planner.nl',
    password: '2@Supabase'
  },
  invalidUser: {
    email: 'invalid@example.com',
    password: 'wrongpassword'
  },
  adminUser: {
    email: 'calvin@eazysoftware.nl',
    password: 'password123'
  },
};

export const testData = {
  truck: {
    licensePlate: 'AB-123-CD',
    brand: 'Mercedes',
    model: 'Actros',
    year: 2023
  },
  company: {
    name: 'Test Recycling BV',
    address: 'Teststraat 123',
    city: 'Amsterdam',
    postalCode: '1000 AA'
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
