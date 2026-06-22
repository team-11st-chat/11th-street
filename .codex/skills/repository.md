# Repository Skill

<references>

- `skills/common.md`

</references>

<repository_triggers>

- Use this skill when repository initialization is requested or required.
- Use this skill when repository readiness must be assessed.
- Use this skill when repository governance resources must be created, verified, or modified.
- Use this skill when branch strategy is created, evaluated, or modified.
- Use this skill when label strategy is created, evaluated, or modified.
- Use this skill when repository templates are created, evaluated, or modified.
- Use this skill when repository structure is organized or reorganized.
- Use this skill when repository artifacts are organized, located, or governed.
- Use this skill when repository maintenance activities affect structure, governance, templates, labels, branches, or artifacts.
- Do not use this skill as the primary skill for feature implementation, issue authoring, commit creation, pull request writing, or review execution.

</repository_triggers>

<repository_initialization>

- Repository initialization must establish the minimum resources required for governance, collaboration, and maintenance.
- Initial setup must define the default branch, branch protection expectations, label taxonomy, repository templates, and artifact locations.
- Required repository resources must include source ownership boundaries, repository governance documentation, branch conventions, label categories, template locations, and artifact locations.
- Required repository governance resources must define who owns repository structure, branch policy, label taxonomy, templates, artifact locations, and maintenance standards.
- Minimum repository setup must make repository purpose, ownership, contribution expectations, review readiness expectations, and artifact locations discoverable.
- Repository readiness must be assessed by verifying that required resources exist, are discoverable, and are consistent with repository governance rules.
- Optional repository resources must be identified separately from required resources.
- Optional resources must be created only when they support verified repository needs.
- Repository bootstrap decisions must be based on verified repository state and intended repository usage.
- Required repository resources must be created only when they do not already exist or when existing resources are insufficient.
- Governance setup must define ownership expectations for repository structure, labels, templates, documentation, and protected branches.
- Initialization changes must remain separate from feature development work.

</repository_initialization>

<branch_strategy>

- The default branch must represent the repository's primary integration source.
- Working branches must be created only when the work requires isolation from the default branch.
- Branch naming conventions must be defined before repository-level branch creation.
- Branch names must communicate work category, ownership context, or tracking reference when required by repository convention.
- Branch lifecycle rules must define when branches are created, updated, merged, archived, or removed.
- Branch ownership must be clear for active work branches.
- Protected branches must be identified before changes that affect merge behavior or release state.
- Merge direction must be consistent with the repository's integration and release model.
- Branch creation decisions must verify current branch, remote branch state, protection rules, and existing branch conventions.
- Repository rules that affect branch creation must be checked before creating or modifying branches.

</branch_strategy>

<branch_conventions>

- Branch naming conventions must be explicitly defined before branch creation rules are applied.
- Default branch naming must be documented as a repository-level convention.
- Working branch categories must be defined according to the repository's collaboration and release model.
- Branch naming patterns must identify the required components for branch names without prescribing concrete branch names.
- Branch consistency requirements must apply to local branches, remote branches, protected branches, and working branches.
- Branch convention changes must verify existing active branches before adoption.
- Branch conventions must remain independent of technology stack and project domain.

</branch_conventions>

<label_strategy>

- Repository labels must be organized into stable categories that support triage, ownership, prioritization, and reporting.
- Label categories must be documented before large-scale label changes.
- Label ownership must be defined for creation, modification, deletion, and taxonomy changes.
- Label names must remain consistent in casing, prefixing, and meaning.
- Label descriptions must clarify intended usage without duplicating other labels.
- Label maintenance must remove ambiguity, duplication, and obsolete labels.
- Label governance changes must be verified against active issues and pull requests before modification.
- Repository-wide label standards must apply consistently across issue, pull request, and review workflows.

</label_strategy>

<required_label_categories>

- Type label categories must exist to classify the primary nature of repository work.
- Domain label categories must exist to identify the affected repository area when the area is known.
- Priority label categories must exist to communicate urgency, impact, and risk.
- Required label categories must be documented before individual labels are created or modified.
- Required label categories must be supported without prescribing concrete label values.
- Missing required label categories must be treated as a repository governance gap.

</required_label_categories>

<template_strategy>

- Issue templates must support consistent intake of repository-relevant requests.
- Pull request templates must support consistent review readiness and traceability.
- Shared repository templates must be maintained in a discoverable location.
- Templates must reflect current repository governance, artifact expectations, and review requirements.
- Template updates must avoid embedding temporary task context.
- Template maintenance must verify that configured templates are discoverable by the repository platform.
- Template strategy must remain independent of feature-specific implementation details.

</template_strategy>

<required_templates>

- Repositories must support issue templates when issue intake is managed through the repository platform.
- Repositories must support pull request templates when code or artifact changes are reviewed through pull requests.
- Required templates must be discoverable by contributors and repository maintainers.
- Required templates must align with repository governance, label strategy, branch strategy, and artifact strategy.
- Template requirements must define expected template coverage without prescribing template contents.
- Missing required templates must be treated as a repository readiness gap.

</required_templates>

<artifact_strategy>

