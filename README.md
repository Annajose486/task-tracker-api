# Task Tracker API

A RESTful Spring Boot API for lightweight project & task management. Users can be
assigned tasks within projects; every task belongs to exactly one project.

- **Java** 17 · **Spring Boot** 3.3 · **Maven**
- **H2** in-memory database (zero external setup)
- JPA/Hibernate entities, service layer, Bean Validation, global error handling
- OpenAPI 3.0 spec: [`openapi.yaml`](openapi.yaml) (project root)
- Swagger UI via springdoc-openapi

## Project structure

```
src/main/java/com/symphonyai/tasktracker/
├── controller/    # REST controllers (thin — delegates to services)
├── service/       # Business logic, transactional boundaries
├── repository/    # Spring Data JPA repositories
├── entity/        # Project, User, Task + enums
├── dto/           # Request/response DTOs (validated with Bean Validation)
├── exception/     # Custom exceptions + @RestControllerAdvice handler
└── web/           # Pagination/sort helper
```

## Requirements

- JDK 17+
- Maven 3.6+ (or use Docker — see below)

## Run

```bash
mvn spring-boot:run
```

The API starts on `http://localhost:8080`.

| Useful URLs | |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| H2 console | http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:tasktrackerdb`, user: `sa`, no password) |

## Tests

```bash
mvn test
```

Covers the service layer with JUnit 5 + Mockito, plus `@SpringBootTest` + MockMvc
integration tests exercising the endpoints end to end.

## Build a jar

```bash
mvn clean package
java -jar target/task-tracker-api-1.0.0.jar
```

## Docker

```bash
docker build -t task-tracker-api .
docker run -p 8080:8080 task-tracker-api
# or
docker compose up --build
```

## Configuration

Externalised in `src/main/resources/application.properties`:

| Property | Default | Purpose |
|---|---|---|
| `app.name` | `Task Tracker API` | Exposed via `GET /api/info` |
| `app.version` | `1.0.0` | Exposed via `GET /api/info` |
| `task.default-priority` | `MEDIUM` | Priority applied when a task is created/updated without one |
| `pagination.default-page-size` | `10` | Default `size` for list endpoints |
| `server.port` | `8080` | HTTP port |

## Data model

- **Project** `1 ── n` **Task** — deleting a project cascade-deletes its tasks
- **User** `1 ── n` **Task** (assignee, nullable) — deleting a user unassigns their tasks
- `Project.name` and `User.email` are unique (violations return `409 Conflict`)

## Endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/info` | App name & version |
| POST | `/api/projects` | Create project |
| GET | `/api/projects` | List projects (paged) |
| GET | `/api/projects/{id}` | Get project |
| PUT | `/api/projects/{id}` | Update project |
| DELETE | `/api/projects/{id}` | Delete project + its tasks |
| GET | `/api/projects/{id}/tasks` | Tasks for a project |
| POST | `/api/users` | Create user |
| GET | `/api/users` | List users (paged) |
| GET | `/api/users/{id}` | Get user |
| PUT | `/api/users/{id}` | Update user |
| DELETE | `/api/users/{id}` | Delete user (unassigns tasks) |
| POST | `/api/tasks` | Create task |
| GET | `/api/tasks` | List tasks — filter with `?status=&projectId=` |
| GET | `/api/tasks/{id}` | Get task |
| PUT | `/api/tasks/{id}` | Update task details/status |
| DELETE | `/api/tasks/{id}` | Delete task |

List endpoints accept `page`, `size`, and `sort` (e.g. `?page=0&size=5&sort=dueDate,desc&sort=title,asc`)
and return `{ content, page, size, totalElements, totalPages }`.

## Sample curl requests

```bash
# App info
curl http://localhost:8080/api/info

# Create a project
curl -X POST http://localhost:8080/api/projects \
  -H "Content-Type: application/json" \
  -d '{"name":"Website Revamp","description":"Rebuild the marketing site"}'

# Create a user
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Ada Lovelace","email":"ada@example.com","role":"DEVELOPER"}'

# Create a task (priority defaults to task.default-priority when omitted)
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Design landing page","projectId":1,"assigneeId":1,"dueDate":"2026-11-01"}'

# List tasks for a project
curl "http://localhost:8080/api/projects/1/tasks"

# Filter tasks by status, sorted by due date
curl "http://localhost:8080/api/tasks?status=TODO&sort=dueDate,asc"

# Update a task's status
curl -X PUT http://localhost:8080/api/tasks/1 \
  -H "Content-Type: application/json" \
  -d '{"title":"Design landing page","status":"IN_PROGRESS","priority":"HIGH","projectId":1,"assigneeId":1}'

# Update a project
curl -X PUT http://localhost:8080/api/projects/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Website Revamp v2","description":"Updated description"}'

# Delete entities
curl -X DELETE http://localhost:8080/api/tasks/1
curl -X DELETE http://localhost:8080/api/users/1
curl -X DELETE http://localhost:8080/api/projects/1
```

## Error responses

All errors return a structured body:

```json
{
  "timestamp": "2026-10-22T10:15:30",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/tasks",
  "fieldErrors": { "title": "Task title is required" }
}
```

| Status | When |
|---|---|
| 400 | Bean Validation failures, malformed JSON, invalid enum/param values |
| 404 | Entity not found (including invalid `projectId`/`assigneeId` references) |
| 409 | Duplicate `Project.name` or `User.email`, FK constraint violations |
| 500 | Unexpected errors |
