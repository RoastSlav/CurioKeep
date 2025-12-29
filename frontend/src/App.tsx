import AppRoutes from "./routes/AppRoutes"
import AppGate from "./components/AppGate"
import {AuthProvider} from "./auth/AuthContext"
import {ThemeProvider} from "./contexts/ThemeContext"

function App() {
    return (
        <ThemeProvider>
            <AuthProvider>
                <AppGate>
                    <AppRoutes/>
                </AppGate>
            </AuthProvider>
        </ThemeProvider>
    )
}

export default App
