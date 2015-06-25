package com.excursion.client.modules

import com.excursion.client.DemoApp.{ChatLoc, Loc, TodoLoc}
import com.excursion.client.components._
import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.extra.router2.RouterCtl
import japgolly.scalajs.react.vdom.all._

object Dashboard {
  // create the React component for Dashboard
  val component = ReactComponentB[RouterCtl[Loc]]("Dashboard").
    render(ctl ⇒ {
      div(
        h2("Dashboard"),
        Motd(),
        Chart(Chart.ChartProps("Test chart", Chart.BarChart,
          ChartData(Seq("A", "B", "C"), Seq(ChartDataset(Seq(1, 2, 3), "Data1"))))),
        div(ctl.link(TodoLoc)("Check your todos!")),
        div(ctl.link(ChatLoc)("Check your Chat Messages!!")))
    }).
    build
}