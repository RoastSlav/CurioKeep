import AppRoutes from "./routes/AppRoutes";
import AppGate from "./components/AppGate";
import { AuthProvider } from "./auth/AuthContext";

function App() {
    return (
        <AuthProvider>
            <AppGate>
                <AppRoutes />
            </AppGate>
        </AuthProvider>
    );
}

export default App;
