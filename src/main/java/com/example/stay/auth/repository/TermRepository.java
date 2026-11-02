package com.example.stay.auth.repository;

import com.example.stay.auth.entity.Term;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TermRepository extends JpaRepository<Term, Long> {

    /** GET /api/terms : 가입 화면에 노출할 약관 */
    List<Term> findAllByActiveTrueOrderByDisplayOrderAsc();

    /** 가입 시 전달된 약관 ID 검증용 */
    List<Term> findAllByIdIn(Collection<Long> ids);

    Optional<Term> findByTypeAndVersion(String type, String version);
}
