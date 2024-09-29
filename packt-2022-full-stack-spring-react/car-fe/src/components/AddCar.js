import {useState} from "react";
import {Button, Dialog, DialogActions, DialogContent, DialogTitle, Stack} from "@mui/material";

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
                    <input type="text" placeholder="Brand" name="brand"
                           value={car.brand} onChange={handleChange}/>
                    <input type="text" placeholder="Model" name="model"
                           value={car.model} onChange={handleChange}/>
                    <input type="text" placeholder="Color" name="color"
                           value={car.color} onChange={handleChange}/>
                    <input type="text" placeholder="Manufacturing year" name="manufacturingYear"
                           value={car.manufacturingYear} onChange={handleChange}/>
                    <input type="text" placeholder="Price" name="price"
                           value={car.price} onChange={handleChange}/>
                    <input type="text" placeholder="Register number" name="registerNumber"
                           value={car.registerNumber} onChange={handleChange}/>
                </DialogContent>
                <DialogActions>
                    <Button onClick={handleClose}>Cancel</Button>
                    <Button onClick={handleSave}>Save</Button>
                </DialogActions>
            </Dialog>
        </div>
    );
}
