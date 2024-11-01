import {API_TODO_URL} from "../api-configs";

function call(path, method, body) {
    const options = {
        method: method,
        headers: {
            'Content-Type': 'application/json'
        }
    }
    if (body) {
        options.body = JSON.stringify(body);
    }
    let url = `${API_TODO_URL}`;
    if (path) {
        url = `${url}/${path}`;
    }
    console.log("API request: ", url, options);
    return fetch(url, options)
        .then(response => {
            if (response.status === 204) {
                return {};
            }
            return response.json();
        })
        .then(json => {
            console.log("API response: ", json);
            if (json === undefined) {
                return;
            }
            return json.data
        })
        .catch(error => {
            console.log("API failed: ", error);
        });
}

function get(path) {
    return call(path, 'GET');
}

function post(path, body) {
    return call(path, 'POST', body);
}

function put(path, body) {
    return call(path, 'PUT', body);
}

function del(path) {
    return call(path, 'DELETE');
}

export function getTodos() {
    return get('');
}

export function getTodoById(id) {
    return get(id);
}

export function addTodo(item) {
    return post('', item);
}

export function updateTodoById(id, item) {
    return put(id, item);
}

export function deleteTodoById(id) {
    return del(id);
}
