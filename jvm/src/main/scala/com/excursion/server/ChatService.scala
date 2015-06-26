package com.excursion.server

import java.time.LocalTime

import akka.actor._
import akka.stream.OverflowStrategy
import akka.stream.scaladsl._
import com.excursion.shared.ChatMessage

trait ChatSession {
  def flow(sender: String): Flow[String, ChatMessage, Unit]

  def injectMessage(message: ChatMessage): Unit
}

object ChatService {
  def apply()(implicit system: ActorSystem): ChatSession = {
    val chatActor = system.actorOf(Props(new Actor {
      var subscribers = Set.empty[ActorRef]

      def receive: Receive = {
        case NewParticipant(name, subscriber) ⇒
          context.watch(subscriber)
          subscribers += subscriber
          sendAdminMessage(s"$name joined!")
        case msg: ReceivedMessage ⇒ dispatch(msg.toChatMessage)
        case ParticipantLeft(person) ⇒ sendAdminMessage(s"$person left!")
        case Terminated(sub) ⇒ subscribers -= sub // clean up dead subscribers
      }

      def sendAdminMessage(msg: String): Unit = dispatch(ChatMessage(user = "admin", text = msg, time = LocalTime.now.toString))

      def dispatch(msg: ChatMessage): Unit = subscribers.foreach(_ ! msg)
    }))

    def chatInSink(sender: String) = Sink.actorRef[ChatEvent](chatActor, ParticipantLeft(sender))

    val chatOutSource = Source.actorRef[ChatMessage](1, OverflowStrategy.fail)

    new ChatSession {
      def flow(sender: String): Flow[String, ChatMessage, Unit] =
        Flow(chatInSink(sender), chatOutSource)(Keep.right) { implicit b ⇒
          (chatActorIn, chatActorOut) ⇒
            import akka.stream.scaladsl.FlowGraph.Implicits._
            val enveloper = b.add(Flow[String].map(ReceivedMessage(sender, _))) // put the message in an envelope
            val merge = b.add(Merge[ChatEvent](2))
            enveloper ~> merge.in(0)
            b.materializedValue ~> Flow[ActorRef].map(NewParticipant(sender, _)) ~> merge.in(1)
            merge ~> chatActorIn
            (enveloper.inlet, chatActorOut.outlet)
        }.mapMaterializedValue(_ ⇒ ())

      def injectMessage(message: ChatMessage): Unit = chatActor ! message // non-streams interface
    }
  }

  private sealed trait ChatEvent

  private case class NewParticipant(name: String, subscriber: ActorRef) extends ChatEvent

  private case class ParticipantLeft(name: String) extends ChatEvent

  private case class ReceivedMessage(sender: String, message: String) extends ChatEvent {
    def toChatMessage: ChatMessage = ChatMessage(text = message, user = sender, time = LocalTime.now.toString)
  }

}