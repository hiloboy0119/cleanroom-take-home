FROM gcr.io/distroless/java:8
MAINTAINER "drew@videoamp.com"

ARG JAR_NAME
ARG JAVA_OPTS
WORKDIR /cleanroom
ADD $JAR_NAME app.jar
EXPOSE 8080
ENV JAVA_OPTS=$JAVA_OPTS
CMD ["app.jar"]