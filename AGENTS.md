# Repository Agent Guide

This repository uses `AGENTS.md` as the primary operating guide for AI-assisted work.

## Core Rules

- Read this file before performing repository-related work.
- Use `.codex/skills/common.md` for all repository-related work.
- Load only the additional skills required for the current request.
- Follow referenced skills before taking task actions.
- Treat `.codex/templates/` as the canonical format for reusable deliverables.
- Verify repository files, Git state, GitHub state, and existing artifacts before making decisions that depend on them.
- Do not invent repository state, labels, branches, issues, pull requests, review state, requirements, policies, or validation results.
- Preserve existing user and teammate work.
- Keep changes scoped to the requested work.
- Avoid destructive actions unless the user explicitly requests them.

## Skill Routing

Load `.codex/skills/common.md` first, then use the following skill according to the request:

| Request | Skill |
| --- | --- |
| Gather, clarify, validate, or document requirements | `.codex/skills/requirements.md` |
| Identify domains, boundaries, ownership, or dependencies | `.codex/skills/domain.md` |
| Produce APIs, data models, ERDs, flows, sequences, states, or business rules | `.codex/skills/specification.md` |
| Plan, create, refine, or validate GitHub Issues | `.codex/skills/issue.md` |
| Initialize or maintain repository structure, branches, labels, templates, or governance | `.codex/skills/repository.md` |
| Analyze staged changes or prepare a commit | `.codex/skills/commit.md` |
| Prepare, create, update, or validate a pull request | `.codex/skills/pr.md` |
| Review changes, respond to feedback, or verify review resolution | `.codex/skills/review.md` |

For implementation requests, apply `common.md`, inspect the relevant requirements and specifications, follow existing repository conventions, implement only the requested scope, and run proportionate validation.

## Multi-Skill Workflow

When a request spans multiple stages, use only the required stages and preserve this order:

```text
Requirements
-> Domain
-> Specification
-> Issue
-> Implementation
-> Commit
-> Pull Request
-> Review
```

- Do not run every stage automatically when the user requested only one stage.
- Do not use a downstream skill to fill gaps owned by an upstream skill.
- Stop or report unresolved information when a missing decision prevents reliable downstream work.
- Repository governance work may use `repository.md` alongside another skill when branch, label, template, or artifact rules must be verified or changed.

## Templates

Use the following canonical templates:

| Deliverable | Template |
| --- | --- |
| Requirements | `.codex/templates/requirements.md` |
| Specification | `.codex/templates/specification.md` |
| GitHub Issue | `.codex/templates/issue.md` |
| Pull Request | `.codex/templates/pr.md` |

Domain analysis and review findings use the output format defined in their skill documents.

## Artifact Locations

Long-lived project artifacts belong in the GitHub Wiki. The local Wiki worktree is:

```text
.agents/wiki-work/
|- Requirements.md
|- Domain.md
`- Specification.md
```

- Requirements belong in the Wiki page `Requirements.md`.
- Domain artifacts belong in the Wiki page `Domain.md`.
- Specifications belong in the Wiki page `Specification.md`.
- When working locally, edit these pages under `.agents/wiki-work/`.
- Issues and pull requests belong in GitHub.
- Preserve traceability between requirements, domains, specifications, issues, commits, pull requests, and reviews.

## Language

- Agent-facing rules and skills use Korean.
- Commit messages use Korean.
- Human-facing issues, pull requests, review comments, and Wiki documentation use the repository team's primary communication language.

## Completion

Before reporting completion:

- Confirm that changed files match the requested scope.
- Confirm that required validation was actually performed.
- Distinguish verified results from unavailable checks or unresolved questions.
- Report created or modified artifacts accurately.
