# Issue Skill

<references>

- `skills/common.md`

</references>

<issue_triggers>

- Use this skill when new GitHub Issues must be planned or created.
- Use this skill when verified requirements must be translated into issue plans.
- Use this skill when verified requirements or specifications must be converted into GitHub Issues.
- Use this skill when large work items must be decomposed into issue-sized units.
- Use this skill when backlog items must be created, refined, or validated.
- Use this skill when implementation work must be planned before coding begins.
- Use this skill when repository work must be prepared for assignment, tracking, or execution.
- Use this skill when issue labels, assignees, templates, or recommended working branch names must be selected or validated.
- Do not use this skill as the primary skill for implementation, commit generation, pull request creation, or code review.

</issue_triggers>

<requirement_analysis>

- Verified requirements must be consumed as input artifacts for issue planning.
- Requirement analysis in this skill must validate whether verified requirements are complete enough to support issue creation.
- Requested work must be understood only to the extent needed for issue classification, scope, readiness, labels, priority, dependencies, and branch naming.
- Scope validation must identify what is included, what is excluded, and what remains unresolved when those boundaries affect issue creation.
- Dependencies must be identified when issue planning depends on repository state, verified specifications, related issues, repository governance, GitHub metadata, or required input artifacts.
- Assumptions must be documented as unresolved unless they are verified by authoritative sources.
- Missing or incomplete requirements must be treated as blockers or planning gaps when they prevent issue-ready work definition.
- This skill must not conduct requirements gathering, requirements clarification interviews, or requirements discovery workflows.

</requirement_analysis>

<issue_decomposition_inputs>

- Issue decomposition must consume verified requirements, verified specifications, repository artifacts, repository documentation, existing issues, repository governance resources, and GitHub metadata.
- Input artifacts may include existing requirements documents, API specifications, ERD documents, flow diagrams, sequence diagrams, architecture artifacts, and repository artifacts when they already exist and are relevant.
- This skill must treat those artifacts as planning inputs and must not generate requirements, API specifications, ERDs, flow diagrams, sequence diagrams, architecture designs, or documentation.
- Issue-ready work units must represent meaningful units of repository work.
- Issue-ready work units should be independently actionable when repository dependencies allow it.
- Issue-ready work units should be independently verifiable when acceptance expectations can be separated.
- Issue-ready work units must avoid combining unrelated outcomes.
- Issue-ready work units must preserve traceability to originating requirements, specifications, artifacts, or related issues.
- Issue decomposition must prioritize implementation-ready issues.
- Issue decomposition must identify missing information that prevents issue readiness.
- Issue decomposition must not create implementation details.
- Issue decomposition must not generate code.

</issue_decomposition_inputs>

<work_decomposition>

- Work must be decomposed from verified requirements and specifications into issue-sized units that can be planned, assigned, implemented, and verified.
- GitHub Issues are the primary planning artifact produced by this skill.
- Issue decomposition must produce issue-ready work units directly.
- Issue decomposition must preserve traceability from requirements and specifications to issues.
- Issue boundaries must align with coherent outcomes, affected ownership areas, and repository impact.
- Issues should be independent enough to progress without hidden coupling.
- Related issues must preserve traceability to the originating requirement, specification, artifact, or parent work item.
- Oversized issues must be split when scope prevents clear ownership, readiness, implementation, or verification.
- Issue fragmentation must be avoided when splitting work would create unnecessary coordination cost.
- Implementation-ready work units must be prioritized over broad or ambiguous tracking items.
- Decomposition must preserve dependency ordering when work cannot be executed independently.
- This skill must not introduce a separate task artifact or intermediate planning layer between requirements, specifications, and GitHub Issues.

</work_decomposition>

<issue_classification>

- Issues must be classified according to verified work type, affected area, ownership area, risk, and repository governance.
- Issue categories must support planning, triage, assignment, reporting, and execution readiness.
- Work categorization must reflect the primary purpose of the issue.
- Ownership context must identify the affected area, maintainer context, or accountable repository boundary when that information is available.
- This skill must not allocate work to teams or people.
- Repository alignment must be verified against repository governance, label strategy, artifact strategy, and branch conventions.
- Classification must not rely on unverified assumptions.

