package com.excursion.server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route.handlerFlow
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.Sink
import com.typesafe.config.Config

object Boot extends App {
  implicit val system = ActorSystem()
  implicit val fm = ActorFlowMaterializer()
  implicit val context = system.dispatcher

  val conf = Conf(system.settings.config)
  implicit val PRODUCTION_MODE = conf.production
  implicit val todoService = TodoApiService()
  implicit val chatService = ChatService()

  val flow = Http(system).bind(conf.host, conf.port).to(Sink.foreach {
    _ handleWith handlerFlow(new ExcursionDirectives().route)
  })

  flow.run
  println(
    s"""
       | Accepting HTTP connections on port ${conf.port}
       | production mode is ${if(conf.production) "on" else "off"}
       | host is ${conf.host}
       | """.stripMargin)
}

trait Conf {
  val host: String
  val port: Int
  val production: Boolean
}

object Conf {
  def apply(config: Config) = new Conf {
    override val host = config.getString("app.host")
    override val port = config.getInt("app.port")
    override val production = config.getBoolean("app.production")
  }
}
