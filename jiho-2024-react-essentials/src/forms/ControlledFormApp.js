import {useState} from "react";

export const ControlledFormApp = () => {

    const [inputValue, setInputValue] = useState("");

    const handleChange = (e) => {
        setInputValue(e.target.value);
    }

    const handleSubmit = (e) => {
        e.preventDefault();
        alert("입력값: " + inputValue);
        console.log("입력값: ", inputValue);
        setInputValue("");
    }

    return (
        <div>
            <h3>Controlled Form</h3>
            <form onSubmit={handleSubmit}>
                <label>
                    Name:
                    <input type="text" value={inputValue} onChange={handleChange}/>
                </label>
                <button type="submit">Submit</button>
            </form>
        </div>
    )
}
