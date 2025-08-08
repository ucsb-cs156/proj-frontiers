module.exports = {
  // Test environment for jsdom (browser-like environment)
  testEnvironment: 'jsdom',
  
  // Setup files after environment is configured
  setupFilesAfterEnv: ['<rootDir>/src/setupTests.js'],
  
  // Module name mapping
  moduleNameMapper: {
    // Handle src baseUrl paths
    '^main/(.*)$': '<rootDir>/src/main/$1',
    '^fixtures/(.*)$': '<rootDir>/src/fixtures/$1',
    '^stories/(.*)$': '<rootDir>/src/stories/$1',
    '^tests/(.*)$': '<rootDir>/src/tests/$1',
    // CSS modules and assets
    '\\.(css|less|scss|sass)$': 'identity-obj-proxy',
    '\\.(jpg|jpeg|png|gif|eot|otf|webp|svg|ttf|woff|woff2|mp4|webm|wav|mp3|m4a|aac|oga)$': 'jest-transform-stub'
  },
  
  // Transform files with Babel using react-app preset
  transform: {
    '^.+\\.(js|jsx)$': ['babel-jest', {
      presets: [['babel-preset-react-app', { runtime: 'automatic' }]]
    }]
  },
  
  // Test match patterns
  testMatch: [
    '<rootDir>/src/**/__tests__/**/*.{js,jsx}',
    '<rootDir>/src/**/*.{spec,test}.{js,jsx}'
  ],
  
  // Coverage configuration (from current package.json config)
  collectCoverageFrom: [
    'src/main/**/*.{js,jsx,ts,tsx}'
  ],
  
  // Files to ignore
  testPathIgnorePatterns: [
    '<rootDir>/node_modules/',
    '<rootDir>/build/'
  ],
  
  // Module file extensions
  moduleFileExtensions: [
    'js',
    'jsx',
    'json'
  ],
  
  // Watch plugins (commented out to avoid compatibility issues)
  // watchPlugins: [
  //   'jest-watch-typeahead/filename',
  //   'jest-watch-typeahead/testname'
  // ],
  
  // Reset mocks between tests
  resetMocks: true
};