# Deploy DripPay on Render

## What failed if you see `UnknownHostException: YOUR_MYSQL_HOST`

You copied the **example** `DB_URL` without replacing the host. Render cannot resolve `YOUR_MYSQL_HOST` as a real server.

**Fix (choose one):**

### A — Set a real `DB_URL`

1. Deploy MySQL (private service) — see below.
2. MySQL service → **Connect** → internal hostname (e.g. `mysql-a1b2c3`).
3. API service → **Environment** → edit `DB_URL`:

```text
jdbc:mysql://mysql-a1b2c3:3306/drippay?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
```

4. `DB_USERNAME` / `DB_PASSWORD` = same as `MYSQL_USER` / `MYSQL_PASSWORD` on the MySQL service.
5. **Save** → **Manual Deploy**.

### B — Use `MYSQL_HOST` (no `DB_URL`)

Delete `DB_URL` on the API service and set:

| Key | Example |
|-----|---------|
| `MYSQL_HOST` | `mysql-a1b2c3` |
| `MYSQL_DATABASE` | `drippay` |
| `MYSQL_PORT` | `3306` |
| `DB_USERNAME` | `drippay` |
| `DB_PASSWORD` | (from MySQL service) |

The app builds the JDBC URL automatically.

---

## 1. MySQL on Render (required)

DripPay uses **MySQL**, not Render Postgres.

1. Follow [Deploy MySQL on Render](https://render.com/docs/deploy-mysql) (fork `render-examples/mysql`, **Private Service**, Docker, disk at `/var/lib/mysql`).
2. Env on MySQL service: `MYSQL_DATABASE=drippay`, `MYSQL_USER`, `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD`.
3. **Same region** as the API (e.g. Oregon).
4. Wait until MySQL is **Live**.

---

## 2. API web service (Docker)

- Repo: `kanika2005/DripPay`, branch `main`
- Runtime: **Docker**, root `Dockerfile` at repo root
- Env: `JWT_SECRET`, `CORS_ORIGINS`, database vars above
- Health: `/actuator/health`

---

## 3. Frontend (Static Site)

- Root: `frontend`
- Build: `npm install && npm run build`
- Publish: `dist`
- Env: `VITE_API_URL=https://YOUR-API.onrender.com/api/v1`

---

## Checklist

- [ ] MySQL private service **Live**
- [ ] `DB_URL` has real hostname **or** `MYSQL_HOST` set (not `YOUR_MYSQL_HOST`)
- [ ] API and MySQL in **same region**
- [ ] `JWT_SECRET` set
- [ ] `CORS_ORIGINS` includes frontend URL
