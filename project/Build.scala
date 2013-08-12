import sbt._
import Keys._

object Build extends sbt.Build {
	import Dependencies._

	lazy val myProject = Project("akka-quartz", file("."))
		.settings(
		organization  := "us.theatr",
		version       := "0.2.0_42.1",
		scalaVersion  := "2.10.1",
		crossScalaVersions := Seq("2.10.1"),
		scalacOptions := Seq("-deprecation", "-encoding", "utf8"),
		resolvers     ++= Dependencies.resolutionRepos,
		publishTo := Some(Resolver.file("file", new File("../../sonatype-work/nexus/storage/towel"))),
		libraryDependencies ++=
		  provided(akkaActor, slf4j, logback) ++ compile(quartz) ++
		  test(specs2, akkaTestkit)

	)
}

