import sbt.inc.Analysis
import interplay.ScalaVersions.scala212
import sbt.Keys.organization
import sbt.ThisBuild


val scala213 = "2.13.1"
val Versions = new {
  val play: String = playVersion(sys.props.getOrElse("play.version", "2.8.2"))
  val playEnhancer = "1.2.2"
  val ebean = "12.3.7"
  val ebeanAgent = "12.3.4"
  val typesafeConfig = "1.3.4"
}

credentials += Credentials(
  "GnuPG Key ID",
  "gpg",
  "5326F9F72B0BAE3316FC9914B3999571BAB4F9CD", // key identifier
  "ignored" // this field is ignored; passwords are supplied by pinentry
)

lazy val root = project
  .in(file("."))
  .enablePlugins(PlayRootProject, CrossPerProjectPlugin)
  .aggregate(core, plugin)
  .settings(
    name := "play-ebean-root",
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
  .enablePlugins(Playdoc, PlayLibrary, JacocoPlugin)
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
    compile in Compile := enhanceEbeanClasses(
      (dependencyClasspath in Compile).value,
      (compile in Compile).value,
      (classDirectory in Compile).value,
      "play/db/ebean/**"
    ),
    jacocoReportSettings := JacocoReportSettings("Jacoco Coverage Report", None, JacocoThresholds(), Seq(JacocoReportFormats.XML), "utf-8")
  )

lazy val plugin = project
  .in(file("sbt-play-ebean"))
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

    resourceGenerators in Compile += generateVersionFile.taskValue,
    scriptedLaunchOpts ++= Seq("-Dplay-ebean.version=" + version.value),
    scriptedDependencies := {
      val () = publishLocal.value
    }
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

// sbt deps
def sbtPluginDep(moduleId: ModuleID, sbtVersion: String, scalaVersion: String) = {
  Defaults.sbtPluginExtra(moduleId, CrossVersion.binarySbtVersion(sbtVersion), CrossVersion.binaryScalaVersion(scalaVersion))
}

// Ebean enhancement
def enhanceEbeanClasses(classpath: Classpath, analysis: Analysis, classDirectory: File, pkg: String): Analysis = {
  // Ebean (really hacky sorry)
  val cp = classpath.map(_.data.toURI.toURL).toArray :+ classDirectory.toURI.toURL
  val cl = new java.net.URLClassLoader(cp)
  val t = cl.loadClass("io.ebean.enhance.Transformer").getConstructor(classOf[ClassLoader], classOf[String]).newInstance(cl, "debug=0").asInstanceOf[AnyRef]
  val ft = cl.loadClass("io.ebean.enhance.ant.OfflineFileTransform").getConstructor(
    t.getClass, classOf[ClassLoader], classOf[String]
  ).newInstance(t, ClassLoader.getSystemClassLoader, classDirectory.getAbsolutePath).asInstanceOf[AnyRef]
  ft.getClass.getDeclaredMethod("process", classOf[String]).invoke(ft, pkg)
  analysis
}

// Version file

def generateVersionFile = Def.task {
  val version = (Keys.version in core).value
  val file = (resourceManaged in Compile).value / "play-ebean.version.properties"
  val content = s"play-ebean.version=$version"
  IO.write(file, content)
  Seq(file)
}


// Notes
// sbt clean compile
// sbt +publishLocal +plugin/test +plugin/scripted
// sbt +publishSigned +plugin/test +plugin/scripted
