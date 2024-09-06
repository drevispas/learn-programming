import React from "react";

function ReducerCounterApp() {
    const initialState = {count: 0};
    const reducer = (state, action) => {
        if (action.type === 'increase') {
            return {count: state.count + 1};
        } else if (action.type === 'decrease') {
            return {count: state.count - 1};
        } else if (action.type === 'reset') {
            return {count: 0}
        } else {
            return state;
        }
    }
    const [state, dispatch] = React.useReducer(reducer, initialState);

    return (
        <div>
            <h2>ReducerCounter</h2>
            <h3>{state.count}</h3>
            <button onClick={() => dispatch({type: 'increase'})}>Increase</button>
            <button onClick={() => dispatch({type: 'decrease'})}>Decrease</button>
            <button onClick={() => dispatch({type: 'reset'})}>Reset</button>
        </div>
    )
}

export default ReducerCounterApp;
