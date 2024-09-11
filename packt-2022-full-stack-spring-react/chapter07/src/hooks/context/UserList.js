import {useContext} from "react";
import {UserContext} from "./UserContext";
import {UserCount} from "./UserCount";


export function UserList() {
    const userContext = useContext(UserContext);

    return (
        <>
            <h3>User List</h3>
            <ul>
                {userContext.map((name, index) => (
                    <li key={index}>{name}</li>
                ))}
            </ul>
            <UserCount />
        </>
    )
}
