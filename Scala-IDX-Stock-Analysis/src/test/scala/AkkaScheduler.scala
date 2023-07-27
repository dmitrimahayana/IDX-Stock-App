import akka.actor.Actor

import scala.concurrent.duration._

object AkkaScheduler {
  def main(args: Array[String]): Unit = {
    val Tick = "tick"
    class TickActor extends Actor {
      def receive = {
        case Tick => //Do something
      }
    }
//    val tickActor = system.actorOf(Props(classOf[TickActor], this))
//    //Use system's dispatcher as ExecutionContext
//    import system.dispatcher
//
//    //This will schedule to send the Tick-message
//    //to the tickActor after 0ms repeating every 50ms
//    val cancellable =
//    system.scheduler.scheduleWithFixedDelay(Duration.Zero, 50.milliseconds, tickActor, Tick)
//
//    //This cancels further Ticks to be sent
//    cancellable.cancel()
  }
}
