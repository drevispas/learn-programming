import React, {useEffect, useState} from "react";
import { DataGrid, GridToolbarContainer, GridToolbarExport, gridClasses } from '@mui/x-data-grid';
import {IconButton, Snackbar} from "@mui/material";
import DeleteIcon from '@mui/icons-material/Delete';
import {AddCar} from "./AddCar";
import {EditCar} from "./EditCar";
import {API_URL} from "../constants";

function CustomToolbar() {
    return (
        <GridToolbarContainer className={gridClasses.toolbarContainer}>
            <GridToolbarExport />
        </GridToolbarContainer>
    );
}

function Carlist() {

    const [cars, setCars] = useState([]);
    const [openSnackbar, setOpenSnackbar] = useState(false);

    const fetchCars = () => {
        const token = sessionStorage.getItem('jwt');
        fetch(API_URL, {headers: {'Authorization': token}})
            .then(response => response.json())
            .then(data => setCars(data._embedded.cars))
            .catch(err => console.error(err))
    }

    const deleteCar = (url) => {
        console.log('Deleting car with url: ' + url);
        if (!window.confirm('Are you sure you want to delete?')) {
            return;
        }
        const token = sessionStorage.getItem('jwt');
        fetch(url, {
            method: 'DELETE', headers: {'Authorization': token}
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
        const token = sessionStorage.getItem('jwt');
        fetch(API_URL, {
            method: 'POST',
            headers: {'Content-Type': 'application/json', 'Authorization': token},
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
        const token = sessionStorage.getItem('jwt');
        fetch(url, {
            method: 'PUT',
            headers: {'Content-Type': 'application/json', 'Authorization': token},
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
                    <IconButton onClick={() => deleteCar(row.id)}>
                        <DeleteIcon color={"error"}/>
                    </IconButton>
                </div>
            )
        },
    ]

    return (
        <React.Fragment>
            <AddCar addCar={addCar}/>
            <div style={{height: 500, width: '100%'}}>
                <DataGrid
                    rows={cars}
                    columns={columns}
                    disableSelectionOnClick={true}
                    getRowId={row => row._links.self.href}
                    components={{ Toolbar: CustomToolbar }}
                />
                <Snackbar open={openSnackbar} autoHideDuration={6000}
                          onClose={() => setOpenSnackbar(false)}
                          message="Car deleted successfully"
                />
            </div>
        </React.Fragment>
    )
}

export default Carlist;
