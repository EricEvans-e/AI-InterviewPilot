const SELF_INTRODUCTION_PATTERNS = [
  /自我介绍/,
  /介绍一下(你自己|自己)/,
  /简单介绍一下(你自己|自己)/,
  /先做.{0,8}自我介绍/,
];

export const isSelfIntroductionQuestion = (
  questionNumber?: string | null,
  questionContent?: string | null,
) => {
  if ((questionNumber ?? "").trim() !== "1") {
    return false;
  }
  const content = (questionContent ?? "").trim();
  if (!content) {
    return false;
  }
  return SELF_INTRODUCTION_PATTERNS.some((pattern) => pattern.test(content));
};

export const getInterviewQuestionLabel = (
  questionNumber?: string | null,
  questionContent?: string | null,
) => (isSelfIntroductionQuestion(questionNumber, questionContent) ? "自我介绍" : "当前题目");
