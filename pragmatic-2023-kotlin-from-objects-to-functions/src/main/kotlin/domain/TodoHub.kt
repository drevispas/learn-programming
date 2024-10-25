package org.example.domain

interface TodoHub {

    fun getList(user: User, listName: ListName): TodoList?
}

class TodoHubImpl(val lists: Map<User, List<TodoList>>): TodoHub {

    override fun getList(user: User, listName: ListName) =
        lists[user]?.firstOrNull { it.listName == listName }
}
