import net.virtualvoid.sbt.graph.Plugin
import org.scalastyle.sbt.ScalastylePlugin
import NewmanReleaseSteps._
import sbtrelease._
import ReleaseStateTransformations._
import ReleasePlugin._
import ReleaseKeys._
import sbt._

name := "newman"

organization := "com.stackmob"

scalaVersion := "2.9.1"

crossScalaVersions := Seq("2.9.1")

scalacOptions ++= Seq("-unchecked", "-deprecation")

libraryDependencies ++= {
    val httpCoreVersion = "4.2.1"
    val httpClientVersion = "4.2.1"
    val scalaCheckVersion = "1.9"
    val specs2Version = "1.9"
    val mockitoVersion = "1.9.0"
    val specs2ScalazCoreVersion = "6.0.1"
    val scalazVersion = "6.0.3"
    val liftJsonVersion = "2.4"
    Seq(
        "org.scalaz" %% "scalaz-core" % scalazVersion,
        "org.apache.httpcomponents" % "httpcore" % httpCoreVersion,
        "org.apache.httpcomponents" % "httpclient" % httpClientVersion,
        "net.liftweb" %% "lift-json-scalaz" % liftJsonVersion,
        "org.scala-tools.testing" %% "scalacheck" % scalaCheckVersion % "test",
        "org.specs2" %% "specs2" % specs2Version % "test",
        "org.pegdown" % "pegdown" % "1.0.2" % "test",
        "org.mockito" % "mockito-all" % mockitoVersion % "test",
        "org.specs2" %% "specs2-scalaz-core" % specs2ScalazCoreVersion % "test"
    )
}

logBuffered := false

ScalastylePlugin.Settings

Plugin.graphSettings

releaseSettings

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  setLaunchConfigReleaseVersion,
  setReadmeReleaseVersion,
  tagRelease,
  publishArtifacts,
  setNextVersion,
  commitNextVersion,
  setLaunchConfigNextVersion,
  pushChanges,
)

publishTo <<= (version) { version: String =>
    val nexus = "https://oss.sonatype.org/"
    if (version.trim.endsWith("SNAPSHOT")) {
        Some("snapshots" at nexus + "content/repositories/snapshots")
     } else {
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    }
}

publishMavenStyle := true

publishArtifact in Test := true

testOptions in Test += Tests.Argument("html", "console")

pomIncludeRepository := { _ => false }

pomExtra := (
  <url>https://github.com/stackmob/newman</url>
  <licenses>
    <license>
      <name>Apache 2</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:stackmob/newman.git</url>
    <connection>scm:git:git@github.com:stackmob/newman.git</connection>
  </scm>
  <developers>
    <developer>
      <id>arschles</id>
      <name>Aaron Schlesinger</name>
      <url>http://www.stackmob.com</url>
    </developer>
    <developer>
      <id>devmage</id>
      <name>Andrew Harris</name>
      <url>http://www.stackmob.com</url>
    </developer>
    <developer>
      <id>taylorleese</id>
      <name>Taylor Leese</name>
      <url>http://www.stackmob.com</url>
    </developer>
    <developer>
      <id>kelseyq</id>
      <name>Kelsey Innis</name>
      <url>http://www.stackmob.com</url>
    </developer>
    <developer>
        <id>jrwest</id>
        <name>Jordan West</name>
        <url>http://github.com/jrwest</url>
    </developer>
  </developers>
)
