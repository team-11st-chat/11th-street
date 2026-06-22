# Requirements Skill

<references>

- `skills/common.md`

</references>

<requirements_triggers>

- Use this skill when new project discussions must be converted into verified requirements.
- Use this skill when feature requests or product requests need requirement discovery, refinement, or validation.
- Use this skill when requested outcomes, user goals, business objectives, constraints, assumptions, or scope boundaries must be clarified before downstream work begins.
- Use this skill when requirements readiness must be evaluated before domain design, specification generation, issue planning, or implementation.
- Use this skill when existing requirements must be reviewed for completeness, consistency, clarity, feasibility, or traceability.
- Do not use this skill as the primary skill for domain design, team allocation, difficulty analysis, API specification generation, ERD generation, flow generation, sequence diagram generation, architecture design, issue creation, commit generation, pull request creation, or code review.

</requirements_triggers>

<requirements_gathering>

- Requirements gathering must collect requested outcomes.
- Requirements gathering must collect user goals when they affect scope, constraints, validation, or downstream planning.
- Requirements gathering must collect business objectives when they are relevant to prioritization, acceptance expectations, constraints, or success criteria.
- Requirements gathering must collect functional requirements.
- Requirements gathering must collect non-functional requirements when quality attributes, performance, reliability, security, accessibility, maintainability, compliance, or operational expectations affect the requested outcome.
- Requirements gathering must collect constraints when they affect what can be delivered or how downstream work must be planned.
- Requirements gathering must collect assumptions when information is not yet verified but may affect requirement interpretation.
- Requirements gathering must prioritize understanding the requested outcome over proposing implementation details.
- Requirements gathering must distinguish verified requirements from assumptions, preferences, ideas, and unresolved questions.

</requirements_gathering>

<requirements_clarification>

- Requirements clarification must identify missing information.
- Requirements clarification must identify ambiguity.
- Requirements clarification must identify conflicting requirements.
- Requirements clarification must identify undefined behavior.
- Requirements clarification must ask targeted follow-up questions when required information cannot be verified from trusted sources.
- Follow-up questions must be minimized and scoped to information that affects scope, constraints, readiness, validation, or downstream planning.
- Clarification must not turn into domain design, architecture design, specification generation, issue planning, or implementation planning.
- Unresolved clarification points must remain explicit until they are answered or accepted as assumptions.

</requirements_clarification>

<scope_definition>

- Scope definition must identify included work.
- Scope definition must identify excluded work.
- Scope definition must identify unresolved work.
- Scope definition must identify dependencies when downstream planning depends on external decisions, repository state, existing artifacts, stakeholder input, or related work.
- Scope definition must identify assumptions that affect boundaries, expected outcomes, constraints, or acceptance expectations.
- Scope boundaries must be explicit whenever downstream domain design, specification generation, issue planning, or implementation depends on them.
- Scope definition must avoid assigning work to teams or people.
- Scope definition must avoid estimating difficulty unless feasibility concerns affect requirement readiness.

</scope_definition>

<constraint_identification>

- Constraints must be identified when they affect requirement interpretation, feasibility, readiness, or downstream planning.
- Business constraints may include budget, policy, stakeholder commitments, release obligations, market timing, contractual limits, or organizational requirements.
- Technical constraints may include platform limits, compatibility requirements, integration limits, performance bounds, security requirements, data constraints, or operational limits.
- Timeline constraints may include deadlines, sequencing expectations, release windows, migration windows, or dependency dates.
- Team constraints may include availability, ownership limits, approval requirements, communication boundaries, or required stakeholder participation.
- Platform constraints may include supported devices, browsers, operating systems, runtimes, hosting environments, third-party services, or deployment targets.
- Integration constraints may include external systems, API contracts, data exchange expectations, authentication requirements, or dependency availability.
- Compliance constraints may include legal, regulatory, privacy, accessibility, audit, retention, or governance requirements.
- Constraints must be distinguished from assumptions, preferences, and implementation choices.

</constraint_identification>

<requirement_validation>

