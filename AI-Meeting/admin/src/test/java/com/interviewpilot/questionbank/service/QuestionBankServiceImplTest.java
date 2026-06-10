package com.interviewpilot.questionbank.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.interviewpilot.questionbank.api.io.req.QuestionSaveReqDTO;
import com.interviewpilot.questionbank.api.io.resp.QuestionCoverageRespDTO;
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

        QuestionCoverageRespDTO coverage = service.coverage(10L, 20L, "专业题", 5);

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

        service.coverage(10L, 20L, "专业认知", 5);

        ArgumentCaptor<QueryWrapper<QuestionDO>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(mapper).selectCount(captor.capture());
        assertTrue(captor.getValue().getSqlSegment().contains("question_type"));
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue("专业题"));
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

    private QuestionBankServiceImpl serviceWith(QuestionMapper mapper) {
        return serviceWith(mapper, mock(MajorService.class));
    }

    private QuestionBankServiceImpl serviceWith(QuestionMapper mapper, MajorService majorService) {
        QuestionBankServiceImpl service = new QuestionBankServiceImpl(majorService);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        return service;
    }

    private MajorDO major(Long id, String category) {
        MajorDO major = new MajorDO();
        major.setId(id);
        major.setCategory(category);
        major.setDelFlag(0);
        return major;
    }

    private QuestionDO question(Long id, Long collegeId, Long majorId) {
        QuestionDO question = new QuestionDO();
        question.setId(id);
        question.setTitle("题目 " + id);
        question.setContent("题目 " + id);
        question.setCollegeId(collegeId);
        question.setMajorId(majorId);
        question.setQuestionType("专业题");
        question.setStatus("approved");
        question.setDelFlag(0);
        return question;
    }
}
