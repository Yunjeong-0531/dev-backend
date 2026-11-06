package com.example.stay.accommodation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 요청 DTO 는 record + Bean Validation. 검증 실패는 공용 핸들러가 400(fieldErrors 포함)으로 응답한다. */
public record AccommodationRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 30) String region,
        @Min(0) int pricePerNight
) {
}
