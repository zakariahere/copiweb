package com.elzakaria.copiweb.service;

import com.elzakaria.copiweb.model.WorkflowCommand;
import com.elzakaria.copiweb.repository.WorkflowCommandRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowCommandService {

    private final WorkflowCommandRepository commandRepo;

    @PostConstruct
    public void seedDefaults() {
        if (commandRepo.count() == 0) {
            seedCommands();
        }
    }

    public List<WorkflowCommand> listCommands() {
        return commandRepo.findAllByOrderByBuiltInDescNameAsc();
    }

    public WorkflowCommand getCommand(Long id) {
        return commandRepo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Workflow command not found: " + id));
    }

    public String assemblePrompt(Long id, Map<String, String> params) {
        var command = getCommand(id);
        String template = command.getPromptTemplate();
        if (template == null || template.isBlank()) return "";
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String value = entry.getValue() != null ? entry.getValue() : "";
            template = template.replace("{{" + entry.getKey() + "}}", value);
        }
        return template;
    }

    private void seedCommands() {
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
            true);

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
            true);

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
            true);

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
            true);

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
            true);

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
            true);

        log.info("Seeded {} default workflow commands", commandRepo.count());
    }

    private void saveCommand(String name, String label, String description, String icon,
                             String promptTemplate, String paramSchema, boolean builtIn) {
        var command = new WorkflowCommand();
        command.setName(name);
        command.setLabel(label);
        command.setDescription(description);
        command.setIcon(icon);
        command.setPromptTemplate(promptTemplate);
        command.setParamSchema(paramSchema != null ? paramSchema.strip() : null);
        command.setBuiltIn(builtIn);
        commandRepo.save(command);
    }
}
