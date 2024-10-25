package org.example.service

import org.example.domain.ListName
import org.example.domain.TodoHub
import org.example.domain.TodoItem
import org.example.domain.TodoList
import org.example.domain.User
import org.example.web.HtmlPage
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes

// 타입 A를 받아 타입 B로 전환하는 함수가 FUN<A,B>
typealias FUN<A, B> = (A) -> (B)

// A->B(this), B->C(other) 사이에 andThen을 적용하면 A->C 함수가 됨. 입력은 타입 A, 출력은 타입 C
infix fun <A, B, C> FUN<A, B>.andThen(other: FUN<B, C>): FUN<A, C> = { a -> other(this(a)) }

data class TodoHandler(val hub: TodoHub) : HttpHandler {

    // HttpHandler 위해 구현할 함수
    override fun invoke(request: Request): Response = routingHttpHandler(request)

    val routingHttpHandler = routes(
        "/todo/{user}/{list}" bind Method.GET to ::fetchList
    )

    private fun fetchList(request: Request): Response = showList(request)

    // andThen 연산자 사용에 주목
    // 요청값부터 응답값까지의 변화를 함수의 연결로 표현함. Request -> ... -> Response
    private val showList = ::extractListData andThen
            ::fetchListContent andThen
            ::renderHtml andThen
            ::createResponse

    // Request -> {user, listName}
    private fun extractListData(request: Request): Pair<User, ListName> {
        val user = request.path("user").orEmpty().let(::User)
        val list = request.path("list").orEmpty().let(::ListName)
        return user to list
    }

    // {user, listName} -> TodoList
    private fun fetchListContent(listId: Pair<User, ListName>): TodoList {
        return hub.getList(listId.first, listId.second)
            ?: error("List `${listId.second}` not found")
    }

    // TodoList -> HtmlPage
    private fun renderHtml(todoList: TodoList): HtmlPage {
        // 상태 변이 결과가 html인데 이것도 객체 하나로 표현함
        return HtmlPage(
            """
            <html>
                <head>
                    <meta charset="utf-8">
                </head>
                <body>
                    <h1>절대</h1>
                    <h2>${todoList.listName.name}</h2>
                    <table>
                        <tbody>${rednerItems(todoList.items)}</tbody>                    
                    </table>
                </body>
            </html>
        """.trimIndent()
        )
    }

    private fun rednerItems(items: List<TodoItem>): String {
        return items.map {
            """
            <tr><td>${it.description}</tr></td>
        """.trimIndent()
        }.joinToString("")
    }

    // HtmlPage -> Response
    private fun createResponse(htmlPage: HtmlPage): Response {
        return Response(Status.OK).body(htmlPage.raw)
    }
}
