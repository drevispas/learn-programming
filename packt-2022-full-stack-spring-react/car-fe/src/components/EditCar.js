import {Button, Dialog, DialogActions, DialogContent, IconButton, Stack, TextField, Toolbar} from "@mui/material";
import EditButton from "@mui/icons-material/Edit";
import {useState} from "react";

export function EditCar(props) {

    const [open, setOpen] = useState(false);
    const [car, setCar] = useState({
        brand: '',
        model: '',
        color: '',
        manufacturingYear: '',
        price: '',
        registerNumber: ''
    });

    const handleOpen = () => {
        setCar({
            brand: props.car.row.brand,
            model: props.car.row.model,
            color: props.car.row.color,
            manufacturingYear: props.car.row.manufacturingYear,
            price: props.car.row.price,
            registerNumber: props.car.row.registerNumber
        });
        setOpen(true);
    }
    const handleClose = () => {
        setOpen(false);
    }
    const handleChange = (e) => {
        setCar({...car, [e.target.name]: e.target.value})
    }
    const handleSave = () => {
        props.updateCar(car, props.car.id);
        handleClose();
    }

    return (
        <div>
            <IconButton onClick={handleOpen}>
                <EditButton color={"primary"}/>
            </IconButton>
            <Dialog open={open} onClose={handleClose}>
                <DialogContent>
                    <Stack spacing={2} mt={1}>
                        <TextField label={"Brand"} name={"brand"} value={car.brand} onChange={handleChange} variant={"standard"} autoFocus />
                        <TextField label={"Model"} name={"model"} value={car.model} onChange={handleChange} variant={"standard"} />
                        <TextField label={"Color"} name={"color"} value={car.color} onChange={handleChange} variant={"standard"} />
                        <TextField label={"Manufacturing year"} name={"manufacturing year"} value={car.manufacturingYear} onChange={handleChange} variant={"standard"} />
                        <TextField label={"Price"} name={"price"} value={car.price} onChange={handleChange} variant={"standard"} />
                        <TextField label={"Register number"} name={"register number"} value={car.registerNumber} onChange={handleChange} variant={"standard"} />
                    </Stack>
                </DialogContent>
                <DialogActions>
                    <Button onClick={handleClose}>Cancel</Button>
                    <Button onClick={handleSave}>Save</Button>
                </DialogActions>
            </Dialog>
        </div>
    );
}
