# Question Bank Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the teacher question-bank workflow: accurate filtering, visible school/major names, simplified question types, review actions, pagination, lobby filter sync, and scroll-safe edit dialogs.

**Architecture:** Keep the existing question-bank API shape and React pages. Add only small fields and service methods where needed, normalize question types at backend boundaries, and implement review actions through the existing status update endpoint plus a frontend batch wrapper.

**Tech Stack:** Spring Boot, MyBatis Plus, JUnit/Mockito, React, TypeScript, TanStack Query, Vitest, Tailwind.

---

### Task 1: Backend Question Metadata And Type Normalization

**Files:**
- Modify: `AI-Meeting/admin/src/main/java/com/interviewpilot/questionbank/api/io/resp/QuestionRespDTO.java`
- Modify: `AI-Meeting/admin/src/main/java/com/interviewpilot/questionbank/service/impl/QuestionBankServiceImpl.java`
- Modify: `AI-Meeting/admin/src/test/java/com/interviewpilot/questionbank/service/QuestionBankServiceImplTest.java`

- [ ] **Step 1: Write/verify failing tests**

Run:

```bash
mvn -pl admin -Dtest=QuestionBankServiceImplTest test
```

Expected before implementation: compilation or assertion failure around `collegeName`, `majorName`, or normalized `questionType`.

- [ ] **Step 2: Implement response enrichment**

Add `collegeName` and `majorName` to the response DTO. Inject `CollegeService` into `QuestionBankServiceImpl`. During `pageByFilter` and `getDetail`, copy question fields and then populate names from `collegeService.listByIds(...)` and `majorService.listByIds(...)`.

- [ ] **Step 3: Normalize question types**

Use three canonical types: `综合题`, `专业题`, `其他题`. Normalize legacy values when returning DTOs and when saving/updating/importing/generating questions. For filtering and random selection, treat canonical types as aliases over old values so existing data still works.

- [ ] **Step 4: Verify backend tests**

Run:

```bash
mvn -pl admin -Dtest=QuestionBankServiceImplTest test
```

Expected: all tests pass.

---

### Task 2: Frontend Types, Options, And Service API

**Files:**
- Modify: `AI-Meeting-Frontend/src/services/questionBankService.ts`
- Modify: `AI-Meeting-Frontend/src/services/teacherService.ts`
- Modify: `AI-Meeting-Frontend/src/services/teacherService.test.ts`

- [ ] **Step 1: Write failing tests**

Add tests that `teacherService.updateQuestionStatus(id, status)` calls `PUT /ip/v1/questions/{id}/status` with query param `status`, and that batch updates call one request per selected id.

- [ ] **Step 2: Reduce question type options**

Change `QUESTION_TYPE_OPTIONS` to only `综合题`, `专业题`, `其他题`. Add `collegeName` and `majorName` to `QuestionRespDTO`.

- [ ] **Step 3: Add review status service methods**

Add `updateQuestionStatus(id, status)` and `batchUpdateQuestionStatus(ids, status)` to `teacherService`.

- [ ] **Step 4: Verify service tests**

Run:

```bash
npm run test:run -- src/services/teacherService.test.ts
```

Expected: tests pass.

---

### Task 3: Teacher Question Table

**Files:**
- Modify: `AI-Meeting-Frontend/src/components/teacher/QuestionBankTable.tsx`
- Test: `AI-Meeting-Frontend/src/components/teacher/QuestionBankTable.test.tsx`

- [ ] **Step 1: Write failing table tests**

Test that the table displays `collegeName` and `majorName`, renders page number buttons plus previous/next, supports row selection and current-page select all, and shows approve/reject buttons for `pending_review` rows.

- [ ] **Step 2: Implement table UI**

Add checkbox column, batch toolbar, page number buttons, direct college/major display, and row-level review buttons. Keep the existing edit/delete actions.

- [ ] **Step 3: Verify table tests**

Run:

```bash
npm run test:run -- src/components/teacher/QuestionBankTable.test.tsx
```