</issue_classification>

<issue_title>

- Issue titles must describe the requested work clearly without classification prefixes.
- Do not add type, status, priority, domain, or workflow prefixes such as `feat:`, `fix:`, `[Feature]`, `[Bug]`, `Issue:`, `Task:`, or similar markers to issue titles.
- Classification must be represented through verified labels, issue body context, and related artifacts rather than title prefixes.
- Issue titles should reflect the primary requested outcome and avoid overstating scope.
- Issue titles should remain concise and use the repository team's primary communication language according to `skills/common.md`.

</issue_title>

<label_selection>

- Label selection must rely on the repository label strategy.
- The Issue Skill may select labels only from verified repository labels.
- Label applicability must be determined from verified issue scope, category, domain, priority, and readiness state.
- Issue creation must attach all applicable verified labels that match the related work content, including work type, affected area, priority, and readiness labels when those categories exist.
- Label consistency must be checked against existing label usage across related issues and pull requests.
- The Issue Skill may identify labels that are required but missing.
- Missing applicable labels or label categories must be reported as repository governance gaps.
- The Issue Skill must not create labels directly.
- Label creation must be handled by Repository Skill governance rules.
- If missing labels are required for issue creation, the agent may escalate to Repository Skill according to `AGENTS.md` multi-skill execution rules.
- Concrete label values must not be invented when repository labels have not been verified.

</label_selection>

<assignee_assignment>

- Issues may define assignees.
- Assignees must be verified repository collaborators when repository information is available.
- Issue creation should assign the issue creator by default when the creator can be verified as a repository collaborator.
- Assignee selection should remain consistent with ownership expectations when applicable.
- An explicitly requested assignee overrides the default creator assignment only when the assignee is verified.
- Assignee values must not be invented.
- Missing assignee information should be reported rather than assumed.
- Assignee assignment decisions must remain traceable to verified repository information.

</assignee_assignment>

<priority_assignment>

- Priority decisions must be evidence-based.
- Priority must account for user impact, repository impact, risk, dependency pressure, and urgency.
- Risk evaluation must consider correctness, reliability, security, maintainability, coordination, and delivery impact.
- Impact evaluation must distinguish local effects from broader repository or user-facing effects.
- Dependency evaluation must identify whether other work is blocked by the issue.
- Urgency evaluation must be based on verified timelines, commitments, regressions, or operational need.
- Priority must not be assigned from preference alone.

</priority_assignment>

<work_branch_definition>

- Expected working branch names may be defined for issue planning and traceability.
- Expected working branch names should be written into the issue body when the issue template supports it.
- Related issue or pull request references should be included when they are verified and needed for GitHub Development traceability.
- The branch name is a recommended implementation branch name.
- The branch name does not mean the branch already exists.
- Branch names must align with repository branch conventions.
- Branch names should preserve traceability between the issue and implementation work.
- Branch naming must reflect verified issue scope and repository rules.
- Branch names must not be defined when repository branch conventions are unknown or insufficiently verified.
- This skill must not create branches.
- Repository branch strategy remains governed by verified repository metadata and governance resources.

</work_branch_definition>

<template_usage>

- Issue descriptions should use `templates/issue.md` when the template exists.
- Template sections should be populated from verified requirements, verified specifications, repository artifacts, existing issue context, and issue decomposition results.
- Missing template information should be marked as unavailable or unresolved rather than invented.
- The `작업 브랜치` section should be populated when branch conventions are verified and a working branch name can be defined.
- The `작업 브랜치` section must not imply that the branch has already been created.
- Issue template usage must preserve traceability between requirements, specifications, artifacts, and GitHub Issues.
- This skill must not duplicate the full issue template content inside skill instructions.

</template_usage>

<issue_readiness>

