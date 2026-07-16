# KOB Demo

A small Vue + Spring Boot + MyBatis-Plus project inspired by your KOB project.

## Run backend

```bash
cd backend
mvn spring-boot:run
```

Backend URL: `http://localhost:8080`

The backend uses MyBatis-Plus with an H2 in-memory database. Tables are created
from `backend/src/main/resources/schema.sql`, and demo data is loaded from
`backend/src/main/resources/data.sql`.

H2 console:

```text
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:kob_demo
User: sa
Password: empty
```

## Run frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend URL: `http://localhost:5173`

Demo account: `alice / 123456`.

Learning path:

1. Read `backend/src/main/java/com/example/kobdemo/KobDemoApplication.java`.
2. Read `frontend/src/api/http.js`.
3. Trace login from `LoginView.vue` to `/api/auth/login`.
4. Trace bot CRUD from `BotsView.vue` to `/api/bots`, then to `BotService` and `BotMapper`.
5. Trace the demo battle from `PlayView.vue` to `/api/games/play`.
