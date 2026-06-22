# Specification Skill

<references>

- `skills/common.md`
- `skills/requirements.md`
- `skills/domain.md`

</references>

<specification_triggers>

- Use this skill when verified requirements and verified domain structures must be transformed into implementation-ready specifications.
- Use this skill for API specification generation.
- Use this skill for data model specification.
- Use this skill for ERD generation.
- Use this skill for flow generation.
- Use this skill for sequence diagram generation.
- Use this skill for business rule specification.
- Use this skill for state definition.
- Use this skill for validation rule definition.
- Use this skill for specification refinement.
- Use this skill after requirements validation and domain analysis.
- Do not use this skill as the primary skill for requirements gathering, requirements clarification, domain identification, domain decomposition, team allocation, architecture design, issue planning, commit generation, pull request creation, or code review.

</specification_triggers>

<api_specification>

- API specifications should define endpoints.
- API specifications should define request structures.
- API specifications should define response structures.
- API specifications should define validation requirements.
- API specifications should define error responses.
- API specifications should define status codes.
- API specifications should define ownership and responsibility boundaries when relevant.
- API specifications must remain traceable to verified requirements and verified domains.
- API specifications must remain consistent with data model, flow, sequence, state, business rule, and validation specifications when those artifacts exist.
- API specifications must not include implementation details.

</api_specification>

<data_model_specification>

- Data model specifications should define entities.
- Data model specifications should define relationships.
- Data model specifications should define constraints.
- Data model specifications should define ownership boundaries.
- Data model specifications should define persistence-related rules when required.
- Data model specifications must remain traceable to verified requirements and verified domains.
- Data model specifications must distinguish verified data rules from assumptions and unresolved decisions.
- Data model specifications must not prescribe implementation technologies unless repository governance or verified requirements require them.

</data_model_specification>

<erd_specification>

- ERD specifications should define entity relationships.
- ERD specifications should define cardinality.
- ERD specifications should define ownership boundaries.
- ERD specifications should define integrity constraints.
- ERD specifications must remain consistent with data model specifications.
- ERD specifications must preserve traceability to verified requirements, verified domains, and related data model specifications.
- ERD specifications must distinguish conceptual relationships from implementation storage choices when that distinction affects downstream planning.

</erd_specification>

<flow_specification>

- Flow specifications should define business flows.
- Flow specifications should define state transitions.
- Flow specifications should define workflow boundaries.
- Flow specifications should define decision points.
- Flow specifications should define exception paths.
- Flow specifications must remain traceable to verified business requirements.
- Flow specifications must remain consistent with state definitions, business rules, validation rules, and sequence specifications when those artifacts exist.
- Flow specifications must distinguish verified behavior from assumptions and unresolved decisions.

</flow_specification>

<sequence_specification>

- Sequence specifications should define interactions between actors and systems.
- Sequence specifications should define integration boundaries.
- Sequence specifications should define interaction ordering.
- Sequence specifications should define external service interactions when relevant.
- Sequence specifications must remain consistent with API and flow specifications.
- Sequence specifications must preserve traceability to verified requirements, verified domains, and relevant integration boundaries.
- Sequence specifications must not create architecture designs or implementation plans.

</sequence_specification>

<state_definition>

- State definitions should define valid states.
- State definitions should define transition rules.
- State definitions should define transition constraints.
- State definitions should define invalid transitions.
- State definitions must remain traceable to business rules.
- State definitions must remain consistent with flow specifications and validation rules when those artifacts exist.
- State definitions must distinguish verified state behavior from assumptions and unresolved decisions.

</state_definition>

<business_rules>

- Business rules should define domain policies.
- Business rules should define behavioral constraints.
- Business rules should define ownership rules.
- Business rules should define validation expectations.
- Business rules must distinguish verified rules from assumptions.
- Business rules must preserve traceability to verified requirements and verified domains.
- Business rules must remain independent from implementation planning and issue planning.

</business_rules>

<validation_rules>

- Validation rules should define input validation.
- Validation rules should define business validation.
- Validation rules should define transition validation.
- Validation rules should define consistency requirements.
- Validation rules must remain traceable to requirements and business rules.
- Validation rules must remain consistent with API specifications, data model specifications, flow specifications, and state definitions when those artifacts exist.
- Validation rules must distinguish validation expectations from implementation mechanisms.

