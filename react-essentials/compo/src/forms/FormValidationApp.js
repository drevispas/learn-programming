import {useState} from "react";

export const FormValidationApp = () => {

    const [formData, setFormData] = useState({
        email: "",
        password: ""
    });

    const [errors, setErrors] = useState({});

    const validateForm = () => {
        let errors = {};
        if (!formData.email) {
            errors.email = "Email is required";
        }
        if (!formData.password) {
            errors.password = "Password is required";
        }
        setErrors(errors);
        return Object.keys(errors).length === 0;
    }

    const handleSubmit = (e) => {
        e.preventDefault();
        if (!validateForm()) return;
        alert("Form submitted");
    }

    const handleChange = (e) => {
        setFormData({...formData, [e.target.name]: e.target.value})
        console.log("formData: ", formData);
    }

    return (
        <div>
            <h3>Form Validation</h3>
            <form onSubmit={handleSubmit}>
                <label htmlFor={"email"}>Email
                    <input type={"text"} id={"email"} name={"email"} value={formData.email} onChange={handleChange}/>
                    {errors.email && <div>{errors.email}</div>}
                </label>
                <label htmlFor={"password"}>Password
                    <input type={"password"} id={"password"} name={"password"} value={formData.password} onChange={handleChange}/>
                    {errors.password && <div>{errors.password}</div>}
                </label>
                <button type={"submit"}>Submit</button>
            </form>
        </div>
    )
}
