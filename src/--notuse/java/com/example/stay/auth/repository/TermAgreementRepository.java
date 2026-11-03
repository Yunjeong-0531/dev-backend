package com.example.stay.auth.repository;

import com.example.stay.auth.entity.TermAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TermAgreementRepository extends JpaRepository<TermAgreement, Long> {

    /** 회원의 전체 동의 이력 (최신순) */
    @Query("select ta from TermAgreement ta join fetch ta.term " +
           "where ta.user.id = :userId order by ta.agreedAt desc")
    List<TermAgreement> findAllByUserId(@Param("userId") Long userId);

    /** 약관(term)별 가장 최근 동의/철회 상태만 조회 — 재동의 필요 여부 판단용 */
    @Query("select ta from TermAgreement ta join fetch ta.term " +
           "where ta.user.id = :userId " +
           "and ta.id in (select max(a.id) from TermAgreement a " +
           "              where a.user.id = :userId group by a.term.id)")
    List<TermAgreement> findLatestByUserId(@Param("userId") Long userId);
}
