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
| `backend/results-service` | Lab results: upload + extraction, review by the doctor, flags (normal / low / high / critical / invalid), patient portal endpoints | 8084 |
| `backend/ai-service` | Rule-based interpretation of validated results (doctor only) | 8085 |
| `backend/glucose-monitoring-service` | **(v2)** Glucose measurements entered by the patient, targets set by the doctor, statistics | 8086 |
| `backend/notification-service` | **(v2)** Alert detection, alert follow-up by the assistant, non-medical messages to the patient | 8087 |
| `frontend` | Angular 19 app (login, patient list/search/create/delete, patient page with visit history) | 4200 |
| `docker-compose.yml` | Optional PostgreSQL with one database per service | 5432 |

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

## Version 2 (new cahier des charges)

What was added on top of the first version:

- **Patient portal** (role `PATIENT`): own record, own validated lab results (+ original file, evolution graphs), own glucose measurements (mg/dL) with day / week / month statistics and the doctor's targets.
- **Patient accounts**: the *assistant* creates the account from the patient file and gives the patient a one-time activation link (valid 72 h; `/activate?token=...`). The patient chooses his password. The administrator cannot create patient accounts.
- **Glucose Monitoring Service** and **Notification Service** (see the table above). Alerts: high / low / critical glucose, unusual variation, persistent high or low values, no measurement for N days, critical lab value. The assistant follows the alerts up (contacted, follow-up organised, consultation proposed, note, resolved). The doctor sees alert type / severity / status but **not the individual values**; the doctor's glucose view is limited to a summary.
- **Patient notifications** (e-mail / SMS): the text never contains medical information. By default messages are only written to the log (`LoggingEmailSender`, `LoggingSmsSender`); set `notifications.email.enabled=true` and the `spring.mail.*` properties for real e-mails, and plug an SMS provider in `SmsSender`.
- **Results**: new flags `CRITICAL_LOW`, `CRITICAL_HIGH` and `INVALID` (impossible value: the result cannot be validated until corrected), cardiology parameters (HDL, potassium, sodium, urea, troponin, NT-proBNP), and an alert to the assistant when a validated result contains a critical value.
- **External Config Server**: every service (except Eureka) can read its configuration from a Config Server on `CONFIG_SERVER_URL` (default `http://localhost:8888`); it is optional, the services start without it. See `docs/config-server.md`.

### Start everything at once (Windows)
Double-click `start-all.bat` (backend + Angular app) and `stop-all.bat` to stop it all, or from PowerShell: `.\start-all.ps1 -WithFrontend`. The script starts each service in its own window, in the right order, and waits for each port before starting the next one. Needs Maven (`mvn`) in the PATH; the first start takes a few minutes.

### Start order
`eureka-server`, (Config Server, if you use it), `auth-service`, `patient-service`, `consultation-service`, `results-service`, `ai-service`, `glucose-monitoring-service`, `notification-service`, `api-gateway`, then the frontend.

### Upgrading an existing database (IMPORTANT)
Hibernate creates a check constraint for enum columns and `ddl-auto: update` does not change it, so with an H2 / PostgreSQL database created by the first version:
- **auth-service**: the new role `PATIENT` is refused by the old constraint;
- **results-service**: the new flags are refused.
In development, stop the services and delete the `data/` folder of `auth-service` and `results-service` (demo accounts and catalogue are recreated). With PostgreSQL, drop and recreate `authdb` and `resultsdb`, or alter the constraint. The new columns of the patient file (e-mail) are added automatically.

### Alerts based on the trend, not on one measurement
A single glucose value (too high or too low) never raises an alert. The ai-service reads, for each patient, the
aggregated history of the last 6 weeks from glucose-monitoring-service (every night at 02:00, or with the button
"Lancer l'analyse IA" on the Alertes page) and only reports a **persistent** problem: enough data (28 days with
measurements, 4 judged weeks) and glucose above the target (or below the low threshold) for at least 75 % of the weeks.
The notification-service then creates one alert marked "IA x/100" for the assistant (no message is sent to the patient
by itself). On the alert page the assistant can call the patient or edit and send the suggested e-mail
(`GET /api/alerts/{id}/email-draft`, `POST /api/alerts/{id}/send-email`). Thresholds are in `ai-service/.../application.yml`
(`ai.glucose-risk.*`). The analysis is rule based (not a trained model): it is an indicator, the doctor decides.

To try it without waiting weeks: log in as a patient, then `POST /api/glucose/me/demo-history?profile=HIGH&days=60`
(profiles STABLE, HIGH, LOW; replaces the last days of that patient with generated values; disable it with
`GLUCOSE_DEMO_ENABLED=false`), then click "Lancer l'analyse IA" as the assistant. The old behaviour (instant alerts)
can be switched back on with `INSTANT_GLUCOSE_ALERTS=true` on the notification-service. Databases are unchanged
(embedded H2); no new table, only a nullable column `ai_score` in `alerts` (added automatically).

### Variables to set outside development
`JWT_SECRET` (same value everywhere), `INTERNAL_API_KEY` (same value in auth, patient, results, glucose and notification services; protects the `/internal/**` endpoints, which the gateway never exposes), `FRONTEND_URL` (auth-service, used to build the activation links), `EUREKA_URL`, `CONFIG_SERVER_URL`.

### Medical values to confirm
The reference ranges, valid ranges and critical limits (results-service `DataInitializer`), the glucose defaults (glucose-monitoring-service, mg/dL) and the AI thresholds are common adult values used as a starting point. The doctor of the cabinet must confirm them before any real use.
