import React from "react";
import ThemeProvider from "./ThemeProvider";
import ThemeConsumer from "./ThemeConsumer";

const ThemeContextApp = () => {
    return (
        <div>
            <h2>ThemeContextApp</h2>
            <ThemeProvider>
                <ThemeConsumer/>
            </ThemeProvider>
        </div>
    );
};

export default ThemeContextApp;
