package us.theatr.akka.quartz

/*
Copyright 2012 Yann Ramin

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
import akka.actor.{Cancellable, ActorRef, Actor}
import akka.event.Logging
import org.quartz.impl.StdSchedulerFactory
import java.util.Properties
import org.quartz._
import utils.Key


case class AddCronSchedule(to: ActorRef, cron: String, message: Any, reply: Boolean = false)
case class AddCronScheduleResult(cancel: Cancellable)
case class RemoveJob(cancel: Cancellable)

private class QuartzIsNotScalaExecutor() extends Job {
	def execute(ctx: JobExecutionContext) {
		val jdm = ctx.getJobDetail.getJobDataMap() // Really?
		val msg = jdm.get("message")
		val actor = jdm.get("actor").asInstanceOf[ActorRef]
		actor ! msg
	}
}

class QuartzActor extends Actor {
	val log = Logging(context.system, this)

	// Create a sane default quartz scheduler
	private[this] val props = new Properties()
	props.setProperty("org.quartz.scheduler.instanceName", context.self.path.name)
	props.setProperty("org.quartz.threadPool.threadCount", "1")
	props.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore")
	props.setProperty("org.quartz.scheduler.skipUpdateCheck", "true") // Whoever thought this was smart shall be shot

	val scheduler = new StdSchedulerFactory(props).getScheduler



	/**
	 * Cancellable to later kill the job. Yes this is mutable, I'm sorry.
	 * @param job
	 */
	class CancelSchedule(val job: JobKey, val trig: TriggerKey) extends Cancellable {
		var cancelled = false
		def isCancelled : Boolean = cancelled
		def cancel() { context.self ! RemoveJob(this) }

	}

	override def preStart() {
		scheduler.start()
		log.info("Scheduler started")
	}

	override def postStop() {
		scheduler.shutdown()
	}

	def receive = {
		case RemoveJob(cancel) => cancel match {
			case cs : CancelSchedule => scheduler.deleteJob(cs.job); cs.cancelled = true
			case _ => log.error("Incorrect cancelable sent")
		}
		case AddCronSchedule(to, cron, message, reply) =>
			// Try to derive a unique name for this job
			val jobkey = new JobKey(Key.DEFAULT_GROUP, "%X".format((to.toString() + message.toString + cron + "job").hashCode))
			val trigkey = new TriggerKey(Key.DEFAULT_GROUP, to.toString() + message.toString + cron + "trigger")

			val jd = org.quartz.JobBuilder.newJob(classOf[QuartzIsNotScalaExecutor])
			val jdm = new JobDataMap()
			jdm.put("message", message)
			jdm.put("actor", to)
			val job = jd.usingJobData(jdm).withIdentity(jobkey).build()

			val trigger = org.quartz.TriggerBuilder.newTrigger().startNow()
				.withIdentity(trigkey).forJob(job)
				.withSchedule(org.quartz.CronScheduleBuilder.cronSchedule(cron)).build()

			scheduler.scheduleJob(job, trigger)
			if (reply) {
				context.sender ! AddCronScheduleResult(new CancelSchedule(jobkey, trigkey))
			}

		case _ => //
	}


}
