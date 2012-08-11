name := "newman"

organization := "com.stackmob"

version := "0.4.1-SNAPSHOT"

scalaVersion := "2.9.1"

crossScalaVersions := Seq("2.9.1")

scalacOptions ++= Seq("-unchecked", "-deprecation")

testOptions in Test += Tests.Argument("html", "console")

publishArtifact in Test := true

publishTo <<= (version) { version: String =>
  val stackmobNexus = "http://nexus/nexus/content/repositories/"
  if (version.trim.endsWith("SNAPSHOT")) Some("snapshots" at stackmobNexus + "snapshots/")
  else                                   Some("releases"  at stackmobNexus + "releases/")
}

resolvers ++= Seq("StackMob Nexus" at "http://nexus/nexus/content/groups/public")

libraryDependencies ++= {
    val httpCoreVersion = "4.2.1"
    val httpClientVersion = "4.2.1"
    val scalaCheckVersion = "1.9"
    val specs2Version = "1.9"
    val mockitoVersion = "1.9.0"
    val specs2ScalazCoreVersion = "6.0.1"
    val scalazVersion = "6.0.3"
    val stackmobCommonVersion = "0.4.0-SNAPSHOT"
    Seq(
        "org.scalaz" %% "scalaz-core" % scalazVersion withSources(),
        "com.stackmob" %% "stackmob-common" % stackmobCommonVersion  withSources() changing(),
        "org.apache.httpcomponents" % "httpcore" % httpCoreVersion withSources(),
        "org.apache.httpcomponents" % "httpclient" % httpClientVersion withSources(),
        "org.scala-tools.testing" %% "scalacheck" % scalaCheckVersion % "test" withSources(),
        "org.specs2" %% "specs2" % specs2Version % "test" withSources(),
        "org.pegdown" % "pegdown" % "1.0.2" % "test" withSources(),
        "org.mockito" % "mockito-all" % mockitoVersion % "test" withSources(),
        "org.specs2" %% "specs2-scalaz-core" % specs2ScalazCoreVersion % "test" withSources()
    )
}

logBuffered := false

net.virtualvoid.sbt.graph.Plugin.graphSettings

