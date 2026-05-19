---
name: Never use "Closes" or "Fixes" in commit messages
description: User-set rule for this project: do not use the GitHub auto-close keywords ("Closes", "Fixes", "Resolves") when referencing issues in commit messages
type: feedback
originSessionId: 5f35835d-855b-4a15-a252-ca49a34e3f63
---
Do not use "Closes #N", "Fixes #N", or "Resolves #N" in commit messages on this project. Reference issues some other way (e.g. "Refs #N", or just inline "#N" in the body) — or don't reference them at all.

**Why:** The user wants control over when issues get closed; GitHub's auto-close on push is unwanted. They told me this after I shipped a commit with `Closes #37.` to master.

**How to apply:** When drafting commit messages or PR bodies that touch an issue, avoid the auto-close keywords. If the user explicitly asks to close an issue, do it manually (e.g. `gh issue close <N>`).
