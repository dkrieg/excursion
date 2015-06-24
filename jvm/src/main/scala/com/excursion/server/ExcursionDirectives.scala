package com.excursion.server

import java.nio.ByteBuffer
import java.time.LocalTime

import akka.actor.ActorSystem
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.StatusCodes.{ NotFound, OK }
import akka.http.scaladsl.model.ws.{ BinaryMessage, Message, TextMessage }
import akka.http.scaladsl.model.{ HttpEntity, HttpResponse }
import akka.http.scaladsl.server.Directives
import akka.stream.FlowMaterializer
import akka.stream.scaladsl.Flow
import akka.stream.stage.{ TerminationDirective, SyncDirective, Context, PushStage }
import akka.util.ByteString
import autowire.Core
import boopickle.{ Pickle, Pickler, Unpickle, Unpickler }
import com.excursion.server.Pages._
import com.excursion.shared.{ ChatMessage, TodoApi }

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

object Router extends autowire.Server[ByteBuffer, Unpickler, Pickler] {
  def read[Result: Unpickler](p: ByteBuffer) = Unpickle[Result].fromBytes(p)

  def write[Result: Pickler](r: Result) = Pickle.intoBytes(r)
}

class ExcursionDirectives(implicit production: Boolean = false,
  fm: FlowMaterializer,
  system: ActorSystem,
  todoService: TodoApi,
  chatSession: ChatSession,
  dispatcher: ExecutionContextExecutor) extends Directives {

  system.scheduler.schedule(15.second, 15.second) {
    val time = LocalTime.now.toString
    chatSession.injectMessage(ChatMessage(
      user = "clock",
      text = s"Bling! The time is $time.",
      time = time))
  }

  val page =
    if (production) template("fullopt") _
    else template("fastopt") _

  val route =
    get {
      pathSingleSlash {
        complete(HttpResponse(entity = HttpEntity(`text/html`, page(todo).render)))
      } ~ pathPrefix("srcmaps") {
        if (production) complete(NotFound)
        else getFromDirectory("../")
      } ~ getFromResourceDirectory("web")
    } ~ post {
      path("api" / Segments) { apiCalls ⇒
        entity(as[ByteString]) { data ⇒
          ctx ⇒
            Router.route[TodoApi](todoService) {
              Core.Request(apiCalls, Unpickle[Map[String, ByteBuffer]].fromBytes(data.asByteBuffer))
            }.flatMap(resBuf ⇒ ctx.complete(HttpResponse(entity = HttpEntity(ByteString(resBuf)))))
        }
      } ~ path("logging") {
        entity(as[String]) { msg ⇒
          println(s"ClientLog: $msg")
          complete(OK)
        }
      }
    } ~ path("chat") {
      parameter('name) { name ⇒
        println(s"chat flow initiated. $name")
        handleWebsocketMessages(webSocketChatFlow(name))
      }
    }

  def webSocketChatFlow(sender: String): Flow[Message, Message, Unit] =
    Flow[Message].
      collect { case TextMessage.Strict(msg) ⇒ msg }.
      via(chatSession.flow(sender)).
      map { case msg: ChatMessage ⇒ BinaryMessage.Strict(ByteString(Pickle.intoBytes(msg))) }.
      via(reportErrorsFlow)

  def reportErrorsFlow[T]: Flow[T, T, Unit] =
    Flow[T].
      transform(
        () ⇒ new PushStage[T, T] {
          def onPush(elem: T, ctx: Context[T]): SyncDirective = ctx.push(elem)

          override def onUpstreamFailure(cause: Throwable, ctx: Context[T]): TerminationDirective = {
            println(s"WS stream failed with $cause")
            super.onUpstreamFailure(cause, ctx)
          }
        })
}
