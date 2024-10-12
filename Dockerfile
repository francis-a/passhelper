FROM --platform=linux/arm64 public.ecr.aws/amazonlinux/amazonlinux:2023

RUN yum -y update \
    && yum install -y unzip tar gzip bzip2-devel ed gcc gcc-c++ gcc-gfortran \
    less libcurl-devel openssl openssl-devel readline-devel xz-devel \
    zlib-devel glibc-static zlib-static \
    python3-pip \
    && rm -rf /var/cache/yum

# Graal VM
ENV GRAAL_VERSION 23.0.0
RUN curl -4 -L https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-${GRAAL_VERSION}/graalvm-community-jdk-${GRAAL_VERSION}_linux-aarch64_bin.tar.gz | tar -xvz
RUN mv graalvm-community-openjdk* /usr/lib/graalvm
ENV JAVA_HOME /usr/lib/graalvm

# Gradle
ENV GRADLE_VERSION 8.10.2
ENV GRADLE_FOLDERNAME gradle-${GRADLE_VERSION}
ENV GRADLE_FILENAME gradle-${GRADLE_VERSION}-bin.zip
RUN curl -LO https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip
RUN unzip gradle-${GRADLE_VERSION}-bin.zip
RUN mv $GRADLE_FOLDERNAME /usr/lib/gradle
RUN ln -s /usr/lib/gradle/bin/gradle /usr/bin/gradle

RUN pip install aws-lambda-builders

VOLUME /project
WORKDIR /project

ENTRYPOINT ["sh"]