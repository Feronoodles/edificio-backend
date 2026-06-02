# Edificio App

API REST para la administracion de un edificio usando Java 17, Spring Boot, PostgreSQL, Spring Security, Swagger/OpenAPI, Lombok y Flyway.

## Requisitos

- Java 17
- Maven 3.9+
- PostgreSQL

## Base de datos

Crea una base de datos local:

```sql
CREATE DATABASE edificio_app;
```

O levanta PostgreSQL con Docker:

```bash
docker compose up -d
```

La configuracion por defecto espera:

```text
DB_URL=jdbc:postgresql://localhost:5432/edificio_app
DB_USERNAME=postgres
DB_PASSWORD=change-me
```

Puedes cambiar esos valores con variables de entorno.

Para desarrollo o VPS puedes usar un archivo `.env` basado en `.env.example`. No subas `.env` al repositorio.

Ejemplo para crear tu `.env`:

```bash
cp .env.example .env
```

En Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

## Ejecutar

```bash
mvn spring-boot:run
```

Swagger queda disponible en:

```text
http://localhost:8080/swagger-ui.html
```

Usuario inicial para probar la API:

```text
usuario: admin
password: el valor de APP_ADMIN_PASSWORD
```

Ese usuario ya no vive en memoria: se crea automaticamente en la tabla `app_users` cuando la aplicacion arranca y no encuentra un usuario con ese username. Puedes cambiarlo con:

```text
APP_ADMIN_USERNAME=admin
APP_ADMIN_PASSWORD=change-me
APP_ADMIN_EMAIL=admin@edificio.local
```

## Login con JWT

Primero solicita un token:

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "TU_APP_ADMIN_PASSWORD"
}
```

La respuesta contiene un `accessToken` y un `refreshToken`. Para consumir endpoints protegidos usa:

```text
Authorization: Bearer TU_ACCESS_TOKEN
```

En Swagger presiona **Authorize** y pega el token en formato:

```text
Bearer TU_ACCESS_TOKEN
```

Cuando expire el access token, solicita otro usando el refresh token:

```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "TU_REFRESH_TOKEN"
}
```

La respuesta devuelve un nuevo `accessToken` y un nuevo `refreshToken`.

Para cerrar sesion y revocar el refresh token:

```http
POST /api/auth/logout
Content-Type: application/json

{
  "refreshToken": "TU_REFRESH_TOKEN"
}
```

La rotacion de refresh token es estricta: cada uso de `/api/auth/refresh` revoca el refresh token anterior y entrega uno nuevo. Si se intenta reutilizar un refresh token ya revocado, la aplicacion revoca las sesiones activas de ese usuario.

La expiracion por defecto es:

```text
access token: 120 minutos
refresh token: 7 dias
```

Puedes cambiarla con:

```text
APP_JWT_EXPIRATION_MINUTES=120
APP_REFRESH_TOKEN_EXPIRATION_DAYS=7
APP_LOGIN_RATE_LIMIT_MAX_ATTEMPTS=5
APP_LOGIN_RATE_LIMIT_WINDOW_MINUTES=1
```

## Auditoria y borrado logico

Departamentos, residentes y pagos guardan auditoria basica:

```text
created_by, created_at, updated_by, updated_at
```

Tambien usan borrado logico:

```text
deleted, deleted_by, deleted_at
```

Los endpoints `DELETE` de departamentos, residentes y pagos no eliminan fisicamente el registro; lo marcan como eliminado y las consultas normales ya no lo devuelven.

Para generar un `APP_JWT_SECRET` fuerte puedes usar:

```bash
openssl rand -base64 64
```

En PowerShell:

```powershell
[Convert]::ToBase64String((1..64 | ForEach-Object { Get-Random -Maximum 256 }))
```

## Endpoints iniciales

- `GET /api/buildings`
- `GET /api/apartments`
- `GET /api/residents`
- `GET /api/payments`

Todos los endpoints de negocio requieren JWT.

## Frontend

La interfaz web esta en `frontend/` y usa React + Vite. En una terminal deja corriendo el backend:

```bash
mvn spring-boot:run
```

En otra terminal instala dependencias y levanta el front:

```bash
cd frontend
npm install
npm run dev
```

Vite abre por defecto en:

```text
http://localhost:5173
```

El frontend usa proxy para enviar `/api/**` al backend en `http://localhost:8080`, asi que no necesitas configurar CORS durante desarrollo.
