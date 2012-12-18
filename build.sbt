name := "newman"

organization := "com.stackmob"

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
    val commonVersion = "0.13.0"
    Seq(
        "org.scalaz" %% "scalaz-core" % scalazVersion,
        "com.stackmob" %% "stackmob-common" % commonVersion,
        "org.apache.httpcomponents" % "httpcore" % httpCoreVersion,
        "org.apache.httpcomponents" % "httpclient" % httpClientVersion,
        "org.scala-tools.testing" %% "scalacheck" % scalaCheckVersion % "test",
        "org.specs2" %% "specs2" % specs2Version % "test",
        "org.pegdown" % "pegdown" % "1.0.2" % "test",
        "org.mockito" % "mockito-all" % mockitoVersion % "test",
        "org.specs2" %% "specs2-scalaz-core" % specs2ScalazCoreVersion % "test" 
    )
}

logBuffered := false

releaseSettings

net.virtualvoid.sbt.graph.Plugin.graphSettings

org.scalastyle.sbt.ScalastylePlugin.Settings

