import Dependencies.ScalaVersions.scala212
import Dependencies.ScalaVersions.scala213
import Dependencies.Versions
import sbt.Append.appendSeq
import xsbti.compile.CompileAnalysis
import sbt.Keys.organization
import sbt.ThisBuild

// Customise sbt-dynver's behaviour to make it work with tags which aren't v-prefixed
dynverVTagPrefix in ThisBuild := false


val scala213 = "2.13.1"
val Versions = new {
  val play: String = playVersion(sys.props.getOrElse("play.version", "2.8.8"))
  val playEnhancer = "1.2.2"
  val ebean = "12.4.2" // when we switch to 12.5+/12.8.1 we need to include "io.ebean" % "ebean-ddl-generator" % Versions.ebean, as dep
  val ebeanAgent = "12.4.2"
  val typesafeConfig = "1.3.4"
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
    organization.value %% name.value % "6.0.0" //previousStableVersion.value
    //.getOrElse(throw new Error("Unable to determine previous version"))
  ),
)

credentials += Credentials(
  "GnuPG Key ID",
  "gpg",
  "5326F9F72B0BAE3316FC9914B3999571BAB4F9CD", // key identifier
  "ignored" // this field is ignored; passwords are supplied by pinentry
)

lazy val root = project
  .in(file("."))
  .aggregate(core, plugin)
  .disablePlugins(MimaPlugin)
  .settings(
    name := "play-ebean-root",
    publish / skip := true,
      crossScalaVersions := Nil,
    releaseCrossBuild := true,
    organization := "io.github.lapidus79",
    homepage := Some(url(s"https://github.com/lapidus79/play-ebean")),
    sources in (Compile, doc) := Seq()
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
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
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
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    sources in (Compile, doc) := Seq(),
    homepage := Some(url(s"https://github.com/lapidus79/play-ebean")),
    publishMavenStyle := true,
    publishArtifact in Test := false,
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
    useGpg := true,
    Dependencies.ebean,
    mimaSettings,
    compile in Compile := enhanceEbeanClasses(
      (dependencyClasspath in Compile).value,
      (compile in Compile).value,
      (classDirectory in Compile).value,
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
  .enablePlugins(PlaySbtPlugin)
  .settings(
    credentials += Credentials(
      "Sonatype Nexus Repository Manager",
      "oss.sonatype.org",
      System.getenv("OSS_ST_USERNAME"),
      System.getenv("OSS_ST_PASSWORD")
    ),
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    homepage := Some(url(s"https://github.com/lapidus79/play-ebean")),
    publishMavenStyle := true,
    publishArtifact in Test := false,
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
    libraryDependencies ++= sbtPlayEbeanDeps,
    useGpg := true,
    sources in (Compile, doc) := Seq(),
    libraryDependencies ++= Seq(
      sbtPluginDep("com.typesafe.sbt" % "sbt-play-enhancer" % Versions.playEnhancer, (sbtVersion in pluginCrossBuild).value, scalaVersion.value),
      sbtPluginDep("com.typesafe.play" % "sbt-plugin" % Versions.play, (sbtVersion in pluginCrossBuild).value, scalaVersion.value)
    ),

    organization := "com.typesafe.sbt",
    Dependencies.plugin,
    addSbtPlugin("com.typesafe.play" % "sbt-plugin" % Versions.play),
    crossScalaVersions := Seq(scala212),
    resourceGenerators in Compile += generateVersionFile.taskValue,
    scriptedLaunchOpts ++= Seq(
      s"-Dscala.version=${scalaVersion.value}",
      s"-Dscala.crossVersions=${(crossScalaVersions in core).value.mkString(",")}",
      s"-Dproject.version=${version.value}",
    ),
    scriptedBufferLog := false,
    scriptedDependencies := (())
  )

playBuildRepoName in ThisBuild := "play-ebean"
// playBuildExtraTests := {
//  (scripted in plugin).toTask("").value
// }
playBuildExtraPublish := {
  (PgpKeys.publishSigned in plugin).value
}

// Dependencies
lazy val ebeanDeps = Seq(
  "io.ebean" % "ebean" % Versions.ebean,
  "io.ebean" % "ebean-agent" % Versions.ebeanAgent
  // "io.ebean" % "ebean-ddl-generator" % Versions.ebean
)

lazy val reflectionDeps = Seq(
  ("org.reflections" % "reflections" % "0.9.11")
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
    val version = (Keys.version in core).value
    val file    = (resourceManaged in Compile).value / "play-ebean.version.properties"
    val content = s"play-ebean.version=$version"
    IO.write(file, content)
    Seq(file)
  }


// Notes
// gpg contains key
// make sure sonatype creds are set as env variables
// make sure export GPG_TTY=$(tty) (otherwise https://github.com/keybase/keybase-issues/issues/2798)
// sbt clean compile
// sbt +publishLocal +plugin/test +plugin/scripted
// sbt +publishSigned +plugin/test +plugin/scripted
// once published you need to go to my sonatype and manually publish the release candidate
// https://oss.sonatype.org/#welcome
// Go to staging repositories
// You then need to "close" the release candidate which will trigger scans
// After success you can perform actual release
