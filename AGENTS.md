# AGENTS.md

## Commit messages

Use the format `<scope>: <message>`.

- `<scope>` is the area of the codebase the change touches, in lowercase (e.g. `app`, `ui`, `theme`, `build`, `deps`, `docs`, `repo`).
- Use `repo` (not `project`) for repository-wide changes such as scaffolding, `.gitignore`, IDE or CI config; "project" is ambiguous with the Gradle project.
- `<message>` is a short, imperative summary in lowercase (e.g. "add app drawer", not "Added app drawer.").
- Keep the subject line under ~72 characters with no trailing period. Add a body after a blank line if the change needs explanation.

Examples:

```
ui: add app drawer grid
build: bump compose bom to latest
docs: add commit convention to agents.md
```
