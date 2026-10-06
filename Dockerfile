# Build stage
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline

COPY src src
RUN ./mvnw -q -B -DskipTests package

# Runtime stage
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Datas "de hoje" (aniversariantes, competência) seguem o horário de Brasília.
ENV TZ=America/Sao_Paulo

RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
