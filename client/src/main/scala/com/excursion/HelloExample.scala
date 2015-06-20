package com.excursion

import japgolly.scalajs.react.React.render
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{React, ReactComponentB}
import org.scalajs.dom

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport

object HelloExample extends JSApp {
  val Hello = ReactComponentB[String]("HelloMessage")
    .render(name => <.div("Hello ", name))
    .build

  @JSExport
  override def main(): Unit = {
    dom.console.info("Router logging is enabled. Enjoy!")
    render(Hello("World"), dom.document.body)
  }
}
