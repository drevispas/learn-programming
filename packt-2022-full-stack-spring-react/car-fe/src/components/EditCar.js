import {Dialog, DialogActions, DialogContent} from "@mui/material";
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
            <button onClick={handleOpen}>Edit</button>
            <Dialog open={open} onClose={handleClose}>
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
                    <button onClick={handleSave}>Save</button>
                </DialogContent>
                <DialogActions>
                    <button onClick={handleClose}>Cancel</button>
                    <button onClick={handleSave}>Save</button>
                </DialogActions>
            </Dialog>
        </div>
    );
}
