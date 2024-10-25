import org.example.domain.ListName
import org.example.domain.TodoHubImpl
import org.example.domain.TodoItem
import org.example.domain.TodoList
import org.example.domain.User
import org.example.service.TodoHandler
import org.http4k.client.JettyClient
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

/**
 * Way of chapter 3.1
 */

fun startTodoFacade(lists: Map<User, List<TodoList>>): TodoFacade {
    val port = 8081
    val hub = TodoHubImpl(lists)
    val server = TodoHandler(hub).asServer(Jetty(port)).start()
    val client = ClientFilters.SetBaseUriFrom(Uri.of("http://localhost:$port/")).then(JettyClient())
    return TodoFacade(client, server)
}

class TodoHandlerItemsTest {

//    val userName = "brad"
//    val listName = "shopping"
    val shoppingItems = listOf("carrot", "milk")
    val user = User("brad")
    val shoppingListName = ListName("shopping")
    val shoppingTodoList = TodoList(shoppingListName, shoppingItems.map(::TodoItem))
    val bradLists = mapOf(user to listOf(shoppingTodoList))

    @Test
    fun `User gets the items of a todo list`() {
        val bradActor = TodoListActor(user.name)
        val facade = startTodoFacade(bradLists)
        facade.runScenario(
            bradActor.canGetItemsOfATodoList(shoppingListName.name, shoppingItems)
        )
    }

    @Test
    fun `Get list by user name name`() {
        val hub = TodoHubImpl(bradLists)
        val todoList = hub.getList(user, shoppingListName)
        expectThat(todoList).isEqualTo(shoppingTodoList)
    }
}
