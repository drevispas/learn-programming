import React from "react";
import {Provider, useDispatch, useSelector} from "react-redux";
import {store} from "./store";
import {decrement, increment} from "./counterSlice";

const Counter = () => {
    // useSelector() is a function that selects a value from the store and subscribes to updates
    const count = useSelector((state) => state.counter.count);
    // useDispatch() is a function that sends an action to the store which receives the action and updates the state
    const dispatch = useDispatch();

    return (
        <div>
            <h2>ReduxCounterApp</h2>
            <h3>Count: {count}</h3>
            <button onClick={() => dispatch(increment())}>Increment</button>
            <button onClick={() => dispatch(decrement())}>Decrement</button>
        </div>
    )
}

const ReduxCounterApp = () => {
    return (
        <Provider store={store}>
            <Counter/>
        </Provider>
    );
}

export default ReduxCounterApp;
