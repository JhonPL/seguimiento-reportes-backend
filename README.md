# Sistema de Seguimiento de Reportes - Llanogas

## 📋 Descripción

Sistema web para centralizar y gestionar los reportes requeridos por entidades de control (SUI, Superservicios, etc.), con seguimiento de plazos, frecuencias y cumplimiento.

## 🚀 Tecnologías

- **Backend**: Spring Boot 3.3.3
- **Base de datos**: H2 (desarrollo) / PostgreSQL (producción)
- **Seguridad**: JWT + Spring Security
- **Documentación**: Swagger/OpenAPI

## 📦 Estructura del Proyecto

```
src/main/java/com/example/demo/
├── config/           # Configuraciones (Security, CORS, etc.)
├── controller/       # Controladores REST
├── dto/              # Data Transfer Objects
├── entity/           # Entidades JPA
├── exception/        # Manejo de excepciones
├── repository/       # Repositorios JPA
├── security/         # JWT y autenticación
└── service/          # Lógica de negocio
```

## 🛠️ Instalación y Ejecución

### Prerrequisitos

- Java 17+
- Maven 3.8+
- (Opcional) PostgreSQL 14+ para producción

### Ejecutar en modo desarrollo

```bash
# Clonar o descargar el proyecto
cd seguimiento-reportes

# Compilar
mvn clean install -DskipTests

# Ejecutar
mvn spring-boot:run
```

La aplicación iniciará en `http://localhost:8081`

### Credenciales de prueba

| Rol | Email | Contraseña |
|-----|-------|------------|
| Admin | admin@llanogas.com | admin123 |

## 📚 Documentación API

- **Swagger UI**: http://localhost:8081/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8081/v3/api-docs
- **H2 Console**: http://localhost:8081/h2-console

## 🔐 Autenticación

El sistema usa JWT. Para autenticarse:

```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"correo":"admin@llanogas.com","contrasena":"admin123"}'
```

Respuesta:
```json
{
  "token": "eyJhbGc...",
  "rol": "ADMINISTRADOR",
  "nombre": "Administrador Sistema",
  "usuarioId": 1
}
```

Usa el token en las siguientes peticiones:
```bash
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8081/api/reportes
```

## 📡 Endpoints Principales

### Autenticación
- `POST /api/auth/login` - Iniciar sesión
- `POST /api/auth/register` - Registrar usuario

### Entidades
- `GET /api/entidades` - Listar entidades de control
- `POST /api/entidades` - Crear entidad
- `PUT /api/entidades/{id}` - Actualizar entidad
- `DELETE /api/entidades/{id}` - Eliminar entidad

### Reportes
- `GET /api/reportes` - Listar reportes
- `GET /api/reportes/{id}` - Obtener reporte
- `POST /api/reportes` - Crear reporte
- `PUT /api/reportes/{id}` - Actualizar reporte

### Instancias de Reporte
- `GET /api/instancias` - Listar todas las instancias
- `GET /api/instancias/reporte/{reporteId}` - Instancias por reporte
- `POST /api/instancias` - Crear instancia
- `PUT /api/instancias/{id}` - Actualizar instancia (cambiar estado, etc.)

### Alertas
- `GET /api/alertas` - Listar alertas
- `GET /api/alertas/usuario/{id}/no-leidas` - Alertas no leídas
- `PATCH /api/alertas/{id}/leer` - Marcar como leída

### Calendario
- `GET /api/calendario/eventos?mes=2025-03` - Eventos del mes
- `GET /api/calendario/mi-calendario?mes=2025-03` - Mi calendario (según rol)
- `GET /api/calendario/buscar` - Búsqueda avanzada

### Estadísticas
- `GET /api/estadisticas/dashboard` - Dashboard principal
- `GET /api/estadisticas/distribucion-estados` - Distribución por estado
- `GET /api/estadisticas/proximos-vencer?dias=7` - Próximos a vencer
- `GET /api/estadisticas/vencidos` - Reportes vencidos

## ⚙️ Configuración para Producción

### PostgreSQL

Editar `src/main/resources/application.properties`:

```properties
# Cambiar H2 por PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/seguimiento_reportes
spring.datasource.username=tu_usuario
spring.datasource.password=tu_password
spring.datasource.driver-class-name=org.postgresql.Driver

# Cambiar dialect
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect

# Cambiar ddl-auto (usar 'validate' o 'none' en producción)
spring.jpa.hibernate.ddl-auto=validate

# Deshabilitar H2 Console
spring.h2.console.enabled=false
```

### Email (Gmail)

```properties
spring.mail.username=reportes@llanogas.com
spring.mail.password=tu-app-password
notificaciones.email.habilitado=true
```

### WhatsApp (Twilio)

```properties
twilio.account.sid=ACxxxxxxxxxx
twilio.auth.token=tu_token
twilio.whatsapp.number=whatsapp:+14155238886
notificaciones.whatsapp.habilitado=true
```

## 🏗️ Crear el JAR ejecutable

```bash
mvn clean package -DskipTests
java -jar target/seguimiento-reportes-0.0.1-SNAPSHOT.jar
```

## 📝 Licencia

Uso interno - Llanogas

---

Desarrollado para el Reto de Transformación Digital - Departamento del Meta
