import {useEffect, useState} from "react";
import {DataGrid} from "@mui/x-data-grid";
import {Snackbar} from "@mui/material";
import {AddCar} from "./AddCar";
import {EditCar} from "./EditCar";

export function Carlist() {

    const [cars, setCars] = useState([]);
    const [openSnackbar, setOpenSnackbar] = useState(false);

    const fetchCars = () => {
        fetch('http://localhost:8080/api/cars')
            .then(response => response.json())
            .then(data => setCars(data._embedded.cars))
            .catch(err => console.error(err))
    }

    const deleteCar = (url) => {
        console.log('Deleting car with url: ' + url);
        if (!window.confirm('Are you sure you want to delete?')) {
            return;
        }
        fetch(url, {
            method: 'DELETE'
        })
            .then(response => {
                if (response.ok) {
                    fetchCars();
                    setOpenSnackbar(true);
                } else {
                    alert('Failed to delete car');
                }
            })
            .catch(err => console.error(err));
    }

    const addCar = (car) => {
        fetch('http://localhost:8080/api/cars', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(car)
        })
            .then(response => {
                if (response.ok) {
                    fetchCars();
                } else {
                    alert('Failed to add car')
                }
            })
            .catch(err => console.error(err));
    }

    const updateCar = (car, url) => {
        fetch(url, {
            method: 'PUT',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(car)
        })
            .then(response => {
                if (response.ok) {
                    fetchCars();
                } else {
                    alert('Failed to update car');
                }
            })
            .catch(err => console.error(err));
    }

    useEffect(() => {
        fetchCars();
    }, []);

    const columns = [
        {field: 'brand', headerName: 'Brand', width: 150},
        {field: 'model', headerName: 'Model', width: 150},
        {field: 'color', headerName: 'Color', width: 150},
        {field: 'manufacturingYear', headerName: 'Manufacturing Year', width: 150},
        {field: 'price', headerName: 'Price', width: 150},
        {field: 'registerNumber', headerName: 'Register Number', width: 150},
        {
            field: '_links.car.href',
            headerName: 'Action1',
            width: 150,
            sortable: false,
            filterable: false,
            renderCell: (row) => (
                <EditCar car={row} updateCar={updateCar}/>
            )
        },
        {
            field: '_links.self.href',
            headerName: 'Action2',
            width: 150,
            sortable: false,
            filterable: false,
            renderCell: (row) => (
                <div>
                    <button onClick={() => deleteCar(row.id)}>Delete</button>
                </div>
            )
        },
    ]

    return (
        <>
            <AddCar addCar={addCar}/>
            <div style={{height: 500, width: '100%'}}>
                <DataGrid columns={columns} rows={cars}
                          getRowId={row => row._links.self.href}
                          disableRowSelectionOnClick={true}
                />
                <Snackbar open={openSnackbar} autoHideDuration={6000}
                          onClose={() => setOpenSnackbar(false)}
                          message="Car deleted successfully"
                />
            </div>
        </>
    )
}