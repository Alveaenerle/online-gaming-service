FROM eclipse-temurin:21-jdk AS base
WORKDIR /app

COPY .mvn/ .mvn
COPY mvnw pom.xml ./
COPY authorization/pom.xml authorization/
COPY ludo/pom.xml ludo/
COPY makao/pom.xml makao/
COPY social/pom.xml social/
COPY tests/pom.xml tests/
COPY menu/pom.xml menu/
COPY common/pom.xml common/

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

FROM base AS dev
ARG MODULE_NAME
ENV MODULE_NAME=${MODULE_NAME}
COPY . .
RUN chmod +x mvnw
CMD ["sh", "-c", "./mvnw -q spring-boot:run -pl ${MODULE_NAME} -am"]

FROM base AS build
ARG MODULE_NAME
COPY . .
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests -pl ${MODULE_NAME} -am

FROM eclipse-temurin:21-jre AS production
ARG MODULE_NAME
WORKDIR /app

RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

COPY --from=build /app/${MODULE_NAME}/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]