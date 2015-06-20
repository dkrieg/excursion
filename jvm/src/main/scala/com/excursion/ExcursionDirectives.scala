package com.excursion

import akka.http.scaladsl.marshalling.MediaTypeOverrider._
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.StatusCodes.NotFound
import akka.http.scaladsl.server.Directives

import scalatags.Text

object Pages {
  import scalatags.Text.all._

  def template(ops: String)(program: Text.TypedTag[String]): Text.TypedTag[String] = {
    html(
      head(
        script(src:="http://cdnjs.cloudflare.com/ajax/libs/react/0.12.1/react-with-addons.min.js")
      ),
      body(
        script(src:=s"js/excursion-$ops.js"),
        script(src:="js/excursion-jsdeps.js"),
        program
      )
    )
  }

  val hello = script("com.excursion.HelloExample().main()")
}

class ExcursionDirectives(implicit production: Boolean = false) extends Directives {
  import Pages._

  val page =
    if(production) template("fullopt") _
    else template("fastopt") _

  val route =
    get {
      path("hello") {
        complete(forResponse(HttpResponse(entity = page(hello).render), `text/html`))
      } ~ pathPrefix("srcmaps") {
        if(production) complete(NotFound)
        else getFromDirectory("../")
      } ~ getFromResourceDirectory("web")
    }
}
