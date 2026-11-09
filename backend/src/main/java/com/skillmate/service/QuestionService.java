package com.skillmate.service;

import com.skillmate.dto.AuthDtos.PageResponse;
import com.skillmate.dto.CatalogDtos.*;
import com.skillmate.entity.*;
import com.skillmate.exception.ApiException;
import com.skillmate.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class QuestionService {
    private final QuestionRepository questions; private final SkillRepository skills; private final AttemptRepository attempts;
    public QuestionService(QuestionRepository questions, SkillRepository skills, AttemptRepository attempts) {
        this.questions = questions; this.skills = skills; this.attempts = attempts;
    }

    public PageResponse<QuestionDto> search(Long skillId, Long subjectId, Difficulty difficulty, String q, int page, int size, Long userId) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("id"));
        var result = questions.search(skillId, subjectId, difficulty, q == null ? "" : q.trim(), pageable);
        Map<Long, AttemptStatus> mine = myStatuses(result.getContent(), userId);
        return PageResponse.of(result.map(x -> QuestionDto.from(x, mine.get(x.getId()))));
    }

    public QuestionDto get(Long id, Long userId) {
        Question q = find(id);
        return QuestionDto.from(q, myStatuses(List.of(q), userId).get(q.getId()));
    }

    @Transactional
    public QuestionDto create(QuestionRequest r) {
        Question q = new Question();
        apply(q, r);
        return QuestionDto.from(questions.save(q), null);
    }

    @Transactional
    public QuestionDto update(Long id, QuestionRequest r) {
        Question q = find(id);
        apply(q, r);
        return QuestionDto.from(questions.save(q), null);
    }

    @Transactional
    public void delete(Long id) { questions.delete(find(id)); }

    private void apply(Question q, QuestionRequest r) {
        q.setQuestionText(r.questionText().trim());
        q.setDifficulty(r.difficulty());
        q.setSkill(skills.findById(r.skillId()).orElseThrow(() -> ApiException.notFound("Skill")));
    }

    /** SOLVED wins over ATTEMPTED when a user has both kinds of attempt for a question. */
    private Map<Long, AttemptStatus> myStatuses(List<Question> list, Long userId) {
        Map<Long, AttemptStatus> map = new HashMap<>();
        if (list.isEmpty()) return map;
        for (Object[] row : attempts.statusesFor(userId, list.stream().map(Question::getId).toList())) {
            map.merge((Long) row[0], (AttemptStatus) row[1], (a, b) -> a == AttemptStatus.SOLVED || b == AttemptStatus.SOLVED ? AttemptStatus.SOLVED : a);
        }
        return map;
    }

    private Question find(Long id) { return questions.findById(id).orElseThrow(() -> ApiException.notFound("Question")); }
}
