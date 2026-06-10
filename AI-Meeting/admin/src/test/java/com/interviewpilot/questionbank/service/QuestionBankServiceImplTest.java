package com.interviewpilot.questionbank.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.interviewpilot.questionbank.api.io.req.QuestionPageReqDTO;
import com.interviewpilot.questionbank.api.io.req.QuestionSaveReqDTO;
import com.interviewpilot.questionbank.api.io.resp.QuestionCoverageRespDTO;
import com.interviewpilot.questionbank.api.io.resp.QuestionRespDTO;
import com.interviewpilot.questionbank.dao.entity.CollegeDO;
import com.interviewpilot.questionbank.dao.entity.MajorDO;
import com.interviewpilot.questionbank.dao.entity.QuestionDO;
import com.interviewpilot.questionbank.dao.mapper.QuestionMapper;
import com.interviewpilot.questionbank.service.impl.QuestionBankServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuestionBankServiceImplTest {

    @Test
    void shouldSelectExactQuestionsFirstThenFallbackToCollegeMajorCategoryAndGeneral() {
        QuestionMapper mapper = mock(QuestionMapper.class);
        MajorService majorService = mock(MajorService.class);
        when(majorService.getById(20L)).thenReturn(major(20L, "计算机类"));
        when(majorService.list()).thenReturn(List.of(
                major(20L, "计算机类"),
                major(21L, "计算机类"),
                major(30L, "财经商贸类")
        ));
        when(mapper.selectList(any()))
                .thenReturn(List.of(question(1L, 10L, 20L)))
                .thenReturn(List.of(question(2L, 10L, null)))
                .thenReturn(List.of(question(3L, 99L, 21L)))
                .thenReturn(List.of(question(4L, null, null)));
        QuestionBankServiceImpl service = serviceWith(mapper, majorService);

        List<QuestionDO> selected = service.randomSelect(10L, 20L, "专业题", 4);

        assertEquals(List.of(1L, 2L, 3L, 4L), selected.stream().map(QuestionDO::getId).toList());

        ArgumentCaptor<Wrapper<QuestionDO>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper, times(4)).selectList(captor.capture());
        List<String> sqlSegments = captor.getAllValues().stream().map(Wrapper::getSqlSegment).toList();
        assertTrue(sqlSegments.get(0).contains("college_id"));
        assertTrue(sqlSegments.get(0).contains("major_id"));
        assertFalse(sqlSegments.get(1).contains("major_id ="));
        assertTrue(sqlSegments.get(2).contains("major_id IN"));
        assertTrue(sqlSegments.get(3).contains("college_id IS NULL"));
        assertTrue(sqlSegments.get(3).contains("major_id IS NULL"));
    }

    @Test
    void shouldReportCoverageForExactAndFallbackApprovedQuestions() {
        QuestionMapper mapper = mock(QuestionMapper.class);
        when(mapper.selectCount(any())).thenReturn(1L);
        when(mapper.selectList(any()))
                .thenReturn(List.of(question(1L, 10L, 20L)))
                .thenReturn(List.of(question(2L, 10L, null)))
                .thenReturn(List.of(question(3L, null, null)));
        QuestionBankServiceImpl service = serviceWith(mapper);

        QuestionCoverageRespDTO coverage = service.coverage(10L, 20L, "专业题", null, null, 5);

        assertEquals(10L, coverage.getCollegeId());
        assertEquals(20L, coverage.getMajorId());
        assertEquals("专业题", coverage.getInterviewMode());
        assertEquals(3, coverage.getApprovedCount());
        assertEquals(1, coverage.getExactMatchCount());
        assertEquals(2, coverage.getFallbackCount());
        assertTrue(coverage.isCanStartImmediately());
        assertTrue(coverage.isMayNeedAiGeneration());
    }

    @Test
    void shouldMapInterviewModeWhenReportingCoverage() {
        QuestionMapper mapper = mock(QuestionMapper.class);
        when(mapper.selectCount(any())).thenReturn(1L);
        when(mapper.selectList(any()))
                .thenReturn(List.of(question(1L, 10L, 20L)))
                .thenReturn(List.of())
                .thenReturn(List.of());
        QuestionBankServiceImpl service = serviceWith(mapper);

        service.coverage(10L, 20L, "专业认知", null, null, 5);

        ArgumentCaptor<QueryWrapper<QuestionDO>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(mapper).selectCount(captor.capture());
        assertTrue(captor.getValue().getSqlSegment().contains("question_type"));
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue("专业题"));
    }

    @Test
    void shouldApplyAbilityTagAndDifficultyWhenReportingCoverage() {
        QuestionMapper mapper = mock(QuestionMapper.class);
        when(mapper.selectCount(any())).thenReturn(1L);
        when(mapper.selectList(any()))
                .thenReturn(List.of(question(1L, 10L, 20L)))
                .thenReturn(List.of())
                .thenReturn(List.of());
        QuestionBankServiceImpl service = serviceWith(mapper);

        service.coverage(10L, 20L, "综合题", "沟通表达", "hard", 5);

        ArgumentCaptor<QueryWrapper<QuestionDO>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(mapper).selectCount(captor.capture());
        QueryWrapper<QuestionDO> query = captor.getValue();
        assertTrue(query.getSqlSegment().contains("ability_tag"));
        assertTrue(query.getSqlSegment().contains("difficulty"));
        assertTrue(query.getParamNameValuePairs().containsValue("沟通表达"));
        assertTrue(query.getParamNameValuePairs().containsValue("hard"));
    }

    @Test
    void shouldExpandCanonicalQuestionTypeToLegacyAliasesWhenPaging() {
        QuestionMapper mapper = mock(QuestionMapper.class);
        Page<QuestionDO> page = new Page<>(1, 10);
        page.setRecords(List.of());
        when(mapper.selectPage(any(Page.class), any())).thenReturn(page);
        QuestionBankServiceImpl service = serviceWith(mapper);
        QuestionPageReqDTO request = new QuestionPageReqDTO();
        request.setPageNum(1);
        request.setPageSize(10);
        request.setQuestionType("综合题");

        service.pageByFilter(request);

        ArgumentCaptor<Wrapper<QuestionDO>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).selectPage(any(Page.class), captor.capture());
        assertTrue(captor.getValue().getSqlSegment().contains("question_type IN"));
        List<String> expandedTypes = ReflectionTestUtils.invokeMethod(
                service,
                "expandQuestionTypes",
                "综合题"
        );
        assertTrue(expandedTypes.contains("综合题"), expandedTypes::toString);
        assertTrue(expandedTypes.contains("结构化"));
        assertTrue(expandedTypes.contains("半结构化"));
        assertTrue(expandedTypes.contains("开放题"));
        assertTrue(expandedTypes.contains("综合素质"));
        assertTrue(expandedTypes.contains("情景应变"));
        assertTrue(expandedTypes.contains("自我介绍"));
    }

    @Test
    void shouldApplyTitleKeywordWhenPaging() {
        QuestionMapper mapper = mock(QuestionMapper.class);
        Page<QuestionDO> page = new Page<>(1, 10);
        page.setRecords(List.of());
        when(mapper.selectPage(any(Page.class), any())).thenReturn(page);
        QuestionBankServiceImpl service = serviceWith(mapper);
        QuestionPageReqDTO request = new QuestionPageReqDTO();
        request.setPageNum(1);
        request.setPageSize(10);
        request.setTitleKeyword("RoBERTa");

        service.pageByFilter(request);

        ArgumentCaptor<QueryWrapper<QuestionDO>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(mapper).selectPage(any(Page.class), captor.capture());
        assertTrue(captor.getValue().getSqlSegment().contains("title"));
        assertTrue(captor.getValue().getSqlSegment().contains("LIKE"));
        assertTrue(captor.getValue().getParamNameValuePairs().values().stream()
                .map(String::valueOf)
                .anyMatch(value -> value.contains("RoBERTa")));
    }

    @Test
    void shouldRespectProvidedValidStatusWhenCreatingImportedQuestion() {
        QuestionMapper mapper = mock(QuestionMapper.class);
        AtomicLong ids = new AtomicLong(1L);
        when(mapper.insert(any(QuestionDO.class))).thenAnswer(invocation -> {
            QuestionDO question = invocation.getArgument(0, QuestionDO.class);
            question.setId(ids.getAndIncrement());
            return 1;
        });
        QuestionBankServiceImpl service = serviceWith(mapper);

        QuestionSaveReqDTO request = new QuestionSaveReqDTO();
        request.setTitle("导入题");
        request.setContent("导入题");
        request.setQuestionType("专业题");
        request.setStatus("pending_review");
        request.setIsAiGenerated(false);

        Long id = service.create(request, 7L);

        assertEquals(1L, id);
        ArgumentCaptor<QuestionDO> captor = ArgumentCaptor.forClass(QuestionDO.class);
        verify(mapper).insert(captor.capture());
        assertEquals("pending_review", captor.getValue().getStatus());
        assertFalse(captor.getValue().getIsAiGenerated());
    }

    @Test
    void shouldPopulateCollegeAndMajorNamesAndNormalizeQuestionTypeWhenPaging() {
        QuestionMapper mapper = mock(QuestionMapper.class);
        MajorService majorService = mock(MajorService.class);
        CollegeService collegeService = mock(CollegeService.class);

        QuestionDO record = question(9L, 10L, 20L);
        record.setQuestionType("结构化");
        Page<QuestionDO> page = new Page<>(1, 10);
        page.setRecords(List.of(record));
        page.setTotal(1);
        page.setCurrent(1);
        page.setSize(10);
        page.setPages(1);

        when(mapper.selectPage(any(Page.class), any())).thenReturn(page);
        doReturn(List.of(college(10L, "浙江机电职业技术学院")))
                .when(collegeService)
                .list(any(Wrapper.class));
        doReturn(List.of(major(20L, "装备制造", "机电一体化技术")))
                .when(majorService)
                .list(any(Wrapper.class));

        QuestionBankServiceImpl service = serviceWith(mapper, majorService, collegeService);
        QuestionPageReqDTO request = new QuestionPageReqDTO();
        request.setPageNum(1);
        request.setPageSize(10);

        var result = service.pageByFilter(request);

        assertEquals(1, result.getRecords().size());
        QuestionRespDTO dto = result.getRecords().get(0);
        assertEquals("浙江机电职业技术学院", dto.getCollegeName());
        assertEquals("机电一体化技术", dto.getMajorName());
        assertEquals("综合题", dto.getQuestionType());
    }

    private QuestionBankServiceImpl serviceWith(QuestionMapper mapper) {
        return serviceWith(mapper, mock(MajorService.class), mock(CollegeService.class));
    }

    private QuestionBankServiceImpl serviceWith(QuestionMapper mapper, MajorService majorService) {
        return serviceWith(mapper, majorService, mock(CollegeService.class));
    }

    private QuestionBankServiceImpl serviceWith(QuestionMapper mapper, MajorService majorService, CollegeService collegeService) {
        QuestionBankServiceImpl service = new QuestionBankServiceImpl(majorService, collegeService);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        return service;
    }

    private MajorDO major(Long id, String category) {
        return major(id, category, null);
    }

    private MajorDO major(Long id, String category, String name) {
        MajorDO major = new MajorDO();
        major.setId(id);
        major.setName(name);
        major.setCategory(category);
        major.setDelFlag(0);
        return major;
    }

    private CollegeDO college(Long id, String name) {
        CollegeDO college = new CollegeDO();
        college.setId(id);
        college.setName(name);
        college.setDelFlag(0);
        return college;
    }

    private QuestionDO question(Long id, Long collegeId, Long majorId) {
        QuestionDO question = new QuestionDO();
        question.setId(id);
        question.setTitle("Question " + id);
        question.setContent("Question " + id);
        question.setCollegeId(collegeId);
        question.setMajorId(majorId);
        question.setQuestionType("专业题");
        question.setStatus("approved");
        question.setDelFlag(0);
        return question;
    }
}
