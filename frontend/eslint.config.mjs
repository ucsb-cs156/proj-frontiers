import js from "@eslint/js";
import globals from "globals";
import reactHooks from "eslint-plugin-react-hooks";
import reactRefresh from "eslint-plugin-react-refresh";
import reactPlugin from "eslint-plugin-react";
import { defineConfig, globalIgnores } from "eslint/config";

// Import the vitest plugin
import vitest from "@vitest/eslint-plugin";

export default defineConfig([
  globalIgnores([
    "dist",
    ".stryker-tmp/",
    ".storybook/",
    "build",
    "coverage",
    "node_modules",
    "public/mockServiceWorker.js",
    "storybook-static/",
  ]),
  {
    files: ["src/**/*.{js,jsx}"],
    extends: [
      js.configs.recommended,
      reactHooks.configs["recommended-latest"],
      reactRefresh.configs.vite,
      reactPlugin.configs.flat.recommended,
    ],
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
      parserOptions: {
        ecmaVersion: "latest",
        ecmaFeatures: { jsx: true },
        sourceType: "module",
      },
    },
    settings: {
      react: {
        version: "detect",
      },
    },
    rules: {
      "no-unused-vars": ["error", { varsIgnorePattern: "^[A-Z_]" }],
      "react/react-in-jsx-scope": "off", //Recomended by React - https://legacy.reactjs.org/blog/2020/09/22/introducing-the-new-jsx-transform.html#eslint
      "react/jsx-uses-react": "off", //Recomended by React - https://legacy.reactjs.org/blog/2020/09/22/introducing-the-new-jsx-transform.html#eslint
      "react/prop-types": "off",                
    },
  },
  {
    // Apply this configuration only to test files
    files: ["**/*.test.{js,jsx}", "**/*.spec.{js,jsx}"],
    plugins: {
      vitest,
    },
    languageOptions: {
      globals: vitest.environments.env.globals, // Use vitest's globals
    },    
    rules: {
      // Vitest recommended rules
      ...vitest.configs.recommended.rules,
      "vitest/expect-expect": "off",
    },
  },
]);