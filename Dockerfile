# Etapa 1: Build con Maven
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build

WORKDIR /app

# Copiar archivos de configuración de Maven
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY mvnw.cmd .

# Descargar dependencias (esta capa se cachea)
RUN mvn dependency:go-offline -B

# Copiar código fuente
COPY src ./src

# Compilar la aplicación
RUN mvn clean package -DskipTests

# Etapa 2: Runtime con JRE ligero
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copiar el JAR desde la etapa de build
COPY --from=build /app/target/seguimiento-reportes-0.0.1-SNAPSHOT.jar app.jar

# Exponer el puerto (Render usa la variable $PORT)
EXPOSE 8080

# Comando para ejecutar la aplicación
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]
```

---

## 📝 **Y el .dockerignore también (copia completo):**
```
# Maven
target/
pom.xml.tag
pom.xml.releaseBackup
pom.xml.versionsBackup
pom.xml.next
release.properties

# IDEs
.idea/
*.iml
*.iws
.vscode/
.settings/
.project
.classpath

# Git
.git/
.gitignore

# OS
.DS_Store
Thumbs.db

# Logs
*.log
logs/

# Node (si tienes frontend en el mismo repo)
node_modules/

# Otros
*.class
*.jar
!.mvn/wrapper/maven-wrapper.jar
