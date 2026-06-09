# Guia de produccion

Esta guia resume el camino estandar para preparar, probar y desplegar la app. La idea es que puedas repasarla cada vez que avances un paso nuevo.

## 1. Que estamos preparando

La app tiene tres piezas principales:

- Backend: Spring Boot, API REST, JWT, Flyway y PostgreSQL.
- Base de datos: PostgreSQL.
- Frontend: React + Vite servido por Nginx en el stack local de prueba.

Backend y frontend pueden vivir en repositorios separados. En ese caso, este repo debe considerarse el repo del backend.

Este repo tiene dos compose con objetivos distintos:

- `docker-compose.prod.yml`: laboratorio local, util si tienes una carpeta `frontend/` al lado para probar todo junto.
- `docker-compose.server.yml`: despliegue real del backend/API en VPS Linux. No levanta frontend.

## 2. Archivos importantes

Backend:

```text
pom.xml
src/main/resources/application.yml
src/main/resources/application-dev.yml
src/main/resources/application-prod.yml
Dockerfile
docker-compose.prod.yml
.env.example
.env
```

Frontend local de prueba, solo si existe una carpeta `frontend/` en tu maquina:

```text
frontend/Dockerfile
frontend/nginx.conf
frontend/src/main.jsx
```

Importante:

- `.env` contiene secretos reales locales y no se sube a Git.
- `.env.example` sirve como plantilla segura.
- `application.yml` contiene configuracion comun y activa `dev` por defecto.
- `application-dev.yml` contiene valores comodos para desarrollo local.
- `application-prod.yml` es la configuracion estricta para produccion.
- `docker-compose.prod.yml` levanta PostgreSQL, backend y frontend juntos para probar.

## 3. Perfiles de Spring

Spring Boot puede cargar configuraciones diferentes segun el perfil activo.

En este proyecto usamos:

```text
dev
prod
test
```

`dev`:

- Es el perfil por defecto.
- Se usa cuando corres `mvn spring-boot:run` sin indicar nada mas.
- Puede tener valores locales comodos.
- No debe usarse en servidor real.

`prod`:

- Se usa para Docker y servidor real.
- Exige variables de entorno reales.
- Desactiva Swagger.
- Reduce detalles de healthcheck.

`test`:

- Se usa en pruebas automatizadas.
- Usa H2 en memoria.
- Esta definido en `src/test/resources/application-test.yml`.

Regla importante:

Los secretos reales van en `.env` o variables del servidor, no en archivos versionados.

## 4. Variables de entorno

Para produccion usamos variables de entorno en vez de escribir secretos en el codigo.

Ejemplo:

```text
SPRING_PROFILES_ACTIVE=prod
DB_USERNAME=edificio_user
DB_PASSWORD=usa-un-password-fuerte
APP_ADMIN_USERNAME=admin
APP_ADMIN_PASSWORD=usa-un-password-fuerte
APP_ADMIN_EMAIL=admin@tu-dominio.com
APP_JWT_SECRET=usa-un-secreto-largo-generado
APP_HTTP_PORT=80
```

Para generar un secreto fuerte en PowerShell:

```powershell
[Convert]::ToBase64String((1..64 | ForEach-Object { Get-Random -Maximum 256 }))
```

Ese comando genera texto aleatorio en base64. Sirve para `APP_JWT_SECRET`, que firma los tokens JWT.

## 5. Comandos de prueba antes de Docker

### Probar backend

```powershell
mvn test
```

Para que sirve:

- Compila el backend.
- Ejecuta los tests.
- Verifica que Spring pueda arrancar en ambiente de prueba.
- Verifica que Flyway aplique migraciones en la base de datos de test.

### Probar frontend

```powershell
cd frontend
npm.cmd run build
```

Para que sirve:

- Compila React/Vite para produccion.
- Genera los archivos finales en `frontend/dist`.
- Ayuda a detectar errores antes de construir la imagen Docker.

Nota para Windows:

Si `npm` o `npx` fallan por politicas de PowerShell, usa `npm.cmd` o `npx.cmd`.

## 6. Validar Docker Compose sin levantar servicios

