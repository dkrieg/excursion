package com.excursion.client.components

import japgolly.scalajs.react.ReactComponentB

object Hello {
  private lazy val component =
    ReactComponentB[String]("Hello").
      render(g => Greeting(s"Hello, $g"))

  def apply(g: String) = component.build(g)
}
