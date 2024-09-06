import React from "react";
import ThemeContext from "./ThemeContext";

const ThemeProvider = ({children}) => {
    const [theme, setTheme] = React.useState('light');
    const toggleTheme = () => {
        setTheme(prevTheme => prevTheme === 'light' ? 'dark' : 'light');
    }
    return (
        <div>
            <ThemeContext.Provider value={{theme, toggleTheme}}>
                {children}
            </ThemeContext.Provider>
        </div>
    );
}

export default ThemeProvider;