```powershell
docker compose -f docker-compose.prod.yml config
```

Para que sirve:

- Revisa que el archivo `docker-compose.prod.yml` sea valido.
- Lee las variables del `.env`.
- Muestra la configuracion final que Docker va a usar.
- No crea contenedores ni modifica datos.

Es un buen comando para revisar antes de hacer `up`.

## 7. Levantar el stack local de produccion

```powershell
docker compose -f docker-compose.prod.yml up --build -d
```

Para que sirve:

- `docker compose`: usa Docker Compose.
- `-f docker-compose.prod.yml`: indica el archivo de produccion local.
- `up`: crea y levanta los servicios.
- `--build`: reconstruye imagenes si hubo cambios.
- `-d`: deja los contenedores corriendo en segundo plano.

Servicios que levanta:

- `postgres`: base de datos.
- `backend`: API Spring Boot.
- `frontend`: Nginx sirviendo React y enviando `/api` al backend.

Cuando termine, la app deberia estar en:

```text
http://localhost
```

## 8. Ver estado de los contenedores

```powershell
docker compose -f docker-compose.prod.yml ps
```

Para que sirve:

- Muestra si los servicios estan arriba.
- Muestra si PostgreSQL y backend estan `healthy`.
- Muestra puertos publicados, como `0.0.0.0:80->80/tcp`.

Estado esperado:

```text
postgres   healthy
backend    healthy
frontend   Up
```

## 9. Ver logs

Logs del backend:

```powershell
docker compose -f docker-compose.prod.yml logs backend
```

Ultimas lineas del backend:

```powershell
docker compose -f docker-compose.prod.yml logs --tail=80 backend
```

Logs de todos los servicios:

```powershell
docker compose -f docker-compose.prod.yml logs
```

Para que sirve:

- Ver errores de arranque.
- Confirmar que Flyway aplico migraciones.
- Confirmar que Spring Boot arranco.
- Diagnosticar problemas de conexion con PostgreSQL.

## 10. Verificar salud del backend

Dentro del contenedor:

```powershell
docker exec edificio_app-backend-1 curl -fsS http://localhost:8080/actuator/health
```

Respuesta esperada:

```json
{"status":"UP","groups":["liveness","readiness"]}
```

Para que sirve:

- Confirma que el backend esta vivo.
- Confirma que el healthcheck usado por Docker funciona.

Nota:

`http://localhost/actuator/health` desde el navegador puede devolver el frontend, porque Nginx publico solo la app y el proxy `/api`. El healthcheck del backend queda interno.

## 11. Verificar que el proxy API funciona

Sin token:

```powershell
try {
  Invoke-WebRequest -Uri http://localhost/api/buildings -UseBasicParsing
} catch {
  $_.Exception.Response.StatusCode.value__
}
```

Respuesta esperada:

```text
401
```

Para que sirve:

- Confirma que Nginx si llega al backend.
- El `401` es correcto porque `/api/buildings` necesita JWT.

## 12. Probar login y endpoint protegido

```powershell
$envVars = @{}
Get-Content .env | Where-Object { $_ -match '^[^#].+=' } | ForEach-Object {
  $name, $value = $_ -split '=', 2
  $envVars[$name] = $value
}

$body = @{
  username = $envVars['APP_ADMIN_USERNAME']
  password = $envVars['APP_ADMIN_PASSWORD']
} | ConvertTo-Json

$login = Invoke-RestMethod -Uri http://localhost/api/auth/login -Method Post -ContentType 'application/json' -Body $body
$headers = @{ Authorization = "Bearer $($login.accessToken)" }
$buildings = Invoke-RestMethod -Uri http://localhost/api/buildings -Headers $headers

[pscustomobject]@{
  LoginHasAccessToken = [bool]$login.accessToken
  LoginHasRefreshToken = [bool]$login.refreshToken
  BuildingsCount = @($buildings).Count
}
```

Para que sirve:

- Lee usuario y password desde `.env`.
- Hace login.
- Verifica que el backend entregue access token y refresh token.
- Usa el access token para consultar un endpoint protegido.

Respuesta esperada:

