import React from "react";
import ThemeContext from "./ThemeContext";

const ThemeConsumer = () => {
    const {theme, toggleTheme} = React.useContext(ThemeContext);

    const themes = {
        light: {
            backgroundColor: '#fff',
            color: '#000'
        },
        dark: {
            backgroundColor: '#000',
            color: '#fff'
        }
    }

    return (
        <button onClick={toggleTheme} style={themes[theme]}>Toggle Theme</button>
    )
}

export default ThemeConsumer;
