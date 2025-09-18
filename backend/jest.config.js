
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
  verbose: true,
  // map source paths (if you use path mapping, include it here)
};

module.exports = config;
