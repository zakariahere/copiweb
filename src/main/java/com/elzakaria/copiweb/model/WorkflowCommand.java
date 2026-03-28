package com.elzakaria.copiweb.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "workflow_command")
@Getter
@Setter
public class WorkflowCommand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Command name without slash, e.g. "forensics"
    @Column(nullable = false, unique = true, length = 64)
    private String name;

    // Human-readable label, e.g. "Analyze Job Failure"
    @Column(nullable = false, length = 128)
    private String label;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 64)
    private String icon = "bi-lightning";

    // Prompt text with {{param}} placeholders
    @Column(name = "prompt_template", columnDefinition = "TEXT")
    private String promptTemplate;

    // JSON array of param definitions:
    // [{"name":"jobName","label":"Job Name","placeholder":"...","required":true,"multiline":false}]
    @Column(name = "param_schema", columnDefinition = "TEXT")
    private String paramSchema;

    @Column(name = "built_in", nullable = false)
    private boolean builtIn = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
