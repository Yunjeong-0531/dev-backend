package com.example.stay.auth.repository;

import com.example.stay.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /** 이메일은 소문자 정규화된 값으로 조회할 것 */
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
