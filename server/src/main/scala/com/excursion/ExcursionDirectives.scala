package com.excursion

import akka.http.scaladsl.marshalling.MediaTypeOverrider._
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.server.Directives
import com.excursion.Pages.hello

object Pages {
  import scalatags.Text.all._
  def hello(ops: String) = HttpResponse(entity =
    html(
      head(
        script(src:="//cdnjs.cloudflare.com/ajax/libs/react/0.12.1/react-with-addons.min.js")
      ),
      body(
        s"Hello, World ${ops}",
        script(src:=s"js/client-${ops}.js"),
        script(src:=s"js/client-jsdeps.js"),
        script(src:=s"js/client-launcher.js")
      )
    ).render
  )
}
class ExcursionDirectives(debug: Boolean = false) extends Directives {
  val route =
    path("") {
      get {
        complete {
          forResponse(hello("fastops"), `text/html`)
        }
      }
    } ~
    path("js" / """\d+""".r) { js =>
      println(js)
      getFromResource(js, `application/javascript`)
    }
}
