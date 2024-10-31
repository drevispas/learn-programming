import {Checkbox, IconButton, InputBase, ListItem, ListItemSecondaryAction, ListItemText} from "@mui/material";
import {DeleteOutlined} from "@mui/icons-material";
import {useState} from "react";

export const Todo = (props) => {

    const [readOnly, setReadOnly] = useState(true);

    const onClickDelete = () => {
        props.deleteItem(props.id);
    }

    const onClickDone = () => {
        props.toggleDone(props.id);
    }

    const turnOffReadOnly = () => {
        setReadOnly(false);
    }

    const turnOnReadOnly = () => {
        setReadOnly(true);
    }

    const onChangeText = (event) => {
        props.editItem(props.id, event.target.value);
    }

    const onKeyPress = (event) => {
        if (event.key === 'Enter') {
            turnOnReadOnly();
        }
    }

    return (
        <ListItem>
            <Checkbox checked={props.item.done} onClick={onClickDone}/>
            <ListItemText>
                <InputBase
                    id={props.id}
                    value={props.item.title}
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
