# 运行时镜像：直接复用本地已构建的 jar（先在宿主机 mvn package）
# 用法：docker build -f docker/Dockerfile.app --build-arg JAR=campus-gateway/target/campus-gateway-1.0.0.jar -t campus/gateway:1.0.0 .
#      构建上下文 = campus-mart 根目录
FROM eclipse-temurin:17-jre-jammy

ARG JAR
ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    JAVA_OPTS="-Xms256m -Xmx768m -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR /app
COPY ${JAR} /app/app.jar
RUN mkdir -p /app/upload /app/logs

EXPOSE 8080 9200 9201 9202 9203 9204
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
