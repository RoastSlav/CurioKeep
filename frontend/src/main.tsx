import {StrictMode} from "react"
import {createRoot} from "react-dom/client"
import {BrowserRouter} from "react-router-dom"
import App from "./App"
import {ToastProvider} from "./components/Toasts"
import {ThemeProvider} from "./contexts/ThemeProvider"
import "./index.css"

createRoot(document.getElementById("root")!).render(
    <StrictMode>
        <BrowserRouter>
            <ThemeProvider>
                <ToastProvider>
                    <App/>
                </ToastProvider>
            </ThemeProvider>
        </BrowserRouter>
    </StrictMode>,
)
