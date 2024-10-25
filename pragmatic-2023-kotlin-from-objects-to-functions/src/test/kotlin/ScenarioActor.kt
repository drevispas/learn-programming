import org.example.domain.ListName
import org.example.domain.TodoItem
import org.example.domain.TodoList
import strikt.api.expectThat
import strikt.assertions.isEqualTo

// Actor는 사람 이름만 구별할 수 있는 인터페이스임
interface ScenarioActor {

    val userName: String
}

// Actor 구현체는 test case별 함수를 가짐
// test data(domain entity)를 입력으로 받아 API 호출하고 결과를 assert함
class TodoListActor(override val userName: String) : ScenarioActor {

    // Step은 Actions가 receiver로서 수행하는 테스트 하위의 세부 태스크 (lambda)
    // typealias Step = TodoActions.() -> Unit
    // 리턴값은 없고 test data를 받아 Actions의 App Facade 기능(App API 호출)을 실행한 후 assert
    // Actor가 Action을 한 것. Test Actor가 Test case Action을 실행하는 것임. 그런데 실제 실행하는 것이 아니라
    // Actions을 이용해 테스트하는 함수를 반환함
    fun canGetItemsOfATodoList(listName: String, itemNames: List<String>): Step = {
        val expected = TodoList(ListName(listName), itemNames.map(::TodoItem))
        val actual = getTodoList(userName, listName)
        expectThat(actual).isEqualTo(expected)
    }
}
