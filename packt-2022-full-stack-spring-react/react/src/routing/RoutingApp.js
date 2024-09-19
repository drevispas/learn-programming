import {BrowserRouter, Link, Route, Routes} from "react-router-dom";
import {Home} from "./Home";
import {Contact} from "./Contact";

export function RoutingApp() {
    return (
        <div>
            <h2>Routing App</h2>
            <BrowserRouter>
                <nav>
                    <Link to="/home">Home</Link>{" "}
                    <Link to={"/contact"}>Contact</Link>
                </nav>
                <Routes>
                    <Route path={"/home"} element={<Home/>}/>
                    <Route path={"/contact"} element={<Contact/>}/>
                </Routes>
            </BrowserRouter>
        </div>
    )
}
