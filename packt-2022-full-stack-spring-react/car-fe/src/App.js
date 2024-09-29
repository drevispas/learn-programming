import logo from './logo.svg';
import './App.css';
import {AppBar, Toolbar, Typography} from "@mui/material";
import Carlist from "./components/Carlist";
import {Login} from "./components/Login";

function App() {
  return (
    <div className="App">
      <AppBar position={"static"}>
          <Toolbar>
            <Typography variant={"h6"}>
                Car Shop
            </Typography>
          </Toolbar>
        </AppBar>
        <Login/>
    </div>
  );
}

export default App;
