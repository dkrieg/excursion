package com.excursion.client.modules

import com.excursion.client.DemoApp.{ChatLoc, DashboardLoc, Loc, TodoLoc}
import com.excursion.client.components.Bootstrap.CommonStyle
import com.excursion.client.components.Icon._
import com.excursion.client.components._
import com.excursion.client.services._
import com.excursion.shared.TodoItem
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.OnUnmount
import japgolly.scalajs.react.extra.router2.RouterCtl
import japgolly.scalajs.react.vdom.all._
import rx._
import rx.ops._

import scalacss.ScalaCssReact._

object MainMenu {
  // shorthand for styles
  @inline private def bss = GlobalStyles.bootstrapStyles

  case class Props(ctl: RouterCtl[Loc], currentLoc: Loc, todos: Rx[Seq[TodoItem]])

  case class MenuItem(label: (Props) ⇒ ReactNode, icon: Icon, location: Loc)

  class Backend(t: BackendScope[Props, _]) extends OnUnmount {
    def mounted(): Unit = {
      // hook up to Todo changes
      val obsItems = t.props.todos.foreach { _ ⇒ t.forceUpdate() }
      onUnmount {
        // stop observing when unmounted (= never in this SPA)
        obsItems.kill()
      }
      MainDispatcher.dispatch(RefreshTodos)
    }
  }

  // build the Todo menu item, showing the number of open todos
  private def buildTodoMenu(props: Props): ReactNode = {
    val todoCount = props.todos().count(!_.completed)
    Seq(
      span("Todo "),
      if (todoCount > 0) span(bss.labelOpt(CommonStyle.danger), bss.labelAsBadge, todoCount)
      else span())
  }

  private val menuItems = Seq(
    MenuItem(_ ⇒ "Dashboard", Icon.dashboard, DashboardLoc),
    MenuItem(buildTodoMenu, Icon.check, TodoLoc),
    MenuItem(_ ⇒ "Chat", Icon.envelope, ChatLoc))

  private val component = ReactComponentB[Props]("MainMenu")
    .stateless
    .backend(new Backend(_))
    .render((P, _, B) ⇒ {
      ul(bss.navbar)(
        // build a list of menu items
        for (item ← menuItems) yield {
          li((P.currentLoc == item.location) ?= (className := "active"),
            P.ctl.link(item.location)(item.icon, " ", item.label(P)))
        })
    })
    .componentDidMount(_.backend.mounted())
    .build

  def apply(props: Props) = component(props)
}