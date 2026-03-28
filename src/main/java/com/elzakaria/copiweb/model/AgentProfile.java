package com.elzakaria.copiweb.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_profile")
@Getter
@Setter
public class AgentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Bootstrap color name: primary, success, warning, danger, info, secondary
    @Column(nullable = false, length = 32)
    private String color = "primary";

    // Bootstrap Icons class: bi-robot, bi-layers, etc.
    @Column(nullable = false, length = 64)
    private String icon = "bi-robot";

    @Column(nullable = false, length = 128)
    private String model = "gpt-4.1";

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "built_in", nullable = false)
    private boolean builtIn = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
