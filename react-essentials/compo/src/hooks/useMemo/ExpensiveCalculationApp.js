import React, {useMemo} from "react";

function calculate(number) {
    console.log('Calculating...');
    return number * number;
}

function ExpensiveCalculationApp() {
    var number = 10;
    const memo = useMemo(() => calculate(number), [number]);

    return (
        <div>
            <h2>ExpensiveCalculation</h2>
            <p>1st calculation result: {memo}</p>
            <p>2nd calculation result: {memo}</p>
            <p>3rd calculation result: {memo}</p>
        </div>
    )
}

export default ExpensiveCalculationApp;
