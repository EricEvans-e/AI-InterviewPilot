# Interview Report Reliability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the interview report page load reliably under delayed finalize/upload/AI generation conditions, while keeping slow enrichments non-blocking.

**Architecture:** Remove synchronous AI review generation from the backend report snapshot path so the base report persists quickly. On the frontend, treat report availability, recording upload, and reference-answer generation as independently delayed resources with targeted retry/poll behavior and longer request budgets where appropriate.

**Tech Stack:** Spring Boot, MyBatis-Plus, React, TanStack Query, Vitest

---

### Task 1: Lock backend report snapshot behavior with tests

**Files:**
- Modify: `AI-Meeting/admin/src/test/java/com/interviewpilot/interview/flow/session/InterviewRecordServiceImplTest.java`

- [ ] Add a test that saves/finalizes a report and asserts `InterviewReportAiReviewService.generateReviewFeedback(...)` is not called.
- [ ] Assert the saved snapshot still contains `reviewFeedback`.
- [ ] Assert read-back report still exposes non-empty rule-based review feedback.

### Task 2: Make report snapshot generation non-blocking

**Files:**
- Modify: `AI-Meeting/admin/src/main/java/com/interviewpilot/interview/flow/report/InterviewRecordServiceImpl.java`

- [ ] Replace synchronous `buildReviewFeedback(...)` usage in `buildSessionSnapshotJson(...)` with direct rule-based feedback generation.
- [ ] Keep read-path enrichment local and fast.
- [ ] Remove dead helper code if no longer used.

### Task 3: Lock frontend report polling behavior with tests

**Files:**
- Modify: `AI-Meeting-Frontend/src/hooks/interview/report/interviewReportData.shared.test.ts`
- Modify: `AI-Meeting-Frontend/src/hooks/interview/report/useInterviewReportData.test.tsx`
- Modify: `AI-Meeting-Frontend/src/services/interviewService.test.ts`

- [ ] Add a test where report fetch/save path hits transient timeout, then later succeeds through polling.
- [ ] Add a test where manual reference-answer generation times out first, then becomes visible after report polling.
- [ ] Add service-level tests for long-timeout config on report/reference-answer endpoints.

### Task 4: Implement frontend resilience for delayed report assets

**Files:**
- Modify: `AI-Meeting-Frontend/src/hooks/interview/report/interviewReportData.shared.ts`
- Modify: `AI-Meeting-Frontend/src/hooks/interview/report/useInterviewReportData.ts`
- Modify: `AI-Meeting-Frontend/src/services/interviewService.ts`

- [ ] Add explicit timeout config support to path-fallback helpers.
- [ ] Give report/read-finalize/reference-answer calls fit-for-purpose timeout budgets.
- [ ] Extend report fetch flow to poll through transient timeout/finalize states instead of surfacing early `Request timeout`.
- [ ] Extend recording URL polling window so delayed uploads can appear without manual refresh.
- [ ] Make manual reference-answer generation recover from timeout by polling report data until answers appear or retry budget is exhausted.

### Task 5: Verify and reconcile docs

**Files:**
- Modify: `README.md`
- Modify: `docs/ai-model-configuration.md`

- [ ] Run targeted backend tests.
- [ ] Run targeted frontend tests plus production build.
- [ ] Update docs to reflect delayed asset polling, manual reference-answer generation, and expected report readiness behavior.
