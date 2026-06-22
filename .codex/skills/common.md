# Common Rules

<github_rules>

- GitHub resources must be managed consistently with repository governance.
- GitHub metadata must reflect verified repository state and work state.
- GitHub decisions must preserve traceability between requirements, implementation, review, and release artifacts.
- GitHub resource changes must avoid conflicting ownership, duplicate meaning, and stale status.

</github_rules>

<label_rules>

- Classification metadata must be based on verified information.
- Classification metadata must remain consistent with repository governance.
- Classification metadata must not encode temporary notes or unverified assumptions.
- Duplicate classification meaning must be avoided.

</label_rules>

<development_rules>

- Tests must be added or updated when behavior changes or risk justifies verification.
- Existing tests must be run when they are relevant and feasible.
- Code must favor maintainability over cleverness.
- Code must be readable without relying on hidden context.
- Code quality must be evaluated against correctness, clarity, cohesion, and long-term change cost.
- Exception handling must preserve useful failure information and avoid masking defects.
- Implementations must prefer simple solutions that satisfy verified requirements.
- Changes must remain consistent with existing project structure, style, and conventions.
- Unrelated changes must be avoided.
- User or teammate changes must be preserved.

</development_rules>

<documentation_rules>

- Documentation must reference artifacts accurately and consistently.
- Documentation must be updated when behavior, interfaces, requirements, or operational expectations change.
- Documentation must distinguish verified facts from unresolved decisions.
- Requirement traceability must be maintained from source request to implementation and review artifacts.
- Documentation must avoid stale, redundant, or conflicting guidance.
- Documentation must use terminology consistently across related artifacts.

</documentation_rules>

## Skill Contract

All skills should follow a consistent structure.

Required sections:

- references
- triggers
- success_criteria
- trusted_sources
- cli_policy
- allowed_cli_commands

Optional sections:

- template_usage
- output
- boundaries
- validation
- readiness

Guidelines:

- Skills define behavior, process, validation, and safety rules.
- Skills should not define reusable deliverable structures when a template exists.
- Templates are the canonical source for deliverable formatting.
- Skills should reference templates rather than duplicate template content.
- Success Criteria should be objective and verifiable.
- Trusted Sources should identify authoritative information sources.
- CLI policies should minimize assumptions and prioritize verification.
- New skills should remain focused on a single primary responsibility.
- Cross-skill responsibilities should be explicit rather than implied.

## Template Registry

Deliverable-producing skills should use the following templates when available:

- requirements -> `templates/requirements.md`
- specification -> `templates/specification.md`
- issue -> `templates/issue.md`
- pr -> `templates/pr.md`

Guidelines:

- Canonical template paths are repository-relative and use `templates/<template-name>.md`.
- Templates are the canonical source of deliverable formatting.
- Skills define behavior, process, validation, and safety rules.
- Skills should reference templates rather than duplicate template structure.
- Missing template information should be marked as unavailable, unresolved, or open rather than invented.
- Template usage must preserve traceability to verified sources.
- New templates should be registered here when introduced.
- New deliverable-producing skills should reference an appropriate template when one exists.

Domain and Review outputs are intentionally skill-owned.

No reusable deliverable template is currently defined.

These skills primarily provide analysis, validation, design decisions, review findings, and workflow guidance rather than reusable deliverable artifacts.

If future reusable deliverables emerge, template adoption may be reconsidered.

## Artifact Lifecycle

Deliverables should preserve the existing workflow order:

```text
Requirements
->
Domain
->
Specification
->
Issue
->
Commit
->
Pull Request
->
Review
```

- Requirements, Domain, and Specification produce repository artifacts.
- Issue and Pull Request produce GitHub artifacts.
- Commit and Repository primarily modify repository state.
- Review produces review findings rather than reusable repository documents.
- Artifact flow must preserve traceability between repository artifacts and GitHub artifacts.

## Artifact Locations

Long-lived documentation artifacts should use the GitHub Wiki. The local Wiki worktree is:

```text
.agents/wiki-work/
|- Requirements.md
|- Domain.md
`- Specification.md
```

Artifact mapping:

- Requirements -> Wiki page `Requirements.md`
- Domain -> Wiki page `Domain.md`
- Specification -> Wiki page `Specification.md`
- Issue -> GitHub Issue
- Pull Request -> GitHub Pull Request
- Review -> GitHub Review / Review Findings

Guidelines:

- Edit Wiki artifacts locally under `.agents/wiki-work/`.
- Publish long-lived artifacts to the repository's GitHub Wiki.
- Do not create a parallel `docs/` tree for requirements, domain, or specification artifacts unless repository governance changes explicitly.
- Traceability between repository artifacts and GitHub artifacts must be preserved.

<language_policy>

- Language standards must distinguish agent-facing content from human-facing collaboration content.
- Skill documents must use English.
- Agent documents must use English.
- Repository governance documents intended for agent parsing or automation must use English.
- Commit messages must use English.
- Issue titles should use the repository team's primary communication language when they are human-facing collaboration artifacts.
- Issue descriptions should use the repository team's primary communication language when they are human-facing collaboration artifacts.
- Pull request titles should use the repository team's primary communication language when they are human-facing collaboration artifacts.
- Pull request descriptions should use the repository team's primary communication language when they are human-facing collaboration artifacts.
- Review comments should use the repository team's primary communication language.
- Human-facing collaboration artifacts should prioritize team communication effectiveness over unnecessary language standardization.
- Repository teams may define their primary communication language without changing agent-facing language requirements.
- Language requirements must remain independent of any specific human language.

</language_policy>

<review_rules>

- Evaluation must prioritize correctness, security, reliability, maintainability, and verification.
- Findings must be based on observable evidence.
- Feedback must distinguish defects, risks, questions, and preferences.
- Criteria must be applied consistently across contributors and change types.
- Conclusions must remain objective, specific, and actionable.

</review_rules>

<repository_rules>

- Ownership boundaries and source-of-truth locations must remain explicit.
- Existing structure and conventions must be respected unless a verified need requires change.
- Metadata and governance resources must be updated only when the change requires it.
- Long-lived rules must remain separate from temporary task context.

</repository_rules>

<agent_rules>

- Skills must reference shared rules instead of duplicating reusable guidance.
- Context must be collected from authoritative local or remote sources before decisions are made.
- Artifacts must be verified before they are reported as complete.
- Decision making must prioritize verified requirements, repository conventions, user intent, and risk reduction.
- Agents must minimize assumptions when relevant information can be obtained.
- Agents must preserve existing user work and avoid unauthorized destructive actions.
- Agents must keep outputs scoped to the requested work.

</agent_rules>

<cli_rules>

- CLI usage must support verification of repository state, file contents, Git history, and GitHub resources.
- Repository state must be verified before making decisions that depend on current files, branches, commits, or status.
- Existing files must be inspected before generating modifications.
- Git history must be inspected when change scope, ownership, regression risk, or workflow consistency depends on it.
- GitHub resources should be verified before creation or modification.
- Agent decisions should be based on verified information whenever possible.
- Assumptions should be minimized when repository information can be obtained through CLI.
- CLI results must be treated as context for decisions, not as a substitute for judgment.
- CLI commands must be scoped to the information or verification needed.

</cli_rules>
