package com.linksentinel.click;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClickRepository extends JpaRepository<Click, Long> {

    long countByLinkId(long linkId);

    @Query(value = """
            select to_char(date_trunc('day', clicked_at), 'YYYY-MM-DD'), count(*)
            from clicks
            where link_id = :linkId
            group by 1
            order by 1
            """, nativeQuery = true)
    List<Object[]> countByDay(@Param("linkId") long linkId);

    @Query("""
            select c.deviceType, count(c) from Click c
            where c.linkId = :linkId
            group by c.deviceType
            """)
    List<Object[]> countByDevice(@Param("linkId") long linkId);

    @Query(value = """
            select coalesce(referrer, 'direct') as label, count(*) as clicks
            from clicks
            where link_id = :linkId
            group by label
            order by clicks desc
            limit 10
            """, nativeQuery = true)
    List<Object[]> countByReferrer(@Param("linkId") long linkId);
}
