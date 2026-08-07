package com.cth.sdm.domain.repository;

import com.cth.sdm.domain.model.SDLCPhase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SDLCPhaseRepository extends JpaRepository<SDLCPhase, Long> {
    Optional<SDLCPhase> findByCode(String code);
}
