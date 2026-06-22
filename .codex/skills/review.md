# Review Skill

<references>

- `skills/common.md`

</references>

<review_triggers>

- Use this skill when a pull request must be reviewed.
- Use this skill when a file, diff, code block, or line range must be reviewed.
- Use this skill when a review comment must be evaluated.
- Use this skill when review feedback must be answered.
- Use this skill when previously reported concerns must be verified.
- Use this skill when review outcomes must be determined.
- Do not use this skill as the primary skill for implementation, issue planning, commit generation, pull request preparation, or repository governance.

</review_triggers>

<review_modes>

- Review execution represents reviewing submitted work.
- Review response represents responding to review feedback.
- Review verification represents validating whether previously reported concerns have been addressed.
- Review mode must be identified before review conclusions are generated.
- Review execution must evaluate the submitted work against verified requirements, diffs, artifacts, and validation evidence.
- Review response must evaluate the feedback, the author's reasoning, and any implemented changes.
- Review verification must evaluate current pull request state rather than relying on previous conclusions.

</review_modes>

<review_lifecycle>

- Review lifecycle activities include Review Execution, Review Response, Review Verification, and Review Decision.
- Review Execution establishes findings and conclusions for submitted work.
- Review Response evaluates how review feedback is handled by the author or reviewer.
- Review Verification evaluates whether previously reported concerns have been addressed.
- Review Decision determines the current review outcome from verified review state.
- Review conclusions may change when new commits are added.
- Review conclusions may change when new evidence is introduced.
- Review conclusions may change when new review comments are created.
- Review conclusions may change when existing concerns are re-evaluated.
- Review conclusions must be based on the current pull request state.
- Previous review conclusions must not prevent re-evaluation.

</review_lifecycle>

<review_scope>

- Supported review scopes include pull request scope, review summary scope, file scope, diff scope, code block scope, line range scope, review comment scope, and conversation scope.
- Review feedback should be attached to the smallest verified scope that accurately represents the concern.
- Line-level feedback should be preferred when the concern is localized.
- File-level or diff-level feedback should be used when the concern spans related changes in a single file or diff.
- Pull request-level feedback should be used when the concern affects the submitted work as a whole.
- Review comment scope should be used when evaluating or responding to existing feedback.
- Conversation scope represents an existing review discussion thread.
- Conversation scope should be used when reviewing discussion context, responding to discussion feedback, verifying discussion resolution, or determining whether a conversation can be resolved.
- Scope selection must be based on verified evidence and reviewer actionability.

</review_scope>

<review_execution>

- Requirement validation must verify whether submitted work satisfies the requested behavior, constraints, and acceptance expectations when available.
- Artifact validation must verify consistency with affected requirements, specifications, diagrams, data models, flows, sequence interactions, and architecture artifacts when relevant.
- Code validation must evaluate correctness, reliability, security, maintainability, readability, and consistency with submitted scope.
- Test validation must verify whether available tests or checks support the submitted changes.
- Risk identification must identify uncertainty that may affect correctness, reliability, security, maintainability, delivery, or coordination.
- Defect identification must be based on verified behavior, code, diff, artifact, or validation evidence.
- Review findings must distinguish defects, risks, suggestions, and questions.
- Review conclusions must remain objective and actionable.

</review_execution>

<review_severity>

- Review findings must be classified as Blocking or Non-Blocking when severity affects review decisions.
- Blocking findings represent concerns that should be resolved before merge.
- Blocking findings may result in Request Changes decisions.
- Blocking findings must be supported by verified evidence.
- Non-Blocking findings represent suggestions, observations, questions, or improvements.
- Non-Blocking findings do not prevent merge.
- Non-Blocking findings may be communicated through review comments or review summaries.
- Severity classification must remain independent from review scope.
- Severity classification must remain independent from review response handling.
- Severity classification must be based on verified evidence and repository expectations.

</review_severity>

<review_context>

- Review conclusions should apply only to the active reviewer unless repository governance defines collective review rules.
- Approval by one reviewer must not imply approval by other reviewers.
- Request Changes by one reviewer must not imply Request Changes by other reviewers.
- Review decisions should be evaluated from the perspective of the active reviewer.
- Review conclusions should remain traceable to the reviewer that produced them.
- Review context must support repositories with multiple reviewers.
- Collective review behavior must be based on verified repository governance when it is applied.

</review_context>

<review_boundaries>

