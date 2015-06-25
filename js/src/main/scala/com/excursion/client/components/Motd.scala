package com.excursion.client.components

import autowire._
import com.excursion.client.components.Bootstrap._
import com.excursion.client.services.AjaxClient
import com.excursion.shared.TodoApi
import japgolly.scalajs.react.vdom.all._
import japgolly.scalajs.react.{BackendScope, ReactComponentB}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

/**
 * This is a simple component demonstrating how to interact with the server
 */
object Motd {

  case class State(message: String)

  class Backend(t: BackendScope[Unit, State]) {
    def refresh() {
      // load a new message from the server
      AjaxClient[TodoApi].motd("User X").call().foreach { message ⇒
        t.modState(_ ⇒ State(message))
      }
    }
  }

  // create the React component for holding the Message of the Day
  private val component = ReactComponentB[Unit]("Motd")
    .initialState(State("loading...")) // show a loading text while message is being fetched from the server
    .backend(new Backend(_))
    .render { (_, S, B) ⇒
      Panel(Panel.Props("Message of the day")) {
        Seq(
          div(S.message),
          Button(Button.Props(B.refresh, CommonStyle.danger)) {
            Seq(Icon.refresh, " Update")
          })
      }
    }
    .componentDidMount(scope ⇒ scope.backend.refresh())
    .buildU

  def apply() = component()
}