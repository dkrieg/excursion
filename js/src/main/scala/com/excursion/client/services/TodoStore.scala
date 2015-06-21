package com.excursion.client.services


import autowire._
import rx._
import com.excursion.client.ukko._
import com.excursion.shared.{TodoItem, TodoApi}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

case object RefreshTodos

case class UpdateAllTodos(todos: Seq[TodoItem])

case class UpdateTodo(item: TodoItem)

trait TodoStore extends Actor {
  override val name = "TodoStore"

  // refine a reactive variable
  private val items = Var(Seq.empty[TodoItem])

  private def updateItems(newItems: Seq[TodoItem]): Unit = {
    // check if todos have really changed
    if (newItems != items()) {
      // use Rx to update, which propagates down to dependencies
      items() = newItems
      newItems.foreach(println)
    }
  }

  override def receive = {
    case RefreshTodos =>
      // load all todos from the server
      AjaxClient[TodoApi].fetch().call().foreach { todos =>
        updateItems(todos)
      }
    case UpdateAllTodos(todos) =>
      updateItems(todos)
  }

  // return as Rx to prevent mutation in dependencies
  def todos:Rx[Seq[TodoItem]] = items
}

// create a singleton instance of TodoStore
object TodoStore extends TodoStore {
  // register this actor with the dispatcher
  MainDispatcher.register(this)
}

object TodoActions {
  def updateTodo(item: TodoItem) = {
    // inform the server to update/add the item
    AjaxClient[TodoApi].update(item).call().foreach { todos =>
      MainDispatcher.dispatch(UpdateAllTodos(todos))
    }
  }

  def deleteTodo(item: TodoItem) = {
    // tell server to delete a todo
    AjaxClient[TodoApi].delete(item.id).call().foreach { todos =>
      MainDispatcher.dispatch(UpdateAllTodos(todos))
    }
  }
}