- Review decisions represent reviewer conclusions.
- Review decisions do not imply merge authorization.
- Approval does not automatically imply merge approval.
- Merge decisions remain outside the scope of review activities unless repository governance explicitly defines otherwise.
- Review outcomes and merge outcomes must remain separate concepts.
- Review conclusions should remain traceable to the reviewer that produced them.
- Review activities must not assume repository-level merge authority.
- Repository-level merge authority must be determined from verified repository governance and platform rules.

</review_boundaries>

<review_comment_generation>

- Review comments must be based on verified evidence.
- Review comments must clearly identify the concern.
- Review comments must explain the impact of the concern.
- Review comments must explain the reason for the concern.
- Review comments must distinguish blocking and non-blocking feedback.
- Comment scope must match the smallest accurate review scope.
- Comments should be actionable enough for the author to respond or make a change.
- Subjective preferences should be avoided unless they are clearly identified as preferences.
- Review comments should not use emojis or decorative symbols.
- Review comments should use the repository team's primary communication language according to `skills/common.md`.

</review_comment_generation>

<review_summary_generation>

- Review summaries must consolidate review findings.
- Review summaries must separate blocking concerns from non-blocking concerns.
- Review summaries must summarize overall review status.
- Review summaries must identify remaining risks.
- Review summaries must preserve reviewer reasoning.
- Review summaries must remain traceable to supporting review evidence.
- Review summaries must distinguish verified conclusions from unresolved questions.

</review_summary_generation>

<review_response>

- Review responses must evaluate feedback objectively.
- Review responses must distinguish agreement from disagreement.
- Review responses must explain reasoning.
- Review responses must explain implemented changes when applicable.
- Review responses must preserve professional communication.
- Review responses may accept feedback.
- Review responses may reject feedback with justification.
- Review responses may request clarification.
- Review responses may propose alternative solutions.
- Review responses must classify feedback handling as Accepted, Rejected, Clarification Requested, or Alternative Proposed when a response outcome is needed.
- Accepted feedback handling means the feedback is agreed with and addressed or planned for action.
- Rejected feedback handling means the feedback is not accepted and the response provides justification.
- Clarification Requested feedback handling means the feedback cannot be acted on without additional information.
- Alternative Proposed feedback handling means a different solution is proposed to address the underlying concern.
- Feedback handling classification must remain separate from review outcome decisions.
- Review responses must not claim resolution without verified supporting changes or reasoning.

</review_response>

<review_verification>

- Previously reported concerns must be re-evaluated against the updated implementation.
- Resolved concerns should be marked as resolved when the concern has been adequately addressed.
- Review conversations should not be resolved when the original concern remains unresolved.
- Newly discovered concerns must be reported separately.
- Review verification must evaluate the current state of the pull request.
- Review verification must not rely solely on previous review conclusions.
- Verification must distinguish fully resolved, partially resolved, unresolved, and superseded concerns when relevant.
- Fully Resolved concerns have been adequately addressed by verified implementation changes, verified evidence, or verified reasoning.
- Partially Resolved concerns have been addressed in part but still leave material concern or uncertainty.
- Unresolved concerns remain materially unaddressed.
- Superseded concerns no longer apply because the reviewed code, requirement, artifact, or context has changed.
- Concern status must be based on verified implementation changes, verified evidence, or verified reasoning.

</review_verification>

<review_decision>

- Comment decisions must be used for non-blocking feedback, suggestions, observations, or questions.
- Comment decisions must not prevent merge.
- Blocking findings may justify Request Changes decisions.
- Non-Blocking findings alone should not justify Request Changes decisions.
- Request Changes decisions must be used when one or more blocking concerns exist from the active reviewer's perspective.
- Request Changes decisions must be used when merge should not proceed until concerns are addressed.
- Blocking concerns must be actionable and supported by evidence.
- Approve decisions require all previously identified blocking concerns associated with the active reviewer to be resolved.
- Approve decisions require all request-changes feedback to be satisfactorily addressed.
- Approve decisions require that no new blocking concerns have been identified.
- Approve decisions require that no unresolved blocking review conversations associated with the active reviewer remain.
- Approve decisions must be based on the current pull request state.
- Previous approval eligibility must be re-evaluated when new commits, new evidence, or new blocking concerns are introduced.
- Approval must not be granted solely because previous blocking concerns were resolved.
- Approval must reflect the current review state.
- Approval decisions should remain reviewer-specific unless repository governance defines collective approval behavior.
- Approve decisions indicate review acceptance by the active reviewer.
- Approve decisions do not imply merge authorization.
- Approve decisions do not bypass repository governance rules.
- Approve decisions do not override branch protection rules.
- Approve decisions remain reviewer decisions rather than repository decisions.
- Resolve Conversation decisions must be used only when a previously reported concern has been adequately addressed.
- Resolve Conversation decisions must not be used when the concern remains unresolved.
- Resolve Conversation decisions must be based on verified implementation changes or verified reasoning.

