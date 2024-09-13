import {AppBar, Container, List, ListItem, ListItemText, Stack, Toolbar, Typography} from "@mui/material";
import {AddItem} from "./AddItem";
import {useState} from "react";

export function MUIApp() {
    const [items, setItems] = useState([]);

    const addItem = (item) => {
        setItems([...items, item]);
    }

    return (
        <Container>
            <AppBar position="static">
                <Toolbar>
                    <Typography variant="h6">
                        Shopping Cart
                    </Typography>
                </Toolbar>
            </AppBar>
            <Stack alignItems={"center"} spacing={2}>
                <AddItem addItem={addItem}/>
                <List>
                    {items.map((item, index) => (
                        <ListItem key={index} divider>
                            <ListItemText primary={item.name} secondary={item.quantity}/>
                        </ListItem>
                    ))}
                </List>
            </Stack>
        </Container>
    )
}
