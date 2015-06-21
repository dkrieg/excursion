package com.excursion.client

import japgolly.scalajs.react.React
import japgolly.scalajs.react.extra.router2._
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom
import com.excursion.client.components.GlobalStyles
import com.excursion.client.logger._
import com.excursion.client.modules._
import com.excursion.client.services.TodoStore

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import scalacss.Defaults._
import scalacss.ScalaCssReact._

object TodoApp extends js.JSApp {
  sealed trait Loc
  case object DashboardLoc extends Loc
  case object TodoLoc extends Loc

  val routerConfig = RouterConfigDsl[Loc].buildConfig { dsl =>
    import dsl._

    (staticRoute(root, DashboardLoc) ~> renderR(ctl => Dashboard.component(ctl))
      | staticRoute("#todo", TodoLoc) ~> renderR(ctl => Todo(TodoStore)(ctl))
      ).notFound(redirectToPage(DashboardLoc)(Redirect.Replace))
  }.renderWith(layout)

  def layout(c: RouterCtl[Loc], r: Resolution[Loc]) = {
    <.div(
      <.nav(^.className := "navbar navbar-inverse navbar-fixed-top")(
        <.div(^.className := "container")(
          <.div(^.className := "navbar-header")(<.span(^.className := "navbar-brand")("SPA Example")),
          <.div(^.className := "collapse navbar-collapse")(
            MainMenu(MainMenu.Props(c, r.page, TodoStore.todos))
          )
        )
      ),
      // currently active module is shown in this container
      <.div(^.className := "container")(r.render())
    )
  }

  @JSExport
  def main(): Unit = {
    log.warn("Application starting")
    log.enableServerLogging("/logging")
    log.info("This message goes to server as well")

    GlobalStyles.addToDocument()
    val router = Router(BaseUrl(dom.window.location.href.takeWhile(_ != '#')), routerConfig)
    React.render(router(), dom.document.body)
  }}
