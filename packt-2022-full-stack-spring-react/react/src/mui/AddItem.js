import {Button, Dialog, DialogActions, DialogContent, DialogTitle, TextField} from "@mui/material";
import {useState} from "react";

export function AddItem({addItem}) {

    const [dialogOpen, setDialogOpen] = useState(false);
    const [item, setItem] = useState({name: "", quantity: 0});

    const handleChange = (e) => {
        setItem({...item, [e.target.name]: e.target.value});
    }

    return (
        <>
            <Button onClick={() => setDialogOpen(true)} variant={"outlined"}>Add Item</Button>
            <Dialog open={dialogOpen} conClose={() => setDialogOpen(false)}>
                <DialogTitle>Add Item</DialogTitle>
                <DialogContent>
                    <TextField name={"name"} value={item.name} lable={"Name"} margin={"dense"} fullWidth onChange={handleChange}/>
                    <TextField name={"quantity"} value={item.quantity} lable={"Quantity"} margin={"dense"} fullWidth onChange={handleChange}/>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => {addItem(item);setDialogOpen(false);}}>Add</Button>
                    <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
                </DialogActions>
            </Dialog>
        </>
    )
}