</validation_rules>

<artifact_management>

- Specification artifacts should be generated in repository-defined locations.
- Specification artifact locations must follow repository governance resources when they exist.
- When no more specific Wiki page is defined, specifications should be written to `.agents/wiki-work/Specification.md` and published as the `Specification` Wiki page.
- Artifact generation should preserve traceability between requirements, domains, specifications, and downstream issues.
- Artifact generation should keep specification artifacts separate from temporary notes, implementation plans, issue plans, and review artifacts.
- Artifact generation must avoid duplicating rules already defined in referenced requirements, domain, or common skills.

</artifact_management>

<specification_validation>

- Specifications should be evaluated for completeness.
- Specifications should be evaluated for consistency.
- Specifications should be evaluated for traceability.
- Specifications should be evaluated for domain alignment.
- Specifications should be evaluated for requirement alignment.
- Specification validation should identify unresolved specification gaps before issue planning begins.
- Specification validation should identify conflicts between API, data model, ERD, flow, sequence, state, business rule, and validation artifacts.
- Specification validation must distinguish verified specification decisions from assumptions and unresolved questions.
- Specification validation must not create requirements, domain decompositions, architecture designs, implementation plans, or GitHub Issues.

</specification_validation>

<outputs>

- Outputs may include API specifications.
- Outputs may include ERDs.
- Outputs may include flow diagrams.
- Outputs may include sequence diagrams.
- Outputs may include state definitions.
- Outputs may include business rule definitions.
- Outputs may include validation rule definitions.
- Outputs may include specification documents.
- Outputs must support downstream issue planning.
- Outputs must preserve traceability to verified requirements, verified domains, and relevant repository artifacts.
- Outputs must distinguish verified specification decisions from assumptions and unresolved questions.
- Outputs must not include requirements gathering, domain decomposition, team allocation, architecture design, issue creation, implementation planning, commit generation, pull request creation, or code review findings.

</outputs>

## Output

Use `templates/specification.md`.

Populate all applicable sections.

Do not omit mandatory sections defined by the template.

Document only verified behavior and requirements.

Do not invent undocumented API behavior, fields, validation, errors, or business rules.

Place unresolved specification decisions under Open Questions.

Place unverified but necessary working assumptions under Assumptions.

The template is the canonical output format for specification deliverables.

## Success Criteria

- Specification artifacts cover the requested interfaces, models, flows, or rules.
- API specifications include request, response, validation, and error cases when applicable.
- Data models define entities, relationships, constraints, and ownership boundaries when applicable.
- Flow, sequence, and state specifications identify decision points and exception paths when applicable.
- Business and validation rules are explicit and traceable.
- Assumptions and unresolved specification gaps are documented.
- Specifications are consistent with verified requirements, domains, and related artifacts.

<trusted_sources>

- Verified requirements are authoritative for requested outcomes, constraints, scope, acceptance expectations, and business rules.
- Verified domain structures are authoritative for domain boundaries, ownership boundaries, dependencies, grouping, and domain responsibility.
- Existing repository artifacts are authoritative when they describe current APIs, data models, flows, sequences, states, business rules, validation rules, or constraints.
- Existing specifications are authoritative when they are current and consistent with verified requirements, verified domains, and repository state.
- Existing documentation is authoritative when it is current and consistent with verified requirements, verified domains, existing specifications, and repository state.
- Repository governance resources are authoritative for specification artifact locations, documentation expectations, ownership boundaries, and downstream workflow expectations.
- Specification decisions must prioritize verified information over assumptions.
- Conflicting information must be reconciled before specifications are finalized.
- Unverified information must be recorded as an assumption or unresolved question rather than treated as a specification decision.

</trusted_sources>

<cli_policy>

- CLI verification may be used when repository artifacts, existing specifications, documentation, or governance resources affect specification generation or validation.
- CLI usage should support verification rather than replace requirements validation, domain analysis, or specification judgment.
- Repository artifacts should be inspected when they affect APIs, data models, flows, sequences, states, business rules, validation rules, constraints, or artifact locations.
- Existing specifications and documentation should be inspected when they affect consistency, traceability, or current repository behavior.
- Git history should be inspected only when artifact provenance or historical specification decisions affect specification generation or validation.
- CLI verification must be scoped to specification generation and specification validation.

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
