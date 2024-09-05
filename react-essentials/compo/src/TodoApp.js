import React from 'react';
import AddTodoForm from "./AddTodoForm";

function TodoApp() {
    const [todos, setTodos] = React.useState([]);

    const addTodo = (todo) => {
        setTodos([...todos, todo]);
    };

    return (
        <div>
            <AddTodoForm addTodo={addTodo} />
            <ul>
                {todos.map((todo, index) => (
                    <li key={index}>{todo}</li>
                ))}
            </ul>
        </div>
    )
}

export default TodoApp;
