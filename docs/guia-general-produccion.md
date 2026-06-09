# Guia general para subir una app a produccion

Esta guia resume los pasos mas importantes para desplegar un proyecto en un VPS. Sirve como plantilla para proyectos con:

- Backend con Docker.
- Base de datos en Docker.
- Frontend compilado como archivos estaticos.
- Nginx como proxy y servidor web.
- HTTPS con Certbot.

La idea general es:

```text
Usuario -> Dominio HTTPS -> Nginx -> Backend Docker
Usuario -> Dominio HTTPS -> Nginx -> Frontend estatico
```

## 1. Separar entornos

Nunca uses la misma configuracion para desarrollo y produccion.

Archivos comunes:

```text
.env.example      Plantilla sin secretos
.env              Variables reales, no se sube a Git
docker-compose.yml
docker-compose.server.yml
```

El `.env` debe estar en `.gitignore`.

Ejemplo:

```env
APP_DOMAIN=api.tudominio.com
APP_CORS_ALLOWED_ORIGINS=https://app.tudominio.com
DB_NAME=app_db
DB_USER=app_user
DB_PASSWORD=CAMBIAR_ESTO
JWT_SECRET=CAMBIAR_ESTO
```

## 2. Preparar el backend

Antes de subirlo al servidor:

```bash
git status
```

Probar localmente:

```bash
docker compose config
docker compose up --build -d
docker compose ps
docker compose logs -f backend
```

Probar salud:

```bash
curl -i http://localhost:8080/actuator/health
```

Si el backend tiene endpoints protegidos, es normal que respondan `401` sin token.

## 3. Preparar el VPS

Entrar al servidor:

```bash
ssh usuario@IP_DEL_VPS
```

Actualizar paquetes:

```bash
sudo apt update
sudo apt upgrade -y
```

Instalar herramientas base:

```bash
sudo apt install -y git curl nginx certbot python3-certbot-nginx
```

Instalar Docker:

```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
```

Luego cerrar sesion y volver a entrar por SSH.

Verificar:

```bash
docker --version
docker compose version
```

## 4. Clonar el repositorio en el VPS

Crear carpeta de proyectos:

```bash
mkdir -p ~/proyectos
cd ~/proyectos
```

Clonar:

```bash
git clone URL_DEL_REPOSITORIO backend
cd backend
```

Crear `.env`:

```bash
nano .env
```

Validar Docker Compose:

```bash
docker compose --env-file .env -f docker-compose.server.yml config
```

Levantar:

```bash
docker compose --env-file .env -f docker-compose.server.yml up --build -d
```

Ver estado:

```bash
docker compose --env-file .env -f docker-compose.server.yml ps
docker compose --env-file .env -f docker-compose.server.yml logs -f backend
```

## 5. Configurar Nginx para el backend

Crear archivo:

```bash
sudo nano /etc/nginx/sites-available/app-api
```

Ejemplo:

