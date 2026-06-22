# Commit Skill

<references>

- `skills/common.md`

</references>

<commit_triggers>

- Use this skill when repository changes must be prepared for commit.
- Use this skill when staged changes must be reviewed before commit creation.
- Use this skill when a commit message must be generated or validated.
- Use this skill when commit scope must be validated.
- Use this skill when commit readiness must be verified.
- Do not use this skill as the primary skill for pull request preparation, code review, repository governance, or implementation planning.

</commit_triggers>

<change_analysis>

- Change analysis must be based on verified repository state.
- Modified files must be analyzed according to content change, ownership, and repository impact.
- Staged files must be distinguished from unstaged files when commit readiness depends on staging state.
- Repository impact must be understood in terms of behavior, documentation, configuration, artifacts, and governance metadata.
- Issue alignment must be verified when the commit is associated with planned work.
- Related changes must be identified by their direct connection to the same work unit.
- Unrelated changes must be identified before commit creation.
- Accidental changes must be separated from intentional changes when they can be detected.
- Change analysis must preserve user or teammate changes that are outside the intended commit scope.

</change_analysis>

<scope_validation>

- A commit should represent a single work unit.
- Related changes may be included when they are necessary to complete, validate, or support the work unit.
- Scope validation must identify unrelated changes.
- Scope validation must identify accidental changes.
- Scope validation must preserve traceability between the issue, task, implementation change, and commit.
- Scope validation must preserve implementation clarity by keeping the commit purpose understandable.
- Changes outside the original issue scope may be included only when they are necessary, justified, and directly connected to the work unit.
- Unrelated cleanup, formatting, refactoring, or documentation changes should be separated unless required by the work unit.
- Scope validation must not assume that every modified file belongs to a different work unit.
- Commit scope must be based on change intent and verified file contents.

</scope_validation>

<commit_classification>

- Commit classification must rely on verified change intent.
- Commit type selection must reflect the primary purpose of the committed work.
- Commit intent classification must distinguish behavior changes, documentation changes, configuration changes, artifact changes, maintenance changes, and governance changes when relevant.
- Repository commit conventions must be followed when they are defined and verified.
- Commit classification must remain consistent with issue classification when the commit is linked to an issue.
- Commit classification must not be inferred from file paths alone when content indicates a different intent.

</commit_classification>

<commit_message>

- Commit messages must describe the actual change.
- Commit messages must remain concise.
- Commit messages must align with verified repository conventions.
- Commit messages must use English according to `skills/common.md`.
- Commit messages should preserve traceability to the associated issue or work unit when repository convention supports it.
- Commit messages must avoid overstating scope.
- Commit messages must not describe unrelated changes that are not included in the commit.
- Commit messages must reflect the staged content when staged content differs from the full working tree.

</commit_message>

<commit_readiness>

- Changes are commit-ready only when scope has been validated.
- Changes are commit-ready only when staged content matches the intended work unit.
- Issue alignment must be verified when the commit is associated with an issue.
- Related changes must be included only when they support the same work unit.
- Unrelated changes must be excluded or identified before commit creation.
- Required verification must be completed or explicitly identified as unavailable.
- Repository consistency must be checked when changes affect structure, documentation, configuration, artifacts, or governance metadata.
- Commit readiness must account for unstaged changes that may indicate incomplete or accidental work.
- Changes that are not commit-ready must be identified before commit creation.

</commit_readiness>

## Success Criteria

- Commit scope represents a single verified work unit.
- Staged changes match the intended commit scope.
- Unrelated or accidental changes are excluded or explicitly identified.
- Commit message describes the actual staged changes.
- Commit message follows verified repository conventions when defined.
- Issue or work-unit traceability is included when applicable.
- Required verification is completed or reported as unavailable.

<trusted_sources>

- Issue definitions are authoritative for planned work scope, expected outcomes, and traceability when the commit is issue-linked.
- Repository state is authoritative for modified files, staged files, unstaged files, current branch, and current working tree status.
- Git history is authoritative for previous commits, repository convention evidence, and related change context.
- GitHub metadata is authoritative for issue metadata, related pull requests, labels, and repository platform state when available.
- Repository artifacts are authoritative when committed changes affect requirements, interfaces, data models, flows, or architecture.
- Repository documentation is authoritative when it defines expected behavior, conventions, or maintenance requirements.
- Repository governance resources are authoritative for branch conventions, commit conventions, artifact expectations, and repository rules.
- Existing pull requests are authoritative for active review or integration context when related to the commit scope.
- Verified file contents are authoritative for what the commit actually changes.
- Commit decisions must prioritize verified information over assumptions.
- Conflicting sources must be reconciled before commit creation.

</trusted_sources>

<cli_policy>

- CLI verification should be used before commit generation when repository information can be obtained directly.
- Modified files must be verified before commit readiness is determined.
- Staged files must be verified before commit message generation.
- Git history should be inspected when repository conventions, related changes, or prior decisions affect commit classification.
- Current branch must be verified when issue traceability or branch conventions affect commit readiness.
- Related repository artifacts should be inspected when they affect scope, verification, or traceability.
- CLI verification must distinguish staged content from unstaged content.
- Commit preparation should rely on verified repository information whenever possible.

</cli_policy>

<allowed_cli_commands>

- `git status`
- `git status --short`
- `git diff`
- `git diff --staged`
- `git diff --cached`
- `git log`
- `git show`
- `git branch`
- `git branch --all`
- `git remote --verbose`
- `git rev-parse`
- `git ls-files`
- `gh issue list`
- `gh issue view`
- `gh repo view`
- `rg`
- `ls`
- `find`
- `cat`
- `pwd`

</allowed_cli_commands>
