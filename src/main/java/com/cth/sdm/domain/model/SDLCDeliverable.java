package com.cth.sdm.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sdlc_deliverables")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SDLCDeliverable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(length = 255)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_id", nullable = false)
    private SDLCPhase phase;
}
