package us.theatr.akka.quartz

import akka.actor.{ActorRef, Actor, Props}
import akka.event.Logging
import org.quartz.impl.StdSchedulerFactory
import java.util.Properties


case class AddCronSchedule(to: ActorRef, cron: String, message: Any, reply: Boolean)
case class AddCronScheduleResult(acs: AddCronSchedule, result: Boolean)


class QuartzActor extends Actor {
	val log = Logging(context.system, this)

	// Create a sane default quartz scheduler
	private[this] val props = new Properties()
	props.setProperty("org.quartz.scheduler.instanceName", context.self.path.name)
	props.setProperty("org.quartz.threadPool.threadCount", "1")
	props.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore")

	val scheduler = new StdSchedulerFactory(props).getScheduler

	override def preStart() {
		scheduler.start()
	}

	override def postStop() {
		scheduler.shutdown()
	}

	def receive = {
		case _ => //
	}


}
