import sbt._

object Dependencies {
	val resolutionRepos = Seq(
		"Sonatype scala" at "https://oss.sonatype.org/content/groups/scala-tools/",
		"Sonatype snaps" at "https://oss.sonatype.org/content/repositories/snapshots/",
		"Typesafe repo" at "http://repo.typesafe.com/typesafe/releases/"
	)

	def compile   (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "compile")
	def provided  (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "provided")
	def test      (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "test")
	def runtime   (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "runtime")
	def container (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "container")

	object V {
		val akka     = "2.3.4"
		val quartz 	 = "2.1.7"
	}

	val specs2      = "org.specs2"           %% "specs2"          % "2.3.13"
	val akkaActor   = "com.typesafe.akka"    %% "akka-actor"      % V.akka
	val akkaTestkit = "com.typesafe.akka"    %% "akka-testkit"    % V.akka
	val quartz		  = "org.quartz-scheduler" %  "quartz"		        % V.quartz
	val logback     = "ch.qos.logback"       %  "logback-classic" % "1.0.0"
	val slf4j       = "org.slf4j"            %  "slf4j-api"       % "1.6.4"

}
