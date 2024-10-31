import './App.css';
import {Todo} from "./component/Todo";
import {useState} from "react";
import {Container, List, Paper} from "@mui/material";
import {AddTodo} from "./component/AddTodo";

function App() {

    const [items, setItems] = useState([]);

    const addItem = (text) => {
        setItems(prevItems => {
            return [...prevItems, {title: text, done: false}]
        })
    }

    const deleteItem = (index) => {
        setItems(prevItems => {
            return prevItems.filter((item, i) => i !== index)
        })
    }

    const toggleDone = (index) => {
        setItems(prevItems => {
            return prevItems.map((item, i) => {
                if (i === index) {
                    return {...item, done: !item.done}
                }
                return item;
            })
        })
    }

    const editItem = (index, text) => {
        setItems(prevItems => {
            return prevItems.map((item, i) => {
                if (i === index) {
                    return {...item, title: text}
                }
                return item;
            })
        })
    }

    return (
        <div className="App">
            <Container maxWidth={"md"}>
                <AddTodo addItem={addItem}/>
                <Paper style={{margin: 16}}>
                    <List>
                        {items.map((item, index) =>
                            <Todo item={item} id={index} deleteItem={deleteItem} toggleDone={toggleDone} editItem={editItem}/>
                        )}
                    </List>
                </Paper>
            </Container>
        </div>
    );
}

export default App;
