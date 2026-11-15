package com.linksentinel.link;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LinkRepository extends JpaRepository<Link, Long> {

    Optional<Link> findByCode(String code);

    boolean existsByCode(String code);

    List<Link> findByOwnerIdOrderByCreatedAtDesc(long ownerId);

    @Query(value = "select nextval('link_code_seq')", nativeQuery = true)
    long nextCodeValue();
}
