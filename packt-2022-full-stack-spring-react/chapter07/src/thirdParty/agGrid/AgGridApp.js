import 'ag-grid-community/styles/ag-grid.css';
import 'ag-grid-community/styles/ag-theme-material.css';
import axios from "axios";
import {useState} from "react";
import {AgGridReact} from "ag-grid-react";

export function AgGridApp() {
    const githubSearchUrl = "https://api.github.com/search/repositories?q=";

    const [searchKeyword, setSearchKeyword] = useState("");
    const [data, setData] = useState([]);

    const columnDefs = [
        {headerName: "Full Name", field: "full_name", sortable: true, filter: true},
        {headerName: "Url", field: "html_url", sortable: true, filter: true,
            cellRenderer: (params) => {
                return <a href={params.value} target="_blank" rel="noreferrer">{params.value}</a>
            }
        },
        {headerName: "Login", field: "owner.login", sortable: true, filter: true},
        {headerName: "Description", field: "description", sortable: true, filter: true},
    ];

    const fetchData = async () => {
        const response = await axios.get(githubSearchUrl + searchKeyword);
        setData(response.data.items);
    }

    return (
        <div>
            <h2>AgGridApp</h2>
            <label>검색어: </label>
            <input type={"text"} value={searchKeyword}
                   onChange={(e) => setSearchKeyword(e.target.value)}/>
            <button onClick={fetchData}>Search</button>
            <div className={"ag-theme-material"} style={{height: 400, width: 800}}>
                <AgGridReact rowData={data} columnDefs={columnDefs}
                                pagination={true} paginationPageSize={10}
                                floatingFilter={true}
                />
            </div>
        </div>
    );
}
