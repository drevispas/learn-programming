import {Checkbox, IconButton, InputBase, ListItem, ListItemText} from "@mui/material";
import {DeleteOutlined} from "@mui/icons-material";
import {useState} from "react";

export const Todo = (props) => {

    const [editedItem, setEditedItem] = useState(props.item);
    const [readOnly, setReadOnly] = useState(true);

    const onClickDelete = () => {
        props.deleteItem(props.item.id);
    }

    const onClickCompleted = (event) => {
        setEditedItem((prevItem) => ({...prevItem, completed: event.target.checked}));
        props.editItem({...editedItem, completed: event.target.checked});
    }

    const turnOffReadOnlyTitle = () => {
        setReadOnly(false);
    }

    const turnOnReadOnlyTitle = () => {
        setReadOnly(true);
    }

    const onChangeTitle = (event) => {
        setEditedItem((prevItem) => ({...prevItem, title: event.target.value}))
    }

    const onKeyPressTitle = (event) => {
        if (event.key === 'Enter') {
            turnOnReadOnlyTitle();
            if (editedItem.title !== props.item.title) {
                props.editItem(editedItem);
            }
        }
    }

    return (
        <ListItem>
            <Checkbox checked={editedItem.completed} onClick={onClickCompleted}/>
            <ListItemText>
                <InputBase
                    id={props.item.id}
                    value={editedItem.title}
                    fullWidth={true}
                    multiline={true}
                    readOnly={readOnly}
                    onClick={turnOffReadOnlyTitle}
                    onChange={onChangeTitle}
                    onKeyDown={onKeyPressTitle}
                />
            </ListItemText>
            <IconButton aria-label={"Delete Todo"} onClick={onClickDelete}>
                <DeleteOutlined/>
            </IconButton>
        </ListItem>
    )
}