Expected: tests pass.

---

### Task 4: Teacher Question Page Filtering And Review Actions

**Files:**
- Modify: `AI-Meeting-Frontend/src/pages/teacher/TeacherQuestionsPage.tsx`
- Modify: `AI-Meeting-Frontend/src/pages/teacher/TeacherQuestionsPage.test.tsx`

- [ ] **Step 1: Extend page tests**

Test full filter payloads for college, major, question type, difficulty, and status. Test clear filters. Test single and batch review actions call the service/mutation and refresh query state.

- [ ] **Step 2: Implement page handlers**

Wire table selection/review callbacks to `teacherService.updateQuestionStatus` or batch update. Refresh after successful review and clear selection after batch actions.

- [ ] **Step 3: Verify page tests**

Run:

```bash
npm run test:run -- src/pages/teacher/TeacherQuestionsPage.test.tsx
```

Expected: tests pass.

---

### Task 5: Lobby Filter Sync

**Files:**
- Modify: `AI-Meeting-Frontend/src/hooks/lobby/useLobbyData.ts`
- Modify: `AI-Meeting-Frontend/src/hooks/lobby/useLobbyData.test.tsx`
- Modify: `AI-Meeting-Frontend/src/components/lobby/LobbyGrid.tsx`

- [ ] **Step 1: Write failing tests**

Test that lobby page queries and coverage use the same selected college, major, difficulty, and single selected question type. Test page changes preserve filters.

- [ ] **Step 2: Implement sync**

Use the same `QUESTION_TYPE_OPTIONS`. Rely on backend canonical aliasing. Use response `collegeName` and `majorName` on cards when present, falling back to local maps.

- [ ] **Step 3: Verify lobby tests**

Run:

```bash
npm run test:run -- src/hooks/lobby/useLobbyData.test.tsx
```

Expected: tests pass.

---

### Task 6: Form And Import Dialog Scrolling

**Files:**
- Modify: `AI-Meeting-Frontend/src/components/teacher/QuestionFormDialog.tsx`
- Modify: `AI-Meeting-Frontend/src/components/teacher/QuestionFormDialog.test.tsx`
- Modify: `AI-Meeting-Frontend/src/components/teacher/QuestionImportDialog.tsx`
- Modify: `AI-Meeting-Frontend/src/components/teacher/QuestionImportDialog.test.tsx`

- [ ] **Step 1: Verify dialog tests**

Ensure tests assert dialog max height, body scroll area, and fixed footer while retaining `作答时间（秒）`.

- [ ] **Step 2: Keep scroll-safe layout**

Use `max-h-[90vh]`, `overflow-hidden`, an internal `ScrollArea`, and a `shrink-0` footer. Preserve all existing fields.

- [ ] **Step 3: Verify dialog tests**

Run:

```bash
npm run test:run -- src/components/teacher/QuestionFormDialog.test.tsx src/components/teacher/QuestionImportDialog.test.tsx
```

Expected: tests pass.

---

### Task 7: Full Verification

**Files:**
- No new files.

- [ ] **Step 1: Frontend targeted tests**

Run:

```bash
npm run test:run -- src/services/teacherService.test.ts src/components/teacher/QuestionBankTable.test.tsx src/pages/teacher/TeacherQuestionsPage.test.tsx src/components/teacher/QuestionFormDialog.test.tsx src/components/teacher/QuestionImportDialog.test.tsx src/hooks/lobby/useLobbyData.test.tsx
```

- [ ] **Step 2: Frontend typecheck**

Run:

```bash
npm run typecheck
```

- [ ] **Step 3: Backend targeted tests**

Run:

```bash
mvn -pl admin -Dtest=QuestionBankServiceImplTest,QuestionImportServiceTest test
```

- [ ] **Step 4: Manual verification checklist**

Verify in browser after restart: teacher question-bank filters, school/major columns, page number navigation, row review, batch review, add/edit dialog scrolling, and lobby filter coverage/start behavior.