- Repository artifacts must have defined locations based on artifact type and ownership.
- Requirements documents must be stored where they can be traced to related implementation and review work.
- API specifications must be maintained as authoritative interface artifacts when the repository owns API contracts.
- ERD documents must be maintained with related data model documentation when the repository owns persistent data structures.
- Flow diagrams must be stored with related process or user interaction documentation.
- Sequence diagrams must be stored with related interaction, integration, or protocol documentation.
- Architecture documents must be stored where repository maintainers can discover current structural decisions.
- Artifact updates must preserve consistency between source documents, generated representations, and repository references.

</artifact_strategy>

<required_artifacts>

- Repositories must support requirements artifacts when work is driven by explicit requirements.
- Repositories must support API specification artifacts when the repository owns or publishes interface contracts.
- Repositories must support data model artifacts when the repository owns persistent or shared data structures.
- Repositories must support flow artifacts when repository behavior depends on process, user, or system flows.
- Repositories must support architecture artifacts when structural decisions affect long-term maintainability or integration.
- Required artifact categories must be supported without prescribing specific document formats.
- Missing relevant artifact categories must be treated as a repository governance or readiness gap.

</required_artifacts>

<repository_structure>

- Directory organization must make primary source, tests, documentation, configuration, scripts, and artifacts discoverable.
- Documentation organization must separate long-lived repository guidance from temporary work notes.
- Configuration organization must keep project, tooling, environment, and platform configuration identifiable.
- Repository artifacts must have discoverable locations that can be found without skill-specific knowledge.
- Long-lived documents must be separated from temporary documents.
- Source code must be separated from repository governance resources.
- Repository governance resources must be grouped where maintainers can find branch, label, template, artifact, and maintenance rules.
- Ownership boundaries must be reflected in file placement, documentation grouping, and maintenance responsibilities.
- New directories must be introduced only when they improve discoverability or ownership clarity.
- Repository structure changes must account for existing conventions before introducing new organization patterns.

</repository_structure>

<repository_hygiene>

- Stale files must be identified by verifying current usage, references, and ownership.
- Generated files must be committed only when repository policy requires them.
- Unused resources must be removed or archived only after ownership and dependency impact are verified.
- Repository cleanliness must preserve a clear distinction between source files, generated artifacts, temporary files, and external outputs.
- Repository consistency must be maintained across naming, file placement, templates, labels, and documentation.
- Maintenance changes must avoid unrelated churn.
- Repository hygiene decisions must preserve traceability for removed or relocated artifacts.

</repository_hygiene>

## Success Criteria

- Requested repository governance or structure changes are reflected in repository state.
- Required branch, label, template, or artifact conventions are documented when requested.
- Existing repository resources are preserved unless verified changes require updates.
- New or updated resources are discoverable in repository-defined locations.
- Repository readiness gaps are reported when required resources are missing.
- GitHub metadata changes match verified repository governance when applicable.
- No unrelated repository changes are included.

<trusted_sources>

- Repository files are authoritative for committed source, documentation, configuration, templates, and artifacts.
- Repository structure is authoritative for current organization, discoverability, and ownership boundaries.
- Repository configuration is authoritative for configured repository behavior when it is committed or platform-backed.
- Git history is authoritative for committed changes, change sequence, and historical repository decisions.
- Git metadata is authoritative for local branch state, remote tracking state, remotes, and current working state.
- GitHub metadata is authoritative for platform-managed repository state, labels, issues, pull requests, branch protections, and repository settings when available.
- Repository templates are authoritative for current intake and review readiness expectations.
- Repository artifacts are authoritative for the requirements, interfaces, data models, flows, and architecture they govern.
- Repository documentation is authoritative when it is current, discoverable, and consistent with repository state.
- Repository governance resources are authoritative for branch, label, template, artifact, structure, and maintenance rules.
- Source priority must favor current repository state over assumptions.
- Conflicts between sources must be resolved by verifying the most current authoritative source for the decision being made.
- Decisions must identify whether the relevant source is local, remote, platform-managed, or documented.
- Unverified information must not override verified repository sources.

</trusted_sources>

<cli_policy>

- CLI verification must be performed before repository-level modifications are made.
- CLI verification should be preferred when repository state, branch state, file contents, Git history, or GitHub resources can be obtained directly.
- Repository initialization decisions must verify existing files, branches, remotes, labels, templates, and documentation locations.
- Branch strategy decisions must verify local and remote branch state.
- Label strategy decisions must verify existing labels and their current usage when available.
- Template strategy decisions must verify existing template files and repository platform configuration.
- Documentation and artifact decisions must verify existing artifact locations and references.
- CLI verification must be scoped to repository management decisions.

</cli_policy>

<allowed_cli_commands>

- `git status`
- `git branch`
- `git branch --all`
- `git remote --verbose`
- `git log`
- `git show`
- `git ls-files`
- `git diff`
- `git rev-parse`
- `git symbolic-ref`
- `gh repo view`
- `gh repo list`
- `gh label list`
- `gh label create`
- `gh label edit`
- `gh issue list`
- `gh pr list`
- `gh api`
- `ls`
- `find`
- `rg`
- `cat`
- `pwd`

</allowed_cli_commands>
