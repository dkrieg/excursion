package com.excursion

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
  val excursion = new ExcursionDirectives()

  val flow = Http(system).bind(conf.host, conf.port).to(Sink.foreach {
    _ handleWith handlerFlow(excursion.route)
  })

  flow.run
}

trait Conf {
  val host: String
  val port: Int
}

object Conf {
  def apply(config: Config) = new Conf {
    override val host = config.getString("app.interface")
    override val port = config.getInt("app.port")
  }
}
