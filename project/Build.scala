import sbt._
import Keys._

object Build extends sbt.Build {
	import Dependencies._

	lazy val myProject = Project("akka-quartz", file("."))
		.settings(
		organization  := "us.theatr",
		version       := "0.3.0-SNAPSHOT",
		scalaVersion  := "2.11.2",
		crossScalaVersions := Seq("2.10.4", "2.11.2"),
		scalacOptions := Seq("-deprecation", "-encoding", "utf8"),
		resolvers     ++= Dependencies.resolutionRepos,
		publishTo := Some(Resolver.file("file", new File("../../ivy-repo"))),
		libraryDependencies ++=
		  provided(akkaActor, slf4j, logback) ++ compile(quartz) ++
		  test(specs2, akkaTestkit)

	)
}

