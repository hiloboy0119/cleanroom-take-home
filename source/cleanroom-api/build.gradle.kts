import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    val dockerPluginVersion = "0.22.1"
    java
    application
    id("com.palantir.docker") version dockerPluginVersion
    id("com.palantir.docker-run") version dockerPluginVersion
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

group = "com.videoamp"
version = "1.0-SNAPSHOT"

val vertxVersion = "3.5.4"
val reactivexVersion = "2.1.9"
val zetasqlVersion = "2019.10.1"

dependencies {
    compile("io.vertx", "vertx-core", vertxVersion)
    compile("io.vertx", "vertx-web", vertxVersion)
    compile("io.vertx", "vertx-rx-java2", vertxVersion)
    compile("io.reactivex.rxjava2", "rxjava", reactivexVersion)
    compile("com.google.zetasql", "zetasql-client", zetasqlVersion)
    compile("com.google.zetasql", "zetasql-types", zetasqlVersion)
    compile("com.google.zetasql", "zetasql-jni-channel", zetasqlVersion)
    compile("com.google.cloud", "google-cloud-bigquery", "1.101.0")
    testCompile("junit", "junit", "4.12")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

application {
    mainClassName = "io.vertx.core.Launcher"
}

tasks {
    test {
        useJUnitPlatform()
    }

    shadowJar {
        classifier = "fat"
        manifest {
            attributes["Main-Verticle"] = "com.videoamp.cleanroom.queryanalyzer.HelloWorld"
        }
        mergeServiceFiles {
            include("META-INF/services/io.vertx.core.spi.VerticleFactory")
        }
    }
}

val gcpProject: String by project

docker {
    val shadowJar = tasks.shadowJar.get()
    name = "gcr.io/${gcpProject}/cleanroom-api:${version}"
    setDockerfile(file("src/main/docker/Dockerfile"))
    files(shadowJar.outputs.files)
    buildArgs(mapOf(
        "JAR_NAME" to shadowJar.archiveFileName.get(),
        "JAVA_OPTS" to "-Xms64m -Xmx128m"
    ))
}

dockerRun {
    val homeDir = System.getProperty("user.home")
    name = "cleanroom-api"
    image = "gcr.io/${gcpProject}/cleanroom-api:${version}"
    volumes(mapOf(
        "${homeDir}/.config/gcloud/" to "/.config/gcloud/"
    ))
    ports("8080:8080")
    env(mapOf(
        "GOOGLE_CLOUD_PROJECT" to gcpProject,
        "GOOGLE_APPLICATION_CREDENTIALS" to "/.config/gcloud/application_default_credentials.json"
    ))
    daemonize = false
    clean = true
}