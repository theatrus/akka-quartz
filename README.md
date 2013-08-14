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

(0.3.0-SNAPSHOT is compatible with Akka 2.2)

## Using ##

Include the following repository to you Ivy/Maven/SBT file:

    "theatr.us" at "http://repo.theatr.us"

Include the following dependency in your `build.sbt`:

    "us.theatr" % "akka-quartz" %% "0.2.0"

(along with any needed Akka dependencies - not included by default)

Create a QuartzActor:

    import us.theatr.akka.quartz._
    val quartzActor = system.actorOf(Props[QuartzActor])

Send it add messages:

    quartzActor ! AddCronSchedule(destinationActorRef, "0/5 * * * * ?", Message())

Now Message() will be delivered to destinationActorRef every 5 seconds.

The Spigot can turn the cron job on or off. All you need to do is to implement

    trait Spigot {
      def open: Boolean
    }

And add it to the ```AddCronSchedule```:

    val leaderSpigot = new Spigot {
      def open = zookeeperClient.isLeader()
    }
    quartzActor ! AddCronSchedule(destinationActorRef, "0/5 * * * * ?", Message(), false, leaderSpigot)

Now Message() will be delivered to destinationActorRef every 5 seconds only if zookeeperClient.isLeader() returns true.
This is useful if your service runs in a cluster and you want a single (or some) of the instances in the clusters to run the cron job.

For more information, please see the unit test or consult the JavaDoc/ScalaDoc.

For more documentation about quartz scheduler see
http://quartz-scheduler.org/api/2.0.0/org/quartz/CronTrigger.html

## Observations ##

Quartz really isn't that fantastic - a ton of cruft, non-sensical bugs, and "enterprise" anti-patterns.
But at least it can parse cron expressions, and I have better things to do than re-implement that.
