package com.dbtraining.reconx.repository;

import com.dbtraining.reconx.repository.entity.ReconBreak;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconBreakRepository extends JpaRepository<ReconBreak, Long> {


    /** TICKET-ADV085 */
    long countByStatus(String status);


    /** TICKET-ADV069 */
    Page<ReconBreak> findAll(Pageable pageable);

}