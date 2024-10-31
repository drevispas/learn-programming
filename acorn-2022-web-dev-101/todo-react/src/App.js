import './App.css';
import {Todo} from "./component/Todo";
import {useState} from "react";
import {List, Paper} from "@mui/material";

function App() {

    const [items, setItems] = useState([
        {title: "title1", done: false}, {title: "title2", done: true}
    ]);

    return (
        <div className="App">
            <Paper style={{margin:16, padding: 16}}>
                <List>
                    {items.map((item, index) =>
                        <Todo title={item.title} done={item.done} id={index}/>
                    )}
                </List>
            </Paper>
        </div>
    );
}

export default App;