```text
LoginHasAccessToken LoginHasRefreshToken BuildingsCount
------------------- -------------------- --------------
True                True                 0
```

`BuildingsCount` puede cambiar si ya tienes edificios registrados.

## 13. Detener el stack

```powershell
docker compose -f docker-compose.prod.yml down
```

Para que sirve:

- Detiene los contenedores.
- Elimina los contenedores y la red creada por Compose.
- No elimina el volumen de PostgreSQL.

Esto significa que los datos quedan guardados.

## 14. Volumen de PostgreSQL

Este compose usa el volumen:

```text
edificio_app_prod_postgres_data
```

Para que sirve:

- Guarda los datos de PostgreSQL aunque apagues los contenedores.
- Evita mezclar esta prueba de produccion local con la base de desarrollo.

Importante:

Las variables `POSTGRES_USER` y `POSTGRES_PASSWORD` solo se aplican la primera vez que se crea el volumen. Si cambias credenciales despues, PostgreSQL no cambia la clave automaticamente.

Ver volumenes relacionados:

```powershell
docker volume ls --format "{{.Name}}" | Select-String "edificio_app"
```

Eliminar el volumen de produccion local:

```powershell
docker compose -f docker-compose.prod.yml down
docker volume rm edificio_app_prod_postgres_data
```

Haz esto solo si estas seguro de que no necesitas los datos.

## 15. Flujo estandar local

Cuando hagamos cambios importantes:

```powershell
mvn test
cd frontend
npm.cmd run build
cd ..
docker compose -f docker-compose.prod.yml config
docker compose -f docker-compose.prod.yml up --build -d
docker compose -f docker-compose.prod.yml ps
```

Luego verificar:

```powershell
docker exec edificio_app-backend-1 curl -fsS http://localhost:8080/actuator/health
```

Y abrir:

```text
http://localhost
```

Tambien puedes ejecutar el smoke test automatizado:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-prod-local.ps1
```

Para que sirve:

- Revisa el estado de los contenedores.
- Verifica el healthcheck interno del backend.
- Verifica que el frontend responda.
- Verifica que `/api` llegue al backend y devuelva `401` sin token.
- Hace login con el admin del `.env`.
- Consulta `/api/buildings` con JWT.

Si quieres probar otro puerto o dominio local:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-prod-local.ps1 -BaseUrl "http://localhost:8081"
```

Rutina completa recomendada en tu maquina local:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\verify-prod-local.ps1
```

Para que sirve:

- Ejecuta `mvn test`.
- Ejecuta `npm.cmd run build` en `frontend/` solo si esa carpeta existe.
- Valida `docker-compose.prod.yml`.
- Reconstruye y levanta el stack con Docker.
- Ejecuta el smoke test automatizado.

Si quieres saltar el build del frontend:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\verify-prod-local.ps1 -SkipFrontendBuild
```

## 16. Antes de subir a un servidor real

Checklist inicial:

- No subir `.env`.
- Usar secretos nuevos para el servidor.
- Confirmar que `SPRING_PROFILES_ACTIVE=prod`.
- Confirmar que Swagger esta deshabilitado en produccion.
- Confirmar que PostgreSQL no queda expuesto publicamente.
- Confirmar que el dominio usa HTTPS.
- Confirmar que hay backups de PostgreSQL.
- Confirmar que el usuario de base de datos no sea `postgres`.

## 17. Que falta por hacer

Siguientes mejoras recomendadas:

- Agregar configuracion HTTPS con Caddy o Nginx en servidor.
- Crear script de deploy.
- Crear backup automatico de PostgreSQL.
- Separar compose local y compose de servidor si el frontend vive en otro repo.
- Agregar mas tests de endpoints criticos.
- Revisar CORS si frontend y backend quedan en dominios distintos.

## 18. Backups de PostgreSQL

Antes de subir una app a produccion real, necesitamos saber como respaldar la base de datos.

