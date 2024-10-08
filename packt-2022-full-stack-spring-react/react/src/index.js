import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './App';
import reportWebVitals from './reportWebVitals';
import {ContextHookApp} from "./hooks/context/ContextHookApp";
import {EventHandlerApp} from "./events/EventHandlerApp";
import {AgGridApp} from "./thirdParty/agGrid/AgGridApp";
import {MUIApp} from "./mui/MUIApp";
import {RoutingApp} from "./routing/RoutingApp";

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
    <React.StrictMode>
        {/*<App />*/}
        {/*  <ContextHookApp />*/}
        {/*  <EventHandlerApp />*/}
        {/*<AgGridApp/>*/}
        {/*<MUIApp/>*/}
        <RoutingApp />
    </React.StrictMode>
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
