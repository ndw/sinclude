import com.nwalsh.gradle.saxon.SaxonXsltTask
import org.jreleaser.gradle.plugin.JReleaserExtension
import org.jreleaser.model.Active
import org.jreleaser.model.api.deploy.maven.MavenCentralMavenDeployer

buildscript {
  repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://dev.saxonica.com/maven") }
  }

  configurations.all {
    resolutionStrategy.eachDependency {
      if (requested.group == "xml-apis" && requested.name == "xml-apis") {
        useVersion("1.4.01")
      }
      if (requested.group == "net.sf.saxon" && requested.name == "Saxon-HE") {
        useVersion(project.properties["saxonVersion"].toString())
      }
      if (requested.group == "org.xmlresolver" && requested.name == "xmlresolver") {
        useVersion("5.3.3")
      }
    }
  }

  dependencies {
    classpath("net.sf.saxon:Saxon-HE:${project.properties["saxonVersion"]}")
    classpath("org.docbook:schemas-docbook:5.2CR5")
    classpath("org.docbook:docbook-xslTNG:2.1.2")
  }
}

plugins {
  id("java-library")
  id("maven-publish")
  id("com.github.gmazzo.buildconfig") version "5.3.5"
  id("com.nwalsh.gradle.saxon.saxon-gradle") version "0.10.4"
  id("com.nwalsh.gradle.relaxng.validate") version "0.10.3"
  id("org.jreleaser") version "1.18.0"
}

val saxonVersion = project.properties["saxonVersion"].toString()
val sincludeVersion = project.properties["sincludeVersion"].toString()
val basename = project.properties["basename"].toString()

repositories {
  mavenLocal()
  mavenCentral()
}

configurations.all {
  resolutionStrategy.eachDependency {
    if (requested.group == "xml-apis" && requested.name == "xml-apis") {
      useVersion("1.4.01")
    }
    if (requested.group == "net.sf.saxon" && requested.name == "Saxon-HE") {
      useVersion(saxonVersion)
    }
    if (requested.group == "org.xmlresolver" && requested.name == "xmlresolver") {
      useVersion("5.1.2")
    }
  }
}

val documentation by configurations.creating
val transform by configurations.creating {
  extendsFrom(configurations["documentation"])
}

dependencies {
  implementation("net.sf.saxon:Saxon-HE:${saxonVersion}")

  testImplementation("junit:junit:4.13.1")

  documentation ("net.sf.saxon:Saxon-HE:${saxonVersion}")
  documentation ("org.docbook:schemas-docbook:5.2CR5")
  documentation ("org.docbook:docbook-xslTNG:2.1.1")
}

buildConfig {
  packageName("com.nwalsh")
  buildConfigField("String", "TITLE", "\"${project.properties["sincludeTitle"]}\"")
  buildConfigField("String", "VERSION", "\"${project.properties["sincludeVersion"]}\"")
  // The SAXON_VERSION isn'"'t really relevant anymore, but removing it
  // now could break code using the API so we"ll leave it...
  buildConfigField("String", "SAXON_VERSION", "\"${saxonVersion}\"")
}

