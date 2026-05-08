import js from '@eslint/js';
import tseslint from 'typescript-eslint';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import jsxA11y from 'eslint-plugin-jsx-a11y';
import prettier from 'eslint-config-prettier';

export default tseslint.config(
    {
        ignores: ['dist/', 'node_modules/', 'coverage/'],
    },
    js.configs.recommended,
    ...tseslint.configs.recommended,
    reactHooks.configs.flat['recommended-latest'],
    reactRefresh.configs.vite,
    jsxA11y.flatConfigs.recommended,
    prettier,
    {
        files: ['**/*.{ts,tsx}'],
        languageOptions: {
            ecmaVersion: 2024,
            globals: {
                window: 'readonly',
                document: 'readonly',
                navigator: 'readonly',
                fetch: 'readonly',
                console: 'readonly',
            },
        },
        rules: {
            // Wir mischen Backticks/Quotes intentional — Prettier formatiert ohnehin.
            '@typescript-eslint/no-unused-vars': ['error', {argsIgnorePattern: '^_'}],
        },
    },
    {
        files: ['**/*.test.{ts,tsx}', 'src/test/**'],
        rules: {
            'react-refresh/only-export-components': 'off',
        },
    },
);
