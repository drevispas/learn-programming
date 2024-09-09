import React from "react";

export const UncontrolledFormApp = () => {

    const inputRef = React.useRef();

    const handleSubmit = (e) => {
        e.preventDefault();
        alert("입력값: " + inputRef.current.value);
        inputRef.current.value = "";
    }

    return (
        <div>
            <h3>Uncontrolled Form</h3>
            <form onSubmit={handleSubmit}>
                <label>
                    Name:
                    <input type="text" ref={inputRef}/>
                </label>
                <button type="submit">Submit</button>
            </form>
        </div>
    )
}
