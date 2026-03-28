package com.elzakaria.copiweb.service;

import com.elzakaria.copiweb.model.AgentProfile;
import com.elzakaria.copiweb.model.WorkflowCommand;
import com.elzakaria.copiweb.repository.AgentProfileRepository;
import com.elzakaria.copiweb.repository.WorkflowCommandRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentProfileService {

    private final AgentProfileRepository profileRepo;
    private final WorkflowCommandRepository commandRepo;

    @PostConstruct
    public void seedDefaults() {
        if (profileRepo.count() == 0) {
            seedProfiles();
        }
        if (commandRepo.count() == 0) {
            seedCommands();
        }
    }

    private void seedProfiles() {
        save("General Purpose", "No domain bias — vanilla Copilot agent for any task.",
            "secondary", "bi-robot", "gpt-4.1", null, true);

        save("Spring Batch Analyst",
            "Specializes in Spring Batch job forensics, failure analysis, and restart strategies.",
            "warning", "bi-layers", "gpt-4.1",
            "You are an expert Spring Batch engineer. When analyzing job failures, always check step execution " +
            "status, exit codes, and exception traces. Suggest restartable step configurations and skip/retry " +
            "policies where appropriate. Reference Spring Batch's JobRepository, StepExecution, and " +
            "ExitStatus in your analysis.",
            true);

        save("API Architect",
            "Contract-first REST API design and OpenAPI scaffolding for Spring Boot.",
            "primary", "bi-diagram-3", "gpt-4.1",
            "You are an expert Spring Boot REST API architect. Always follow contract-first design with " +
            "OpenAPI 3. Generate clean @RestController classes with proper @Valid validation, @ExceptionHandler " +
            "error handling, and @Operation/@Tag API documentation annotations. Prefer record DTOs and " +
            "immutable request/response objects.",
            true);

        save("Spring Integration Debug",
            "Inspects and debugs Spring Integration message flows, channels, and adapters.",
            "info", "bi-bezier2", "gpt-4.1",
            "You are a Spring Integration expert. When debugging message flows, inspect channel types " +
            "(DirectChannel, QueueChannel, PublishSubscribeChannel), gateway interfaces, and adapter " +
            "configurations. Identify backpressure, thread contention, and message loss issues. " +
            "Always suggest using IntegrationGraphServer for runtime visualization.",
            true);

        save("DB Migration Engineer",
            "Plans and reviews Flyway/Liquibase database migrations for safety and correctness.",
            "danger", "bi-database-gear", "gpt-4.1",
            "You are a database migration expert specializing in Flyway and Liquibase for PostgreSQL. " +
            "Always check migration ordering, reversibility, data integrity constraints, and index impact. " +
            "Flag any destructive operations (DROP, TRUNCATE) and suggest safe alternatives. " +
            "Provide estimated downtime and blue/green deployment strategies when relevant.",
            true);

        save("Code Reviewer",
            "Multi-concern code review: logic errors, security, performance, Spring best practices.",
            "success", "bi-eye", "gpt-4.1",
            "You are a senior Spring Boot code reviewer. Review for: " +
            "(1) Correctness and logic errors, " +
            "(2) Security vulnerabilities (OWASP Top 10: SQLi, XSS, IDOR, insecure deserialization), " +
            "(3) Performance anti-patterns (N+1 queries, missing @Transactional, unnecessary eager loading), " +
            "(4) Spring misuse (incorrect bean scopes, improper @Transactional propagation, circular deps), " +
            "(5) Test coverage gaps. Be specific — cite line numbers and suggest exact fixes.",
            true);

        log.info("Seeded {} default agent profiles", profileRepo.count());
    }

    private AgentProfile save(String name, String description, String color, String icon,
                               String model, String systemPrompt, boolean builtIn) {
        var p = new AgentProfile();
        p.setName(name);
        p.setDescription(description);
        p.setColor(color);
        p.setIcon(icon);
        p.setModel(model);
        p.setSystemPrompt(systemPrompt);
        p.setBuiltIn(builtIn);
        return profileRepo.save(p);
    }

    private void seedCommands() {
        var profiles = profileRepo.findAllByOrderByBuiltInDescNameAsc();
        AgentProfile batchAnalyst = profiles.stream().filter(p -> p.getName().equals("Spring Batch Analyst")).findFirst().orElse(null);
        AgentProfile apiArchitect = profiles.stream().filter(p -> p.getName().equals("API Architect")).findFirst().orElse(null);
        AgentProfile dbEngineer = profiles.stream().filter(p -> p.getName().equals("DB Migration Engineer")).findFirst().orElse(null);
        AgentProfile reviewer = profiles.stream().filter(p -> p.getName().equals("Code Reviewer")).findFirst().orElse(null);

        saveCommand("forensics", "Analyze Job Failure",
            "Diagnose a Spring Batch job failure and suggest a fix.", "bi-bug",
            "Analyze this Spring Batch job failure:\n\n" +
            "Job Name: {{jobName}}\n" +
            "Failed Step: {{stepName}}\n" +
            "Exception:\n{{exception}}\n\n" +
            "Please:\n" +
            "1. Diagnose the root cause\n" +
            "2. Determine if it is a data, infrastructure, or configuration issue\n" +
            "3. Suggest a fix or workaround\n" +
            "4. Recommend skip/retry policy adjustments if applicable",
            """
            [
              {"name":"jobName","label":"Job Name","placeholder":"EXPORT_ORDERS","required":true},
              {"name":"stepName","label":"Failed Step","placeholder":"readItemsStep","required":false},
              {"name":"exception","label":"Exception / Stack Trace","placeholder":"java.lang.NullPointerException at...","required":true,"multiline":true}
            ]
            """,
            batchAnalyst, true);

        saveCommand("scaffold", "Scaffold REST API",
            "Generate a Spring Boot REST controller from an entity description.", "bi-code-slash",
            "Generate a complete Spring Boot REST API for the following entity:\n\n" +
            "Entity Name: {{entityName}}\n" +
            "Fields: {{fields}}\n" +
            "Operations: {{operations}}\n\n" +
            "Include:\n" +
            "- @RestController with @RequestMapping\n" +
            "- Full CRUD based on requested operations\n" +
            "- @Valid input validation with record DTOs\n" +
            "- Spring Data JPA repository interface\n" +
            "- OpenAPI annotations (@Operation, @Tag)\n" +
            "- Proper @ExceptionHandler error handling",
            """
            [
              {"name":"entityName","label":"Entity Name","placeholder":"Product","required":true},
              {"name":"fields","label":"Fields","placeholder":"id: Long, name: String, price: BigDecimal, category: String","required":true},
              {"name":"operations","label":"Operations","placeholder":"GET (list + by id), POST, PUT, DELETE","required":false}
            ]
            """,
            apiArchitect, true);

        saveCommand("migrate", "Plan DB Migration",
            "Plan a database schema migration with Flyway/Liquibase.", "bi-database-up",
            "Plan a database migration for the following change:\n\n" +
            "Change Description: {{changeDescription}}\n" +
            "Affected Tables: {{tables}}\n" +
            "Database: PostgreSQL\n\n" +
            "Provide:\n" +
            "1. Flyway migration script (Vx__description.sql)\n" +
            "2. Data integrity checks before and after\n" +
            "3. Rollback strategy\n" +
            "4. Index impact analysis\n" +
            "5. Estimated downtime and safe deployment approach",
            """
            [
              {"name":"changeDescription","label":"Change Description","placeholder":"Add nullable 'category' column to 'product' table","required":true},
              {"name":"tables","label":"Affected Tables","placeholder":"product, product_category","required":false}
            ]
            """,
            dbEngineer, true);

        saveCommand("review", "Code Review",
            "Multi-concern code review pass on a snippet or file.", "bi-eye",
            "Review the following code for correctness, security, performance, and Spring best practices:\n\n" +
            "{{code}}\n\n" +
            "Focus areas:\n" +
            "- Logic errors and edge cases\n" +
            "- Security vulnerabilities (OWASP Top 10)\n" +
            "- Performance issues (N+1 queries, missing transactions, unnecessary locks)\n" +
            "- Spring anti-patterns and misuse\n" +
            "- Missing test coverage areas\n\n" +
            "Be specific — cite line numbers and suggest exact fixes.",
            """
            [
              {"name":"code","label":"Code to Review","placeholder":"Paste your Java / Spring code here...","required":true,"multiline":true}
            ]
            """,
            reviewer, true);

        saveCommand("monitor", "Start Monitor Loop",
            "Ask the agent to monitor a metric or log pattern and report anomalies.", "bi-activity",
            "Monitor the following and alert me if anomalies are detected:\n\n" +
            "Target: {{target}}\n" +
            "Anomaly Condition: {{condition}}\n" +
            "Check Interval: {{interval}}\n\n" +
            "For each anomaly found:\n" +
            "1. Describe what was detected\n" +
            "2. Assess severity (LOW / MEDIUM / HIGH / CRITICAL)\n" +
            "3. Suggest immediate action",
            """
            [
              {"name":"target","label":"What to Monitor","placeholder":"Spring Batch job EXPORT_ORDERS, last 100 executions","required":true},
              {"name":"condition","label":"Anomaly Condition","placeholder":"Job failure rate > 5% or step duration > 2x average","required":true},
              {"name":"interval","label":"Check Interval","placeholder":"Every 15 minutes","required":false}
            ]
            """,
            null, true);

        saveCommand("integration-debug", "Debug Integration Flow",
            "Inspect a Spring Integration flow for issues like backpressure or message loss.", "bi-bezier2",
            "Debug the following Spring Integration flow:\n\n" +
            "Flow Description: {{flowDescription}}\n" +
            "Observed Symptom: {{symptom}}\n\n" +
            "Please:\n" +
            "1. Identify likely root causes\n" +
            "2. Check for thread pool exhaustion or channel backpressure\n" +
            "3. Suggest wiretap or logging channel additions for tracing\n" +
            "4. Recommend configuration changes",
            """
            [
              {"name":"flowDescription","label":"Flow Description","placeholder":"InboundFileAdapter -> transformer -> ServiceActivator -> JdbcOutboundGateway","required":true},
              {"name":"symptom","label":"Observed Symptom","placeholder":"Messages disappearing after transformer, no errors in logs","required":true}
            ]
            """,
            profiles.stream().filter(p -> p.getName().equals("Spring Integration Debug")).findFirst().orElse(null),
            true);

        log.info("Seeded {} default workflow commands", commandRepo.count());
    }

    private void saveCommand(String name, String label, String description, String icon,
                              String promptTemplate, String paramSchema,
                              AgentProfile agentProfile, boolean builtIn) {
        var c = new WorkflowCommand();
        c.setName(name);
        c.setLabel(label);
        c.setDescription(description);
        c.setIcon(icon);
        c.setPromptTemplate(promptTemplate);
        c.setParamSchema(paramSchema != null ? paramSchema.strip() : null);
        c.setAgentProfile(agentProfile);
        c.setBuiltIn(builtIn);
        commandRepo.save(c);
    }

    public List<AgentProfile> listProfiles() {
        return profileRepo.findAllByOrderByBuiltInDescNameAsc();
    }

    public AgentProfile getProfile(Long id) {
        return profileRepo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Agent profile not found: " + id));
    }

    public AgentProfile createProfile(AgentProfile profile) {
        profile.setBuiltIn(false);
        return profileRepo.save(profile);
    }

    public void deleteProfile(Long id) {
        var profile = getProfile(id);
        if (profile.isBuiltIn()) {
            throw new IllegalStateException("Cannot delete a built-in agent profile");
        }
        profileRepo.delete(profile);
    }
}
