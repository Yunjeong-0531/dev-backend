package com.example.stay.accommodation.service;

import com.example.stay.accommodation.dto.AccommodationRequest;
import com.example.stay.accommodation.dto.AccommodationResponse;
import com.example.stay.accommodation.entity.Accommodation;
import com.example.stay.accommodation.exception.AccommodationErrorCode;
import com.example.stay.accommodation.repository.AccommodationRepository;
import com.example.stay.common.BusinessException;
import com.example.stay.common.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccommodationService {

    private final AccommodationRepository repository;

    @Transactional(readOnly = true)
    public List<AccommodationResponse> list() {
        return repository.findAll().stream().map(AccommodationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AccommodationResponse get(Long id) {
        return AccommodationResponse.from(findOrThrow(id));
    }

    @Transactional
    public AccommodationResponse create(AuthUser actor, AccommodationRequest req) {
        // 호스트 id 는 요청 본문이 아니라 "로그인 정보"에서 가져온다 (남의 이름으로 등록 방지)
        Accommodation saved = repository.save(
                Accommodation.create(actor.id(), req.name(), req.region(), req.pricePerNight()));
        return AccommodationResponse.from(saved);
    }

    @Transactional
    public AccommodationResponse update(AuthUser actor, Long id, AccommodationRequest req) {
        Accommodation a = findOrThrow(id);
        assertCanModify(actor, a);
        a.update(req.name(), req.region(), req.pricePerNight());
        return AccommodationResponse.from(a);
    }

    @Transactional
    public void delete(AuthUser actor, Long id) {
        Accommodation a = findOrThrow(id);
        assertCanModify(actor, a);
        repository.delete(a);
    }

    /**
     * [역할 + 소유권] 검사. 컨트롤러의 @HostOrAdmin 은 "HOST/ADMIN 인가"만 보고,
     * "이 숙소가 내 것인가"는 데이터를 읽어야 알 수 있으므로 서비스에서 확인한다.
     *  - ADMIN: 모든 숙소 가능
     *  - HOST : 본인이 등록한 숙소만 가능
     */
    private void assertCanModify(AuthUser actor, Accommodation a) {
        if (actor.isAdmin() || a.isOwnedBy(actor.id())) {
            return;
        }
        throw new BusinessException(AccommodationErrorCode.NOT_ACCOMMODATION_OWNER);
    }

    private Accommodation findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(AccommodationErrorCode.ACCOMMODATION_NOT_FOUND));
    }
}