</review_decision>

<review_readiness>

- Review can begin when pull request completeness is sufficient to evaluate submitted work.
- Traceability completeness should be verified before review conclusions are generated.
- Validation evidence availability should be checked before test-related conclusions are made.
- Artifact consistency should be checked when submitted work affects repository artifacts.
- Repository consistency should be checked when submitted work affects structure, configuration, documentation, artifacts, or governance metadata.
- Missing review prerequisites should be identified before review execution.
- Review readiness must be based on verified pull request state, diffs, commits, artifacts, and validation evidence.

</review_readiness>

## Standard Output Format

### Findings

#### Critical
- Blocking issues that must be resolved before approval.

#### Major
- Significant issues affecting correctness, maintainability, security, reliability, or requirements compliance.

#### Minor
- Non-blocking improvements, cleanups, or suggestions.

### Severity Mapping
- Critical findings are Blocking findings and support Request Changes decisions.
- Major findings may be Blocking or Non-Blocking based on verified impact.
- Minor findings are Non-Blocking findings and support Comment decisions.
- Review recommendations must follow the current Blocking and Non-Blocking review decision rules.

### Recommendation
- Approve
- Request Changes
- Comment

### Validation Notes
- Evidence supporting findings.
- Assumptions made during review.
- Areas that could not be verified.

## Success Criteria

- Review mode and scope are identified.
- Findings are categorized by blocking or non-blocking severity.
- Every finding references supporting evidence from verified sources.
- Risks, questions, suggestions, and defects are clearly distinguished.
- Review comments are attached to the smallest accurate scope when comments are produced.
- Previously reported concerns are classified by current resolution status when verification is requested.
- Final recommendation is provided from the active reviewer's perspective.
- No unsupported assumptions are included in review conclusions.

<trusted_sources>

- Pull requests are authoritative for submitted scope, branch state, review state, and current review context.
- Review comments are authoritative for previously reported concerns, questions, suggestions, and reviewer expectations.
- Review conversations are authoritative for thread state, resolution status, and response history.
- Issues are authoritative for requested work, acceptance expectations, and traceability when linked to reviewed work.
- Commits are authoritative for submitted change history and work units.
- Diffs are authoritative for the actual submitted code, documentation, configuration, and artifact changes.
- Repository artifacts are authoritative for affected requirements, specifications, data models, flows, sequence interactions, and architecture decisions.
- Repository documentation is authoritative when it defines expected behavior, repository expectations, or validation requirements.
- Validation evidence is authoritative for completed checks, tests, and verification results.
- Git history is authoritative for commit sequence, prior changes, and change provenance.
- GitHub metadata is authoritative for pull request state, review state, checks, labels, linked issues, and conversation status when available.
- Review decisions must prioritize verified information over assumptions.
- Conflicting information must be reconciled before review conclusions are made.

</trusted_sources>

<cli_policy>

- CLI verification should be used before review conclusions are generated when repository information can be obtained directly.
- Pull request state should be verified before review execution, response, verification, or decision making.
- Commit history should be verified when change provenance or submitted scope affects review conclusions.
- Diffs should be verified before generating findings or comments.
- Review comments and conversations should be verified before responding to or resolving feedback.
- Related issues should be verified when requested scope or acceptance expectations affect review conclusions.
- Validation evidence should be verified before test and readiness conclusions are made.
- Repository artifacts should be inspected when they affect artifact consistency or review scope.
- Review activities should rely on verified repository information whenever possible.

</cli_policy>

<allowed_cli_commands>

- `gh pr view`
- `gh pr diff`
- `gh pr checks`
- `gh pr review`
- `gh issue view`
- `gh issue list`
- `gh api`
- `git status`
- `git diff`
- `git log`
- `git show`
- `git branch`
- `git rev-parse`
- `rg`
- `ls`
- `find`
- `cat`
- `pwd`

</allowed_cli_commands>
