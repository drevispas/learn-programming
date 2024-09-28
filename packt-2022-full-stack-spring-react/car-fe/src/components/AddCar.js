import {useState} from "react";
import {Dialog, DialogActions, DialogContent, DialogTitle} from "@mui/material";

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
            <button onClick={handleOpen}>Add car</button>
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
                    <button onClick={handleClose}>Cancel</button>
                    <button onClick={handleSave}>Save</button>
                </DialogActions>
            </Dialog>
        </div>
    );
}
