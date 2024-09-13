import {useState} from "react";

export function EventHandlerApp() {
    const [input, setInput] = useState({
        firstName: "",
        lastName: "",
        email: "",
    });

    const handleSubmit = (event) => {
        event.preventDefault();
        console.log("Form submitted: value: ", input);
    }

    const handleInput = (event) => {
        console.log("Input value: ", event.target.value);
        setInput({...input, [event.target.name]: event.target.value});
    }

    return (
        <form onSubmit={handleSubmit}>
            <label> First Name:</label>
            <input type={"text"} name={"firstName"} value={input.firstName} onChange={handleInput}/>
            <label> Last Name:</label>
            <input type={"text"} name={"lastName"} value={input.lastName} onChange={handleInput}/>
            <label> Email:</label>
            <input type={"text"} name={"email"} value={input.email} onChange={handleInput}/>
            <button type={"submit"}>Submit</button>
        </form>
    )
}
