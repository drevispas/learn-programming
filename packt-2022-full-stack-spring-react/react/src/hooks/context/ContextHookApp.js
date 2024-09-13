import {UserContext} from "./UserContext";
import {UserList} from "./UserList";

export function ContextHookApp() {
    const userNames = ["John", "Doe", "Jane"];
    return (
        <UserContext.Provider value={userNames}>
            <UserList />
        </UserContext.Provider>
    );
}
