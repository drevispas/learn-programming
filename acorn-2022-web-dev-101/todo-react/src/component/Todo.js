import {Checkbox, IconButton, InputBase, ListItem, ListItemSecondaryAction, ListItemText} from "@mui/material";
import {DeleteOutlined} from "@mui/icons-material";
import {useState} from "react";

export const Todo = (props) => {

    const [text, setText] = useState(props.item.title);
    const [readOnly, setReadOnly] = useState(true);

    const onClickDelete = () => {
        props.deleteItem(props.item.id);
    }

    const onClickDone = () => {
        props.toggleDone(props.item.id);
    }

    const turnOffReadOnly = () => {
        setReadOnly(false);
    }

    const turnOnReadOnly = () => {
        setReadOnly(true);
    }

    const onChangeText = (event) => {
        setText(event.target.value);
    }

    const onKeyPress = (event) => {
        if (event.key === 'Enter') {
            turnOnReadOnly();
            if (text !== props.item.title) {
                props.editItem(props.item.id, text);
            }
        }
    }

    return (
        <ListItem>
            <Checkbox checked={props.item.done} onClick={onClickDone}/>
            <ListItemText>
                <InputBase
                    id={props.item.id}
                    value={text}
                    fullWidth={true}
                    multiline={true}
                    readOnly={readOnly}
                    onClick={turnOffReadOnly}
                    onChange={onChangeText}
                    onKeyDown={onKeyPress}
                />
            </ListItemText>
            <IconButton aria-label={"Delete Todo"} onClick={onClickDelete}>
                <DeleteOutlined/>
            </IconButton>
        </ListItem>
    )
}
