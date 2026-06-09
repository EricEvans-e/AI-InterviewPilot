const SIMPLE_MAP_STYLE_QUESTION_PATTERN = /^\{\s*question\s*=\s*([\s\S]*?)\s*\}$/i;
const JAVA_MAP_STYLE_QUESTION_PATTERN =
  /(?:^|[,{]\s*)question\s*=\s*([\s\S]*?)(?=,\s*[A-Za-z_][A-Za-z0-9_]*\s*=|\s*}$)/i;

export const normalizeInterviewQuestionText = (value: string | null | undefined) => {
  const trimmed = value?.trim() ?? "";
  const simpleMatch = trimmed.match(SIMPLE_MAP_STYLE_QUESTION_PATTERN);
  if (simpleMatch?.[1]) {
    return simpleMatch[1].trim();
  }

  const javaMapMatch = trimmed.match(JAVA_MAP_STYLE_QUESTION_PATTERN);
  return (javaMapMatch?.[1] ?? trimmed).trim();
};
