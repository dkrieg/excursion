package com.excursion.server

import scalatags.Text.TypedTag
import scalatags.Text.all._

object Pages {

  def template(ops: String)(program: TypedTag[String]): TypedTag[String] = {
    html(
      head(
        meta(content := "width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no", name := "viewport"),
        link(href := "stylesheets/main.min.css", rel := "stylesheet", `type` := "text/css")),
      body(
        script(src := s"js/excursion-$ops.js"),
        script(src := "js/excursion-jsdeps.js"),
        program))
  }

  val todo = script("com.excursion.client.DemoApp().main()")
}

