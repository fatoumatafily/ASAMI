FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -q -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S asami && adduser -S asami -G asami
COPY --from=build /app/target/*.jar app.jar
USER asami

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