- Issue readiness must be evaluated before issue creation when implementation work is expected.
- Ready issues must have clear scope, expected outcome, relevant context, and acceptance expectations.
- Dependency clarity must identify prerequisite work, blocked decisions, required artifacts, and related issues.
- Required input artifacts must be identified when verified requirements, specifications, diagrams, architecture references, or repository documentation are needed for issue creation.
- Required context must include the repository information needed to understand and execute the work.
- Acceptance expectations must be specific enough to evaluate completion.
- Missing information must be identified before an issue is treated as implementation-ready.
- Issues that are not implementation-ready must identify the verified input artifacts or decisions needed before issue creation or implementation readiness.
- This skill must not perform requirements interviews, domain design, team allocation, difficulty analysis, specification generation, architecture design, or documentation authoring to fill readiness gaps.

</issue_readiness>

## Success Criteria

- Issue scope is clear, actionable, and traceable to verified inputs.
- Issue template is applied when available.
- Title follows verified repository conventions when defined and does not include classification prefixes.
- Acceptance criteria are present and verifiable.
- Required labels are attached from verified repository labels.
- Assignee is set to the verified creator by default, or to the explicitly requested verified assignee.
- Recommended branch name is included only when conventions are verified.
- Verified related work is referenced when needed for GitHub Development traceability.
- GitHub issue URL exists when issue creation is requested.

<trusted_sources>

- Verified requirements are authoritative for requested outcomes and constraints.
- Verified specifications are authoritative for intended behavior, scope, and acceptance expectations.
- Repository artifacts are authoritative for existing requirements, interfaces, data models, flows, architecture, and issue planning context when relevant.
- Existing issues are authoritative for current backlog state, related work, prior decisions, and duplicate detection.
- Repository documentation is authoritative when it is current and consistent with repository state.
- Repository governance resources are authoritative for label strategy, branch conventions, templates, and artifact expectations.
- Repository issue templates are authoritative for issue body formatting when repository conventions define template usage.
- `templates/issue.md` is authoritative for agent-generated issue body formatting when it exists.
- Verified repository labels are authoritative for issue label selection.
- Verified repository collaborators are authoritative for issue assignee assignment.
- Repository state is authoritative for current files, branches, remotes, and working tree context.
- Git history is authoritative for historical issue-related changes and prior decisions when relevant.
- GitHub metadata is authoritative for platform-managed issue state, labels, milestones, assignees, and related pull requests when available.
- Existing pull requests are authoritative for active implementation work related to planned issues.
- Issue planning must prioritize verified information over assumptions.
- Issue planning must treat verified requirements, verified specifications, repository artifacts, repository documentation, existing issues, repository governance resources, and GitHub metadata as inputs rather than artifacts to generate.
- Issue decomposition must prioritize verified requirements, specifications, repository artifacts, existing issues, repository governance resources, GitHub metadata, and repository documentation.
- Conflicting sources must be reconciled before issue creation or modification.

</trusted_sources>

<cli_policy>

- CLI verification should be used when issue planning depends on repository state, existing issues, labels, artifacts, or documentation.
- Existing issues should be inspected when duplicate work, related work, or backlog state may affect issue creation.
- Repository labels should be verified before label selection.
- Repository collaborators should be verified before assignee assignment when repository information is available.
- Issue templates should be inspected before issue body generation when template-based issue creation is requested or repository conventions define a template.
- Repository artifacts should be discovered or inspected when they affect scope, dependencies, or readiness.
- Repository documentation should be inspected when it defines requirements, governance, or implementation context.
- GitHub resources should be verified before issue creation or modification when platform access is available.
- CLI verification must reduce assumptions and improve issue planning reliability.

</cli_policy>

<allowed_cli_commands>

- `gh issue list`
- `gh issue view`
- `gh issue create`
- `gh issue edit`
- `gh label list`
- `gh repo view`
- `gh repo view --json viewerPermission`
- `gh pr list`
- `gh api`
- `git status`
- `git branch`
- `git branch --all`
- `git remote --verbose`
- `git log`
- `git show`
- `git ls-files`
- `git diff`
- `rg`
- `ls`
- `find`
- `cat`
- `pwd`

</allowed_cli_commands>
