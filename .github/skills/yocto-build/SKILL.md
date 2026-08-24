---
name: yocto-build
description: 'Build this Yocto/STM32MP project and follow its commit/style conventions. Use when building with bitbake, running a target/recipe, or preparing a commit message for this repo.'
---

# Yocto Build

## Build

1. Source the environment for the target machine:
   ```
   source .env-mp2
   ```
2. Run bitbake as usual, e.g.:
   ```
   bitbake <target>
   ```

## Commit Messages

- Follow Conventional Commits: `<type>(<scope>): <summary>`
- Types: `feat`, `fix`, `docs`, `refactor`, `chore`, `test`, `build`, `ci`
- Keep the summary short and imperative (max 72 chars)
- Keep commit messages concise overall (short body only if needed)

## Code Style

- Use only ASCII characters in code, comments, and documentation