- Requirements must be evaluated for completeness.
- Requirements must be evaluated for consistency.
- Requirements must be evaluated for clarity.
- Requirements must be evaluated for feasibility at the level needed to determine whether downstream planning can proceed.
- Requirements must be evaluated for traceability to user requests, stakeholder input, existing requirement documents, repository documentation, or other trusted sources.
- Validation must identify missing information that prevents downstream planning.
- Validation must identify conflicts that must be resolved before requirements are finalized.
- Validation must identify assumptions that remain unresolved.
- Validation must not produce domain models, API specifications, ERDs, flow diagrams, sequence diagrams, architecture designs, implementation plans, or GitHub Issues.

</requirement_validation>

<requirement_readiness>

- Requirements are ready when scope is sufficiently defined for downstream planning.
- Requirements are ready when constraints are sufficiently defined for downstream planning.
- Requirements are ready when assumptions are identified and either verified, accepted, or marked as unresolved.
- Requirements are ready when dependencies are identified.
- Requirements are ready when expected outcomes are clear enough for downstream domain design, specification generation, or issue planning to proceed.
- Requirements are ready when missing information no longer blocks the next requested workflow.
- Requirements that are not ready must identify remaining gaps, unresolved decisions, conflicting information, missing stakeholders, missing artifacts, or unverified assumptions.
- Readiness evaluation must distinguish requirements that are ready for downstream planning from requirements that are ready for implementation.

</requirement_readiness>

<outputs>

- The primary output of this skill is a verified requirements artifact.
- Outputs may include requirement summaries.
- Outputs may include requirement documents.
- Outputs may include requirement decisions.
- Outputs may include requirement constraints.
- Outputs may include requirement assumptions.
- Outputs must separate verified requirements from assumptions and unresolved questions.
- Outputs must preserve traceability to trusted sources when source information affects downstream planning.
- Outputs must support downstream domain design, specification generation, and issue planning.
- Outputs must not include domain design, API specifications, ERDs, flow diagrams, sequence diagrams, architecture designs, GitHub Issues, commits, pull requests, or code review findings.

</outputs>

## Output

Use `templates/requirements.md`.

Populate all applicable sections.

Do not omit mandatory sections defined by the template.

Document only verified requirements.

Place unknown information under Open Questions instead of guessing.

Place unverified but necessary working assumptions under Assumptions.

Do not invent business rules, requirements, or policies.

Acceptance Criteria should describe observable outcomes rather than implementation details.

If requirements are incomplete, explicitly indicate this in Readiness Assessment.

The template is the canonical output format for requirement deliverables.

## Success Criteria

- Requested outcomes are stated as verified requirements.
- Functional requirements are complete enough for downstream planning.
- Relevant non-functional requirements and constraints are documented.
- Assumptions are separated from verified requirements.
- Open questions identify unresolved decisions or missing information.
- Requirement readiness is stated for the next requested workflow.
- Traceability to source requests or trusted artifacts is preserved.

<trusted_sources>

- User requests are authoritative for requested outcomes, intent, priorities, and constraints when they are current and unambiguous.
- Verified stakeholder requirements are authoritative for stakeholder goals, business objectives, constraints, and acceptance expectations.
- Existing requirement documents are authoritative when they are current and consistent with verified stakeholder input.
- Existing repository documentation is authoritative when it describes current behavior, constraints, governance, or existing requirements.
- Existing project artifacts are authoritative when they are current, relevant, and consistent with verified requirements.
- Repository governance resources are authoritative for requirement artifact locations, documentation expectations, ownership boundaries, and downstream workflow expectations.
- Repository files are authoritative for current repository behavior only when requirement validation depends on existing implementation or documentation.
- Requirements decisions must prioritize verified information over assumptions.
- Conflicting information must be reconciled before requirements are finalized.
- Unverified information must be recorded as an assumption or unresolved question rather than treated as a requirement.

</trusted_sources>

<cli_policy>

- CLI verification may be used when repository artifacts, existing documentation, prior requirements, or governance resources affect requirement gathering or validation.
- CLI usage should support verification rather than replace stakeholder input or requirement clarification.
- Repository files should be inspected when they provide current context needed to validate existing behavior, constraints, documentation, or prior requirements.
- Git history should be inspected only when historical requirement decisions, prior behavior, or artifact provenance affect requirement validation.
- CLI verification must be scoped to requirement gathering, requirement validation, and requirement readiness.

</cli_policy>

<allowed_cli_commands>

- `git ls-files`
- `git show`
- `git log`
- `cat`
- `rg`
- `find`
- `ls`
- `pwd`

</allowed_cli_commands>
