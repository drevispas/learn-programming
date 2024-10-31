let apiHost;

const hostname = window && window.location && window.location.hostname;

if (hostname === 'localhost') {
  apiHost = 'http://localhost:8080';
}

export const API_BASE_URL = `${apiHost}/api`;
export const API_TODO_URL = `${API_BASE_URL}/todos`;
