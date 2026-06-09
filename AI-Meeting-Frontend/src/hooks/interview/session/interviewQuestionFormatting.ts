const MAP_STYLE_QUESTION_PATTERN = /^\{\s*question\s*=\s*([\s\S]*?)\s*\}$/i;

export const normalizeInterviewQuestionText = (value: string | null | undefined) => {
  const trimmed = value?.trim() ?? "";
  const match = trimmed.match(MAP_STYLE_QUESTION_PATTERN);
  return (match?.[1] ?? trimmed).trim();
};
