akka-quartz
===============

The Akka scheduler is limited, and using Apache Camel to run timers is silly.

## Features ##

* Quartz scheduler
* Akka
* Actors
* Fin

### Versions ###

Works with Akka 2.1.x on Scala 2.10.x

## Using ##

Include the following repository to you Ivy/Maven/SBT file:

    http://repo.theatr.us

Include the following dependency in your `build.sbt`:

    "us.theatr" % "akka-quartz" % "0.2.0"

(along with any needed Akka dependencies - not included by default)

Create a QuartzActor:

    val quartzActor = system.actorOf(Props[QuartzActor])

Send it add messages:

    quartzActor ! AddCronSchedule(destinationActorRef, "0/5 * * * * ?", Message())

Now Message() will be delivered to destinationActorRef every 5 seconds.

For more information, please see the unit test or consult the JavaDoc/ScalaDoc.

## Observations ##

Quartz really isn't that fantastic - a ton of cruft, non-sensical bugs, and "enterprise" anti-patterns.
But at least it can parse cron expressions, and I have better things to do than re-implement that.
