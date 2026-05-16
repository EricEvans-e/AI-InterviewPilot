# PRD Gap Fixes — 3 Low-Hanging Bugs + Admin Stats

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix 3 backend/frontend bugs that block core features (radar chart score display, admin dashboard stats, college/major filtering), plus seed sample data for Zhejiang vocational colleges.

**Architecture:** Backend is Java Spring Boot + MyBatis-Plus. Frontend is React + TypeScript with TanStack Query. All fixes are surgical — no structural changes needed.

**Tech Stack:** Java 21, Spring Boot 3, MyBatis-Plus 3.5, Sa-Token, React 18, TypeScript, TanStack Query, Tailwind CSS, Lucide icons.

---

## Task 1: Fix `professionalMatchScore` naming inconsistency (LOW)

**Problem:** `RadarChartDTO.professionalMatchScore` does not match the JSON key the frontend expects (`professionalScore`). This causes the "专业匹配" dimension to display as null/0 in the interview report radar chart and score bar.

**Root cause chain:**
- `DimensionScoreResult.professionalScore` (correct) →
- `WeightedRadarComputationStrategy.fillDimensionScores()` calls `setProfessionalMatchScore()` (wrong) →
- `RadarChartDTO.professionalMatchScore` (wrong) →
- JSON serialized as `"professionalMatchScore"` →
- Frontend reads `"professionalScore"` → **MISS**

**Files to modify (3):**

### Step 1.1: Rename field in RadarChartDTO

- [ ] File: `AI-Meeting/admin/src/main/java/com/interviewpilot/interview/api/io/resp/RadarChartDTO.java`
- [ ] Change: `private Integer professionalMatchScore;` → `private Integer professionalScore;`

### Step 1.2: Update setter in WeightedRadarComputationStrategy

- [ ] File: `AI-Meeting/admin/src/main/java/com/interviewpilot/interview/application/strategy/WeightedRadarComputationStrategy.java`
- [ ] Change: `radarChart.setProfessionalMatchScore(...)` → `radarChart.setProfessionalScore(...)`

### Step 1.3: Update setter in InterviewRecordServiceImpl

- [ ] File: `AI-Meeting/admin/src/main/java/com/interviewpilot/interview/flow/report/InterviewRecordServiceImpl.java`
- [ ] Change: `radarChart.setProfessionalMatchScore(record.getProfessionalScore());` → `radarChart.setProfessionalScore(record.getProfessionalScore());`

### Step 1.4: Verify

- [ ] Run `mvn compile` in `AI-Meeting/` — no compile errors
- [ ] Grep the entire backend for `professionalMatchScore` — should return 0 results

---

## Task 2: Admin Dashboard Stats API + Frontend Wiring (MEDIUM)

**Problem:** `AdminDashboardPage.tsx` shows "--" for 3 of 4 stat cards because no backend stats endpoint exists.

**Approach:** Create a single `GET /api/ip/v1/users/stats` endpoint returning all 4 metrics in one call. Place the controller in the `user` domain. Protect with `@SaCheckRole("admin")`.

### Step 2.1: Create AdminStatsRespDTO

- [ ] File: `AI-Meeting/admin/src/main/java/com/interviewpilot/user/api/io/resp/AdminStatsRespDTO.java` (NEW)
- [ ] Create a `@Data` DTO with 4 fields: `totalUsers`, `todayActive`, `weekTrainingCount`, `avgScore`

### Step 2.2: Add mapper methods for stats queries

- [ ] File: `AI-Meeting/admin/src/main/java/com/interviewpilot/interview/dao/mapper/InterviewRecordMapper.java`
- [ ] Add 3 `@Select` methods: `countTodayActiveUsers`, `countWeekTraining`, `avgInterviewScore`

### Step 2.3: Add getStats to UserService + UserServiceImpl

- [ ] File: `AI-Meeting/admin/src/main/java/com/interviewpilot/user/service/UserService.java`
- [ ] File: `AI-Meeting/admin/src/main/java/com/interviewpilot/user/service/impl/UserServiceImpl.java`
- [ ] Implement `getStats()` using `userMapper.selectCount` + injected `InterviewRecordMapper`

### Step 2.4: Add stats endpoint to UserController

- [ ] File: `AI-Meeting/admin/src/main/java/com/interviewpilot/user/api/UserController.java`
- [ ] Add `@GetMapping("/stats")` with `@SaCheckRole("admin")`

### Step 2.5: Frontend — add getStats to adminService

- [ ] File: `AI-Meeting-Frontend/src/services/adminService.ts`
- [ ] Add `AdminStatsResult` interface and `getStats()` method

### Step 2.6: Frontend — wire stats into AdminDashboardPage

- [ ] File: `AI-Meeting-Frontend/src/pages/admin/AdminDashboardPage.tsx`
- [ ] Replace placeholder "--" values with real data from `adminService.getStats()`

---

## Task 3: College/Major/ExamOutline Filter Bugs (HIGH)

**Problem:** Three `getByPage()` methods ignore all filter parameters. `MajorController.list()` ignores `collegeId`.

### Step 3.1: Fix CollegeServiceImpl.getByPage()

- [ ] File: `AI-Meeting/admin/src/main/java/com/interviewpilot/questionbank/service/impl/CollegeServiceImpl.java`
- [ ] Add `.like(name)`, `.eq(province)`, `.eq(city)`, `.eq(type)`, `.eq(level)` conditions

### Step 3.2: Fix MajorServiceImpl.getByPage()

- [ ] File: `AI-Meeting/admin/src/main/java/com/interviewpilot/questionbank/service/impl/MajorServiceImpl.java`
- [ ] Add `.eq(collegeId)`, `.like(name)` conditions

### Step 3.3: Fix ExamOutlineServiceImpl.getByPage()

- [ ] File: `AI-Meeting/admin/src/main/java/com/interviewpilot/questionbank/service/impl/ExamOutlineServiceImpl.java`
- [ ] Add `.eq(collegeId)`, `.eq(majorId)`, `.eq(year)` conditions

### Step 3.4: Fix MajorController.list() to accept collegeId

- [ ] File: `AI-Meeting/admin/src/main/java/com/interviewpilot/questionbank/api/MajorController.java`
- [ ] Add `@RequestParam(value = "collegeId", required = false) Long collegeId`
- [ ] File: `MajorService.java` + `MajorServiceImpl.java` — add `listByCollegeId(Long collegeId)`

---

## Task 4: Seed Data SQL for Zhejiang Vocational Colleges

### Step 4.1: Create seed SQL file

- [ ] File: `AI-Meeting/admin/src/main/resources/sql/seed_zhejiang_colleges.sql` (NEW)
- [ ] 10 colleges, ~24 majors, 2 exam outlines

### Step 4.2: Execute and verify

- [ ] Execute against dev database
- [ ] Verify counts and API responses

---

## Task 5: Verification (Build + Smoke Test)

- [ ] Backend: `mvn clean compile` — BUILD SUCCESS
- [ ] Frontend: `npx tsc --noEmit` — no type errors
- [ ] Smoke test: admin dashboard shows real numbers, college filters work, radar chart shows 专业匹配 score
