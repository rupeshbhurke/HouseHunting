#!/usr/bin/env bash
# The repo's checks. Exit 0 only when the branch is fit to review.
#
# The gate runs this script from the main checkout with the job's worktree as
# the working directory. Keep it fast, deterministic and non-interactive: no
# watch modes, no prompts, nothing that rewrites files.
set -euo pipefail

# A fresh worktree has no local.properties: it is gitignored, so Gradle cannot
# find the Android SDK and fails before any check runs. Provision it from
# ANDROID_HOME, else from the main checkout's copy. local.properties is
# gitignored in the worktree too, so this never shows up in a job's diff.
#
# secrets.properties is deliberately NOT copied. The Secrets Gradle plugin
# falls back to secrets.defaults.properties, so the build never needs the real
# Maps key, and job worktrees stay free of it.
if [ ! -f local.properties ]; then
  if [ -n "${ANDROID_HOME:-}" ]; then
    printf 'sdk.dir=%s\n' "${ANDROID_HOME//\\//}" > local.properties
  else
    main_checkout="$(cd "$(git rev-parse --git-common-dir)/.." && pwd)"
    if [ -f "$main_checkout/local.properties" ]; then
      cp "$main_checkout/local.properties" local.properties
    else
      echo "No local.properties here and no ANDROID_HOME set: Gradle cannot locate the Android SDK." >&2
      exit 1
    fi
  fi
fi

# Lint, unit tests and the debug build, in one Gradle invocation so the
# configuration phase runs once.
#
# connectedAndroidTest is deliberately absent: it needs a running emulator or
# device, so it is neither deterministic nor non-interactive. That leaves the
# Room migration test and the repository/DAO tests outside the gate - run
# `./gradlew connectedAndroidTest` by hand before merging a change that touches
# entities, migrations or repositories.
./gradlew --console=plain lintDebug testDebugUnitTest assembleDebug
