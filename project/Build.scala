import sbtassembly.Plugin._
import AssemblyKeys._
import sbt._
import Keys._
import BuildSettings._

object BuildSettings {
  
  val org = "com.stackmob"
  val vsn = "0.1.0"
  val scalaVsn = "2.9.1"
  
  val buildSettings = Defaults.defaultSettings ++ Seq(
    checksums in update := Nil,
    organization := org,
    version := vsn,
    scalaVersion := scalaVsn,
    shellPrompt := ShellPrompt.buildShellPrompt,
    scalacOptions ++= Seq("-deprecation", "-unchecked") 
  )
 
}

object Resolvers {
  val stackmobNexus = "StackMob Nexus" at "http://nexus/nexus/content/groups/public"
  val allResolvers = Seq(stackmobNexus)
}

object Dependencies {
	val apacheHttpCore = "org.apache.httpcomponents" % "httpcore" % "4.2-beta1" withSources()
	val apacheHttpClient = "org.apache.httpcomponents" % "httpclient" % "4.1.2" withSources()
  val scalaCheck = "org.scala-tools.testing" %% "scalacheck" % "1.9" % "test" withSources()
  val specs2 = "org.specs2" %% "specs2" % "1.9" % "test" withSources()
  val mockito = "org.mockito" % "mockito-all" % "1.9.0" % "test" withSources()
  val specs2Core = "org.specs2" %% "specs2-scalaz-core" % "6.0.1" % "test" withSources()
  val scalaz = "org.scalaz" %% "scalaz-core" % "6.0.3" withSources()
  val stackmobCommon = "com.stackmob" %% "stackmob-common" % "0.3.0-SNAPSHOT" withSources() changing()
  
  val allDeps = Seq(apacheHttpCore, apacheHttpClient, scalaCheck, specs2, mockito, specs2Core, scalaz, stackmobCommon)
}

object NewmanBuild extends Build {

  lazy val newman = Project("newman", file("."),
    settings = buildSettings ++ 
      assemblySettings ++ 
      addArtifact(Artifact("newman", "assembly"), assembly) ++
      Seq(
        name := "newman",
        resolvers := Resolvers.allResolvers, 
        libraryDependencies ++= Dependencies.allDeps,
        jarName in assembly <<= (name) { _ + ".jar" }
      )
  )
}

object ShellPrompt {
  object devnull extends ProcessLogger {
    def info (s: => String) {}
    def error (s: => String) { }
    def buffer[T] (f: => T): T = f
  }
  def currBranch = (
    ("git status -sb" lines_! devnull headOption)
      getOrElse "-" stripPrefix "## "
    )

  val buildShellPrompt = {
    (state: State) => {
      val currProject = Project.extract (state).currentProject.id
      "sbt %s:%s> ".format (
        currProject, currBranch
      )
    }
  }
}
