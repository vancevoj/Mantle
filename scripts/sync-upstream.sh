#!/usr/bin/env bash
#
# sync-upstream.sh - keep this NeoForge 1.21.1 Mantle fork in sync with upstream
# SlimeKnights/Mantle. Companion to the TConstruct fork's script of the same name.
#
#   1. Ensures the `upstream` remote points at SlimeKnights/Mantle.
#   2. Fetches upstream.
#   3. Fast-forwards / merges the reference branch (default 1.20).
#   4. Flags NEW upstream branches (a future 1.21 / NeoForge branch is the signal
#      to switch this port over to tracking official directly).
#   5. Lists upstream commits not yet reflected in the 1.21 port branch.
#
# Usage:
#   scripts/sync-upstream.sh            # sync the 1.20 reference branch
#   scripts/sync-upstream.sh <branch>   # sync a different reference branch

set -euo pipefail

UPSTREAM_URL="https://github.com/SlimeKnights/Mantle.git"
REF_BRANCH="${1:-1.20}"
PORT_BRANCH="1.21"

say()  { printf '\033[1;36m==>\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m!! \033[0m %s\n' "$*"; }
ok()   { printf '\033[1;32m ok\033[0m %s\n' "$*"; }

if ! git remote get-url upstream >/dev/null 2>&1; then
  say "Adding upstream remote -> $UPSTREAM_URL"
  git remote add upstream "$UPSTREAM_URL"
fi

before="$(git branch -r --list 'upstream/*' | sed 's/^[ *]*//' | sort || true)"
say "Fetching upstream..."
git fetch --prune --tags upstream
after="$(git branch -r --list 'upstream/*' | sed 's/^[ *]*//' | sort)"

new_branches="$(comm -13 <(printf '%s\n' "$before") <(printf '%s\n' "$after") || true)"
if [ -n "$new_branches" ]; then
  warn "NEW upstream branches appeared:"
  printf '%s\n' "$new_branches" | sed 's/^/      /'
  if printf '%s\n' "$new_branches" | grep -qiE '1\.21|neoforge|neo'; then
    warn "One of these looks like a 1.21 / NeoForge branch - time to track official Mantle directly."
  fi
fi

if git show-ref --verify --quiet "refs/heads/$REF_BRANCH"; then
  say "Updating reference branch '$REF_BRANCH' from upstream/$REF_BRANCH"
  start_branch="$(git rev-parse --abbrev-ref HEAD)"
  git switch "$REF_BRANCH"
  if git merge --ff-only "upstream/$REF_BRANCH" 2>/dev/null; then
    ok "Fast-forwarded $REF_BRANCH."
  else
    warn "Cannot fast-forward; merging."
    git merge --no-edit "upstream/$REF_BRANCH"
  fi
  git switch "$start_branch"
else
  warn "Local branch '$REF_BRANCH' not found; skipping reference update."
fi

if git show-ref --verify --quiet "refs/heads/$PORT_BRANCH"; then
  count="$(git rev-list --count "$PORT_BRANCH..upstream/$REF_BRANCH" 2>/dev/null || echo 0)"
  say "Upstream $REF_BRANCH commits not yet in $PORT_BRANCH: $count"
  if [ "$count" != "0" ]; then
    git --no-pager log --oneline -15 "$PORT_BRANCH..upstream/$REF_BRANCH" | sed 's/^/      /'
    echo "    Cherry-pick a fix with: git switch $PORT_BRANCH && git cherry-pick <hash>"
  fi
fi

ok "Sync complete."
