FROM eclipse-temurin:25-jdk AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src
RUN chmod +x mvnw && ./mvnw -q -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --uid 10001 app
COPY --from=build /src/target/linksentinel.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
