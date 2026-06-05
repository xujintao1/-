# ---- build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# 使用阿里云 Maven 镜像，避免国内直连中央仓库握手失败
COPY settings.xml /root/.m2/settings.xml
COPY pom.xml .
RUN mvn -q -B -s /root/.m2/settings.xml dependency:go-offline
COPY src ./src
RUN mvn -q -B -s /root/.m2/settings.xml clean package -DskipTests

# ---- runtime ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
