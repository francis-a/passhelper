plugins {
    application
    id("org.graalvm.buildtools.native") version "0.10.5"
    kotlin("jvm") version "2.1.10"
}

group = "io.eyecu"
version = "0-SNAPSHOT"

val javaVersion = JavaLanguageVersion.of(23)

java {
    toolchain {
        languageVersion = javaVersion
    }
}

kotlin {
    jvmToolchain {
        languageVersion = javaVersion
    }
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

repositories {
    mavenCentral()
}

application {
    mainClass.set("com.amazonaws.services.lambda.runtime.api.client.AWSLambda")
}

dependencies {
    implementation(enforcedPlatform("com.fasterxml.jackson:jackson-bom:2.18.3"))
    implementation(enforcedPlatform("software.amazon.awssdk:bom:2.31.6"))

    implementation("com.nimbusds:nimbus-jose-jwt:10.0.2")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    implementation("software.amazon.awssdk:dynamodb")
    implementation("software.amazon.awssdk:dynamodb-enhanced")
    implementation("software.amazon.awssdk:ses")
    implementation("software.amazon.awssdk:cognitoidentityprovider")
    implementation("software.amazon.awssdk:s3")

    implementation("com.amazonaws:aws-lambda-java-events:3.15.0")
    implementation("com.amazonaws:aws-lambda-java-core:1.2.3")
    implementation("com.amazonaws:aws-lambda-java-runtime-interface-client:2.4.2")

    implementation("org.thymeleaf:thymeleaf:3.1.3.RELEASE")

    implementation("io.github.oshai:kotlin-logging-jvm:7.0.5")
    implementation("org.slf4j:slf4j-simple:2.0.17")
    implementation("net.sf.biweekly:biweekly:0.6.8")

    testImplementation(platform("org.junit:junit-bom:5.12.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    testImplementation(platform("org.testcontainers:testcontainers-bom:1.20.5"))
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:localstack")
    testImplementation("org.assertj:assertj-core:3.27.3")

    implementation(platform("org.mockito:mockito-bom:5.16.1"))
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")

}

tasks.test {
    useJUnitPlatform()
}

graalvmNative {
    toolchainDetection = true
    binaries {
        named("main") {
            imageName.set("passhelper")
            verbose = false
            quickBuild = false
        }
        named("test") {
            verbose = true
            quickBuild = true
        }
        all {
            javaLauncher = javaToolchains.launcherFor {
                languageVersion = javaVersion
            }
        }
    }

    metadataRepository {
        enabled = true
    }
}