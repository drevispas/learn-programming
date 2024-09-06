import React from 'react';
import useFetchUser from './useFetchUser';

function UserFetcherApp() {
    const url = 'https://reqres.in/api/users/2';
    const {user, loading, error} = useFetchUser(url);

    if (loading === true) {
        return <p>Loading...</p>
    }
    if (error) {
        return <p>{error.message}</p>
    }
    return (
        <div>
            <h1>User</h1>
            {user && (
            <div>
                <p>Name: {user.first_name} {user.last_name}</p>
                <p>Email: {user.email}</p>
            </div>
            )}
        </div>
    )
}

export default UserFetcherApp;
