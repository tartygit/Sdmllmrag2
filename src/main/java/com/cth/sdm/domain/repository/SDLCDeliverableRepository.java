package com.cth.sdm.domain.repository;

import com.cth.sdm.domain.model.SDLCDeliverable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SDLCDeliverableRepository extends JpaRepository<SDLCDeliverable, Long> {
    Optional<SDLCDeliverable> findByCode(String code);
    List<SDLCDeliverable> findByPhaseId(Long phaseId);
}
