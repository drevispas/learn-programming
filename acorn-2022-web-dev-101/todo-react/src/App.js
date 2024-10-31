import './App.css';
import {Todo} from "./component/Todo";
import {useState} from "react";
import {Container, List, Paper} from "@mui/material";
import {AddTodo} from "./component/AddTodo";

function App() {

    const [items, setItems] = useState([
        {title: "title1", done: false}, {title: "title2", done: true}
    ]);

    const addItem = (text) => {
        setItems(prevItems => {
            return [...prevItems, {title: text, done: false}]
        })
    }

    return (
        <div className="App">
            <Container maxWidth={"md"}>
                <AddTodo addItem={addItem}/>
                <Paper style={{margin: 16}}>
                    <List>
                        {items.map((item, index) =>
                            <Todo item={item} id={index}/>
                        )}
                    </List>
                </Paper>
            </Container>
        </div>
    );
}

export default App;
