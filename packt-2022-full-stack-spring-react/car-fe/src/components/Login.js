import {TOKEN_URL} from "../constants";
import {useState} from "react";
import {Button, Snackbar, Stack, TextField} from "@mui/material";
import Carlist from "./Carlist";

export function Login() {

    const [authentication, setAuthentication] = useState({
        username: '',
        password: ''
    });
    const [authenticated, setAuthenticated] = useState(false);
    const [openLoginFailed, setOpenLoginFailed] = useState(false);

    const handleChange = (e) => {
        setAuthentication({...authentication, [e.target.name]: e.target.value})
    }
    const login = () => {
        console.log('Logging in with', JSON.stringify(authentication));
        fetch(TOKEN_URL, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(authentication)
        })
            .then(response => {
                if (response.ok) {
                    // jwt = "Bearer eyJ**********"
                    const jwt = response.headers.get('Authorization');
                    if (jwt) {
                        sessionStorage.setItem('jwt', jwt);
                        setAuthenticated(true);
                    }
                } else {
                    setOpenLoginFailed(true);
                }
            })
            .catch(err => console.error(err));
    }
    const logout = () => {
        sessionStorage.removeItem('jwt');
        setAuthenticated(false);
    }

    return (
        authenticated ? <Carlist/> :
            <div>
                <Stack spacing={2} alignItems={'center'} mt={2}>
                    <TextField label={"Username"} name={"username"} value={authentication.username}
                               onChange={handleChange} variant={"standard"} autoFocus/>
                    <TextField label={"Password"} name={"password"} value={authentication.password}
                               onChange={handleChange} variant={"standard"}/>
                    <Button variant={"outlined"} color={"primary"} onClick={login}>Login</Button>
                </Stack>
                <Snackbar open={openLoginFailed} autoHideDuration={6000} onClose={() => setOpenLoginFailed(false)}
                          message={"Login failed: Check your username and password"}
                />
            </div>
    );
}
