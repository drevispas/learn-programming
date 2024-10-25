package org.example

import org.example.domain.ListName
import org.example.domain.TodoHubImpl
import org.example.domain.TodoItem
import org.example.domain.TodoList
import org.example.domain.User
import org.example.service.TodoHandler
import org.http4k.server.Jetty
import org.http4k.server.asServer

// http4k spike
fun main() {
    val shoppingItems = listOf("carrot", "milk")
    val user = User("brad")
    val shoppingListName = ListName("shopping")
    val shoppingTodoList = TodoList(shoppingListName, shoppingItems.map(::TodoItem))
    val bradLists = mapOf(user to listOf(shoppingTodoList))
    val hub = TodoHubImpl(bradLists)
    val app = TodoHandler(hub)
    app.asServer(Jetty(9090)).start()
}
