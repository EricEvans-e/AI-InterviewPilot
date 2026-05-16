#!/usr/bin/env python
"""
Import interview questions from docx files into MySQL database.

Directory structure:
  src/1/{college_dir}/{major_dir}/{college_short}{major}.docx

Each docx contains 4 questions, each with 3 reference answers.
Format: question -> "模拟题 2025 {college_full} {major}" -> "█ 参考一/二/三" -> answer
"""

import os
import re
import zipfile
import xml.etree.ElementTree as ET
import pymysql
import sys

sys.stdout.reconfigure(encoding='utf-8')

# ── Config ──────────────────────────────────────────────────────────
DB_CONFIG = {
    'host': '127.0.0.1',
    'port': 3307,
    'user': 'root',
    'password': '122333',
    'database': 'mainshi_agent',
    'charset': 'utf8mb4',
}

SRC_DIR = os.path.join(os.path.dirname(__file__), '..', 'src', '1')

# College directory name → full name (from docx metadata)
# Will be auto-detected from docx, but we also define fallbacks
COLLEGE_NAME_MAP = {
    '丽水职业面试题库': '丽水职业技术学院',
    '义乌工商面试题库': '义乌工商职业技术学院',
    '台州科技面试题库': '台州科技职业学院',
    '台州职业面试题库': '台州职业技术学院',
    '嘉兴南洋面试题库': '嘉兴南洋职业技术学院',
    '嘉兴职业面试题库': '嘉兴职业技术学院',
    '宁波卫生面试图库': '宁波卫生职业技术学院',
    '宁波城市面试题库': '宁波城市职业技术学院',
    '杭州万向面试题库': '杭州万向职业技术学院',
    '杭州科技面试题库': '杭州科技职业技术学院',
    '温州商学院面试题库': '温州商学院',
    '温州科技面试题库': '温州科技职业学院',
    '温州职业面试题库': '温州职业技术学院',
    '湖州职业面试题库': '湖州职业技术学院',
    '绍兴职业面试题库': '绍兴职业技术学院',
    '金职大面试题库': '金华职业技术大学',
}

NS = {'w': 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}

# ── Docx Parser ─────────────────────────────────────────────────────

def parse_docx_paragraphs(filepath):
    """Extract all non-empty paragraph texts from a docx file."""
    with zipfile.ZipFile(filepath) as z:
        tree = ET.parse(z.open('word/document.xml'))
    root = tree.getroot()
    paragraphs = []
    for p in root.findall('.//w:p', NS):
        texts = [t.text for t in p.findall('.//w:t', NS) if t.text]
        line = ''.join(texts).strip()
        if line:
            paragraphs.append(line)
    return paragraphs


def extract_college_full_name(paragraphs):
    """Extract full college name from '模拟题 2025 {college} {major}' line."""
    for line in paragraphs:
        m = re.match(r'模拟题\s+\d{4}\s+(.+?)\s+\S+', line)
        if m:
            return m.group(1)
    return None


def parse_questions_and_answers(paragraphs):
    """
    Parse 4 questions with their reference answers from paragraph list.

    Structure per question:
      - question text (non-empty, not metadata, not reference marker)
      - "模拟题 2025 ..." (metadata, skip)
      - "█\xa0参考一" or "█ 参考一" (marker)
      - answer 1 text
      - "█\xa0参考二" or "█ 参考二" (marker)
      - answer 2 text
      - "█\xa0参考三" or "█ 参考三" (marker)
      - answer 3 text
    """
    # Filter out metadata lines and reference markers
    questions = []
    current_question = None
    answers = []
    state = 'seek_question'  # seek_question, seek_answer

    # Normalize NBSP to regular space for matching
    normalized = [p.replace('\xa0', ' ') for p in paragraphs]

    for i, line in enumerate(normalized):
        # Skip metadata line
        if re.match(r'模拟题\s+\d{4}', line):
            continue

        # Check for reference answer markers
        ref_match = re.match(r'█\s*参考([一二三])', line)
        if ref_match:
            state = 'seek_answer'
            continue

        # If we're in seek_answer state and this is not a marker, it's an answer
        if state == 'seek_answer':
            answers.append(paragraphs[i])  # Use original text (with NBSP)
            if len(answers) == 3:
                # We have all 3 answers for current question
                if current_question:
                    questions.append({
                        'question': current_question,
                        'answers': answers.copy(),
                    })
                current_question = None
                answers = []
                state = 'seek_question'
            continue

        # Otherwise, it's a question text
        if state == 'seek_question' and not current_question:
            current_question = paragraphs[i]

    # Handle edge case: last question without all answers
    if current_question and answers:
        questions.append({
            'question': current_question,
            'answers': answers,
        })

    return questions


# ── Database Operations ─────────────────────────────────────────────

def get_existing_colleges(cursor):
    """Get existing college name → id mapping."""
    cursor.execute("SELECT id, name FROM college WHERE del_flag = 0")
    return {row[1]: row[0] for row in cursor.fetchall()}


def get_existing_majors(cursor):
    """Get existing (college_id, major_name) → id mapping."""
    cursor.execute("SELECT id, college_id, name FROM major WHERE del_flag = 0")
    return {(row[1], row[2]): row[0] for row in cursor.fetchall()}


