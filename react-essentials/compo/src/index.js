import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './App';
import reportWebVitals from './reportWebVitals';
import TodoApp from "./useState/TodoApp";
import CounterApp from "./useState/CounterApp";
import UserFetcherApp from "./useEffect/UserFetcherApp";
import InputFocusApp from "./useRef/InputFocusApp";
import ExpensiveCalculationApp from "./useMemo/ExpensiveCalculationApp";
import ReducerCounterApp from "./useReducer/ReducerCounterApp";
import ThemeContextApp from "./Context/ThemeContextApp";
import ReduxCounterApp from "./redux/ReduxCounterApp";

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
    <React.StrictMode>
        <TodoApp/>
        <hr/>
        <CounterApp/>
        <hr/>
        <UserFetcherApp/>
        <hr/>
        <InputFocusApp/>
        <hr/>
        <ExpensiveCalculationApp/>
        <hr/>
        <ReducerCounterApp/>
        <hr/>
        <ThemeContextApp/>
        <hr/>
        <ReduxCounterApp/>
    </React.StrictMode>
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
