import Dependencies.ScalaVersions.scala212
import Dependencies.Versions
import sbt.Append.appendSeq
import sbt.ThisBuild
import xsbti.compile.CompileAnalysis

ThisBuild / version      := "6.2.2"
ThisBuild / isSnapshot  := false

Global / excludeLintKeys += homepage
Global / excludeLintKeys += organization
Global / excludeLintKeys += scmInfo

organization := "io.github.lapidus79"
ThisBuild / organization := "io.github.lapidus79"
ThisBuild / publishMavenStyle := true

publishConfiguration := publishConfiguration.value.withOverwrite(true)
publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

// Customise sbt-dynver's behaviour to make it work with tags which aren't v-prefixed
ThisBuild / dynverVTagPrefix := false


val scala213 = "2.13.1"

// Sanity-check: assert that version comes from a tag (e.g. not a too-shallow clone)
// https://github.com/dwijnand/sbt-dynver/#sanity-checking-the-version
Global / onLoad := (Global / onLoad).value.andThen { s =>
  val v = version.value
  if (dynverGitDescribeOutput.value.hasNoTags)
    throw new MessageOnlyException(
      s"Failed to derive version from git tags. Maybe run `git fetch --unshallow`? Version: $v"
    )
  s
}

lazy val mimaSettings = Seq(
  mimaPreviousArtifacts := Set(
    "io.github.lapidus79" %% name.value % "6.0.0" //previousStableVersion.value
    //.getOrElse(throw new Error("Unable to determine previous version"))
  ),
)

credentials += Credentials(
  "GnuPG Key ID",
  "gpg",
  "5326F9F72B0BAE3316FC9914B3999571BAB4F9CD", // key identifier
  "ignore" // this field is ignored; passwords are supplied by pinentry
)

lazy val root = project
  .in(file("."))
  .aggregate(core, plugin)
  .disablePlugins(MimaPlugin)
  .settings(
    name := "play-ebean-root",
    crossScalaVersions := Nil,
    publish / skip := true,
    organization := "io.github.lapidus79",
    homepage := Some(url(s"https://github.com/lapidus79/play-ebean")),
    sources in(Compile, doc) := Seq()
  )
  .settings(
    credentials += Credentials(
      "Sonatype Nexus Repository Manager",
      "oss.sonatype.org",
      System.getenv("OSS_ST_USERNAME"),
      System.getenv("OSS_ST_PASSWORD")
    ),
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
      else Some("releases" at nexus + "service/local/staging/deploy/maven2")
    }
  )

lazy val core = project
  .in(file("play-ebean"))
  .settings(
    credentials += Credentials(
      "Sonatype Nexus Repository Manager",
      "oss.sonatype.org",
      System.getenv("OSS_ST_USERNAME"),
      System.getenv("OSS_ST_PASSWORD")
    ),
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
      else Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },

    sources in(Compile, doc) := Seq(),
    homepage := Some(url(s"https://github.com/lapidus79/play-ebean")),
    publishMavenStyle := true,
    Test / publishArtifact := false,
    pomIncludeRepository := { _ => false },
    pomExtra :=
      <scm>
        <url>git@github.com:lapidus79/play-ebean.git</url>
        <connection>scm:git:git@github.com:lapidus79/play-ebean.git</connection>
      </scm>
        <developers>
          <developer>
            <id>playframework</id>
            <name>Play Framework Team</name>
            <url>https://github.com/playframework</url>
          </developer>
          <developer>
            <id>lapidus79</id>
            <name>Lapidus79</name>
            <url>https://github.com/lapidus79</url>
          </developer>
        </developers>
  )
  .settings(
    name := "play-ebean",
    crossScalaVersions := Seq(scala212, scala213),
    libraryDependencies ++= playEbeanDeps,
    organization := "io.github.lapidus79",
    // useGpg := true,
    Dependencies.ebean,
    mimaSettings,
    Compile / compile := enhanceEbeanClasses(
      (Compile / dependencyClasspath).value,
      (Compile / compile).value,
      (Compile / classDirectory).value,
      "play/db/ebean/**"
    ),
    jacocoReportSettings := JacocoReportSettings(
      "Jacoco Coverage Report",
      None,
      JacocoThresholds(),
      Seq(JacocoReportFormats.XML),
      "utf-8"
    )
  )

