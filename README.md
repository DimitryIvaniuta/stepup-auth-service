# Step-Up Authentication Service (Risk-Based)

Implements **risk-based step-up authentication**:

- Safe actions stay smooth (no extra prompts)
- Risky actions require OTP/2FA (step-up)
- Decision is stored (audit) and monitoring event is published to Kafka
- Kafka publishing is crash-safe via **transactional outbox**

## Repository
Suggested GitHub repo:
- Name: `stepup-auth-service`
- Description: `Java/Spring Boot risk-based step-up authentication (OTP) with Postgres/Flyway, Redis, Kafka KRaft, outbox, tests, Postman`

## Run locally

```bash
docker compose up -d
gradle bootRun
```

## Postman
Import `postman/stepup-auth.postman_collection.json`

Flow:
1) Register
2) Login -> saves `{{token}}`
3) Authorize safe -> APPROVED
4) Authorize risky -> STEP_UP_REQUIRED (+ dev-only `otpPreview`)
5) Verify OTP

## Tests
Integration tests use Testcontainers (Docker required):
```bash
gradle test
```
