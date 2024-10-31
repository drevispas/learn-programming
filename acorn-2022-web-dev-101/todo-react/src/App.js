import './App.css';
import {Todo} from "./component/Todo";
import {useState} from "react";
import {Container, List, Paper} from "@mui/material";
import {AddTodo} from "./component/AddTodo";

function App() {

    const [items, setItems] = useState([
        {title: "title1", done: false}, {title: "title2", done: true}
    ]);

    return (
        <div className="App">
            <Container maxWidth={"md"}>
                <AddTodo/>
                <Paper style={{margin: 16}}>
                    <List>
                        {items.map((item, index) =>
                            <Todo title={item.title} done={item.done} id={index}/>
                        )}
                    </List>
                </Paper>
            </Container>
        </div>
    );
}

export default App;
