import {useState} from "react";
import {Button, Dialog, DialogActions, DialogContent, DialogTitle, Stack, TextField} from "@mui/material";

export function AddCar(props) {
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
        setOpen(true);
    }
    const handleClose = () => {
        setOpen(false);
    }
    const handleChange = (e) => {
        setCar({...car, [e.target.name]: e.target.value})
    }
    const handleSave = () => {
        props.addCar(car);
        handleClose();
    }

    return (
        <div>
            <Stack mt={2} mb={2}>
                <Button onClick={handleOpen} variant={"contained"}>New Car</Button>
            </Stack>
            <Dialog open={open} onClose={handleClose}>
                <DialogTitle>Add car</DialogTitle>
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
