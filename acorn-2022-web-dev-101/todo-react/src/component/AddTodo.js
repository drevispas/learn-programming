import {Button, Grid, Grid2, TextField} from "@mui/material";
import {useState} from "react";

export const AddTodo = (props) => {

    const [text, setText] = useState("");

    const onTextChange = (event) => {
        setText(event.target.value);
    }

    const onButtonClick = () => {
        props.addItem(text);
    }

    const onKeyPress = (event) => {
        if (event.key === 'Enter') {
            props.addItem(text);
        }
    }

    return (
        <Grid container={true} style={{marginTop: 20}}>
            <Grid xs={11} md={11} item style={{paddingRight: 16}}>
                <TextField placeholder={"Add Todo"} fullWidth={true}
                           onChange={onTextChange} onKeyDown={onKeyPress}/>
            </Grid>
            <Grid xs={1} md={1} item>
                <Button fullWidth style={{height: '100%'}} color={"secondary"} variant={"outlined"}
                        onClick={onButtonClick}>+</Button>
            </Grid>
        </Grid>
    )
}