def insert_college(cursor, name):
    """Insert a new college and return its id."""
    cursor.execute(
        "INSERT INTO college (name, code, type, province, city, level, remark) "
        "VALUES (%s, '', '综合', '浙江', '', '高职', '从面试题库导入')",
        (name,)
    )
    return cursor.lastrowid


def insert_major(cursor, college_id, major_name):
    """Insert a new major and return its id."""
    cursor.execute(
        "INSERT INTO major (college_id, name, code, category, target_type, test_form, test_content, score_structure, year) "
        "VALUES (%s, %s, '', '', '普高', '面试', '', '', 2026)",
        (college_id, major_name)
    )
    return cursor.lastrowid


def insert_question(cursor, college_id, major_id, title, content):
    """Insert a question into the question table."""
    cursor.execute(
        "INSERT INTO question (title, content, question_type, college_id, major_id, "
        "difficulty, answer_time_seconds, reference_answer, is_ai_generated, status, year) "
        "VALUES (%s, %s, '开放题', %s, %s, 'medium', 120, %s, 0, 'approved', 2026)",
        (title, content, college_id, major_id, content)
    )
    return cursor.lastrowid


# ── Main ────────────────────────────────────────────────────────────

def main():
    base = os.path.normpath(SRC_DIR)
    if not os.path.isdir(base):
        print(f"ERROR: Source directory not found: {base}")
        return

    conn = pymysql.connect(**DB_CONFIG)
    cursor = conn.cursor()

    try:
        existing_colleges = get_existing_colleges(cursor)
        existing_majors = get_existing_majors(cursor)

        stats = {
            'colleges_added': 0,
            'majors_added': 0,
            'questions_added': 0,
            'files_parsed': 0,
            'errors': [],
        }

        college_dirs = sorted(os.listdir(base))
        print(f"Found {len(college_dirs)} college directories")

        for college_dir in college_dirs:
            college_path = os.path.join(base, college_dir)
            if not os.path.isdir(college_path):
                continue

            # Resolve college full name
            college_full = COLLEGE_NAME_MAP.get(college_dir, college_dir)

            # Get or create college
            if college_full in existing_colleges:
                college_id = existing_colleges[college_full]
                print(f"  College exists: {college_full} (id={college_id})")
            else:
                college_id = insert_college(cursor, college_full)
                existing_colleges[college_full] = college_id
                stats['colleges_added'] += 1
                print(f"  College added: {college_full} (id={college_id})")

            # Process each major subdirectory
            major_dirs = sorted(os.listdir(college_path))
            for major_dir in major_dirs:
                major_path = os.path.join(college_path, major_dir)
                if not os.path.isdir(major_path):
                    continue

                # Get or create major
                major_key = (college_id, major_dir)
                if major_key in existing_majors:
                    major_id = existing_majors[major_key]
                else:
                    major_id = insert_major(cursor, college_id, major_dir)
                    existing_majors[major_key] = major_id
                    stats['majors_added'] += 1

                # Find the docx file
                docx_files = [f for f in os.listdir(major_path) if f.endswith('.docx')]
                if not docx_files:
                    stats['errors'].append(f"No docx in {college_dir}/{major_dir}")
                    continue

                docx_path = os.path.join(major_path, docx_files[0])

                try:
                    paragraphs = parse_docx_paragraphs(docx_path)

                    # Auto-detect full college name from docx
                    detected_name = extract_college_full_name(paragraphs)
                    if detected_name and detected_name != college_full:
                        # Update mapping if we detect a different name
                        if detected_name in existing_colleges:
                            # Use existing college with detected name
                            real_college_id = existing_colleges[detected_name]
                        else:
                            # Create new college with detected name
                            real_college_id = insert_college(cursor, detected_name)
                            existing_colleges[detected_name] = real_college_id
                            stats['colleges_added'] += 1
                            print(f"  College added (detected): {detected_name} (id={real_college_id})")
                        college_id = real_college_id

                    qa_list = parse_questions_and_answers(paragraphs)

                    if len(qa_list) < 4:
                        stats['errors'].append(
                            f"{college_dir}/{major_dir}: expected 4 questions, got {len(qa_list)}"
                        )
                    elif len(qa_list) > 4:
                        # Some files have duplicate question sets; take first 4
                        qa_list = qa_list[:4]

                    for qa in qa_list:
                        # Build reference answer text
                        ref_text = ""
                        for j, ans in enumerate(qa['answers'], 1):
                            ref_text += f"参考答案{j}：\n{ans}\n\n"

                        insert_question(
                            cursor,
                            college_id,
                            major_id,
                            qa['question'],
                            ref_text.strip()
                        )
                        stats['questions_added'] += 1

                    stats['files_parsed'] += 1

                except Exception as e:
                    stats['errors'].append(f"{college_dir}/{major_dir}: {e}")

            conn.commit()

        # Summary
        print(f"\n{'='*60}")
        print(f"Import complete!")
        print(f"  Files parsed:     {stats['files_parsed']}")
        print(f"  Colleges added:   {stats['colleges_added']}")
        print(f"  Majors added:     {stats['majors_added']}")
        print(f"  Questions added:  {stats['questions_added']}")
        if stats['errors']:
            print(f"\n  Errors ({len(stats['errors'])}):")
            for err in stats['errors']:
                print(f"    - {err}")
        print(f"{'='*60}")

    finally:
        cursor.close()
        conn.close()


if __name__ == '__main__':
    main()
