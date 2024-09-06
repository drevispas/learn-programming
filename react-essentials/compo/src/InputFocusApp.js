import React from 'react';

function InputFocusApp() {

    const inputRef = React.useRef(null);

    const focusInput = () => {
        inputRef.current.focus();
    }

    return (
        <div>
            <input ref={inputRef} type={"text"} />
            <button onClick={focusInput}>Focus input</button>
        </div>
    )
}

export default InputFocusApp;
