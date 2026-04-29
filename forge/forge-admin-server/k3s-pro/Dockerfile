# 构建上下文为本目录（Jenkins：cd forge-admin-server 后 docker build … k3s-pro）
# 前置步骤会将可执行 jar 复制为 APP_LIST 对应名称（如 forge_ims.jar）。
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 多个 .jar 时 COPY 目标须为目录；取第一个作为可执行包（流水线应只保留一个 fat jar）
COPY *.jar /tmp/ctx/
RUN set -eu; \
  JAR="$(ls -1 /tmp/ctx/*.jar | head -n1)"; \
  test -n "$JAR" || { echo "ERROR: 上下文中无 jar"; exit 1; }; \
  cp "$JAR" /app/app.jar; \
  rm -rf /tmp/ctx

# 运行时强制 prod（若 Maven 打包未加 -Pprod，jar 内 application.yml 仍可能是 dev）；Helm/K8s 可再覆盖 SPRING_PROFILES_ACTIVE
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS=""
# EXPOSE 仅说明容器内「预期」端口；实际监听仍以 Spring 为准（默认 application.yml server.port=8580）
EXPOSE 80

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar app.jar"]
