package com.excursion.client.components

import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.all._

object Greeting {
  private lazy val component =
    ReactComponentB[String]("Greeting").
      render(div(_))

  def apply(g: String) = component.build(g)
}
