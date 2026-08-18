# Security

## Reporting

If you find a credential, personal identifier, or security issue in this evaluation repository, please open a private security advisory instead of a public issue.

## Test-only values

Default users, passwords, JWT fallback strings, DingTalk values, CIS URLs, database settings, and callback tokens in the snapshots are test or demonstration values. They must not be used in production.

## Production warning

Before deploying any candidate:

- require secrets through environment variables or a secret manager;
- remove all fallback credentials;
- use a real MySQL test environment;
- configure HTTPS and production CORS rules;
- validate external DingTalk/CIS integrations;
- fix the defects documented in `docs/acceptance-report.md`.

