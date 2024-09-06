import {AuthService} from "./AuthService";
import {Navigate} from "react-router-dom";

export const PrivateRoute= ({children}) => {
    return (
        AuthService.isAuthenticated ? children : <Navigate to={"/login"} />
    )
}
