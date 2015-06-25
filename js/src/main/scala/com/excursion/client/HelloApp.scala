package com.excursion.client

import com.excursion.client.components.Hello
import japgolly.scalajs.react.React
import org.scalajs.dom._

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport

object HelloApp extends JSApp {
  @JSExport
  override def main(): Unit = {
    console.info("JSApp is running")
    React.render(Hello("Out There"), document.body)
  }
}

