import sbt._
import sbt.{Build => SbtBuild}
import sbt.Keys._
import scala.scalajs.sbtplugin.ScalaJSPlugin._
import scala.scalajs.sbtplugin.ScalaJSPlugin.ScalaJSKeys._
import sbtunidoc.Plugin._
import sbtunidoc.Plugin.UnidocKeys._

object Build extends SbtBuild {
  val projectVersion = "0.14-SNAPSHOT"

  val sharedSettings = Defaults.defaultSettings ++ Seq(
    organization := "org.monifu",
    version := projectVersion,

    scalaVersion := "2.11.2",
    crossScalaVersions ++= Seq("2.10.4"),

    initialize := {
       val _ = initialize.value // run the previous initialization
       val classVersion = sys.props("java.class.version")
       val specVersion = sys.props("java.specification.version")
       assert(
        classVersion.toDouble >= 50 && specVersion.toDouble >= 1.6,
        s"JDK version 6 or newer is required for building this project " + 
        s"(SBT instance running on top of JDK $specVersion with class version $classVersion)")
    },

    scalacOptions ++= Seq(
      "-unchecked", "-deprecation", "-feature", "-Xlint", "-target:jvm-1.6", "-Yinline-warnings",
      "-optimise", "-Ywarn-adapted-args", "-Ywarn-dead-code", "-Ywarn-inaccessible",
      "-Ywarn-nullary-override", "-Ywarn-nullary-unit"
    ),

    scalacOptions <<= baseDirectory.map { bd => Seq("-sourcepath", bd.getAbsolutePath) },
    scalacOptions in (ScalaUnidoc, unidoc) <<= baseDirectory.map { bd =>
      Seq(
        "-Ymacro-no-expand",
        "-sourcepath", bd.getAbsolutePath
      )
    },

    parallelExecution in Test := false,

    resolvers ++= Seq(
      "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases",
      Resolver.sonatypeRepo("releases")
    ),

    // -- Settings meant for deployment on oss.sonatype.org

    publishMavenStyle := true,

    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },

    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false }, // removes optional dependencies
    
    pomExtra :=
      <url>http://www.monifu.org/</url>
      <licenses>
        <license>
          <name>Apache License, Version 2.0</name>
          <url>https://www.apache.org/licenses/LICENSE-2.0</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:monifu/monifu.js.git</url>
        <connection>scm:git:git@github.com:monifu/monifu.js.git</connection>
      </scm>
      <developers>
        <developer>
          <id>alex_ndc</id>
          <name>Alexandru Nedelcu</name>
          <url>https://www.bionicspirit.com/</url>
        </developer>
      </developers>
  )

  // -- Actual Projects

  lazy val monifuJS = Project(id="monifu", base = file("."), 
    settings=sharedSettings ++ unidocSettings ++ Seq(
      scalacOptions in (ScalaUnidoc, sbtunidoc.Plugin.UnidocKeys.unidoc) ++=
        Opts.doc.sourceUrl(s"https://github.com/monifu/monifu.js/tree/v$projectVersion/monifu€{FILE_PATH}.scala"),
      scalacOptions in (ScalaUnidoc, sbtunidoc.Plugin.UnidocKeys.unidoc) ++=
        Opts.doc.title(s"Monifu.js"),
      scalacOptions in (ScalaUnidoc, sbtunidoc.Plugin.UnidocKeys.unidoc) ++=
        Opts.doc.version(s"$projectVersion"),
      scalacOptions in (ScalaUnidoc, sbtunidoc.Plugin.UnidocKeys.unidoc) ++= 
        Seq("-doc-root-content", "rootdoc.txt")
    ))
    .dependsOn(monifuCoreJS, monifuRxJS).aggregate(monifuCoreJS, monifuRxJS)

  lazy val monifuCoreJS = Project(
    id = "monifu-core",
    base = file("monifu-core"),
    settings = sharedSettings ++ scalaJSSettings ++ Seq(
      libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _ % "compile"),
      libraryDependencies ++= Seq(
        "org.scala-lang.modules.scalajs" %% "scalajs-jasmine-test-framework" % scalaJSVersion % "test"
      )
    )
  )

  lazy val monifuRxJS = Project(
    id = "monifu-rx",
    base = file("monifu-rx"),
    settings = sharedSettings ++ scalaJSSettings ++ Seq(
      libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _ % "compile"),
      libraryDependencies ++= Seq(
        "org.scala-lang.modules.scalajs" %% "scalajs-jasmine-test-framework" % scalaJSVersion % "test"
      )
    )
  ).dependsOn(monifuCoreJS) 
}
