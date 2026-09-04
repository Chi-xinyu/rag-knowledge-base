# ============================================================
# Dockerfile：把 Spring Boot 后端打成 Docker 镜像
#
# 多阶段构建：
#   第 1 阶段（build）：用带 Maven + JDK21 的大镜像编译打包成 jar
#   第 2 阶段（run）  ：用只有 JRE 的轻量镜像运行 jar
#
# 【加速优化（重要）】
#   1. 使用 maven-settings.xml（阿里云镜像），容器内下载依赖走国内源，快 5~10 倍
#   2. 使用 BuildKit 缓存挂载（--mount=type=cache,target=/root/.m2）：
#      第一次构建下载的依赖会缓存下来，第二次起【不再重新下载】
#   3. 需在 docker-compose.yml 的 build 里开启 DOCKER_BUILDKIT
# ============================================================
# 声明使用最新 Dockerfile 语法（启用 BuildKit 缓存挂载需要）
# syntax=docker/dockerfile:1

# ---------- 第 1 阶段：编译打包 ----------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# 把国内 Maven 镜像配置放进去（放 .m2 目录下，Maven 会自动读取）
COPY maven-settings.xml /root/.m2/settings.xml

# 先复制 pom.xml，下载依赖（利用缓存：pom 没变就不重下）
COPY pom.xml .
# --mount=type=cache：把 /root/.m2 挂为持久缓存，二次构建不重下依赖
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B

# 复制源码，打包
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -B

# ---------- 第 2 阶段：运行 ----------
FROM eclipse-temurin:21-jre

WORKDIR /app

# 把打好的 jar 复制过来
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
