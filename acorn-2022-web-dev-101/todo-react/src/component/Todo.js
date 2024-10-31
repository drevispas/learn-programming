import {Checkbox, IconButton, InputBase, ListItem, ListItemSecondaryAction, ListItemText} from "@mui/material";
import {DeleteOutlined} from "@mui/icons-material";

export const Todo = (props) => {

    const onClickDelete = () => {
        props.deleteItem(props.id);
    }

    return (
        <ListItem>
            <Checkbox checked={props.item.done}/>
            <ListItemText>
                <InputBase
                    id={props.id}
                    value={props.item.title}
                    fullWidth={true}
                    multiline={true}
                />
            </ListItemText>
            <IconButton aria-label={"Delete Todo"} onClick={onClickDelete}>
                <DeleteOutlined/>
            </IconButton>
        </ListItem>
    )
}
