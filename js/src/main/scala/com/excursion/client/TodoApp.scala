package com.excursion.client

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport

object TodoApp extends JSApp {
  sealed trait Loc
  case object DashboardLoc extends Loc
  case object TodoLoc extends Loc

  val routerConfig = RouterConfigDsl[Loc].buildConfig { dsl =>

    (staticRoute(root, DashboardLoc) ~> renderR(ctl => Dashboard.component(ctl))
      | staticRoute("#todo", TodoLoc) ~> renderR(ctl => Todo(TodoStore)(ctl))
      ).notFound(redirectToPage(DashboardLoc)(Redirect.Replace))
  }.renderWith(layout)

  @JSExport
  override def main(): Unit = {

  }
}
