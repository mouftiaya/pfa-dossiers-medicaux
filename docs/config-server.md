# External Config Server

The Config Server lives in its own repository (maintained by a team member). The services of this project are only **clients**:

- dependency `spring-cloud-starter-config` in every service except `eureka-server`;
- `spring.config.import: optional:configserver:${CONFIG_SERVER_URL:http://localhost:8888}` in each `application.yml`.

`optional:` means a service still starts with its local `application.yml` when the Config Server is down, so nothing blocks development.

## What the Config Server repository should contain

One file per service, named after `spring.application.name` (values there override the local ones):

`api-gateway.yml`, `auth-service.yml`, `patient-service.yml`, `consultation-service.yml`, `results-service.yml`, `ai-service.yml`, `glucose-monitoring-service.yml`, `notification-service.yml`, and an `application.yml` for what is shared.

Good candidates for the shared `application.yml`:

```yaml
jwt:
  secret: <same secret everywhere, at least 32 characters>
internal:
  api-key: <same key everywhere>
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka
```

Good candidates for `notification-service.yml`:

```yaml
alerts:
  dedup-hours: 6
notifications:
  clinic-name: Cabinet de diabétologie
  clinic-phone: "05 00 00 00 00"
  min-interval-minutes: 360
  email:
    enabled: false
```

Do not commit real secrets in a public Git repository: use `{cipher}` values or environment variables.
