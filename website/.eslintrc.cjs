module.exports = {
  root: true,
  env: {
    browser: true,
    es2022: true,
    node: true,
  },
  extends: [
    'eslint:recommended',
    'plugin:@typescript-eslint/recommended',
  ],
  parser: '@typescript-eslint/parser',
  parserOptions: {
    ecmaVersion: 'latest',
    sourceType: 'module',
  },
  plugins: [
    '@typescript-eslint',
    'security',
    'no-secrets',
  ],
  rules: {
    // Security rules (manually configured instead of using plugin:security/recommended)
    'security/detect-object-injection': 'warn',
    'security/detect-non-literal-regexp': 'warn',
    'security/detect-unsafe-regex': 'error',
    'security/detect-buffer-noassert': 'error',
    'security/detect-child-process': 'warn',
    'security/detect-disable-mustache-escape': 'error',
    'security/detect-eval-with-expression': 'error',
    'security/detect-no-csrf-before-method-override': 'error',
    'security/detect-non-literal-fs-filename': 'warn',
    'security/detect-non-literal-require': 'warn',
    'security/detect-possible-timing-attacks': 'warn',
    'security/detect-pseudoRandomBytes': 'error',

    // No-secrets plugin
    'no-secrets/no-secrets': ['error', {
      'tolerance': 4.5,
      'ignoreContent': [
        'localhost',
        '127.0.0.1',
        'example.com',
        'test',
        'mock',
        'fixture',
      ],
    }],

    // TypeScript rules
    '@typescript-eslint/no-explicit-any': 'warn',
    '@typescript-eslint/no-unused-vars': ['warn', {
      'argsIgnorePattern': '^_',
      'varsIgnorePattern': '^_',
    }],

    // General code quality
    'no-console': ['warn', { allow: ['warn', 'error'] }],
    'no-debugger': 'error',
    'no-eval': 'error',
    'no-implied-eval': 'error',
    'no-new-func': 'error',
    'no-script-url': 'error',
  },
  overrides: [
    {
      // Astro files
      files: ['*.astro'],
      parser: 'astro-eslint-parser',
      parserOptions: {
        parser: '@typescript-eslint/parser',
        extraFileExtensions: ['.astro'],
      },
      extends: ['plugin:astro/recommended'],
    },
    {
      // Config files - relax some rules
      files: ['*.config.js', '*.config.ts', '*.config.mjs', '.eslintrc.cjs'],
      rules: {
        '@typescript-eslint/no-var-requires': 'off',
        'security/detect-non-literal-require': 'off',
      },
    },
  ],
  ignorePatterns: [
    'dist/',
    '.astro/',
    'node_modules/',
    '*.min.js',
  ],
};
