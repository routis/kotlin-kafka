import kotlinx.knit.KnitPluginExtension
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  dependencies {
    classpath("org.jetbrains.kotlinx:kotlinx-knit:0.4.0")
  }
}

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.arrowGradleConfig.kotlin)
  alias(libs.plugins.arrowGradleConfig.nexus)
  alias(libs.plugins.arrowGradleConfig.publish)
  alias(libs.plugins.dokka)
}

apply(plugin = "kotlinx-knit")

allprojects {
  repositories {
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    mavenCentral()
  }
  group = property("projects.group").toString()
  version = property("projects.version").toString()
}

dependencies {
  api(libs.kotlin.stdlib)
  api(libs.kotlinx.coroutines.core)
  api(libs.kotlinx.coroutines.jdk8)

  // Kafka, TODO split into separate modules (?)
  api(libs.kafka.clients)
  api(libs.kafka.streams)
  api(libs.kafka.connect)

  testImplementation(libs.kotest.runner.junit5)
  testImplementation(libs.kotest.property)
  testImplementation(libs.kotest.framework)
  testImplementation(libs.kotest.assertions)
}

configure<KnitPluginExtension> {
  siteRoot = "https://nomisrev.github.io/kotlin-kafka/"
}

tasks {
  withType<DokkaTask>().configureEach {
    outputDirectory.set(rootDir.resolve("docs"))
    moduleName.set("kotlin-kafka")
    dokkaSourceSets {
      named("main") {
        includes.from("README.md")
        perPackageOption {
          matchingRegex.set(".*\\.internal.*")
          suppress.set(true)
        }
        sourceLink {
          localDirectory.set(file("src/main/kotlin"))
          remoteUrl.set(uri("https://github.com/nomisRev/kotlin-kafka/tree/main/src/main/kotlin").toURL())
          remoteLineSuffix.set("#L")
        }
      }
    }
  }

  getByName("knitPrepare").dependsOn(getTasksByName("dokka", true))

  withType<Test>().configureEach {
    useJUnitPlatform()
  }

  withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
  }

  val cleanDocs = register<Delete>("cleanDocs") {
    val folder = file("docs")
    val docsContent = folder.listFiles().filter { it != folder }
    delete(docsContent)
  }
}

nexusPublishing {
  repositories {
    named("sonatype") {
      nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
      snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
    }
  }
}