Crear un backup manual en Windows:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\backup-postgres.ps1
```

Crear un backup manual en Linux:

```bash
bash ./scripts/backup-postgres.sh
```

Para que sirve:

- Lee `DB_USERNAME` y `DB_PASSWORD` desde `.env`.
- Entra al contenedor PostgreSQL.
- Ejecuta `pg_dump`.
- Guarda un archivo `.sql` en `backups/`.

La carpeta `backups/` esta ignorada por Git porque puede contener datos reales.

Ejemplo de archivo generado:

```text
backups/edificio_app-20260608-101530.sql
```

Regla importante:

Un backup no esta realmente probado hasta que hiciste una restauracion en una base de prueba. Mas adelante agregaremos una rutina segura para restaurar sin poner en riesgo la base principal.

Restaurar el ultimo backup en una base de prueba en Windows:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\restore-check-postgres.ps1
```

Restaurar el ultimo backup en una base de prueba en Linux:

```bash
bash ./scripts/restore-check-postgres.sh
```

Para que sirve:

- Busca el backup `.sql` mas reciente en `backups/`.
- Crea una base temporal llamada `edificio_app_restore_check`.
- Restaura el backup ahi.
- Verifica cuantas tablas quedaron restauradas.
- No toca la base principal `edificio_app`.

