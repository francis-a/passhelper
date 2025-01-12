FROM --platform=linux/arm64 public.ecr.aws/sam/build-provided.al2023:latest

# Graal VM
ENV GRAAL_VERSION=23.0.1
RUN curl -4 -L https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-${GRAAL_VERSION}/graalvm-community-jdk-${GRAAL_VERSION}_linux-aarch64_bin.tar.gz | tar -xvz
RUN mv graalvm-community-openjdk* /usr/lib/graalvm
ENV JAVA_HOME=/usr/lib/graalvm

VOLUME /project
WORKDIR /project

ENTRYPOINT ["sh"]