# CoPiWeb

> An advanced web dashboard for managing and observing [GitHub Copilot SDK](https://github.com/github/copilot-sdk-java) agent sessions in real time.

Built with **Spring Boot 4**, **Thymeleaf**, and **Server-Sent Events** — watch your AI agents think, use tools, and spawn subagents live in the browser. Includes a domain-specialized **Agent Catalog** and a **Workflow Command** system for launching pre-built prompt templates from within any session.

---

## Features

- **Agent Catalog** — pick from domain-specialized agents (Spring Batch, API Architect, Spring Integration, DB Migration, Code Reviewer, or General Purpose) or create your own with a custom system prompt and model
- **Workflow Commands** — type `/` in any session to open a command palette; select a command, fill in its parameters, and the assembled prompt is inserted into the chat ready to send
- **Session management** — create, resume, and delete Copilot SDK agent sessions with configurable model, system prompt, and streaming settings; pre-fill model and system prompt by selecting an agent profile
- **Real-time streaming** — response chunks arrive in the browser as they're generated via SSE, no polling
- **Live event console** — every SDK event (tool calls, subagent spawns, errors, idle signals) is displayed in a scrollable console as it happens
- **Tool execution timeline** — each tool invocation shows its name, arguments, and result with a live running/done state
- **Full history** — all events are persisted to PostgreSQL and viewable as a complete ordered timeline per session
- **Dashboard overview** — active sessions, total sessions, events today, and recent activity at a glance
- **REST API** — all operations are also available as JSON endpoints under `/api/**`

---

## Stack

| Layer | Technology |
|-------|------------|
| Language | Java 25 |
| Framework | Spring Boot 4.0.5 |
| AI SDK | [copilot-sdk-java](https://github.com/github/copilot-sdk-java) 0.2.1-java.0 |
| UI | Thymeleaf 3.1 + Bootstrap 5.3 |
| Streaming | Spring MVC `SseEmitter` |
| Persistence | Spring Data JPA + PostgreSQL |
| Build | Maven (wrapper included) |
| Dev DB | Docker Compose (auto-started) |

---

## Prerequisites

- Java 17+ (project uses Java 25)
- Docker (for the PostgreSQL container)
- [GitHub Copilot CLI](https://docs.github.com/en/copilot/github-copilot-in-the-cli) ≥ 0.0.411-1, authenticated (`gh auth login`)
- GitHub Packages credentials to resolve the SDK artifact (see below)

### GitHub Packages setup

The Copilot SDK is hosted on GitHub Packages. Add credentials to `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>github-copilot-sdk</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_GITHUB_TOKEN</password>  <!-- needs read:packages scope -->
    </server>
  </servers>
</settings>
```

---

## Getting Started

```bash
# Clone
git clone https://github.com/your-username/copiweb.git
cd copiweb

# Run (starts PostgreSQL via Docker Compose automatically)
./mvnw spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080).

Spring Boot's Docker Compose integration starts the PostgreSQL container on first run. Hibernate creates the tables automatically (`ddl-auto=update`). On first startup, the 6 built-in agent profiles and 6 workflow commands are seeded into the database automatically.

---

## Usage

### 1. Pick an agent and create a session

Go to **Agents** to browse the built-in domain profiles, or click **New Session** and pick one from the agent picker at the top. Selecting an agent pre-fills the model and system prompt fields. You can still override both.

**Built-in agents:**

| Agent | Specialisation |
|-------|---------------|
| General Purpose | No domain bias — vanilla agent for any task |
| Spring Batch Analyst | Job forensics, failure analysis, skip/retry strategies |
| API Architect | Contract-first REST API design and OpenAPI scaffolding |
| Spring Integration Debug | Message flow inspection, backpressure, adapter debugging |
| DB Migration Engineer | Flyway/Liquibase planning, safety review, rollback strategy |
| Code Reviewer | Logic errors, security (OWASP), performance, Spring anti-patterns |

### 2. Chat with the agent

Type a message and press **Send** or `Ctrl+Enter`. The response streams in real time. While the agent is running:

- Response text appears chunk-by-chunk in the **Response** pane
- Tool executions appear as cards in the **Tool Executions** timeline with live status
- Every SDK event is logged to the **Event Console** on the right

### 3. Use workflow commands

Type `/` in the message input to open the command palette. Navigate with arrow keys or click a command:

| Command | What it does |
|---------|-------------|
| `/forensics` | Analyze a Spring Batch job failure — diagnose root cause and suggest fix |
| `/scaffold` | Generate a Spring Boot REST controller from an entity description |
| `/migrate` | Plan a Flyway migration script with rollback and index analysis |
| `/review` | Multi-concern code review (security, performance, Spring best practices) |
| `/monitor` | Ask the agent to watch a metric or log pattern for anomalies |
| `/integration-debug` | Debug a Spring Integration flow for backpressure or message loss |

Commands with parameters open a form modal — fill in the fields and click **Use Prompt** to assemble and insert the final prompt into the chat.

### 4. Observe and replay

Click **History** on any session to see the complete event log — user messages, assistant responses, tool calls with full JSON args/results, subagent events — in order.

---

## Project Structure

```
src/main/java/com/elzakaria/copiweb/
├── config/          # CopilotClient Spring bean, async thread pool
├── model/           # JPA entities (AgentSession, AgentEvent, AgentProfile, WorkflowCommand) + enums
├── repository/      # Spring Data JPA repositories
├── agent/           # SDK session handle, in-memory registry, event→DB+SSE bridge
├── service/         # Session orchestration, agent profile seeding, model cache, SSE management
├── web/             # MVC controllers (Dashboard, AgentCatalog) + REST API + SSE endpoint
└── dto/             # Request/response records

src/main/resources/
├── templates/
│   ├── layout/      # Shared base layout (Bootstrap 5, navbar)
│   ├── sessions/    # Session list, new, detail, history
│   ├── agents/      # Agent catalog (list, create custom)
│   └── commands/    # Workflow command catalog
└── static/
    └── js/
        ├── sse-client.js        # SSE stream consumer
        ├── chat.js              # Send/abort, input state
        └── command-palette.js   # "/" command palette + param modal
```

### Key architectural decisions

**`CopilotClient` as a singleton Spring bean** — started on `@PostConstruct`, closed via `destroyMethod = "close"` on context shutdown.

**SDK first, DB second** — `createSession` calls the SDK to get the `session_id` before any DB write, avoiding NOT NULL constraint violations on the first insert.

**SSE for streaming** — SDK events fire on the SDK's internal thread and are immediately broadcast to all subscribed `SseEmitter` instances for the session. Delta chunks are broadcast only (not persisted); the complete `AssistantMessageEvent` is what gets written to the DB.

**`@Async` persistence** — event handlers dispatch DB writes to the Spring async executor so they never block the SDK's event dispatch thread.

**Agent profiles & workflow commands are seeded once** — `AgentProfileService.seedDefaults()` runs on `@PostConstruct` and only inserts rows when the tables are empty, so restarts don't re-seed.

**Command palette is fully client-side** — `command-palette.js` fetches `/api/commands` on load and drives the dropdown and param modal with vanilla JS; no server round-trip until the user hits "Use Prompt" to assemble the final filled template.

---

## REST API

### Sessions

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/sessions` | List all sessions |
| `POST` | `/api/sessions` | Create a session |
| `GET` | `/api/sessions/{id}` | Get session details |
| `DELETE` | `/api/sessions/{id}` | Delete a session |
| `POST` | `/api/sessions/{id}/message` | Send a message (response via SSE) |
| `POST` | `/api/sessions/{id}/abort` | Abort current turn |
| `GET` | `/api/sessions/{id}/history` | Full event list |
| `GET` | `/api/sessions/{sdkId}/stream` | SSE event stream (`text/event-stream`) |
| `GET` | `/api/models` | List available models |

### Agent Catalog

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/agents` | List all agent profiles |
| `POST` | `/api/agents` | Create a custom agent profile |
| `DELETE` | `/api/agents/{id}` | Delete a custom agent profile (built-in profiles cannot be deleted) |

### Workflow Commands

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/commands` | List all workflow commands (includes param schema) |
| `POST` | `/api/commands/{id}/assemble` | Fill a command's prompt template with params; returns `{"prompt": "..."}` |

---

## Development

```bash
# Run with live reload (DevTools enabled)
./mvnw spring-boot:run

# Run tests
./mvnw test

# Build JAR
./mvnw package

# Build Docker image
./mvnw spring-boot:build-image
```

To point at an external Postgres instead of Docker Compose, set:

```properties
spring.boot.docker-compose.enabled=false
spring.datasource.url=jdbc:postgresql://host:5432/mydb
spring.datasource.username=user
spring.datasource.password=pass
```

---

## License

MIT
