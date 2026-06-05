# ---- build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# 使用阿里云 Maven 镜像，避免国内直连中央仓库握手失败
COPY settings.xml /root/.m2/settings.xml
COPY pom.xml .
COPY src ./src
# 直接 package（只下载实际需要的依赖）；加重试与超时参数，抵抗网络握手偶发中断
RUN mvn -B -s /root/.m2/settings.xml \
        -Dmaven.wagon.http.retryHandler.count=10 \
        -Dmaven.wagon.http.retryHandler.requestSentEnabled=true \
        -Dmaven.wagon.httpconnectionManager.ttlSeconds=180 \
        -Dmaven.wagon.rto=180000 \
        clean package -DskipTests

# ---- runtime ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
