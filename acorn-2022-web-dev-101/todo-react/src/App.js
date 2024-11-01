import './App.css';
import {Todo} from "./component/Todo";
import {useEffect, useState} from "react";
import {Container, List, Paper} from "@mui/material";
import {AddTodo} from "./component/AddTodo";
import {addTodo, deleteTodoById, getTodos, updateTodoById} from "./service/ApiService";

function App() {

    const [items, setItems] = useState([]);

    // Fetch todo list from API and set it to state
    useEffect(() => {
        getTodos().then(items => {
            setItems(items.map(item => {
                return {id: item.id, title: item.title, completed: item.completed}
            }))
        });
    }, []);

    const addItem = (newItem) => {
        newItem.completed = false;
        addTodo(newItem).then(r => {
            setItems(prevItems => [...prevItems, newItem])
        });
    }

    const deleteItem = (id) => {
        console.log("Delete item: ", id);
        deleteTodoById(id).then(r => {
            setItems(prevItems => prevItems.filter((item) => item.id !== id))
        });
    }

    const editItem = (editedItem) => {
        console.log("Edit item: ", editedItem);
        updateTodoById(editedItem.id, editedItem).then(r => {
            setItems(prevItems => prevItems.map(item => {
                if (item.id === editedItem.id) {
                    return editedItem;
                }
                return item;
            }))
        })
    }

    return (
        <div className="App">
            <Container maxWidth={"md"}>
                <AddTodo addItem={addItem}/>
                <Paper style={{margin: 16}}>
                    <List>
                        {items.map((item) =>
                            <Todo item={item} deleteItem={deleteItem} editItem={editItem}/>
                        )}
                    </List>
                </Paper>
            </Container>
        </div>
    );
}

export default App;
