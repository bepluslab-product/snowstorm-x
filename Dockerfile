FROM maven:3.6.3-openjdk-11 AS builder
COPY . /usr/src/app
WORKDIR /usr/src/app
RUN mvn clean install -DskipTests=true -Ddependency-check.skip=true

# get snomed-drools-rules
RUN apt-get install git -y
RUN git clone https://github.com/IHTSDO/snomed-drools-rules.git

FROM adoptopenjdk/openjdk11:alpine
LABEL maintainer="SNOMED International <tooling@snomed.org>"
ARG SUID=1042
ARG SGID=1042

VOLUME /tmp

# Create necessary directories
RUN mkdir /app
WORKDIR /app
COPY --from=builder /usr/src/app/snomed-drools-rules snomed-drools-rules

# Copy jar from builder image
COPY --from=builder /usr/src/app/target/snowstorm-*.jar snowstorm.jar

# Create the snowstorm user
RUN addgroup -g $SGID snowstorm && \
    adduser -D -u $SUID -G snowstorm snowstorm

# Change permissions.
RUN chown -R snowstorm:snowstorm /app

# Run as the snowstorm user.
USER snowstorm
EXPOSE 8080
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","snowstorm.jar"]
