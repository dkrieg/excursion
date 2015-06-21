package com.excursion.shared

trait TodoApi {
  def motd(name: String): String
  def fetch(): Seq[TodoItem]
  def update(item: TodoItem): Seq[TodoItem]
  def delete(itemId: String): Seq[TodoItem]
}