package org.example.domain

// Todo = map of (User, list of TodoList)
// TodoList = (ListName, list of TodoItem)
// ex: (shopping, [carrots, milk]): 쇼핑이라는 할일리스트에는 2개의 아이템이 있음
data class TodoList(val listName: ListName, val items: List<TodoItem>)

// ListName 타입을 따로 만든 이유는 할일리스트 이름들을 제한하기 위함임
data class ListName(val name: String)

data class TodoItem(val description: String)

enum class TodoStatus { TODO, IN_PROGRESS, DONE, BLOCKED }
