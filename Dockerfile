# =========================================================
# La receta de la "fiambrera" (imagen de Docker) de la API.
# Tiene DOS fases:
#   1. "compilar": una cocina con el JDK y Maven que convierte el código en un .jar
#   2. la caja final: solo lleva Java (JRE) y el .jar. Es más pequeña y más segura,
#      porque no lleva ni el código fuente ni las herramientas de compilar.
# =========================================================

# ---------- Fase 1: compilar ----------
FROM eclipse-temurin:21-jdk AS compilar
WORKDIR /app

# Primero solo lo que hace falta para descargar las librerías.
# Docker guarda cada paso: si el pom.xml no cambia, la próxima vez se salta la descarga (va mucho más rápido).
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline

# Ahora el código, y a compilar. Los tests no se pasan aquí: ya los pasamos antes de cada commit.
COPY src/ src/
RUN ./mvnw -q package -DskipTests

# ---------- Fase 2: la caja final ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Por seguridad, la app no se ejecuta como "root" (el administrador), sino con un usuario sin permisos
RUN useradd --system --uid 1001 finanzas
COPY --from=compilar /app/target/finanzas-*.jar app.jar
USER finanzas

# Que Java no use más memoria de la que tiene la caja (en los servidores gratis hay poca)
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
