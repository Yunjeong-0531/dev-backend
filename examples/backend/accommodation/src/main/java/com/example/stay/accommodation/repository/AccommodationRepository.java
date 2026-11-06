package com.example.stay.accommodation.repository;

import com.example.stay.accommodation.entity.Accommodation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccommodationRepository extends JpaRepository<Accommodation, Long> {

    List<Accommodation> findAllByHostId(Long hostId);
}
