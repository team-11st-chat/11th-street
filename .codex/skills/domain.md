# Domain Skill

<references>

- `skills/common.md`
- `skills/requirements.md`

</references>

<domain_triggers>

- Use this skill when verified requirements must be transformed into domain structures.
- Use this skill for domain identification.
- Use this skill for domain decomposition.
- Use this skill for domain boundary analysis.
- Use this skill for domain allocation guidance.
- Use this skill for ownership analysis.
- Use this skill for dependency analysis.
- Use this skill for domain complexity analysis when allocation guidance depends on workload balance.
- Use this skill after requirements validation and before specification generation.
- Do not use this skill as the primary skill for requirements gathering, requirements clarification, API specification generation, ERD generation, flow generation, sequence diagram generation, architecture design, issue creation, commit generation, pull request creation, or code review.

</domain_triggers>

<domain_identification>

- Domains should be identified from verified requirements.
- Domain identification must identify business capabilities.
- Domain identification must identify responsibility boundaries.
- Domain identification must identify ownership boundaries.
- Domain identification must identify cohesive functional areas.
- Domains should represent meaningful responsibility groups rather than implementation units.
- Domain identification must avoid creating implementation details.
- Domain identification must preserve traceability to the verified requirements that justify each domain.

</domain_identification>

<domain_boundary_definition>

- Domain boundaries should separate responsibilities clearly.
- Domain boundaries should minimize unnecessary coupling.
- Domain boundaries should preserve cohesion.
- Domain boundaries should reduce cross-domain dependencies when practical.
- Boundary definitions should prioritize maintainability and ownership clarity.
- Boundary definitions must distinguish domain responsibility from implementation structure.
- Boundary definitions must remain traceable to verified requirements and relevant repository artifacts.

</domain_boundary_definition>

<domain_dependency_analysis>

- Domain dependencies must identify upstream dependencies.
- Domain dependencies must identify downstream dependencies.
- Domain dependencies must identify shared dependencies.
- Domain dependencies must identify integration dependencies.
- Dependencies should be explicit.
- Dependency analysis should identify coupling risks.
- Dependency analysis must distinguish required dependencies from assumed dependencies.
- Dependency analysis should preserve traceability to the requirements or artifacts that establish each dependency.

</domain_dependency_analysis>

<domain_grouping>

- Domain grouping should support team allocation.
- Domain grouping should support ownership planning.
- Domain grouping should support specification planning.
- Domain grouping should support downstream planning without creating implementation plans.
- Closely coupled domains may be grouped when separation would introduce unnecessary coordination cost.
- Domain grouping must preserve meaningful responsibility boundaries.
- Domain grouping must avoid grouping unrelated outcomes solely for convenience.
- Domain grouping should identify coordination needs when grouped domains still have distinct responsibilities.

</domain_grouping>

<domain_complexity_analysis>

- Domain complexity may be evaluated when allocation guidance depends on workload balance.
- Complexity analysis may consider business complexity.
- Complexity analysis may consider dependency complexity.
- Complexity analysis may consider integration complexity.
- Complexity analysis may consider operational complexity.
- Complexity analysis may consider ownership complexity.
- Complexity analysis should support domain grouping decisions.
- Complexity analysis should support allocation guidance.
- Complexity analysis must remain independent from implementation estimates.
- Complexity analysis must remain independent from project scheduling.
- Complexity analysis must remain independent from issue planning.
- Complexity analysis must avoid technology-specific implementation assumptions.
- Complexity analysis must preserve traceability to verified requirements, domain boundaries, and dependency analysis.

</domain_complexity_analysis>

<domain_allocation_strategy>

