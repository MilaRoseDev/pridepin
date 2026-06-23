# PridePin 🏳️‍⚧️

A safe-space mapping application built for the transgender community. Users can discover, rate, and review LGBTQ+-friendly locations on an interactive map.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Local Setup](#local-setup)
- [Environment Variables](#environment-variables)
- [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [Deployment](#deployment)
- [Project Structure](#project-structure)

---

## Features

- JWT-based authentication (register / login)
- Interactive Leaflet.js map with category-coloured markers
- Add, update, and soft-delete locations
- Rate locations 1–5 stars and leave comments
- Paginated location and review browsing
- Role-based access control (USER / ADMIN)
- OpenAPI / Swagger UI for interactive API exploration
- Soft deletes throughout — nothing is truly destroyed

---

## Tech Stack

| Layer      | Technology                          |
|------------|-------------------------------------|
| Language   | Java 17                             |
| Framework  | Spring Boot 4.0.3                   |
| Security   | Spring Security 7 + JJWT 0.12.6    |
| Persistence| Spring Data JPA + Hibernate         |
| Database   | PostgreSQL 15+                      |
| Build      | Maven 3.9+                          |
| Docs       | SpringDoc OpenAPI 2.x               |
| Frontend   | Vanilla JS + Leaflet.js + Bootstrap |

---

## Prerequisites

- **Java 17** (or later) — `java -version`
- **Maven 3.9+** — `mvn -version`
- **PostgreSQL 15+** running locally or via Docker
- A terminal / shell

---

## Local Setup

### 1. Clone the repository

```bash
git clone https://github.com/your-username/pridepin.git
cd pridepin
```

### 2. Create the PostgreSQL database

```sql
-- Connect to psql as a superuser, then:
CREATE USER pridepin WITH PASSWORD 'pridepin';
CREATE DATABASE pridepin OWNER pridepin;
GRANT ALL PRIVILEGES ON DATABASE pridepin TO pridepin;
```

Or with Docker (no local Postgres needed):

```bash
docker run -d \
  --name pridepin-db \
  -e POSTGRES_USER=pridepin \
  -e POSTGRES_PASSWORD=pridepin \
  -e POSTGRES_DB=pridepin \
  -p 5432:5432 \
  postgres:15-alpine
```

### 3. Delete the placeholder properties file

```bash
rm src/main/resources/application.properties
```

The app uses `application.yml` exclusively.

### 4. Configure environment variables (optional)

For local development, the defaults in `application.yml` work out of the box.
For anything beyond a local demo, set the variables described in [Environment Variables](#environment-variables).

---

## Environment Variables

| Variable         | Default (dev only)                          | Description                          |
|------------------|---------------------------------------------|--------------------------------------|
| `DB_URL`         | `jdbc:postgresql://localhost:5432/pridepin` | JDBC connection URL                  |
| `DB_USERNAME`    | `pridepin`                                  | Database username                    |
| `DB_PASSWORD`    | `pridepin`                                  | Database password                    |
| `JWT_SECRET`     | see `application.yml`                       | Base64-encoded HMAC secret (≥256 bit)|
| `JWT_EXPIRATION` | `86400000` (24 h)                           | Token TTL in milliseconds            |

> **Security warning:** The default JWT secret is for local development only.
> Generate a proper secret for any shared or public environment:
> ```bash
> openssl rand -base64 64
> ```

---

## Running the Application

```bash
# Compile and run
mvn spring-boot:run

# Or build a fat jar first
mvn clean package -DskipTests
java -jar target/pridepin-0.0.1-SNAPSHOT.jar
```

Once started:

| URL                              | Description          |
|----------------------------------|----------------------|
| `http://localhost:8080/`         | Interactive map UI   |
| `http://localhost:8080/swagger-ui.html` | Swagger UI   |
| `http://localhost:8080/v3/api-docs`     | Raw OpenAPI JSON |

---

## API Documentation

See [docs/API.md](docs/API.md) for the full endpoint reference.
Interactive docs are also available at `/swagger-ui.html` while the app is running.

---

## Deployment

### Environment checklist before going live

1. Set `JWT_SECRET` to a cryptographically random base64 value (≥64 bytes).
2. Set `DB_PASSWORD` to a strong, unique password.
3. Set `spring.jpa.hibernate.ddl-auto` to `validate` (never `update` or `create` in production).
4. Put the app behind a TLS-terminating reverse proxy (nginx, Caddy, or your cloud provider's load balancer).
5. Read [docs/SECURITY.md](docs/SECURITY.md) before exposing this to the internet.

### Railway (recommended for portfolios)

```bash
# Install Railway CLI
npm i -g @railway/cli
railway login
railway init
railway add postgresql   # provisions a free Postgres instance
railway up
```

Set `JWT_SECRET` in the Railway dashboard under Variables.

### Render

1. Connect your GitHub repo at https://render.com/new
2. Runtime: **Java**, build command: `mvn clean package -DskipTests`
3. Start command: `java -jar target/pridepin-0.0.1-SNAPSHOT.jar`
4. Add a **Render PostgreSQL** service and link via `DATABASE_URL`
5. Set remaining env vars in the Render dashboard

### Docker

```dockerfile
# Build
mvn clean package -DskipTests
docker build -t pridepin .

# Run
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/pridepin \
  -e DB_USERNAME=pridepin \
  -e DB_PASSWORD=pridepin \
  -e JWT_SECRET=<your-secret> \
  pridepin
```

A minimal `Dockerfile`:

```dockerfile
FROM eclipse-temurin:17-jre-alpine
COPY target/pridepin-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

---

## Project Structure

```
pridepin/
├── docs/
│   ├── API.md
│   ├── ARCHITECTURE.md
│   └── SECURITY.md
├── src/
│   ├── main/
│   │   ├── java/com/pridepin/pridepin/
│   │   │   ├── config/          # Security + OpenAPI config
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── dto/
│   │   │   │   ├── request/     # Inbound DTOs
│   │   │   │   └── response/    # Outbound DTOs
│   │   │   ├── entity/          # JPA entities
│   │   │   ├── enums/           # Role, LocationCategory
│   │   │   ├── exception/       # Custom exceptions + global handler
│   │   │   ├── repository/      # Spring Data JPA repositories
│   │   │   ├── security/        # JWT util + filter + UserDetailsService
│   │   │   └── service/         # Business logic
│   │   └── resources/
│   │       ├── application.yml
│   │       └── static/
│   │           └── index.html   # Single-file frontend
│   └── test/
├── CHANGELOG.md
├── README.md
└── pom.xml
```

---

## Contributing

This is a portfolio project. Pull requests are welcome. Please open an issue first to discuss significant changes.

## License

MIT
