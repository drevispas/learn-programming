import {Checkbox, IconButton, InputBase, ListItem, ListItemSecondaryAction, ListItemText} from "@mui/material";
import {DeleteOutlined} from "@mui/icons-material";

export const Todo = (props) => {
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
            <IconButton aria-label={"Delete Todo"}>
                <DeleteOutlined/>
            </IconButton>
        </ListItem>
    )
}
