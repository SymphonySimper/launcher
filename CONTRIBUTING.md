# Contributing

Read [AGENTS.md](AGENTS.md) for the performance rules and commit style, and [DESIGN.md](DESIGN.md)
before changing anything visible.

## Building

The [justfile](justfile) has the common tasks; run `just` to list them. `just install` puts the
optimised release build on a connected phone, and `just backup` / `just restore` save and bring back
the launcher's settings over adb. It uses Android Studio's bundled JDK when `JAVA_HOME` isn't set.

## Releasing

To publish a release, fast-forward the `release` branch to main as it is on GitHub:

```sh
just release   # or: git push origin origin/main:release
```

The Release workflow then builds a signed APK and an app bundle (`.aab`, for Google Play) and
publishes both as a GitHub release. It creates the `v<version>` tag on that commit itself, so don't
tag by hand. The version is `<commit date in UTC>.<commit count>` (like `2026.11.28.100`), and the
count makes every release install over the one before it. The count needs the full history, so the
build fails on a shallow clone (`git fetch --unshallow` fixes it). Signing uses the secrets listed
at the top of [release.yml](.github/workflows/release.yml).

The Build workflow only runs when started by hand (Actions → Build → Run workflow, or
`gh workflow run build.yml`). It builds a release APK signed with a debug key (no secrets) and keeps it as a workflow
artifact for 7 days; it doesn't publish anything.
