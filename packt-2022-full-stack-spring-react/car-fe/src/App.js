import logo from './logo.svg';
import './App.css';
import {AppBar, Toolbar, Typography} from "@mui/material";
import {Carlist} from "./components/Carlist";

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
        <Carlist/>
    </div>
  );
}

export default App;
