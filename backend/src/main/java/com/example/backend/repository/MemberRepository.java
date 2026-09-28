package com.example.backend.repository;

import com.example.backend.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Database access for Member.
 * Spring Data JPA writes the implementation of this interface at startup.
 */
@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    // SELECT * FROM members WHERE email = ?
    Optional<Member> findByEmail(String email);

    // SELECT COUNT(*) > 0 FROM members WHERE email = ?
    boolean existsByEmail(String email);
}
