package us.theatr.akka.quartz

import org.specs2.execute._
import org.specs2.mutable._
import akka.testkit.TestActorRef
import akka.actor.{Props, ActorSystem, Actor}
import akka.util.Timeout
import akka.pattern.ask
import scala.concurrent.duration.Duration
import scala.util.{Success}

class QuartzActor$Test extends Specification  {

	object SpecActors {
		case class Tickle(id: Int)
		case class PopTickle()
		class RecvActor extends Actor {
			var lastMsg : Option[Tickle] = None
			def receive = {
				case a : Tickle => lastMsg = Some(a)
				case PopTickle() => {
					context.sender ! lastMsg
					lastMsg = None
				}
				case c => println("Unknown message on the recvactor!!" + c)
			}
		}
	}

	def withSystem(b : ActorSystem => ResultLike) = {
  	implicit val system = ActorSystem("GAT")
  	try {
  		b(system).toResult
  	} finally {
  	  system.shutdown()
  	}
	}

	"Basic single actors should" should {
   	implicit val timeout = Timeout(Duration(5, "seconds"))

		"add a cron job" in {
			withSystem { implicit system =>
				val ar = TestActorRef(new QuartzActor)
				val recv = TestActorRef(new SpecActors.RecvActor)
				val f = (ar ? AddCronSchedule(recv, "* * * * * ?", SpecActors.Tickle(100), true))
				f.value.get must beLike {
					case Success(t : AddCronScheduleResult) => ok
				}
				Thread.sleep(5000)
				(recv ? SpecActors.PopTickle()).value.get must beLike {
					case Success(Some(SpecActors.Tickle(100))) => ok
				}
			}
		}

		"add a cron job with open spigot" in {
			withSystem { implicit system =>
				val ar = TestActorRef(new QuartzActor)
				val recv = TestActorRef(new SpecActors.RecvActor)
				ar ? AddCronSchedule(recv, "* * * * * ?", SpecActors.Tickle(150), true, new Spigot(){val open = true})
				Thread.sleep(5000)
				(recv ? SpecActors.PopTickle()).value.get must beLike {
					case Success(Some(SpecActors.Tickle(150))) => ok
				}
			}
		}

		"add a cron job with closed spigot" in {
			withSystem { implicit system =>
				val ar = TestActorRef(new QuartzActor)
				val recv = TestActorRef(new SpecActors.RecvActor)
				val f = (ar ? AddCronSchedule(recv, "* * * * * ?", SpecActors.Tickle(100), true, new Spigot(){val open = false}))
				f.value.get must beLike {
					case Success(t : AddCronScheduleResult) => ok
				}
				Thread.sleep(3000)
				(recv ? SpecActors.PopTickle()).value.get must beLike {
					case Success(None) => ok
				}
			}
		}

		"add then cancel messages" in {
			withSystem { implicit system =>
				val ar = TestActorRef(new QuartzActor)
				val recv = TestActorRef(new SpecActors.RecvActor)
				val d = ar ? AddCronSchedule(recv, "4 4 * * * ?", SpecActors.Tickle(200), true)
				val cancel = d.value.get match {
					case Success(AddCronScheduleSuccess(cancel)) => cancel
				}
				cancel.cancel()
				Thread.sleep(100)
				cancel.isCancelled must beEqualTo(true)
			}
		}

		"fail with invalid cron expressions" in {
			withSystem { implicit system =>
				val ar = TestActorRef(new QuartzActor)
				val recv = TestActorRef(new SpecActors.RecvActor)
				(ar ? AddCronSchedule(recv, "clearly invalid", SpecActors.Tickle(300), true)).value.get must beLike {
					case Success(AddCronScheduleFailure(e)) => ok
				}
			}
		}
	}

}
