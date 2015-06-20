package com.excursion.shared

trait Api {
  def motd(name: String): String
  def get(): Seq[TodoItem]
  def update(item: TodoItem): Seq[TodoItem]
  def delete(itemId: String): Seq[TodoItem]
}