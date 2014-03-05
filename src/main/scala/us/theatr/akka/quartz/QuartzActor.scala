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


/**
 * Message to add a cron scheduler. Send this to the QuartzActor
 * @param to The ActorRef describing the desination actor
 * @param cron A string Cron expression
 * @param message Any message
 * @param reply Whether to give a reply to this message indicating success or failure (optional)
 */
case class AddCronSchedule(to: ActorRef, cron: String, message: Any, reply: Boolean = false, spigot: Spigot = OpenSpigot)

trait AddCronScheduleResult

/**
 * Indicates success for a scheduler add action.
 * @param cancel The cancellable allows the job to be removed later. Can be invoked directly -
 *               canceling will send an internal RemoveJob message
 */
case class AddCronScheduleSuccess(cancel: Cancellable) extends AddCronScheduleResult

/**
 * Indicates the job couldn't be added. Usually due to a bad cron expression.
 * @param reason The reason
 */
case class AddCronScheduleFailure(reason: Throwable) extends AddCronScheduleResult

/**
 * Remove a job based upon the Cancellable returned from a success call.
 * @param cancel
 */
case class RemoveJob(cancel: Cancellable)


/**
 * Internal class to make Quartz work.
 * This should be in QuartzActor, but for some reason Quartz
 * ends up with a construction error when it is.
 */
private class QuartzIsNotScalaExecutor() extends Job {
	def execute(ctx: JobExecutionContext) {
		val jdm = ctx.getJobDetail.getJobDataMap() // Really?
		val spigot = jdm.get("spigot").asInstanceOf[Spigot]
		if (spigot.open) {
			val msg = jdm.get("message")
			val actor = jdm.get("actor").asInstanceOf[ActorRef]
			actor ! msg
		}
	}
}

trait Spigot {
	def open: Boolean
}

object OpenSpigot extends Spigot {
  val open = true
}

/**
 * The base quartz scheduling actor. Handles a single quartz scheduler
 * and processes Add and Remove messages.
 */
class QuartzActor extends Actor {
	val log = Logging(context.system, this)

	// Create a sane default quartz scheduler
	private[this] val props = new Properties()
	props.setProperty("org.quartz.scheduler.instanceName", context.self.path.name)
	props.setProperty("org.quartz.threadPool.threadCount", "1")
	props.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore")
	props.setProperty("org.quartz.scheduler.skipUpdateCheck", "true")	// Whoever thought this was smart shall be shot

	val scheduler = new StdSchedulerFactory(props).getScheduler


	/**
	 * Cancellable to later kill the job. Yes this is mutable, I'm sorry.
	 * @param job
	 */
	class CancelSchedule(val job: JobKey, val trig: TriggerKey) extends Cancellable {
		var cancelled = false

		def isCancelled: Boolean = cancelled

		def cancel() = {
			context.self ! RemoveJob(this)
			cancelled = true
			true
		}

	}

	override def preStart() {
		scheduler.start()
		log.info("Scheduler started")
	}

	override def postStop() {
		scheduler.shutdown()
	}

	// Largely imperative glue code to make quartz work :)
	def receive = {
		case RemoveJob(cancel) => cancel match {
			case cs: CancelSchedule => scheduler.deleteJob(cs.job); cs.cancelled = true
			case _ => log.error("Incorrect cancelable sent")
		}
		case AddCronSchedule(to, cron, message, reply, spigot) =>
			// Try to derive a unique name for this job
			// Using hashcode is odd, suggestions for something better?
			val jobkey = new JobKey("%X".format((to.toString() + message.toString + cron + "job").hashCode))
			// Perhaps just a string is better :)
			val trigkey = new TriggerKey(to.toString() + message.toString + cron + "trigger")
			// We use JobDataMaps to pass data to the newly created job runner class
			val jd = org.quartz.JobBuilder.newJob(classOf[QuartzIsNotScalaExecutor])
			val jdm = new JobDataMap()
			jdm.put("spigot", spigot)
			jdm.put("message", message)
			jdm.put("actor", to)
			val job = jd.usingJobData(jdm).withIdentity(jobkey).build()

			try {
				scheduler.scheduleJob(job, org.quartz.TriggerBuilder.newTrigger().startNow()
					.withIdentity(trigkey).forJob(job)
					.withSchedule(org.quartz.CronScheduleBuilder.cronSchedule(cron)).build())

				if (reply)
					context.sender ! AddCronScheduleSuccess(new CancelSchedule(jobkey, trigkey))

			} catch { // Quartz will drop a throwable if you give it an invalid cron expression - pass that info on
				case e: Throwable =>
					log.error("Quartz failed to add a task: ", e)
					if (reply)
						context.sender ! AddCronScheduleFailure(e)

			}
		// I'm relatively unhappy with the two message replies, but it works

		case _ => //
	}


}
