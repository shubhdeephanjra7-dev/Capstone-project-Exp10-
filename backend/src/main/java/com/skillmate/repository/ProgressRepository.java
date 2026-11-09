package com.skillmate.repository;

import com.skillmate.entity.Progress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ProgressRepository extends JpaRepository<Progress, Long> {
    List<Progress> findByUserId(Long userId);
    Optional<Progress> findByUserIdAndSkillId(Long userId, Long skillId);
}