tasks.withType<AbstractTestTask> {
  testLogging {
    showStandardStreams = true
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.jar {
  archiveBaseName = "sinclude-${sincludeVersion}"
  manifest {
    attributes(mapOf("Built-By" to "Norman Walsh",
                     "Implementation-Vendor" to "Norman Walsh",
                     "Implementation-Title" to "Saxon XInclude Processor",
                     "Implementation-Version" to sincludeVersion))
  }
}

tasks.javadoc {
  if (JavaVersion.current().isJava9Compatible) {
    (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
  }
}

val javadocJar = tasks.register<Jar>("javadocJar") {
  dependsOn("javadoc")
  archiveClassifier = "javadoc"
  from(tasks.javadoc)
}

val sourcesJar = tasks.register<Jar>("sourcesJar") {
  dependsOn("generateBuildConfig")
  archiveClassifier = "sources"
  from(sourceSets.named("main").get().allSource)
}

tasks.register<Copy>("distCopyJar") {
  dependsOn("jar")
  from(tasks.jar)
  into(layout.buildDirectory.dir("${basename}-${sincludeVersion}/lib"))
}

tasks.register<Copy>("distCopyReadme") {
  from("README.org")
  into(layout.buildDirectory.dir("${basename}-${sincludeVersion}"))
}

tasks.register<Zip>("zipDist") {
  dependsOn("distCopyReadme", "distCopyJar")
  from(layout.buildDirectory.dir("${basename}-${sincludeVersion}"))
  into("${basename}-${sincludeVersion}")
  archiveFileName = "${basename}-${sincludeVersion}.zip"
}

tasks.register("dist") {
  dependsOn("test", "zipDist")
  doLast {
    println("Built dist for ${basename} version ${sincludeVersion}")
  }
}

// ============================================================

tasks.register("copyJarResources") {
  outputs.dir(layout.buildDirectory.dir("website"))

  val dbjar = configurations.named("transform").get().getFiles()
      .filter { jar -> jar.toString().contains("docbook-xslTNG") }
      .elementAtOrNull(0)

  doLast {
    if (dbjar == null) {
      throw GradleException("Failed to locate DocBook xslTNG jar file")
    }

    copy {
      from(zipTree(dbjar.toString()))
      into(layout.buildDirectory.dir("website"))
      include("org/docbook/xsltng/resources/**")
      eachFile {
        relativePath = RelativePath(true, *relativePath.segments.drop(4).toTypedArray())
      }
    }
  }

  doLast {
    delete(layout.buildDirectory.dir("website/org"))
  }
}

tasks.register<Copy>("copyStaticResources") {
  into(layout.buildDirectory.dir("website"))
  from(layout.projectDirectory.dir("src/website/resources"))
}

tasks.register("copyResources") {
  dependsOn("copyJarResources", "copyStaticResources")
}

tasks.register<SaxonXsltTask>("website") {
  dependsOn("copyResources")
  inputs.dir(layout.projectDirectory.dir("src/website/xml"))
  outputs.files(fileTree("dir" to layout.buildDirectory.dir("website"),
                         "include" to "*.html"))

  input(layout.projectDirectory.file("src/website/xml/sinclude.xml"))
  stylesheet(layout.projectDirectory.file("src/website/xsl/docbook.xsl"))
  output(layout.buildDirectory.file("website/index.html").get())
  args(listOf("-init:org.docbook.xsltng.extensions.Register"))
  parameters (
      mapOf(
          "mediaobject-input-base-uri" to "file:${layout.buildDirectory.get()}/xml/",
          "mediaobject-output-base-uri" to "/",
          "chunk" to "index.html",
          "chunk-output-base-uri" to "${layout.buildDirectory.get()}/website/",
          "docbook-transclusion" to "true",
          "sinclude-version" to sincludeVersion
      )
  )
}

// ============================================================

configure<PublishingExtension> {
  publications {
    register<MavenPublication>("maven") {
      from(components["java"])
      pom {
        name.set(project.name)
        groupId = "com.nwalsh"
        artifactId = "sinclude"
        version = sincludeVersion
        description.set(project.description ?: project.name)
        url.set("https://github.com/ndw/sinclude")
        licenses {
          license {
            name.set("Apache License version 2.0")
            url.set("https://www.apache.org/licenses/LICENSE-2.0")
          }
        }
        developers {
          developer {
            id.set("com.nwalsh")
            name.set("Norman Walsh")
            email.set("ndw@nwalsh.com")
          }
        }
        scm {
          connection.set("scm:git@github.com:ndw/sinclude.git")
          developerConnection.set("scm:git@github.com:ndw/sinclude.git")
          url.set("https://github.com/ndw/sinclude")
        }
      }

      artifact(sourcesJar.get())
      artifact(javadocJar.get())
    }
  }

  repositories {
    maven {
      url = layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
    }
  }
}

configure<JReleaserExtension> {
  gitRootSearch = true
  project {
    group = "com.nwalsh"
    version = sincludeVersion
    description = "An XInclude processor for Saxon"
    authors = listOf("ndw")
    license = "Apache License version 2.0"
    links {
      homepage = "https://github.com/ndw/sinclude"
      bugTracker = "https://github.com/ndw/sinclude/issues"
      contact = "https://github.com/ndw/sinclude"
    }
    inceptionYear = "2020"
    vendor = "Norman Walsh"
    copyright = "Copyright (c)2020-2025 Norman Walsh"
  }

  release {
    github {
      commitAuthor {
        name = "Norman Walsh"
        email = "ndw@nwalsh.com"
      }
    }
  }

  signing {
    active = Active.ALWAYS
    armored = true
  }

  deploy {
    maven {
      mavenCentral {
        register("sonatype") {
          active = Active.ALWAYS
          stage = MavenCentralMavenDeployer.Stage.UPLOAD
          url = "https://central.sonatype.com/api/v1/publisher"
          stagingRepositories.add("build/staging-deploy")
        }
      }
    }
  }
}
