# AI Guidelines

## Scope
- Prefer changes in `meta-playground`.
- Do not modify vendor/upstream layers unless required.
- Keep changes minimal, explicit, and reproducible.

## Yocto Rules
- Prefer `.bbappend` over copying full recipes.
- Avoid hardcoded host paths in recipes/configs.
- Keep machine-specific behavior behind variables or overrides.

## Documentation
- For build/boot-impacting changes, update `readme.md` with tested target and status: `OK`, `NOK`, or `?`.

## Commit Rules (Required)
Use Conventional Commits:
- Format: `<type>(<scope>): <summary>`
- Types: `feat`, `fix`, `docs`, `refactor`, `chore`, `test`, `build`, `ci`
- Subject: imperative, concise, max 72 chars
- Add body when behavior/boot flow changes
- Breaking changes: include `BREAKING CHANGE:` footer

Examples:
- `feat(image): add amy debug utils toggle`
- `fix(boot): restore mp1 fip load path`
- `docs(readme): update mp2 boot status matrix`
