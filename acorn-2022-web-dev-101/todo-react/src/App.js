import './App.css';
import {Todo} from "./component/Todo";
import {useEffect, useState} from "react";
import {Container, List, Paper} from "@mui/material";
import {AddTodo} from "./component/AddTodo";
import {addTodo, getTodos, updateTodoById} from "./service/ApiService";

function App() {

    const [items, setItems] = useState([]);

    // Fetch todo list from API and set it to state
    useEffect(() => {
        getTodos().then(todos => {
            setItems(todos.map(item => {
                return {id: item.id, title: item.title, done: item.completed}
            }))
        });
    }, []);

    const addItem = (text) => {
        const item = {title: text, done: false};
        addTodo(item).then(r => {
            setItems(prevItems => {
                return [...prevItems, {title: text, done: false}]
            })
        });
    }

    const deleteItem = (id) => {
        setItems(prevItems => {
            return prevItems.filter((item, i) => i !== id)
        })
    }

    const toggleDone = (id) => {
        setItems(prevItems => {
            return prevItems.map((item, i) => {
                if (i === id) {
                    return {...item, done: !item.done}
                }
                return item;
            })
        })
    }

    const editItem = (id, text) => {
        const item = {id: id, title: text};
        updateTodoById(id, item).then(r => {
            setItems(prevItems => {
                return prevItems.map((item) => {
                    if (item.id === id) {
                        return {...item, title: text}
                    }
                    return item;
                })
            });
        });
    }

    return (
        <div className="App">
            <Container maxWidth={"md"}>
                <AddTodo addItem={addItem}/>
                <Paper style={{margin: 16}}>
                    <List>
                        {items.map((item) =>
                            <Todo item={item} deleteItem={deleteItem} toggleDone={toggleDone} editItem={editItem}/>
                        )}
                    </List>
                </Paper>
            </Container>
        </div>
    );
}

export default App;
