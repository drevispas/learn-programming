import React from 'react';

function AddTodoForm({addTodo}) {
    const [input, setInput] = React.useState('');

    const handleSubmit = (event) => {
        event.preventDefault();
        addTodo(input);
        setInput('');
    }

    return (
        <form onSubmit={handleSubmit}>
            <input type="text" value={input} onChange={(e)=>setInput(e.target.value)} />
            <button type="submit">Add Todo</button>
        </form>
    )
}

export default AddTodoForm;
