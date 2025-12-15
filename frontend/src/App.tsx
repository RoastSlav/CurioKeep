import { useEffect, useState } from "react";

type HealthResponse = {
    status: string;
};

function App() {
    const [health, setHealth] = useState<HealthResponse | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        let cancelled = false;

        async function load() {
            try {
                setLoading(true);
                setError(null);

                const res = await fetch("/api/health");
                if (!res.ok) {
                    throw new Error(`HTTP ${res.status}`);
                }

                const data = (await res.json()) as HealthResponse;
                if (!cancelled) setHealth(data);
            } catch (e) {
                if (!cancelled) setError(e instanceof Error ? e.message : String(e));
            } finally {
                if (!cancelled) setLoading(false);
            }
        }

        load();
        return () => {
            cancelled = true;
        };
    }, []);

    return (
        <div style={{ padding: 24, fontFamily: "system-ui" }}>
            <h1>CurioKeep</h1>

            <h2>Backend health</h2>
            {loading && <p>Loading…</p>}
            {error && <p style={{ color: "crimson" }}>Error: {error}</p>}
            {health && <pre>{JSON.stringify(health, null, 2)}</pre>}
        </div>
    );
}

export default App;
