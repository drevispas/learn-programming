import {useEffect, useState} from "react";
import {DataGrid} from "@mui/x-data-grid";

export function Carlist() {

    const [cars, setCars] = useState([]);

    const fetchCars = () => {
        fetch('http://localhost:8080/api/cars')
            .then(response => response.json())
            .then(data => setCars(data._embedded.cars))
            .catch(err => console.error(err))
    }

    const deleteCar = (url) => {
        console.log('Deleting car with url: ' + url);
        fetch(url, {
            method: 'DELETE'
        })
            .then(response => {
                if (response.ok) {
                    fetchCars();
                } else {
                    alert('Failed to delete car');
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
            field: '_links.self.href',
            headerName: 'Actions',
            width: 150,
            renderCell: (row) => (
                <div>
                    <button onClick={() => deleteCar(row.id)}>Delete</button>
                </div>
            )
        }
    ]

    return (
        <div style={{height:500, width:'100%'}}>
            <DataGrid columns={columns} rows={cars}
                      getRowId={row => row._links.self.href}
                      disableRowSelectionOnClick={true}
            />
        </div>
    )
}