Restaurar un backup especifico en Windows:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\restore-check-postgres.ps1 -BackupPath ".\backups\edificio_app-20260607-232516.sql"
```

Restaurar un backup especifico en Linux:

```bash
bash ./scripts/restore-check-postgres.sh ./backups/edificio_app-20260607-232516.sql
```

Si la restauracion de prueba funciona, el backup tiene mucho mas valor: ya no solo existe el archivo, tambien sabemos que PostgreSQL puede leerlo y reconstruir la base.

## 19. Windows local vs Linux servidor

Los scripts `.ps1` son para tu maquina Windows:

```text
scripts/backup-postgres.ps1
scripts/restore-check-postgres.ps1
scripts/smoke-prod-local.ps1
scripts/verify-prod-local.ps1
```

Los scripts `.sh` son para Linux:

```text
scripts/backup-postgres.sh
scripts/restore-check-postgres.sh
scripts/smoke-prod-local.sh
scripts/verify-prod-local.sh
```

La idea es que el flujo sea el mismo en ambos sistemas:

```text
probar -> levantar -> smoke test -> backup -> restore check
```

En Windows usamos PowerShell. En Linux usamos Bash.

El concepto importante no cambia: Docker Compose mantiene datos en volumenes nombrados. Segun la documentacion de Docker Compose, cuando `docker compose up` recrea contenedores, preserva los volumenes montados. Por eso PostgreSQL conserva datos aunque se recree el contenedor.

En servidor real:

- La app se reconstruye desde imagen/codigo.
- PostgreSQL guarda datos en un volumen Docker.
- El backup se saca con `pg_dump` desde el contenedor PostgreSQL.
- La restauracion de prueba se hace en otra base, no sobre la principal.

## 20. Preparacion de servidor Linux

Para servidor real puedes usar una de estas variantes:

```text
docker-compose.server-nginx.yml  si el VPS ya tiene Nginx instalado
docker-compose.server.yml        si quieres usar Caddy como proxy del proyecto
.env.server.example
```

Como tu VPS ya tiene Nginx para otros proyectos, usa:

```text
docker-compose.server-nginx.yml
```

No uses Caddy en ese VPS, porque Caddy intentaria ocupar puertos `80` y `443`, que ya maneja Nginx.

La diferencia con `docker-compose.prod.yml` es esta:

- `docker-compose.prod.yml`: laboratorio local full-stack en tu maquina.
- `docker-compose.server-nginx.yml`: despliegue backend/API en un VPS Linux que ya tiene Nginx.
- `docker-compose.server.yml`: alternativa si el proyecto usara Caddy propio.

Servicios del compose con Nginx externo:

```text
backend   API Spring Boot con perfil prod
postgres  base de datos PostgreSQL privada dentro de Docker
```

No incluye frontend porque el frontend vive en otro repositorio y se despliega por separado.

El backend publica solo en localhost del VPS:

```text
127.0.0.1:18080 -> backend:8080
```

Eso significa que internet no entra directo al backend. Nginx recibe el trafico HTTPS y lo reenvia a `127.0.0.1:18080`.

PostgreSQL no publica el puerto `5432`, porque no debe quedar abierto a internet.

### Variables del servidor

En el servidor se crea un `.env` basado en:

```text
.env.server.example
```

Variables principales:

```text
APP_DOMAIN=api.tu-dominio.com
APP_CORS_ALLOWED_ORIGINS=https://tu-frontend.com
BACKEND_HOST_PORT=18080
DB_USERNAME=edificio_user
DB_PASSWORD=...
APP_ADMIN_USERNAME=admin
APP_ADMIN_PASSWORD=...
APP_ADMIN_EMAIL=admin@tu-dominio.com
APP_JWT_SECRET=...
SPRING_PROFILES_ACTIVE=prod
```

`APP_DOMAIN` es el dominio donde vivira el API.

Ejemplo:

```text
APP_DOMAIN=api.midominio.com
```

`APP_CORS_ALLOWED_ORIGINS` es el dominio desde donde el frontend podra llamar al API.

Ejemplo:

```text
APP_CORS_ALLOWED_ORIGINS=https://midominio.com
```

Si frontend y backend viven en dominios distintos, CORS importa. Si no configuras CORS, el navegador puede bloquear llamadas aunque el backend este funcionando.

### Validar compose de servidor

En Windows, solo para revisar configuracion:

```powershell
docker compose --env-file .env.server.example -f docker-compose.server-nginx.yml config
```

En Linux:

```bash
docker compose --env-file .env -f docker-compose.server-nginx.yml config
```

### Levantar en servidor Linux

En el VPS:

```bash
docker compose --env-file .env -f docker-compose.server-nginx.yml up --build -d
```

No uses `docker-compose.prod.yml` en el VPS si este repo no contiene frontend.

Ver estado:

```bash
docker compose --env-file .env -f docker-compose.server-nginx.yml ps
```

Ver logs del backend:

```bash
docker compose --env-file .env -f docker-compose.server-nginx.yml logs backend
```

### Configurar Nginx del VPS

Plantilla:

```text
server/nginx-edificio-api.conf.example
```

Ejemplo Nginx:

```nginx
server {
    server_name api.tu-dominio.com;

    location / {
        proxy_pass http://127.0.0.1:18080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Eso significa:

- Nginx escucha `api.tu-dominio.com`.
- Reenvia al backend publicado en `127.0.0.1:18080`.
- Mantiene headers importantes para que el backend sepa host, IP real y protocolo.

Luego normalmente activas HTTPS con Certbot:

```bash
sudo certbot --nginx -d api.tu-dominio.com
```

Para que HTTPS funcione, el dominio debe apuntar al servidor y Nginx debe tener abiertos los puertos 80 y 443.

### Smoke test en Linux

Cuando el dominio ya apunte al servidor:

```bash
BASE_URL=https://api.tu-dominio.com bash ./scripts/smoke-prod-local.sh
```

Aunque el script diga `local`, tambien sirve para servidor si cambias `BASE_URL`.

### Backups en servidor

Backup:

```bash
bash ./scripts/backup-postgres.sh
```

Restauracion de prueba:

```bash
bash ./scripts/restore-check-postgres.sh
```

Esto respalda/restaura la base PostgreSQL del contenedor Docker del servidor.

Los scripts usan el servicio `postgres` de Docker Compose, no un nombre fijo de contenedor. Esto evita errores cuando Docker genera nombres distintos segun la carpeta del proyecto.

### Automatizar backups diarios con cron

En el VPS puedes instalar un backup diario:

```bash
bash ./scripts/install-backup-cron.sh
```

Por defecto:

```text
hora: 02:30
retencion: 14 dias
log: logs/backup-postgres.log
```

Puedes cambiar hora y retencion:

```bash
BACKUP_TIME=03:15 BACKUP_RETENTION_DAYS=30 bash ./scripts/install-backup-cron.sh
```

Ver tareas cron instaladas:

```bash
crontab -l
```

Ver logs del backup:

```bash
tail -n 80 logs/backup-postgres.log
```

Ejecutar backup manual con retencion:

```bash
BACKUP_RETENTION_DAYS=30 bash ./scripts/backup-postgres.sh
```

Importante:

El cron guarda backups dentro del VPS. Eso protege contra errores de app o base, pero no contra perdida total del servidor. Mas adelante conviene copiar backups fuera del VPS, por ejemplo a otro servidor, almacenamiento S3 compatible, Google Drive, o descarga periodica local.

## 21. Despliegue del frontend

El frontend vive en otro repositorio:

```text
https://github.com/Feronoodles/edificio-frontend
```

La estrategia recomendada es:

```text
PC local: compila React/Vite y genera dist/
VPS: Nginx sirve los archivos estaticos de dist/
```

Asi el VPS no necesita Node, npm ni pnpm para servir la app.

### Dominios usados

Con subdominios separados:

```text
Frontend: https://app.tu-dominio.com
Backend:  https://api.tu-dominio.com
```

En el backend, el `.env` del VPS debe tener:

```text
APP_DOMAIN=api.tu-dominio.com
APP_CORS_ALLOWED_ORIGINS=https://app.tu-dominio.com
BACKEND_HOST_PORT=18080
```

En el frontend, al compilar, debes usar:

```text
VITE_API_BASE_URL=https://api.tu-dominio.com
```

No agregues `/api` al final. El codigo del frontend ya agrega rutas como `/api/auth/login`.

Correcto:

```text
VITE_API_BASE_URL=https://api.tu-dominio.com
```

Incorrecto:

```text
VITE_API_BASE_URL=https://api.tu-dominio.com/api
```

### Actualizar el repo frontend local

En tu PC local:

```powershell
cd C:\Users\quena\Desktop\proyectos\java\edificio-frontend
git pull
```

Si Git dice que hay cambios locales que bloquearian el pull:

```powershell
git stash push -m "backup local frontend antes de pull" -- src/main.jsx src/styles.css
git pull
```

Verificar commit esperado:

```powershell
git log -1 --oneline
```

Debe estar en un commit que incluya el uso de `VITE_API_BASE_URL`.

Verificar que el codigo usa la variable:

```powershell
Select-String -Path .\src\main.jsx -Pattern "VITE_API_BASE_URL|API_BASE_URL|import.meta.env"
```

### Compilar frontend en Windows

En PowerShell:

```powershell
cd C:\Users\quena\Desktop\proyectos\java\edificio-frontend
Remove-Item -Recurse -Force .\dist -ErrorAction SilentlyContinue
$env:VITE_API_BASE_URL="https://api.tu-dominio.com"
npm.cmd ci
npm.cmd audit --audit-level=moderate
npm.cmd run build
```

Si ya tienes dependencias instaladas y solo estas recompilando:

```powershell
$env:VITE_API_BASE_URL="https://api.tu-dominio.com"
npm.cmd run build
```

Verificar que la URL del API quedo dentro del build:

```powershell
Select-String -Path .\dist\assets\*.js -Pattern "https://api.tu-dominio.com"
```

Si no aparece nada, el build no tomo `VITE_API_BASE_URL`. Repite el build en la misma terminal donde configuraste la variable.

### Subir dist al VPS

Desde tu PC local:

```powershell
scp -r .\dist\* usuario@IP_DEL_VPS:/tmp/edificio-frontend-dist/
```

En el VPS:

```bash
sudo mkdir -p /var/www/edificio-frontend
sudo rsync -av --delete /tmp/edificio-frontend-dist/ /var/www/edificio-frontend/
rm -rf /tmp/edificio-frontend-dist
sudo chown -R www-data:www-data /var/www/edificio-frontend
sudo find /var/www/edificio-frontend -type d -exec chmod 755 {} \;
sudo find /var/www/edificio-frontend -type f -exec chmod 644 {} \;
sudo nginx -t
sudo systemctl reload nginx
```

La carpeta debe quedar asi:

```text
/var/www/edificio-frontend/index.html
/var/www/edificio-frontend/assets/
```

No debe quedar asi:

```text
/var/www/edificio-frontend/dist/index.html
```

### Configurar Nginx para el frontend

Crear archivo:

```bash
sudo nano /etc/nginx/sites-available/edificio-frontend
```

Contenido:

```nginx
server {
    server_name app.tu-dominio.com;

    root /var/www/edificio-frontend;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

Activar sitio:

```bash
sudo ln -s /etc/nginx/sites-available/edificio-frontend /etc/nginx/sites-enabled/edificio-frontend
sudo nginx -t
sudo systemctl reload nginx
```

Activar HTTPS con Certbot:

```bash
sudo certbot --nginx -d app.tu-dominio.com
```

Probar:

```bash
curl -I https://app.tu-dominio.com
```

Respuesta esperada:

```text
HTTP/2 200
```

### Verificar login desde navegador

Abrir:

```text
https://app.tu-dominio.com
```

En DevTools > Network, el login debe llamar a:

```text
https://api.tu-dominio.com/api/auth/login
```

No debe llamar a:

```text
https://app.tu-dominio.com/api/auth/login
```

Si llama al dominio del frontend, reconstruye el frontend con `VITE_API_BASE_URL` correcto y vuelve a subir `dist`.

## 22. Errores comunes y diagnostico

### Error 403 al cargar frontend

Normalmente es Nginx sirviendo archivos estaticos.

Revisar:

```bash
ls -la /var/www/edificio-frontend
sudo tail -n 80 /var/log/nginx/error.log
```

Debe existir:

```text
index.html
assets/
```

Corregir permisos:

```bash
sudo chown -R www-data:www-data /var/www/edificio-frontend
sudo find /var/www/edificio-frontend -type d -exec chmod 755 {} \;
sudo find /var/www/edificio-frontend -type f -exec chmod 644 {} \;
sudo nginx -t
sudo systemctl reload nginx
```

### Error 405 al hacer login

Normalmente significa que el frontend esta llamando al dominio equivocado:

```text
https://app.tu-dominio.com/api/auth/login
```

Eso esta mal si el backend vive en:

```text
https://api.tu-dominio.com
```

Solucion:

```powershell
$env:VITE_API_BASE_URL="https://api.tu-dominio.com"
npm.cmd run build
```

Luego subir otra vez `dist` al VPS.

### Error CORS en navegador

Revisar `.env` del backend en el VPS:

```bash
cat .env
```

Debe tener:

```text
APP_CORS_ALLOWED_ORIGINS=https://app.tu-dominio.com
```

Sin `/` al final.

Luego reiniciar backend:

```bash
docker compose --env-file .env -f docker-compose.server-nginx.yml up --build -d
```

### Backend responde por curl pero frontend falla

Probar API directamente:

```bash
curl -i https://api.tu-dominio.com/actuator/health
curl -i https://api.tu-dominio.com/api/buildings
```

Esperado:

```text
/actuator/health -> 200
/api/buildings   -> 401 sin token
```

Si eso funciona, el backend esta bien. El problema suele estar en build del frontend, CORS o Nginx frontend.

## 23. Checklist final de produccion

Backend:

- `docker-compose.server-nginx.yml` levantado en VPS.
- `postgres` healthy.
- `backend` healthy.
- `https://api.tu-dominio.com/actuator/health` devuelve `200`.
- `https://api.tu-dominio.com/api/buildings` devuelve `401` sin token.
- Login devuelve `accessToken` y `refreshToken`.
- `APP_CORS_ALLOWED_ORIGINS` apunta al frontend.

Frontend:

- Build hecho con `VITE_API_BASE_URL=https://api.tu-dominio.com`.
- `dist` copiado a `/var/www/edificio-frontend`.
- Nginx frontend apunta a `/var/www/edificio-frontend`.
- `https://app.tu-dominio.com` devuelve `200`.
- Login desde navegador llama a `https://api.tu-dominio.com/api/auth/login`.

Backups:

- `bash ./scripts/backup-postgres.sh` genera backup.
- `bash ./scripts/restore-check-postgres.sh` restaura en base de prueba.
- Cron de backups instalado con `bash ./scripts/install-backup-cron.sh`.
- `crontab -l` muestra la tarea.
- `logs/backup-postgres.log` se revisa periodicamente.
