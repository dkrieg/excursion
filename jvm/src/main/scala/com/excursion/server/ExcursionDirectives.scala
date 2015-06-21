package com.excursion.server

import java.nio.ByteBuffer

import akka.http.scaladsl.marshalling.MediaTypeOverrider._
import akka.http.scaladsl.model.HttpEntity.Strict
import akka.http.scaladsl.model.{RequestEntity, HttpEntity, HttpResponse}
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.StatusCodes.{NotFound, OK, InternalServerError}
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.server.{RouteResult, StandardRoute, Route, Directives}
import akka.stream.FlowMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import autowire.Core
import boopickle.{Pickle, Pickler, Unpickle, Unpickler}
import com.excursion.shared.TodoApi

import scala.concurrent.{Awaitable, Await, ExecutionContextExecutor, Future}
import scala.util.{Success, Try, Failure}
import scalatags.Text

import scala.concurrent.duration._

object Router extends autowire.Server[ByteString, Unpickler, Pickler] {
  def read[Result: Unpickler](p: ByteString) = Unpickle[Result].fromBytes(p.asByteBuffer)
  def write[Result: Pickler](r: Result) = ByteString(Pickle.intoBytes(r))
}

object Pages {
  import scalatags.Text.all._

  def template(ops: String)(program: Text.TypedTag[String]): Text.TypedTag[String] = {
    html(
      head(
        meta(content:="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no", name:="viewport"),
        link(href:="stylesheets/main.min.css", rel:="stylesheet", `type`:="text/css")
      ),
      body(
        script(src:=s"js/excursion-$ops.js"),
        script(src:="js/excursion-jsdeps.js"),
        program
      )
    )
  }

  val todo = script("com.excursion.client.TodoApp().main()")
}

class ExcursionDirectives(implicit production: Boolean = false,
                          fm: FlowMaterializer,
                          todoService: TodoApiService,
                          dispatcher: ExecutionContextExecutor) extends Directives {
  import Pages._

  val page =
    if(production) template("fullopt") _
    else template("fastopt") _

  val route =
    get {
      pathSingleSlash {
        complete(HttpResponse(entity = HttpEntity(`text/html`, page(todo).render)))
      } ~ pathPrefix("srcmaps") {
        if(production) complete(NotFound)
        else getFromDirectory("../")
      } ~ getFromResourceDirectory("web")
    } ~ post {
      path("api" / Segments) { s =>
        extract(_.request.entity) { entity: RequestEntity => ctx =>
          val future = entity.toStrict(5.seconds).map { se: Strict =>
            Router.route[TodoApi](todoService) {
              val values = Unpickle[Map[String, ByteBuffer]].fromBytes(se.data.asByteBuffer).mapValues(ByteString(_))
//              println(values)
              Core.Request(s, values)
            }.map(responseData => HttpResponse(entity = HttpEntity(`application/octet-stream`, responseData)))
          }
          Await.result(future, 5.seconds).map {
            RouteResult.Complete
          }
        }
      } ~ path("logging") {
        entity(as[String]) { msg =>
          ctx =>
            println(s"ClientLog: $msg")
            ctx.complete(OK)
        }
      }
    }
}
