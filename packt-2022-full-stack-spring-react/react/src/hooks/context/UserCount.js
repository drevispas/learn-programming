import {UserContext} from "./UserContext";
import {useContext} from "react";

export function UserCount() {
    const userContext = useContext(UserContext);
    return (
        <p>User Count: {userContext.length}</p>
    )
}