- Domain allocation may consider domain complexity.
- Domain allocation may consider workload balance.
- Domain allocation may consider team size.
- Domain allocation may consider responsibility balance.
- Domain allocation may consider dependency pressure.
- Domain allocation may consider ownership clarity.
- Domain allocation may consider coordination cost.
- Domain allocation may recommend domain distribution patterns suitable for different team sizes.
- Allocation strategies must not assign work to specific people.
- Allocation strategies should define allocation guidance only.
- Allocation strategies should identify domain ownership considerations without creating implementation plans, issue plans, or project schedules.
- Allocation strategies must remain independent from implementation planning.
- Allocation strategies should preserve traceability to domain boundaries, domain complexity analysis, dependencies, and verified requirements.

</domain_allocation_strategy>

<domain_validation>

- Domains should be evaluated for cohesion.
- Domains should be evaluated for coupling.
- Domains should be evaluated for ownership clarity.
- Domains should be evaluated for dependency complexity.
- Domains should be evaluated for maintainability.
- Domain structures should support downstream specification generation.
- Domain structures should be validated against verified requirements.
- Domain structures should identify unresolved boundary, ownership, or dependency concerns.
- Domain validation must not produce API specifications, ERDs, flow diagrams, sequence diagrams, architecture designs, implementation plans, or GitHub Issues.

</domain_validation>

<outputs>

- Outputs may include domain definitions.
- Outputs may include domain boundaries.
- Outputs may include domain dependency maps.
- Outputs may include domain allocation recommendations.
- Outputs may include domain ownership guidance.
- Outputs must support downstream specification generation.
- Outputs must preserve traceability to verified requirements and relevant artifacts.
- Outputs must distinguish verified domain decisions from assumptions and unresolved questions.
- Outputs must not include requirements gathering, specification generation, issue planning, implementation planning, review workflows, or code generation.

</outputs>

## Standard Output Format

### Domain Decision
- Recommended domain ownership and boundaries.
- Explain why the responsibility belongs to the selected domain.

### Entities / Value Objects
- Main entities, value objects, and their responsibilities.
- Include only entities relevant to the requested feature or design question.

### Relationships
- Relationships with other domains or aggregates.
- Ownership, dependency direction, and coupling concerns.

### Business Rules
- Domain invariants, lifecycle rules, and state transitions.
- Include validation rules only when they are part of domain policy.

### Transaction Boundary
- Recommended transaction scope.
- Identify which operations should be atomic and which can be handled asynchronously.

### Risks / Open Questions
- Ambiguous ownership, missing policy decisions, or integration risks.
- List only unresolved items that affect implementation or design correctness.

## Success Criteria

- Domain boundaries are defined from verified requirements.
- Domain responsibilities are cohesive and non-overlapping.
- Key entities or capability areas are identified.
- Domain relationships and dependencies are documented.
- Ownership guidance is provided when supported by verified context.
- Coupling risks or unresolved boundary questions are identified.
- Domain decisions remain consistent with verified requirements and artifacts.

<trusted_sources>

- Verified requirements are authoritative for domain identification, boundaries, dependencies, grouping, and allocation guidance.
- Requirement artifacts are authoritative when they are current and consistent with verified requirements.
- Existing repository artifacts are authoritative when they describe current domains, responsibilities, dependencies, ownership, or constraints.
- Existing documentation is authoritative when it is current and consistent with verified requirements and repository state.
- Existing domain definitions are authoritative when they are current and consistent with verified requirements and repository artifacts.
- Repository governance resources are authoritative for ownership boundaries, artifact expectations, documentation locations, and downstream workflow expectations.
- Domain decisions must prioritize verified information over assumptions.
- Conflicting information must be reconciled before domain structures are finalized.
- Unverified information must be recorded as an assumption or unresolved question rather than treated as a domain decision.

</trusted_sources>

<cli_policy>

- CLI verification may be used when repository artifacts, existing documentation, or existing domain structures affect domain analysis.
- CLI usage should support verification rather than domain discovery.
- Repository artifacts should be inspected when they affect domain boundaries, dependencies, ownership, grouping, or allocation guidance.
- Existing documentation should be inspected when it defines current domain responsibilities or constraints.
- Git history should be inspected only when artifact provenance or historical domain decisions affect domain analysis.
- CLI verification must be scoped to domain analysis and domain validation.

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
