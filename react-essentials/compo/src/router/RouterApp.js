import React from "react";
import {BrowserRouter, Link, Route, Routes, useParams} from 'react-router-dom';
import {About} from "./About";
import {Home} from "./Home";
import {Contact} from "./Contact";

const UserList = () => {
    const userList = [
        {id: 1, name: 'User 1'},
        {id: 2, name: 'User 2'}
    ];
    return (
        <div>
            <h2>User List</h2>
            <ul>
                {userList.map(user => (
                    <li key={user.id}>
                        <Link to={`/users/${user.id}`}>{user.name}</Link>
                    </li>
                ))}
            </ul>
        </div>
    )
}

const UserProfile = () => {
    const {userId} = useParams();
    return (
        <div>
            <h2>User Profile</h2>
            <p>UserId: {userId}</p>
        </div>
    );
}

export const RouterApp = () => {
    return (
        <BrowserRouter>
            <div>
                <nav>
                    <ul>
                        <li><Link to="/">Home</Link></li>
                        <li><Link to="/about">About</Link></li>
                        <li><Link to="/contact">Contact</Link></li>
                    </ul>
                    <ul>
                        <li><Link to="/users">User List</Link></li>
                        <ul>
                            <li><Link to="/users/1">User 1</Link></li>
                            <li><Link to="/users/2">User 2</Link></li>
                        </ul>
                    </ul>
                </nav>

                <Routes>
                    <Route path="/" element={<Home/>}/>
                    <Route path="/about" element={<About/>}/>
                    <Route path="/contact" element={<Contact/>}/>
                    <Route path="/users" element={<UserList/>}/>
                    <Route path="/users/:userId" element={<UserProfile/>}/>
                </Routes>
            </div>
        </BrowserRouter>
    );
}
