# PR Skill

<references>

- `skills/common.md`

</references>

<pr_triggers>

- Use this skill when completed work must be prepared for pull request submission.
- Use this skill when an existing pull request must be updated.
- Use this skill when work must be prepared for review.
- Use this skill when review readiness must be validated.
- Use this skill when completed work must be summarized for reviewers.
- Use this skill when pull request labels or assignees must be assigned or validated.
- Do not use this skill as the primary skill for implementation, issue planning, repository governance, commit generation, or code review.

</pr_triggers>

<traceability_validation>

- Pull requests must preserve traceability across requirements, issues, commits, submitted changes, and related artifacts.
- Issue-to-PR traceability must identify the requested work that the pull request addresses.
- When a related issue exists, pull request creation must reference the issue in the pull request body so GitHub Development traceability can link the pull request and issue.
- Use a closing keyword such as `Closes #<issue-number>` when the pull request is expected to complete the issue; otherwise use a non-closing reference such as `Refs #<issue-number>`.
- Related label traceability must identify which pull request labels are derived from related issues, repository label strategy, or verified pull request scope.
- Assignee ownership traceability must identify issue ownership context, pull request author context, or verified repository ownership information when assignees are assigned.
- Commit-to-PR traceability must identify which commits contribute to the submitted work.
- Requirement traceability must identify the originating requirement, specification, task, or planning artifact when available.
- Artifact traceability must identify affected requirements, API specifications, data models, flows, sequence interactions, or architecture artifacts when relevant.
- Reviewers must be able to identify what work was requested, what work was completed, and what changes were submitted.
- Reviewers must be able to identify how related issues, labels, and assignee ownership context remain consistent when metadata is assigned.
- Traceability gaps must be identified before pull request submission.
- Traceability must be based on verified repository and GitHub information.

</traceability_validation>

<change_summary>

- Change summaries must be based on verified repository changes.
- Change summaries must explain what changed and why.
- Completed work must be summarized according to the actual submitted branch and commits.
- Repository impact must be described when changes affect structure, configuration, documentation, artifacts, or governance metadata.
- Behavioral changes must be identified when submitted changes affect user-visible or system-visible behavior.
- Documentation changes must be identified when repository documentation is created, updated, relocated, or removed.
- Artifact changes must be identified when requirements, specifications, diagrams, data models, or architecture artifacts are affected.
- Implementation details should be omitted unless they are necessary for reviewer understanding.
- Change summaries must avoid describing work that is not included in the pull request.

</change_summary>

<test_validation>

- Test validation must distinguish verified results from assumptions.
- Test reporting must identify what was tested.
- Validation reporting must identify what was verified.
- Verification completeness must identify what remains unverified.
- Test results must be based on executed checks, inspected artifacts, or verified repository state.
- Missing or unavailable validation must be identified before pull request submission.
- Required verification must align with the submitted scope and repository expectations.
- Reviewers must be able to understand validation confidence and remaining risk.

</test_validation>

<artifact_validation>

- Artifact consistency must be validated when submitted changes affect repository artifacts.
- Documentation consistency must be validated when submitted changes affect documented behavior, requirements, or repository expectations.
- Specification consistency must be validated when submitted changes affect API contracts, data models, flows, sequence interactions, or architecture decisions.
- Architecture consistency must be validated when submitted changes affect structural boundaries, integrations, or long-term design decisions.
- Affected requirements, API specifications, ERDs, flow diagrams, sequence diagrams, and architecture artifacts must remain traceable.
- Artifact changes must be consistent with related repository documentation and submitted code or configuration changes.
- Artifact validation gaps must be identified before pull request submission.

</artifact_validation>

<assignee_assignment>

- Pull requests may define assignees.
- Assignees must be verified repository collaborators when repository information is available.
- Assignee selection should remain consistent with issue ownership when applicable.
- Pull request creation should assign the pull request creator by default when the creator can be verified as a repository collaborator.
- Related issue assignees may be used instead of the default creator only when issue ownership is verified and more appropriate for the submitted work.
- An explicitly requested assignee overrides the default creator assignment only when the assignee is verified.
- Assignee values must not be invented.
- Missing assignee information should be reported rather than assumed.
- Assignee assignment decisions must remain traceable to verified repository information.
- Assignee assignment must not imply reviewer assignment, project assignment, milestone assignment, or merge automation.

</assignee_assignment>

<label_propagation>

- Pull request labels should align with related issue labels when applicable.
- Labels should be verified before assignment.
- Applicable labels may be derived from verified issue metadata.
- Pull request creation must attach all applicable verified labels that match the related work content.
- When a related issue exists, applicable labels should be propagated from the verified issue labels unless the submitted pull request scope clearly requires a different verified label set.
- Label assignment should remain consistent with repository label strategy.
- Missing labels must not be invented.
- Label inconsistencies between issues and pull requests should be identified when detected.
- Label assignment decisions must remain traceable to verified repository information.
- Label propagation must not create or modify repository label governance unless repository governance work is explicitly requested.

</label_propagation>

<review_readiness>

- Pull requests are review-ready only when traceability has been validated.
- Pull requests are review-ready only when the change summary accurately reflects submitted work.
- Pull requests are review-ready only when validation evidence is identified.
- Pull requests are review-ready only when affected artifacts and documentation are consistent or gaps are identified.
- Repository consistency must be checked when changes affect structure, configuration, templates, labels, branches, documentation, or artifacts.
- Pull requests that are not review-ready must be identified before submission.
- Review readiness must be based on verified branch, diff, commit, issue, and validation information.

