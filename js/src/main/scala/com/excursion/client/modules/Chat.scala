package com.excursion.client.modules

import boopickle._
import com.excursion.client.logger._
import com.excursion.shared.ChatMessage
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.OnUnmount
import japgolly.scalajs.react.extra.router2.BaseUrl
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom
import org.scalajs.dom._
import org.scalajs.dom.html.{ Input, TextArea }

import scala.scalajs.js.typedarray.TypedArrayBuffer.wrap
import scala.scalajs.js.typedarray.{ ArrayBuffer, TypedArrayBuffer }

object Chat {
  case class Props(baseUrl: BaseUrl)

  case class State(
    messages: List[ChatMessage] = List.empty,
    webSocket: Option[WebSocket] = None,
    login: Option[String] = None)

  val `jabber-user` = Ref[Input]("jabber-user")
  val `jabber-message` = Ref[TextArea]("jabber-message")

  class Backend(t: BackendScope[Props, State]) extends OnUnmount {
    private def initWebSocket[T](wsUri: String) = {
      val ws = new WebSocket(wsUri)
      ws.onmessage = { (evt: MessageEvent) ⇒
        val reader = new dom.FileReader
        reader.onloadend = { pe: ProgressEvent ⇒
          messageReceived(Unpickle[ChatMessage].fromBytes(wrap(reader.result.asInstanceOf[ArrayBuffer])))
        }
        reader.readAsArrayBuffer(evt.data.asInstanceOf[Blob])
      }
      onUnmount({
        () ⇒ ws.close()
      })
      ws
    }

    def connect(e: ReactEventI): Unit = {
      e.preventDefault()
      log.info("logging into chat server")
      val userE = `jabber-user`(t).get.getDOMNode()
      t.modState(s ⇒ s.copy(
        webSocket = Some(initWebSocket(t.props.baseUrl.value.replace("http", "ws") + "chat?name=" + userE.value)),
        login = Some(userE.value)))
      t.forceUpdate()
    }

    def messageReceived(msg: ChatMessage) = {
      t.modState(s ⇒ s.copy(messages = (msg :: s.messages).take(10)))
      t.forceUpdate()
    }

    def sendMessage(e: ReactEventI) = {
      e.preventDefault()
      val messageE = `jabber-message`(t).get.getDOMNode()
      t.state.webSocket.get.send(messageE.value)
      messageE.value = ""
    }
  }

  val Chat = ReactComponentB[Props]("Chat").
    initialState(State()).
    backend(new Backend(_)).
    render((P, S, B) ⇒ {
      def renderMessage(msg: ChatMessage) = {
        <.div(^.`class` := "message",
          <.p(^.`class` := "speech",
            <.span(^.`class` := "dateTime", msg.time),
            <.span(^.`class` := "text", msg.text),
            <.span(^.`class` := "user", msg.user)))
      }
      <.div(^.`class` := "row",
        <.div(^.`class` := "col-md-6",
          <.h4("Jabber"),
          <.div(^.`class` := "form-group",
            if (S.login.isEmpty) {
              Seq(
                <.div(<.input(^.ref := `jabber-user`, ^.`class` := "form-control", ^.`type` := "text", ^.placeholder := "Enter your Username ...", ^.autoFocus := "true")),
                <.p(
                  <.div(
                    <.button(^.`type` := "button", ^.`class` := "btn btn-primary", ^.onClick ==> B.connect, "Login"))))
            } else {
              Seq(
                <.div(<.textarea(^.ref := `jabber-message`, ^.`class` := "form-control", ^.rows := "3", ^.cols := "70", ^.placeholder := "Enter your message ...")),
                <.p(
                  <.div(
                    <.button(^.`type` := "button", ^.`class` := "btn btn-primary", ^.onClick ==> B.sendMessage, "Jabber away"))))
            })),
        <.div(^.`class` := "col-md-1"),
        <.div(^.`class` := "well col-md-5",
          <.h4("Jibber Jabber"),
          <.div(^.`class` := "messages",
            S.messages map renderMessage)))
    }).
    configure(OnUnmount.install).
    build

  def apply(props: Props) = Chat(props)
}
