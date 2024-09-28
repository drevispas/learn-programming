import {useEffect, useState} from "react";

export function Carlist() {

    const [cars, setCars] = useState([]);

    useEffect(() => {
        fetch('http://localhost:8080/api/cars')
            .then(response => response.json())
            .then(data => setCars(data._embedded.cars))
            .catch(err => console.error(err))
    }, []);

    return (
        <div>
            <table>
                <tbody>
                {
                    cars.map((car, index)=> {
                        <tr key={index}>
                            <td>{car.brand}</td>
                            <td>{car.model}</td>
                            <td>{car.color}</td>
                            <td>{car.year}</td>
                            <td>{car.price}</td>
                        </tr>
                    })
                }
                </tbody>
            </table>
        </div>
    )
}
