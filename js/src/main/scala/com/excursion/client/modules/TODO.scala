package com.excursion.client.modules

import com.excursion.client.DemoApp.Loc
import com.excursion.client.components.Bootstrap._
import com.excursion.client.components._
import com.excursion.client.logger._
import com.excursion.client.services._
import com.excursion.shared._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.OnUnmount
import japgolly.scalajs.react.extra.router2.RouterCtl
import japgolly.scalajs.react.vdom.all._
import rx._
import rx.ops._

import scalacss.ScalaCssReact._

object Todo {

  case class Props(todos: Rx[Seq[TodoItem]], router: RouterCtl[Loc])

  case class State(selectedItem: Option[TodoItem] = None, showTodoForm: Boolean = false)

  abstract class RxObserver[BS <: BackendScope[_, _]](scope: BS) extends OnUnmount {
    protected def observe[T](rx: Rx[T]): Unit = {
      val obs = rx.foreach(_ ⇒ scope.forceUpdate())
      // stop observing when unmounted
      onUnmount(obs.kill())
    }
  }

  class Backend(t: BackendScope[Props, State]) extends RxObserver(t) {
    def mounted(): Unit = {
      // hook up to TodoStore changes
      observe(t.props.todos)
      // dispatch a message to refresh the todos, which will cause TodoStore to fetch todos from the server
      MainDispatcher.dispatch(RefreshTodos)
    }

    def editTodo(item: Option[TodoItem]): Unit = {
      // activate the todo dialog
      t.modState(s ⇒ s.copy(selectedItem = item, showTodoForm = true))
    }

    def deleteTodo(item: TodoItem): Unit = {
      TodoActions.delete(item)
    }

    def todoEdited(item: TodoItem, cancelled: Boolean): Unit = {
      if (cancelled) {
        // nothing to do here
        log.debug("Todo editing cancelled")
      } else {
        log.debug(s"Todo edited: $item")
        TodoActions.update(item)
      }
      // hide the todo dialog
      t.modState(s ⇒ s.copy(showTodoForm = false))
    }
  }

  private val component = ReactComponentB[Props]("TODO")
    .initialState(State()) // initial state from TodoStore
    .backend(new Backend(_))
    .render { (P, S, B) ⇒
      Panel(Panel.Props("What needs to be done")) {
        Seq(
          TodoList(TodoList.Props(P.todos(), TodoActions.update, item ⇒ B.editTodo(Some(item)), B.deleteTodo)),
          Button(Button.Props(() ⇒ B.editTodo(None)), Icon.plusSquare, " New"),
          if (S.showTodoForm) TodoForm(TodoForm.Props(S.selectedItem, B.todoEdited))
          else Seq.empty[ReactElement])
      }
    }
    .componentDidMount(_.backend.mounted())
    .configure(OnUnmount.install)
    .build

  /** Returns a function compatible with router location system while using our own props */
  def apply(store: TodoStore) = (router: RouterCtl[Loc]) ⇒ component(Props(store.todos, router))

}

object TodoForm {
  // shorthand for styles
  @inline private def bss = GlobalStyles.bootstrapStyles

  case class Props(item: Option[TodoItem], submitHandler: (TodoItem, Boolean) ⇒ Unit)

  case class State(item: TodoItem, cancelled: Boolean = true)

  class Backend(t: BackendScope[Props, State]) {
    def submitForm(): Unit = {
      // mark it as NOT cancelled (which is the default)
      t.modState(s ⇒ s.copy(cancelled = false))
    }

    def formClosed(): Unit = {
      // call parent handler with the new item and whether form was OK or cancelled
      if (t.state.item.content.isEmpty) {
        t.modState(s => s.copy(cancelled = true))
      }
      t.props.submitHandler(t.state.item, t.state.cancelled)
    }

    def updateDescription(e: ReactEventI) = {
      // update TodoItem content
      t.modState(s ⇒ s.copy(item = s.item.copy(content = e.currentTarget.value)))
    }

    def updatePriority(e: ReactEventI) = {
      // update TodoItem priority
      val newPri = e.currentTarget.value match {
        case p if p == TodoHigh.toString ⇒ TodoHigh
        case p if p == TodoNormal.toString ⇒ TodoNormal
        case p if p == TodoLow.toString ⇒ TodoLow
      }
      t.modState(s ⇒ s.copy(item = s.item.copy(priority = newPri)))
    }
  }

  private val component = ReactComponentB[Props]("TodoForm")
    .initialStateP(p ⇒ State(p.item.getOrElse(TodoItem("", 0, "", TodoNormal, completed = false))))
    .backend(new Backend(_))
    .render { (P, S, B) ⇒
      log.debug(s"User is ${if (S.item.id.isEmpty) "adding" else "editing"} a todo")
      Modal(Modal.Props(header(if (S.item.id.isEmpty) "Add new todo" else "Edit todo"), footer(B), B.formClosed)) {
        Seq(
          div(bss.formGroup,
            label(`for` := "description", "Description"),
            input(tpe := "text", bss.formControl, id := "description", value := S.item.content,
              placeholder := "write description", onChange ==> B.updateDescription)),
          div(bss.formGroup,
            label(`for` := "priority", "Priority"),
            select(bss.formControl, id := "priority", value := S.item.priority.toString, onChange ==> B.updatePriority,
              option(value := TodoHigh.toString, "High"),
              option(value := TodoNormal.toString, "Normal"),
              option(value := TodoLow.toString, "Low"))))
      }
    }
    .build

  def apply(props: Props) = component(props)

  private object header {
    private val component =
      ReactComponentB[(Bootstrap.Modal.Backend, String)]("TodoFormHeader")
        .render { t ⇒
          val (be, headerText) = t
          span(button(tpe := "button", bss.close, onClick --> be.hide(), Icon.close), h4(headerText))
        }
        .build

    def apply(headerText: String) = (be: Bootstrap.Modal.Backend) ⇒ component((be, headerText))
  }

  private object footer {
    private val component =
      ReactComponentB[(Backend, Bootstrap.Modal.Backend)]("TodoFormFooter")
        .render { t ⇒
          Button(Button.Props(() ⇒ {
            val (b, be) = t
            b.submitForm()
            be.hide()
          }), "OK")
        }
        .build

    def apply(b: Backend) = (be: Bootstrap.Modal.Backend) ⇒ component((b, be))
  }
}

