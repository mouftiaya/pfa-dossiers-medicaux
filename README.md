# Diabetes clinic – starter (Spring microservices + Angular)

First working slice: **login → patient list**, going through the whole chain.

```
Angular (4200) ──> API Gateway (8080) ──> auth-service    (8081) ──> auth database
                        │                  patient-service (8082) ──> patient database
                        └── Eureka (8761): services register here and are found by name
```

| Folder | What it is | Port |
|---|---|---|
| `backend/eureka-server` | Service registry | 8761 |
| `backend/api-gateway` | Single entry point, routes `/api/auth/**` and `/api/patients/**`, CORS for Angular | 8080 |
| `backend/auth-service` | Login, issues a JWT (HS256) with the user's role | 8081 |
| `backend/patient-service` | Patient CRUD, checks the JWT and the role | 8082 |
| `backend/consultation-service` | Visit history per patient (date, reason, notes, prescribed analyses, diagnosis) | 8083 |
| `frontend` | Angular 19 app (login, patient list/search/create/delete, patient page with visit history) | 4200 |
| `docker-compose.yml` | Optional PostgreSQL with `authdb` and `patientdb` | 5432 |

## Prerequisites
JDK 17 or 21, Maven (or IntelliJ's bundled one), Node.js LTS (18.19+, 20.11+ or 22+). Docker is optional (only for PostgreSQL).

## Run it

**1. Database**
Nothing to install: by default each service uses an embedded H2 database, stored in a `data/` folder next to where you start the service. To reset the data, stop the service and delete that folder.

*Optional, later:* to use PostgreSQL instead (via `docker compose up -d`, or a local install with databases `authdb` and `patientdb`), set the environment variables `DB_URL`, `DB_USER` and `DB_PASSWORD` before starting the services, e.g. `DB_URL=jdbc:postgresql://localhost:5432/authdb` for auth-service.

**2. Backend** – start in this order, wait for each to finish starting.
In IntelliJ: open `backend/pom.xml` as a project, then Run each `*Application` class.
Or from a terminal, in four terminals:
```
cd backend/eureka-server   && mvn spring-boot:run
cd backend/auth-service    && mvn spring-boot:run
cd backend/patient-service && mvn spring-boot:run
cd backend/consultation-service && mvn spring-boot:run
cd backend/api-gateway     && mvn spring-boot:run
```
Check http://localhost:8761: `AUTH-SERVICE`, `PATIENT-SERVICE`, `CONSULTATION-SERVICE` and `API-GATEWAY` must be listed.
If you add the new module in IntelliJ and it is not recognised, reload the Maven project (Maven tab > reload icon).

**3. Frontend**
```
cd frontend
npm install
npm start
```
Open http://localhost:4200.

## Demo accounts (created on first start, DEV ONLY)
| Username | Password | Role | Can |
|---|---|---|---|
| `doctor` | `doctor123` | DOCTOR | read, create, update, delete patients; record and delete consultations |
| `assistant` | `assistant123` | ASSISTANT | read, create, update patients; read consultations (cannot record them) |
| `admin` | `admin123` | ADMIN | no access to patient data (reserved for user management later) |

Three fictional patients are also created on first start.

## Quick test without the frontend
```
curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"username":"doctor","password":"doctor123"}'
curl http://localhost:8080/api/patients -H "Authorization: Bearer <token from above>"
```

## Things to know
- `ddl-auto: update` creates the tables for you: fine for a PFA in development, not for production.
- The JWT secret has a default value in both services' `application.yml`. Set the `JWT_SECRET` environment variable (same value for both) outside development.
- Port already in use? Change `server.port` in the service's `application.yml` (and the gateway route / `frontend/src/app/config.ts` if it is the gateway).
- Adding the next service (consultation, lab results, AI): copy `patient-service`, change name, port, database and package, add a route in the gateway's `application.yml`, add the database name in `docker/init-db.sql` (for an existing volume: `docker compose down -v` first, or create it manually).
