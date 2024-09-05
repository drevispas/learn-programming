import React from 'react';

function UserFetcherApp() {
    const [user, setUser] = React.useState(null);
    const [loading, setLoading] = React.useState(true);
    const [error, setError] = React.useState(null);

    React.useEffect(() => {
        fetch('https://reqres.in/api/users/1')
            .then(response => {
                if (!response.ok) {
                    throw new Error('Failed to fetch user');
                }
                return response.json();
            })
            .then(json => {
                setUser(json.data);
                setLoading(false);
            })
            .catch(error => {
                setError(error);
                setLoading(false);
            })
    })

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
