package com.elzakaria.copiweb.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "agent_session")
@Getter
@Setter
public class AgentSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true, length = 128)
    private String sessionId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 128)
    private String model;

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "working_directory")
    private String workingDirectory;

    @Column(name = "selected_agent_name", length = 256)
    private String selectedAgentName;

    @Column(name = "selected_agent_display_name", length = 256)
    private String selectedAgentDisplayName;

    @Column(nullable = false)
    private boolean streaming = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SessionStatus status = SessionStatus.CREATING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @Column(name = "turn_count", nullable = false)
    private int turnCount = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sequence ASC")
    @JsonIgnore
    private List<AgentEvent> events = new ArrayList<>();
}
