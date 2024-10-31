import {Checkbox, InputBase, ListItem, ListItemText} from "@mui/material";

export const Todo = (props) => {
    return (
        <ListItem>
            <Checkbox checked={props.done}/>
            <ListItemText>
                <InputBase
                    value={props.title}
                    fullWidth={true}
                    multiline={true}
                />
            </ListItemText>
        </ListItem>
    )
}
