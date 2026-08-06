package com.cth.sdm.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.Set;

@Entity
@Table(name = "sdlc_phases")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SDLCPhase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(length = 255)
    private String description;

    @OneToMany(mappedBy = "phase", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<SDLCDeliverable> deliverables;
}
