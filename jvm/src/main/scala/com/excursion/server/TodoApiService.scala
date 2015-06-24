package com.excursion.server

import java.util.{ UUID, Date }

import com.excursion.shared._

object TodoApiService {
  def apply() = new TodoApi {

    var todos = Seq(
      TodoItem("41424344-4546-4748-494a-4b4c4d4e4f50", 0x61626364, "Wear shirt that says “Life”. Hand out lemons on street corner.", TodoLow, completed = false),
      TodoItem("2", 0x61626364, "Make vanilla pudding. Put in mayo jar. Eat in public.", TodoNormal, completed = false),
      TodoItem("3", 0x61626364, "Walk away slowly from an explosion without looking back.", TodoHigh, completed = false),
      TodoItem("4", 0x61626364, "Sneeze in front of the pope. Get blessed.", TodoNormal, completed = true))

    override def motd(name: String): String = s"Welcome to SPA, $name! Time is now ${new Date}"

    override def fetch(): Seq[TodoItem] = {
      println(s"Sending ${todos.size} Todo items")
      todos
    }

    override def update(item: TodoItem): Seq[TodoItem] = {
      if (todos.exists(_.id == item.id)) {
        todos = todos.collect {
          case i if i.id == item.id ⇒ item
          case i ⇒ i
        }
        println(s"Todo item was updated: $item")
      } else {
        val newItem = item.copy(id = UUID.randomUUID().toString)
        todos :+= newItem
        println(s"Todo item was added: $newItem")
      }
      todos
    }

    override def delete(itemId: String): Seq[TodoItem] = {
      println(s"Deleting item with id = $itemId")
      todos = todos.filterNot(_.id == itemId)
      todos
    }
  }
}