lazy val plugin = project
  .in(file("sbt-play-ebean"))
  .enablePlugins(SbtPlugin)
  .disablePlugins(MimaPlugin)
  .settings(
    credentials += Credentials(
      "Sonatype Nexus Repository Manager",
      "oss.sonatype.org",
      System.getenv("OSS_ST_USERNAME"),
      System.getenv("OSS_ST_PASSWORD")
    ),
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
      else Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    homepage := Some(url(s"https://github.com/lapidus79/play-ebean")),
    publishMavenStyle := true,
    Test / publishArtifact := false,
    pomIncludeRepository := { _ => false },
    pomExtra :=
      <scm>
        <url>git@github.com:lapidus79/play-ebean.git</url>
        <connection>scm:git:git@github.com:lapidus79/play-ebean.git</connection>
      </scm>
        <developers>
          <developer>
            <id>playframework</id>
            <name>Play Framework Team</name>
            <url>https://github.com/playframework</url>
          </developer>
          <developer>
            <id>lapidus79</id>
            <name>Lapidus79</name>
            <url>https://github.com/lapidus79</url>
          </developer>
        </developers>
  )
  .settings(
    name := "sbt-play-ebean",
    organization := "io.github.lapidus79",
    Dependencies.plugin,
    addSbtPlugin("com.typesafe.play" % "sbt-plugin" % Versions.play),
    crossScalaVersions := Seq(scala212),

    Compile / resourceGenerators += generateVersionFile.taskValue,
    scriptedLaunchOpts ++= Seq(
      s"-Dscala.version=${scalaVersion.value}",
      s"-Dscala.crossVersions=${(core / crossScalaVersions).value.mkString(",")}",
      s"-Dproject.version=${version.value}"
    ),
    scriptedBufferLog := false,
    scriptedDependencies := (()),
    sources in(Compile, doc) := Seq(),
  )

// playBuildRepoName in ThisBuild := "play-ebean"
// playBuildExtraTests := {
//  (scripted in plugin).toTask("").value
// }
// playBuildExtraPublish := {
//  (PgpKeys.publishSigned in plugin).value
// }

// Dependencies
lazy val ebeanDeps = Seq(
  "io.ebean" % "ebean" % Versions.ebean,
  "io.ebean" % "ebean-agent" % Versions.ebeanAgent
  // "io.ebean" % "ebean-ddl-generator" % Versions.ebean
)

lazy val reflectionDeps = Seq(
  ("org.reflections" % "reflections" % "0.9.12")
    .exclude("com.google.code.findbugs", "annotations")
    .classifier("")
)

lazy val playEbeanDeps = ebeanDeps ++ Seq(
  "com.typesafe.play" %% "play-java-jdbc" % Versions.play,
  "com.typesafe.play" %% "play-jdbc-evolutions" % Versions.play,
  "com.typesafe.play" %% "play-guice" % Versions.play % Test,
  "com.typesafe.play" %% "filters-helpers" % Versions.play % Test,
  "com.typesafe.play" %% "play-test" % Versions.play % Test
) ++ reflectionDeps

lazy val sbtPlayEbeanDeps = ebeanDeps ++ Seq(
  "com.typesafe" % "config" % Versions.typesafeConfig
)

def sbtPluginDep(moduleId: ModuleID, sbtVersion: String, scalaVersion: String) = {
  Defaults.sbtPluginExtra(
    moduleId,
    CrossVersion.binarySbtVersion(sbtVersion),
    CrossVersion.binaryScalaVersion(scalaVersion)
  )
}

// Ebean enhancement
def enhanceEbeanClasses(
                         classpath: Classpath,
                         analysis: CompileAnalysis,
                         classDirectory: File,
                         pkg: String
                       ): CompileAnalysis = {
  // Ebean (really hacky sorry)
  val cp = classpath.map(_.data.toURI.toURL).toArray :+ classDirectory.toURI.toURL
  val cl = new java.net.URLClassLoader(cp)
  val t = cl
    .loadClass("io.ebean.enhance.Transformer")
    .getConstructor(classOf[ClassLoader], classOf[String])
    .newInstance(cl, "debug=0")
    .asInstanceOf[AnyRef]
  val ft = cl
    .loadClass("io.ebean.enhance.ant.OfflineFileTransform")
    .getConstructor(
      t.getClass,
      classOf[ClassLoader],
      classOf[String]
    )
    .newInstance(t, ClassLoader.getSystemClassLoader, classDirectory.getAbsolutePath)
    .asInstanceOf[AnyRef]
  ft.getClass.getDeclaredMethod("process", classOf[String]).invoke(ft, pkg)
  analysis
}

// Version file
def generateVersionFile =
  Def.task {
    //val version = (core / Keys.version).value
    val version = "6.2.2"
    val file = (Compile / resourceManaged).value / "play-ebean.version.properties"
    val content = s"play-ebean.version=$version"
    IO.write(file, content)
    Seq(file)
  }
