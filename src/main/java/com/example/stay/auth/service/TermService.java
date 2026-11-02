package com.example.stay.auth.service;

import com.example.stay.auth.dto.TermResponse;
import com.example.stay.auth.entity.Term;
import com.example.stay.auth.entity.TermAgreement;
import com.example.stay.auth.entity.User;
import com.example.stay.auth.repository.TermAgreementRepository;
import com.example.stay.auth.repository.TermRepository;
import com.example.stay.common.BusinessException;
import com.example.stay.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TermService {

    private final TermRepository termRepository;
    private final TermAgreementRepository termAgreementRepository;

    @Transactional(readOnly = true)
    public List<TermResponse> getActiveTerms() {
        return termRepository.findAllByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(TermResponse::from)
                .toList();
    }

    /**
     * 가입 시 약관 동의 검증 + 이력 저장.
     *  - 요청에 포함된 약관 ID 는 모두 현재 노출 중인(active) 약관이어야 한다.
     *  - 필수 약관은 모두 포함되어야 한다.
     */
    @Transactional
    public void recordAgreements(User user, Collection<Long> agreedTermIds, String ip) {
        Set<Long> agreed = agreedTermIds == null ? Set.of() : new HashSet<>(agreedTermIds);
        List<Term> activeTerms = termRepository.findAllByActiveTrueOrderByDisplayOrderAsc();
        Set<Long> activeIds = new HashSet<>();
        activeTerms.forEach(t -> activeIds.add(t.getId()));

        if (!activeIds.containsAll(agreed)) {
            throw new BusinessException(ErrorCode.INVALID_TERMS);
        }
        for (Term t : activeTerms) {
            if (t.isRequired() && !agreed.contains(t.getId())) {
                throw new BusinessException(ErrorCode.REQUIRED_TERMS_NOT_AGREED);
            }
        }
        List<TermAgreement> agreements = activeTerms.stream()
                .filter(t -> agreed.contains(t.getId()))
                .map(t -> TermAgreement.of(user, t, true, ip))
                .toList();
        termAgreementRepository.saveAll(agreements);
    }
}