```nginx
server {
    server_name api.tudominio.com;

    location / {
        proxy_pass http://127.0.0.1:18080;
        proxy_http_version 1.1;

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Activar:

```bash
sudo ln -s /etc/nginx/sites-available/app-api /etc/nginx/sites-enabled/app-api
sudo nginx -t
sudo systemctl reload nginx
```

Activar HTTPS:

```bash
sudo certbot --nginx -d api.tudominio.com
```

Probar:

```bash
curl -i https://api.tudominio.com/actuator/health
```

## 6. Preparar el frontend

En tu PC local:

```powershell
cd RUTA_DEL_FRONTEND
git pull
```

Configurar URL del API antes de compilar:

```powershell
$env:VITE_API_BASE_URL="https://api.tudominio.com"
npm.cmd ci
npm.cmd run build
```

Verificar que el build uso la URL correcta:

```powershell
Select-String -Path .\dist\assets\*.js -Pattern "https://api.tudominio.com"
```

Subir al VPS:

```powershell
scp -r .\dist\* usuario@IP_DEL_VPS:/tmp/app-frontend-dist/
```

## 7. Publicar frontend con Nginx

En el VPS:

```bash
sudo mkdir -p /var/www/app-frontend
sudo rsync -av --delete /tmp/app-frontend-dist/ /var/www/app-frontend/
rm -rf /tmp/app-frontend-dist
sudo chown -R www-data:www-data /var/www/app-frontend
sudo find /var/www/app-frontend -type d -exec chmod 755 {} \;
sudo find /var/www/app-frontend -type f -exec chmod 644 {} \;
```

Crear Nginx:

```bash
sudo nano /etc/nginx/sites-available/app-frontend
```

Ejemplo para React/Vite:

```nginx
server {
    server_name app.tudominio.com;

    root /var/www/app-frontend;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

Activar:

```bash
sudo ln -s /etc/nginx/sites-available/app-frontend /etc/nginx/sites-enabled/app-frontend
sudo nginx -t
sudo systemctl reload nginx
```

HTTPS:

```bash
sudo certbot --nginx -d app.tudominio.com
```

Probar:

```bash
curl -I https://app.tudominio.com
```

Debe responder `200`.

## 8. Checklist de pruebas

Backend:

```bash
curl -i https://api.tudominio.com/actuator/health
curl -i https://api.tudominio.com/api/ruta-protegida
```

Esperado:

```text
health -> 200
ruta protegida sin token -> 401
```

Frontend:

```text
Abrir https://app.tudominio.com
Probar login
Revisar DevTools > Network
Confirmar que llama a https://api.tudominio.com
```

## 9. Backups de base de datos

Crear backup manual:

```bash
docker compose --env-file .env -f docker-compose.server.yml exec -T postgres pg_dump -U DB_USER DB_NAME > backup.sql
```

Restaurar en una base de prueba antes de confiar en el backup:

```bash
cat backup.sql | docker compose --env-file .env -f docker-compose.server.yml exec -T postgres psql -U DB_USER DB_NAME_TEST
```

Automatizar con cron:

```bash
crontab -e
```

Ejemplo diario a las 2:30 AM:

```cron
30 2 * * * cd /home/usuario/proyectos/backend && bash ./scripts/backup-postgres.sh >> logs/backup-postgres.log 2>&1
```

Importante: guardar backups solo dentro del VPS no protege contra perdida total del servidor. Lo ideal es copiarlos periodicamente fuera del VPS.

## 10. Flujo normal para actualizar produccion

Backend:

```bash
cd ~/proyectos/backend
git pull
docker compose --env-file .env -f docker-compose.server.yml up --build -d
docker compose --env-file .env -f docker-compose.server.yml ps
```

Frontend:

```powershell
cd RUTA_DEL_FRONTEND
git pull
$env:VITE_API_BASE_URL="https://api.tudominio.com"
npm.cmd ci
npm.cmd run build
scp -r .\dist\* usuario@IP_DEL_VPS:/tmp/app-frontend-dist/
```

En el VPS:

```bash
sudo rsync -av --delete /tmp/app-frontend-dist/ /var/www/app-frontend/
sudo nginx -t
sudo systemctl reload nginx
```

## 11. Errores comunes

### 403 en frontend

Revisar:

```bash
ls -la /var/www/app-frontend
sudo tail -n 80 /var/log/nginx/error.log
```

Corregir permisos:

```bash
sudo chown -R www-data:www-data /var/www/app-frontend
sudo find /var/www/app-frontend -type d -exec chmod 755 {} \;
sudo find /var/www/app-frontend -type f -exec chmod 644 {} \;
sudo systemctl reload nginx
```

### 405 en login

Normalmente el frontend esta llamando a su propio dominio en vez del API.

Mal:

```text
https://app.tudominio.com/api/auth/login
```

Bien:

```text
https://api.tudominio.com/api/auth/login
```

Solucion: recompilar frontend con la variable correcta.

### Error CORS

Revisar `.env` del backend:

```env
APP_CORS_ALLOWED_ORIGINS=https://app.tudominio.com
```

Luego reiniciar backend:

```bash
docker compose --env-file .env -f docker-compose.server.yml up --build -d
```

### Nginx no recarga

Probar configuracion:

```bash
sudo nginx -t
```

Ver logs:

```bash
sudo journalctl -u nginx -n 80 --no-pager
```

## 12. Checklist final

- Dominio apunta al VPS.
- Backend corre en Docker.
- Base de datos tiene volumen persistente.
- Nginx apunta al puerto interno correcto.
- HTTPS activo para frontend y backend.
- Frontend fue compilado con la URL correcta del API.
- Login funciona desde navegador.
- Backups manuales funcionan.
- Backups automaticos estan programados.
- Existe una forma de restaurar backup en prueba.
- `.env` no esta en Git.
- Los repositorios estan limpios y con commits subidos.