</review_readiness>

<pr_title>

- Pull request titles must describe the submitted work clearly.
- Pull request titles must align with verified repository conventions.
- Pull request titles must reflect the primary outcome of the pull request.
- Pull request titles must not include classification prefixes.
- Do not add type, status, priority, domain, or workflow prefixes such as `feat:`, `fix:`, `[Feature]`, `[Bug]`, `PR:`, `Task:`, or similar markers to pull request titles.
- Classification must be represented through verified labels, pull request body context, and related issues rather than title prefixes.
- Pull request titles must avoid overstating scope.
- Pull request titles should remain concise.
- Pull request titles should use the repository team's primary communication language according to `skills/common.md`.

</pr_title>

<pr_description>

- Pull request descriptions must communicate purpose, scope, validation, and related work.
- Pull request descriptions must be readable and maintainable for reviewers and future maintainers.
- Pull request descriptions must be based on verified changes, commits, issues, and artifacts.
- Pull request descriptions must distinguish completed work from remaining or unverified work.
- Pull request descriptions must preserve traceability to related issues, requirements, commits, and artifacts when available.
- Pull request descriptions must avoid unrelated implementation details unless needed for reviewer understanding.
- Pull request descriptions should use the repository team's primary communication language according to `skills/common.md`.

</pr_description>

<template_usage>

- Pull request descriptions should use `templates/pr.md` when the template exists.
- The pull request template is the canonical pull request deliverable format.
- Template sections should be populated from verified requirements, verified specifications, issues, commits, artifacts, validation evidence, submitted changes, and pull request preparation results.
- Missing template information should be marked as unavailable or unresolved rather than invented.
- Template usage must preserve traceability between requirements, specifications, issues, commits, artifacts, validation evidence, and submitted changes.
- This skill must not duplicate the full pull request template content inside skill instructions.

</template_usage>

## Success Criteria

- Pull request template is applied when available.
- Required pull request template sections are populated or explicitly marked as unavailable.
- PR title reflects the submitted work and repository conventions without classification prefixes.
- PR description accurately summarizes purpose, scope, and changes.
- Linked issues, requirements, or artifacts are referenced when applicable.
- Related issues are referenced with closing or non-closing keywords according to whether the pull request completes the issue.
- Validation evidence is included or missing validation is stated.
- Labels and assignees match verified repository metadata.
- Assignee is set to the verified creator by default, or to a more appropriate explicitly requested or issue-derived verified assignee.
- Review readiness gaps are identified before submission.
- Pull request URL exists when PR creation is requested.

<trusted_sources>

- Issues are authoritative for requested work, scope, task planning, and acceptance expectations when the pull request is issue-linked.
- Related issue metadata is authoritative for issue labels, ownership context, classification, priority, and assignees when label or assignee assignment depends on issue state.
- Commits are authoritative for submitted change history and completed work units.
- Repository state is authoritative for current branch, modified files, submitted diffs, and working tree context.
- Repository artifacts are authoritative for affected requirements, specifications, data models, flows, sequence interactions, and architecture decisions.
- Repository documentation is authoritative when it defines expected behavior, repository expectations, or validation requirements.
- Git history is authoritative for branch commits, commit sequence, and change provenance.
- GitHub metadata is authoritative for existing pull requests, issue links, labels, branch metadata, and repository platform state when available.
- Verified repository labels are authoritative for pull request label assignment.
- Verified repository collaborators are authoritative for pull request assignee assignment.
- Repository governance resources are authoritative for branch conventions, templates, artifact expectations, and pull request readiness expectations.
- Existing pull requests are authoritative for active review state, previous submission context, and update requirements.
- Verified diffs are authoritative for the actual submitted changes.
- Validation evidence is authoritative for completed verification.
- Pull request decisions must prioritize verified information over assumptions.
- Conflicting sources must be reconciled before pull request creation or update.

</trusted_sources>

<cli_policy>

- CLI verification should be used before pull request generation when repository information can be obtained directly.
- Current branch must be verified before pull request creation or update.
- Commit history must be verified before change summarization and traceability validation.
- Related issues should be verified when pull request scope depends on issue definitions.
- Related issue metadata may be inspected when label or assignee assignment depends on issue ownership or issue classification.
- Pull request labels should be verified before assignment when repository information is available.
- Pull request assignees should be verified before assignment when repository information is available.
- Repository artifacts should be inspected when they affect traceability, consistency, or review readiness.
- Modified files and diffs must be verified before pull request description generation.
- Validation evidence must be verified before test reporting.
- Pull request preparation should rely on verified repository information whenever possible.

</cli_policy>

<allowed_cli_commands>

- `git status`
- `git status --short`
- `git branch`
- `git branch --all`
- `git remote --verbose`
- `git log`
- `git show`
- `git diff`
- `git diff --stat`
- `git rev-parse`
- `git ls-files`
- `gh issue list`
- `gh issue view`
- `gh label list`
- `gh pr list`
- `gh pr create`
- `gh pr view`
- `gh pr edit`
- `gh repo view`
- `gh repo view --json viewerPermission`
- `gh api`
- `rg`
- `ls`
- `find`
- `cat`
- `pwd`

</allowed_cli_commands>
