import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    application
    id("org.graalvm.buildtools.native") version "0.10.4"
    kotlin("jvm") version "2.0.21"
}

group = "io.eyecu"
version = "0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_22
    toolchain {
        languageVersion = JavaLanguageVersion.of(22)
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_22)
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

application {
    mainClass.set("com.amazonaws.services.lambda.runtime.api.client.AWSLambda")
}

dependencies {
    implementation(enforcedPlatform("com.fasterxml.jackson:jackson-bom:2.18.2"))
    implementation(enforcedPlatform("software.amazon.awssdk:bom:2.29.20"))

    implementation("com.nimbusds:nimbus-jose-jwt:9.47")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    implementation("software.amazon.awssdk:dynamodb")
    implementation("software.amazon.awssdk:dynamodb-enhanced")
    implementation("software.amazon.awssdk:ses")
    implementation("software.amazon.awssdk:cognitoidentityprovider")
    implementation("software.amazon.awssdk:s3")

    implementation("com.amazonaws:aws-lambda-java-events:3.14.0")
    implementation("com.amazonaws:aws-lambda-java-core:1.2.3")
    implementation("com.amazonaws:aws-lambda-java-runtime-interface-client:2.4.2")

    implementation("org.thymeleaf:thymeleaf:3.1.2.RELEASE")

    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")
    implementation("org.slf4j:slf4j-simple:2.0.16")
    implementation("net.sf.biweekly:biweekly:0.6.8")

    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    testImplementation(platform("org.testcontainers:testcontainers-bom:1.20.4"))
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:localstack")
    testImplementation("org.assertj:assertj-core:3.26.3")

    implementation(platform("org.mockito:mockito-bom:5.14.2"))
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")

}

tasks.withType<Test> {
    useJUnitPlatform()
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("passhelper")
            verbose = false
            fallback = false
            quickBuild = false
        }
    }
    metadataRepository {
        enabled = true
    }
}