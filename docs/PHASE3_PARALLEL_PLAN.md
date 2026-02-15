# Phase3 Parallel Execution Plan

## Goal
Complete Phase3 with stable object storage backup/restore, global repo sync, and usable settings UX.

## Branch & Worktree
- `codex/p3-backend` -> `/tmp/personal_web_worktrees/backend`
- `codex/p3-frontend` -> `/tmp/personal_web_worktrees/frontend`
- `codex/p3-devops` -> `/tmp/personal_web_worktrees/devops`
- `codex/p3-integration` -> `/tmp/personal_web_worktrees/integration`

## Sprint-1 Tasks

### Thread A (Backend) - Object Storage Hardening
- Task `BE-01`: Improve OSS error mapping in `StorageBackupService` (400/403/404/500 clear messages).
- Task `BE-02`: Strengthen restore/backup path safety and edge checks.
- Task `BE-03`: Add tests for `/papers/{id}/backup`, `/backup-status`, `/restore` success/failure paths.
- Task `BE-04`: Add structured logs for backup/restore result and latency.

Acceptance:
- `mvn -q test` passes.
- Backup failure never breaks paper upload/download main flow.
- API returns deterministic error code/message for missing file, missing OSS object, bad config.

### Thread B (Frontend) - Backup UX Completion
- Task `FE-01`: On paper detail page, display backup status with clear progress and failure reason.
- Task `FE-02`: Add disabled/guard state for backup and restore buttons when conditions are not met.
- Task `FE-03`: Improve settings page feedback for repo sync and README rebuild actions.
- Task `FE-04`: Add repository entry UX polish (link open, validation, inline errors).

Acceptance:
- `npm run build` passes.
- No silent failure in backup/repo operations.
- User can understand current status and next action from UI only.

### Thread C (Integration/Review)
- Task `INT-01`: Review BE/FE commits for API contract compatibility.
- Task `INT-02`: Run integration checks after cherry-pick.
- Task `INT-03`: Update deployment/runtime docs when behavior changes.

## Handoff Protocol
- Each thread commits in small steps and pushes branch.
- Every handoff message must include:
  - branch name
  - commit hash list
  - changed files summary
  - test command and result
  - known risk

Template:
`[HANDOFF] branch=<branch> commits=<hash1,hash2> test="<cmd>:PASS/FAIL" risk="<text>"`

## Integration Rule
- Integration only via `cherry-pick` into `codex/p3-integration`.
- Resolve conflicts in integration branch only.
- `main/develop` are not direct development targets during this sprint.
