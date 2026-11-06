package com.example.stay.accommodation.dto;

import com.example.stay.accommodation.entity.Accommodation;

/** 응답에 엔티티를 그대로 내보내지 말고 DTO 로 변환한다. */
public record AccommodationResponse(Long id, Long hostId, String name, String region, int pricePerNight) {

    public static AccommodationResponse from(Accommodation a) {
        return new AccommodationResponse(a.getId(), a.getHostId(), a.getName(), a.getRegion(), a.getPricePerNight());
    }
}
