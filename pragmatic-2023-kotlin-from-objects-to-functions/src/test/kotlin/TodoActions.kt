import org.example.domain.ListName
import org.example.domain.TodoItem
import org.example.domain.TodoList
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.junit.jupiter.api.fail

// Actions는 request 받아서 API를 호출하는 역할
interface TodoActions {

    fun getTodoList(userName: String, listName: String): TodoList?
}

// Step은 Actions가 수행하는 테스트 하위의 세부 태스크
typealias Step = TodoActions.() -> Unit

// 생성자로 client, server를 받음
// Actions를 상속해서 api 호출부를 구현함
// Facade는 Actor의 API 요청을 대행해 줌
class TodoFacade(val client: HttpHandler, val server: AutoCloseable) : TodoActions {

    // Call server  "/todo/{user}/{list}" using client
    override fun getTodoList(userName: String, listName: String): TodoList {
        val response = client(Request(Method.GET, "/todo/$userName/$listName"))
        return if (response.status == Status.OK) parseResponse(response.bodyString())
        else fail(response.toMessage())
    }

    // Facade 객체를 receiver로 Actor에게 받은 lambda를 실행함
    fun runScenario(vararg steps: Step) {
        server.use {
            // this: TodoActions
            steps.forEach { step -> step(this) }
        }
    }

    // API 응답인 HTML을 TodoList로 되돌린다.
    private fun parseResponse(html: String): TodoList {
        val nameRegex = "<h2>.*<".toRegex()
        val listName = ListName(extractListName(nameRegex, html))
        val itemsRegex = "<td>.*?<".toRegex()
        val items = itemsRegex.findAll(html)
            .map { TodoItem(extractItemDesc(it)) }.toList()
        return TodoList(listName, items)
    }

    private fun extractListName(nameRegex: Regex, html: String): String =
        nameRegex.find(html)?.value
            ?.substringAfter("<h2>")
            ?.dropLast(1)
            .orEmpty()

    private fun extractItemDesc(matchResult: MatchResult): String =
        matchResult.value.substringAfter("<td>").dropLast(1)
}
