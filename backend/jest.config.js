/** @type {import('jest').Config} */
const config = {
  preset: 'ts-jest',
  testEnvironment: 'node',
  roots: ['<rootDir>/tests', '<rootDir>/src'],
  moduleFileExtensions: ['ts', 'js', 'json', 'node'],
  transform: {
    '^.+\\.(ts|tsx)$': 'ts-jest',
  },
  testTimeout: 20000,
  collectCoverage: true,
  coverageDirectory: '<rootDir>/tests/coverage',
  collectCoverageFrom: [
    'src/**/*.ts',
    '!src/index.ts',
    '!src/database/**',
    '!src/routes/**',
    '!src/app.ts',
  ],
  coverageThreshold: {
    global: {
      lines: 75,
      statements: 75,
      functions: 75,
    },
    './src/services/goals.service.ts': {
      lines: 90,
      statements: 90,
      functions: 90,
    },
    './src/controllers/goals.controller.ts': {
      lines: 85,
      statements: 85,
      functions: 85,
    },
  },
  verbose: true,
};

module.exports = config;
