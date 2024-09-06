import {createSlice} from '@reduxjs/toolkit';

const initialState = {
    count: 0,
};

const counterSlice = createSlice({
    name: 'counter',
    initialState,
    reducers: {
        // curly braces are needed to return an object
        increment: (state) => {state.count++},
        decrement: (state) => {state.count--},
    }
});

export const {increment, decrement} = counterSlice.actions;
export default counterSlice.reducer;
