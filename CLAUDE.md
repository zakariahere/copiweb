# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the application (starts Docker Compose postgres automatically via spring-boot-docker-compose)
./mvnw spring-boot:run

# Build
./mvnw package

# Run tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=MyTestClass

# Build Docker image
./mvnw spring-boot:build-image
```

## Architecture

This is a **Spring Boot 4.0.5** web application (Java 25) that provides an advanced UI for managing and observing GitHub Copilot SDK agents.

**Stack:**
- `spring-boot-starter-webmvc` — Spring MVC controllers
- `Thymeleaf` — server-side HTML templates (to be added to pom.xml)
- `spring-boot-starter-data-jpa` + PostgreSQL — persistence for agent sessions, history, logs
- `copilot-sdk-java` (`0.2.1-java.0`) — AI agent orchestration (to be added to pom.xml)
- `Lombok` — boilerplate reduction
- `spring-boot-docker-compose` — auto-starts `compose.yaml` (postgres) on `spring-boot:run`

**Intended structure:**
```
src/main/java/com/elzakaria/copiweb/
  agent/       — CopilotClient wrappers, session management, event handling
  web/         — Spring MVC @Controller classes + REST endpoints
  model/       — JPA entities (AgentSession, AgentEvent, etc.)
  repository/  — Spring Data JPA repositories
src/main/resources/
  templates/   — Thymeleaf HTML templates
  static/      — JS/CSS assets
```

**Copilot SDK key integration points:**
- `CopilotClient` is `AutoCloseable` — manage as a Spring `@Bean` singleton, call `client.start().get()` on startup
- Sessions are created via `client.createSession(SessionConfig)` — store session IDs in DB for resume/history
- All SDK methods return `CompletableFuture` — use `@Async` or reactive patterns on the Spring side
- Streaming output uses `AssistantMessageDeltaEvent` — push to browser via SSE (`SseEmitter`) or WebSocket
- `SessionIdleEvent` signals turn completion
- Agent activity (tool calls, errors) is observable via `ToolExecutionStartEvent`, `ToolExecutionCompleteEvent`, `SessionErrorEvent`

**Maven dependency to add for Copilot SDK:**
```xml
<dependency>
    <groupId>com.github</groupId>
    <artifactId>copilot-sdk-java</artifactId>
    <version>0.2.1-java.0</version>
</dependency>
```

**Maven dependency to add for Thymeleaf:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

## Database

Docker Compose (`compose.yaml`) runs Postgres with:
- DB: `mydatabase`, user: `myuser`, password: `secret`
- Port mapped dynamically (Spring Boot auto-configures datasource)

Set in `application.properties` when needed:
```properties
spring.jpa.hibernate.ddl-auto=update
```
