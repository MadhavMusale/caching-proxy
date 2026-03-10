import { useState, useEffect, useCallback, useRef } from "react";

// Cloud-ready: set VITE_API_URL env var for production deployment
const API_BASE = (typeof __API_URL__ !== "undefined" ? __API_URL__ : null)
  || import.meta.env.VITE_API_URL
  || "http://localhost:8080";

// ── Utilities ──────────────────────────────────────────────────────────────
const fmt = (n) => (n ?? 0).toLocaleString();
const fmtRate = (n) => `${(n ?? 0).toFixed(1)}%`;

function useInterval(fn, ms) {
  const ref = useRef(fn);
  useEffect(() => { ref.current = fn; }, [fn]);
  useEffect(() => {
    const id = setInterval(() => ref.current(), ms);
    return () => clearInterval(id);
  }, [ms]);
}

// ── API helper with API key ─────────────────────────────────────────────────
function apiFetch(path, apiKey, options = {}) {
  return fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(apiKey ? { "X-API-Key": apiKey } : {}),
      ...(options.headers || {}),
    },
  });
}

// ── Icons (inline SVG) ─────────────────────────────────────────────────────
const Icon = ({ d, size = 16, stroke = "currentColor" }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none"
       stroke={stroke} strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
    <path d={d} />
  </svg>
);

const Icons = {
  zap: "M13 2L3 14h9l-1 8 10-12h-9l1-8z",
  activity: "M22 12h-4l-3 9L9 3l-3 9H2",
  layers: "M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5",
  trash: "M3 6h18M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2",
  send: "M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z",
  refresh: "M23 4v6h-6M1 20v-6h6M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15",
  check: "M20 6L9 17l-5-5",
  x: "M18 6L6 18M6 6l12 12",
  globe: "M12 2a10 10 0 100 20A10 10 0 0012 2zM2 12h20M12 2a15.3 15.3 0 014 10 15.3 15.3 0 01-4 10 15.3 15.3 0 01-4-10 15.3 15.3 0 014-10z",
  clock: "M12 22a10 10 0 100-20 10 10 0 000 20zM12 6v6l4 2",
  server: "M20 9V7a2 2 0 00-2-2H4a2 2 0 00-2 2v10a2 2 0 002 2h3m13-13l-9 9-4-4",
  database: "M12 2c5.52 0 10 1.34 10 3v14c0 1.66-4.48 3-10 3S2 20.66 2 19V5c0-1.66 4.48-3 10-3zm0 0c5.52 0 10 1.34 10 3M2 12c0 1.66 4.48 3 10 3s10-1.34 10-3M2 8c0 1.66 4.48 3 10 3s10-1.34 10-3",
  trendingUp: "M23 6l-9.5 9.5-5-5L1 18M17 6h6v6",
  eye: "M1 12S5 4 12 4s11 8 11 8-4 8-11 8S1 12 1 12zm11-3a3 3 0 100 6 3 3 0 000-6z",
  settings: "M12 15a3 3 0 100-6 3 3 0 000 6zM19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z",
  key: "M21 2l-2 2m-7.61 7.61a5.5 5.5 0 11-7.778 7.778 5.5 5.5 0 017.777-7.777zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4",
  shield: "M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z",
  lock: "M19 11H5a2 2 0 00-2 2v7a2 2 0 002 2h14a2 2 0 002-2v-7a2 2 0 00-2-2zM7 11V7a5 5 0 0110 0v4",
  alertTriangle: "M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0zM12 9v4M12 17h.01",
};

// ── API Key Gate ────────────────────────────────────────────────────────────
function ApiKeyGate({ onAuthenticated }) {
  const [key, setKey] = useState("");
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  // Check if saved key exists in sessionStorage
  useEffect(() => {
    const saved = sessionStorage.getItem("proxy_api_key");
    if (saved) onAuthenticated(saved);
  }, []);

  const tryKey = async () => {
    if (!key.trim()) return;
    setLoading(true);
    setError(null);
    try {
      const res = await fetch(`${API_BASE}/cache/stats`, {
        headers: { "X-API-Key": key.trim() },
      });
      if (res.ok) {
        sessionStorage.setItem("proxy_api_key", key.trim());
        onAuthenticated(key.trim());
      } else if (res.status === 401 || res.status === 403) {
        setError("Invalid API key. Check your .env file.");
      } else {
        // If backend is not requiring auth (health check passes), proceed
        onAuthenticated(key.trim());
      }
    } catch {
      setError("Cannot reach backend at " + API_BASE);
    }
    setLoading(false);
  };

  return (
    <div className="gate-overlay">
      <div className="gate-card">
        <div className="gate-logo">
          <Icon d={Icons.zap} size={24} stroke="#0a0c10" />
        </div>
        <h1 className="gate-title">CacheProxy</h1>
        <p className="gate-sub">Enter your API key to access the dashboard</p>
        <div className="gate-input-wrap">
          <Icon d={Icons.key} size={14} />
          <input
            className="gate-input"
            type="password"
            placeholder="X-API-Key value from your .env"
            value={key}
            onChange={e => setKey(e.target.value)}
            onKeyDown={e => e.key === "Enter" && tryKey()}
            autoFocus
          />
        </div>
        {error && (
          <div className="gate-error">
            <Icon d={Icons.alertTriangle} size={13} /> {error}
          </div>
        )}
        <button className="gate-btn" onClick={tryKey} disabled={loading || !key.trim()}>
          {loading ? <span className="spinner" /> : <Icon d={Icons.shield} size={14} />}
          {loading ? "Authenticating…" : "Access Dashboard"}
        </button>
        <p className="gate-hint">
          Default dev key: <code>dev-key-change-me-in-production</code>
        </p>
      </div>
    </div>
  );
}

// ── Stat Card ──────────────────────────────────────────────────────────────
function StatCard({ label, value, sub, accent, icon, glow }) {
  return (
    <div className={`stat-card ${glow ? "glow-" + glow : ""}`}>
      <div className="stat-icon" style={{ color: accent }}>
        <Icon d={icon} size={20} />
      </div>
      <div className="stat-value" style={{ color: accent }}>{value}</div>
      <div className="stat-label">{label}</div>
      {sub && <div className="stat-sub">{sub}</div>}
    </div>
  );
}

function MiniBar({ value, max, color }) {
  const pct = max > 0 ? Math.min(100, (value / max) * 100) : 0;
  return (
    <div className="mini-bar-bg">
      <div className="mini-bar-fill" style={{ width: `${pct}%`, background: color }} />
    </div>
  );
}

function RequestRow({ req }) {
  const time = new Date(req.timestamp);
  const timeStr = time.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" });
  return (
    <div className={`req-row ${req.cacheHit ? "hit" : "miss"}`}>
      <span className="req-badge">{req.method}</span>
      <span className="req-path">{req.path}</span>
      <span className="req-origin">{req.origin?.replace(/https?:\/\//, "")}</span>
      <span className={`req-status ${req.cacheHit ? "hit" : "miss"}`}>
        {req.cacheHit ? "HIT" : "MISS"}
      </span>
      <span className="req-time">{req.cacheHit ? "<1ms" : `${req.responseTimeMs}ms`}</span>
      <span className="req-ts">{timeStr}</span>
    </div>
  );
}

function EndpointRow({ ep, maxCount }) {
  const hitRate = ep.requestCount > 0 ? (ep.cacheHits / ep.requestCount * 100) : 0;
  return (
    <div className="endpoint-row">
      <div className="ep-path">{ep.path}</div>
      <MiniBar value={ep.requestCount} max={maxCount} color="var(--accent-blue)" />
      <div className="ep-stats">
        <span>{fmt(ep.requestCount)} req</span>
        <span className="ep-hit-rate">{fmtRate(hitRate)} hit</span>
      </div>
    </div>
  );
}

// ── Request Tester ─────────────────────────────────────────────────────────
function ProxyTester({ apiKey }) {
  const [origin, setOrigin] = useState("http://dummyjson.com");
  const [path, setPath] = useState("/products/1");
  const [method, setMethod] = useState("GET");
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [xCache, setXCache] = useState(null);
  const [elapsed, setElapsed] = useState(null);
  const [rateLimit, setRateLimit] = useState(null);

  const send = async () => {
    setLoading(true);
    setResult(null);
    const t0 = Date.now();
    try {
      const url = `${API_BASE}/proxy${path}?origin=${encodeURIComponent(origin)}`;
      const res = await fetch(url, {
        method,
        headers: { "X-API-Key": apiKey },
      });
      const body = await res.text();
      setElapsed(Date.now() - t0);
      setXCache(res.headers.get("X-Cache") || "—");
      setRateLimit({
        remaining: res.headers.get("X-RateLimit-Remaining"),
        limit: res.headers.get("X-RateLimit-Limit"),
      });
      let pretty = body;
      try { pretty = JSON.stringify(JSON.parse(body), null, 2); } catch {}
      setResult({ status: res.status, body: pretty });
    } catch (e) {
      setResult({ status: "ERR", body: e.message });
    }
    setLoading(false);
  };

  const shortcuts = [
    { label: "Products", path: "/products" },
    { label: "Product #1", path: "/products/1" },
    { label: "Users", path: "/users" },
    { label: "Posts", path: "/posts" },
    { label: "Todos", path: "/todos" },
  ];

  return (
    <div className="tester-panel">
      <div className="panel-header">
        <Icon d={Icons.send} size={16} />
        <span>Request Tester</span>
        {rateLimit?.remaining && (
          <span style={{ marginLeft: "auto", fontFamily: "var(--font-mono)", fontSize: 11, color: "var(--muted)" }}>
            {rateLimit.remaining}/{rateLimit.limit} req remaining
          </span>
        )}
      </div>

      <div className="shortcuts">
        {shortcuts.map(s => (
          <button key={s.path} className="shortcut-btn" onClick={() => setPath(s.path)}>{s.label}</button>
        ))}
      </div>

      <div className="tester-row">
        <select className="method-select" value={method} onChange={e => setMethod(e.target.value)}>
          {["GET", "POST", "PUT", "DELETE"].map(m => <option key={m}>{m}</option>)}
        </select>
        <input className="tester-input" value={path}
          onChange={e => setPath(e.target.value)} placeholder="/products/1" />
        <button className="send-btn" onClick={send} disabled={loading}>
          {loading ? <span className="spinner" /> : <Icon d={Icons.send} size={14} />}
          {loading ? "Sending…" : "Send"}
        </button>
      </div>

      <div className="tester-row">
        <Icon d={Icons.globe} size={14} />
        <input className="tester-input full" value={origin}
          onChange={e => setOrigin(e.target.value)} placeholder="http://dummyjson.com" />
      </div>

      {result && (
        <div className="result-box">
          <div className="result-meta">
            <span className={`status-badge s${Math.floor(result.status / 100)}xx`}>{result.status}</span>
            {xCache && (
              <span className={`cache-badge ${xCache === "HIT" ? "hit" : "miss"}`}>
                {xCache === "HIT" ? "⚡ Cache HIT" : "☁ Cache MISS"}
              </span>
            )}
            {elapsed != null && <span className="elapsed-badge">{elapsed}ms</span>}
          </div>
          <pre className="result-body">{result.body}</pre>
        </div>
      )}
    </div>
  );
}

// ── Settings Panel ─────────────────────────────────────────────────────────
function SettingsPanel({ onClear, apiKey }) {
  const [origin, setOrigin] = useState("http://dummyjson.com");
  const [saved, setSaved] = useState(false);
  const [clearing, setClearing] = useState(false);
  const [cleared, setCleared] = useState(false);

  const saveOrigin = async () => {
    try {
      await apiFetch("/cache/origin", apiKey, {
        method: "PUT",
        body: JSON.stringify({ origin }),
      });
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
    } catch {}
  };

  const clearCache = async () => {
    setClearing(true);
    try {
      await apiFetch("/cache/clear", apiKey, { method: "POST" });
      setCleared(true);
      onClear();
      setTimeout(() => setCleared(false), 2000);
    } catch {}
    setClearing(false);
  };

  const logout = () => {
    sessionStorage.removeItem("proxy_api_key");
    window.location.reload();
  };

  return (
    <div className="settings-panel">
      <div className="panel-header"><Icon d={Icons.settings} size={16} /><span>Configuration</span></div>

      <div className="setting-group">
        <label className="setting-label">Origin Server URL</label>
        <div className="tester-row" style={{ padding: 0, marginTop: 8 }}>
          <input className="tester-input full" value={origin}
            onChange={e => setOrigin(e.target.value)} placeholder="http://dummyjson.com" />
          <button className={`send-btn ${saved ? "success" : ""}`} onClick={saveOrigin}>
            {saved ? <Icon d={Icons.check} size={14} /> : "Save"}
          </button>
        </div>
        <p className="setting-hint">All proxy requests without an explicit origin will use this URL.</p>
      </div>

      <div className="setting-group">
        <label className="setting-label">Security Info</label>
        <div className="security-info">
          <div className="sec-row"><Icon d={Icons.key} size={13} /><span>API Key auth: <strong style={{color:"var(--accent-green)"}}>Active</strong></span></div>
          <div className="sec-row"><Icon d={Icons.shield} size={13} /><span>SSRF protection: <strong style={{color:"var(--accent-green)"}}>Active</strong></span></div>
          <div className="sec-row"><Icon d={Icons.activity} size={13} /><span>Rate limiting: <strong style={{color:"var(--accent-green)"}}>Active (100 req/min)</strong></span></div>
          <div className="sec-row"><Icon d={Icons.database} size={13} /><span>Cache backend: <strong style={{color:"var(--accent-blue)"}}>Redis</strong></span></div>
        </div>
      </div>

      <div className="setting-group danger-zone">
        <label className="setting-label red">Danger Zone</label>
        <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
          <button className={`danger-btn ${cleared ? "cleared" : ""}`} onClick={clearCache} disabled={clearing}>
            <Icon d={cleared ? Icons.check : Icons.trash} size={14} />
            {clearing ? "Clearing…" : cleared ? "Cleared!" : "Clear All Cache"}
          </button>
          <button className="danger-btn" onClick={logout} style={{ borderColor: "rgba(255,255,255,0.15)", color: "var(--muted)" }}>
            <Icon d={Icons.lock} size={14} /> Log Out
          </button>
        </div>
        <p className="setting-hint">Clearing cache removes all Redis entries. Logout clears your API key from this session.</p>
      </div>
    </div>
  );
}

// ── Hit Rate Gauge ─────────────────────────────────────────────────────────
function HitRateGauge({ rate }) {
  const r = 52, cx = 64, cy = 64;
  const circumference = 2 * Math.PI * r;
  const pct = Math.min(100, Math.max(0, rate));
  const offset = circumference - (pct / 100) * circumference;
  const color = pct >= 70 ? "#00e5a0" : pct >= 40 ? "#f59e0b" : "#ef4444";
  return (
    <div className="gauge-wrap">
      <svg width="128" height="128" viewBox="0 0 128 128">
        <circle cx={cx} cy={cy} r={r} fill="none" stroke="rgba(255,255,255,0.06)" strokeWidth="10" />
        <circle cx={cx} cy={cy} r={r} fill="none" stroke={color} strokeWidth="10"
          strokeDasharray={circumference} strokeDashoffset={offset} strokeLinecap="round"
          transform={`rotate(-90 ${cx} ${cy})`}
          style={{ transition: "stroke-dashoffset 0.6s ease, stroke 0.4s ease" }} />
        <text x={cx} y={cy - 4} textAnchor="middle" fill={color} fontSize="18"
          fontWeight="700" fontFamily="'DM Mono', monospace">{pct.toFixed(1)}%</text>
        <text x={cx} y={cy + 14} textAnchor="middle" fill="rgba(255,255,255,0.4)"
          fontSize="9" fontFamily="system-ui">HIT RATE</text>
      </svg>
    </div>
  );
}

// ── Main App ────────────────────────────────────────────────────────────────
export default function App() {
  const [apiKey, setApiKey] = useState(null);
  const [stats, setStats] = useState(null);
  const [tab, setTab] = useState("dashboard");
  const [pulse, setPulse] = useState(false);
  const [backendUp, setBackendUp] = useState(null);

  const fetchStats = useCallback(async () => {
    if (!apiKey) return;
    try {
      const res = await apiFetch("/cache/stats", apiKey);
      if (!res.ok) throw new Error();
      const data = await res.json();
      setStats(data);
      setBackendUp(true);
      setPulse(true);
      setTimeout(() => setPulse(false), 300);
    } catch {
      setBackendUp(false);
    }
  }, [apiKey]);

  useEffect(() => { fetchStats(); }, [fetchStats]);
  useInterval(fetchStats, 3000);

  if (!apiKey) {
    return <ApiKeyGate onAuthenticated={setApiKey} />;
  }

  const maxCount = stats?.topEndpoints?.reduce((m, e) => Math.max(m, e.requestCount), 1) ?? 1;

  return (
    <>
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=DM+Mono:wght@300;400;500&family=Syne:wght@400;600;700;800&display=swap');

        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
        :root {
          --bg: #0a0c10; --surface: #111318; --surface2: #16191f;
          --border: rgba(255,255,255,0.07); --text: #e8eaf0; --muted: rgba(232,234,240,0.45);
          --accent-green: #00e5a0; --accent-blue: #3d9eff;
          --accent-amber: #f59e0b; --accent-red: #ef4444;
          --font-display: 'Syne', sans-serif; --font-mono: 'DM Mono', monospace;
        }
        body { background: var(--bg); color: var(--text); font-family: var(--font-display); min-height: 100vh; }
        body::before {
          content: ''; position: fixed; inset: 0; pointer-events: none; z-index: 0;
          background-image:
            repeating-linear-gradient(0deg, transparent, transparent 39px, rgba(255,255,255,0.015) 40px),
            repeating-linear-gradient(90deg, transparent, transparent 39px, rgba(255,255,255,0.015) 40px);
        }

        /* ── Gate ── */
        .gate-overlay {
          position: fixed; inset: 0; background: var(--bg);
          display: flex; align-items: center; justify-content: center; z-index: 999;
        }
        .gate-card {
          background: var(--surface); border: 1px solid var(--border); border-radius: 16px;
          padding: 40px; width: 420px; display: flex; flex-direction: column;
          align-items: center; gap: 16px;
          box-shadow: 0 0 60px rgba(0,229,160,0.06);
        }
        .gate-logo {
          width: 52px; height: 52px; border-radius: 14px;
          background: linear-gradient(135deg, var(--accent-green), var(--accent-blue));
          display: flex; align-items: center; justify-content: center;
        }
        .gate-title { font-size: 24px; font-weight: 800; letter-spacing: -0.5px; }
        .gate-sub { font-size: 13px; color: var(--muted); text-align: center; }
        .gate-input-wrap {
          display: flex; align-items: center; gap: 10px; width: 100%;
          background: rgba(255,255,255,0.04); border: 1px solid var(--border);
          border-radius: 10px; padding: 0 14px;
        }
        .gate-input {
          flex: 1; background: none; border: none; color: var(--text);
          font-family: var(--font-mono); font-size: 14px; padding: 12px 0; outline: none;
        }
        .gate-error {
          display: flex; align-items: center; gap: 8px; font-size: 12px;
          color: var(--accent-red); background: rgba(239,68,68,0.08);
          border: 1px solid rgba(239,68,68,0.2); border-radius: 8px; padding: 10px 14px;
          width: 100%;
        }
        .gate-btn {
          width: 100%; background: var(--accent-green); color: #0a0c10;
          border: none; padding: 13px; border-radius: 10px;
          font-family: var(--font-display); font-size: 14px; font-weight: 700;
          cursor: pointer; display: flex; align-items: center; justify-content: center;
          gap: 8px; transition: all 0.2s;
        }
        .gate-btn:hover:not(:disabled) { filter: brightness(1.1); }
        .gate-btn:disabled { opacity: 0.5; cursor: not-allowed; }
        .gate-hint { font-size: 11px; color: rgba(255,255,255,0.2); text-align: center; }
        .gate-hint code { color: rgba(255,255,255,0.35); font-family: var(--font-mono); }

        /* ── Shell ── */
        .shell { position: relative; z-index: 1; display: flex; flex-direction: column; min-height: 100vh; }
        .header {
          display: flex; align-items: center; justify-content: space-between;
          padding: 0 32px; height: 60px; border-bottom: 1px solid var(--border);
          background: rgba(10,12,16,0.9); backdrop-filter: blur(16px);
          position: sticky; top: 0; z-index: 100;
        }
        .header-brand { display: flex; align-items: center; gap: 12px; }
        .header-logo {
          width: 32px; height: 32px; border-radius: 8px;
          background: linear-gradient(135deg, var(--accent-green), var(--accent-blue));
          display: flex; align-items: center; justify-content: center;
        }
        .header-title { font-size: 15px; font-weight: 700; letter-spacing: -0.3px; }
        .header-sub { font-size: 11px; color: var(--muted); font-family: var(--font-mono); }
        .header-right { display: flex; align-items: center; gap: 16px; }
        .status-dot {
          width: 8px; height: 8px; border-radius: 50%;
          background: var(--accent-green); box-shadow: 0 0 8px var(--accent-green);
          animation: dotPulse 2s ease infinite;
        }
        .status-dot.down { background: var(--accent-red); box-shadow: 0 0 8px var(--accent-red); animation: none; }
        @keyframes dotPulse { 0%,100%{opacity:1} 50%{opacity:0.4} }

        .refresh-btn {
          background: none; border: 1px solid var(--border); color: var(--muted);
          padding: 6px 10px; border-radius: 6px; cursor: pointer; font-size: 12px;
          display: flex; align-items: center; gap: 6px; transition: all 0.2s;
          font-family: var(--font-display);
        }
        .refresh-btn:hover { color: var(--text); border-color: rgba(255,255,255,0.15); }
        .refresh-btn.pulse svg { animation: spin 0.4s ease; }
        @keyframes spin { to { transform: rotate(360deg); } }

        .api-key-badge {
          background: rgba(0,229,160,0.08); border: 1px solid rgba(0,229,160,0.2);
          color: var(--accent-green); padding: 4px 10px; border-radius: 20px;
          font-size: 11px; font-family: var(--font-mono); display: flex; align-items: center; gap: 6px;
        }

        /* ── Nav ── */
        .nav {
          display: flex; gap: 2px; padding: 0 32px;
          border-bottom: 1px solid var(--border); background: var(--surface);
        }
        .nav-btn {
          display: flex; align-items: center; gap: 8px;
          padding: 12px 16px; background: none; border: none;
          color: var(--muted); font-family: var(--font-display);
          font-size: 13px; font-weight: 600; cursor: pointer;
          border-bottom: 2px solid transparent; margin-bottom: -1px; transition: all 0.2s;
        }
        .nav-btn:hover { color: var(--text); }
        .nav-btn.active { color: var(--accent-green); border-bottom-color: var(--accent-green); }

        /* ── Main ── */
        .main { flex: 1; padding: 28px 32px; max-width: 1400px; margin: 0 auto; width: 100%; }

        /* ── Stats Grid ── */
        .stats-grid {
          display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
          gap: 16px; margin-bottom: 28px;
        }
        .stat-card {
          background: var(--surface); border: 1px solid var(--border);
          border-radius: 12px; padding: 20px;
          display: flex; flex-direction: column; gap: 6px;
          position: relative; overflow: hidden;
        }
        .stat-card::after {
          content: ''; position: absolute; inset: 0;
          background: linear-gradient(135deg, rgba(255,255,255,0.025) 0%, transparent 60%);
          pointer-events: none;
        }
        .stat-card.glow-green { border-color: rgba(0,229,160,0.25); }
        .stat-card.glow-blue { border-color: rgba(61,158,255,0.25); }
        .stat-card.glow-amber { border-color: rgba(245,158,11,0.25); }
        .stat-icon { opacity: 0.8; margin-bottom: 4px; }
        .stat-value { font-size: 28px; font-weight: 800; font-family: var(--font-mono); letter-spacing: -1px; line-height: 1; }
        .stat-label { font-size: 11px; color: var(--muted); text-transform: uppercase; letter-spacing: 0.08em; font-weight: 600; }
        .stat-sub { font-size: 11px; color: var(--muted); font-family: var(--font-mono); }

        /* ── Dashboard Grid ── */
        .dash-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
        .dash-grid .full-width { grid-column: 1 / -1; }
        @media(max-width: 900px) { .dash-grid { grid-template-columns: 1fr; } }

        /* ── Panels ── */
        .panel { background: var(--surface); border: 1px solid var(--border); border-radius: 12px; overflow: hidden; }
        .panel-header {
          display: flex; align-items: center; gap: 10px;
          padding: 16px 20px; border-bottom: 1px solid var(--border);
          font-size: 13px; font-weight: 600; color: var(--muted);
          text-transform: uppercase; letter-spacing: 0.06em;
        }
        .panel-body { padding: 20px; }

        /* ── Gauge ── */
        .gauge-panel {
          background: var(--surface); border: 1px solid var(--border);
          border-radius: 12px; padding: 20px;
          display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px;
        }

        /* ── Mini Bar ── */
        .mini-bar-bg { height: 4px; background: rgba(255,255,255,0.06); border-radius: 2px; overflow: hidden; flex: 1; }
        .mini-bar-fill { height: 100%; border-radius: 2px; transition: width 0.5s ease; }

        /* ── Endpoint rows ── */
        .endpoint-row { display: flex; flex-direction: column; gap: 6px; padding: 12px 0; border-bottom: 1px solid var(--border); }
        .endpoint-row:last-child { border-bottom: none; }
        .ep-path { font-family: var(--font-mono); font-size: 12px; color: var(--accent-blue); }
        .ep-stats { display: flex; justify-content: space-between; font-size: 11px; color: var(--muted); }
        .ep-hit-rate { color: var(--accent-green); }

        /* ── Request rows ── */
        .req-row {
          display: grid; grid-template-columns: 52px 1fr 130px 62px 60px 80px;
          align-items: center; gap: 12px; padding: 9px 0;
          border-bottom: 1px solid var(--border); font-size: 12px; font-family: var(--font-mono);
        }
        .req-row:last-child { border-bottom: none; }
        .req-badge { background: rgba(61,158,255,0.12); color: var(--accent-blue); padding: 2px 6px; border-radius: 4px; font-size: 10px; font-weight: 700; text-align: center; }
        .req-path { color: var(--text); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
        .req-origin { color: var(--muted); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
        .req-status { font-weight: 700; font-size: 10px; }
        .req-status.hit { color: var(--accent-green); }
        .req-status.miss { color: var(--accent-amber); }
        .req-time { color: var(--muted); }
        .req-ts { color: rgba(255,255,255,0.25); font-size: 10px; }

        /* ── Tester / Settings ── */
        .tester-panel, .settings-panel { background: var(--surface); border: 1px solid var(--border); border-radius: 12px; overflow: hidden; }
        .tester-panel .panel-header, .settings-panel .panel-header {
          display: flex; align-items: center; gap: 10px; padding: 16px 20px;
          border-bottom: 1px solid var(--border); font-size: 13px; font-weight: 600;
          color: var(--muted); text-transform: uppercase; letter-spacing: 0.06em;
        }
        .shortcuts { display: flex; gap: 8px; padding: 16px 20px; flex-wrap: wrap; border-bottom: 1px solid var(--border); }
        .shortcut-btn {
          background: rgba(61,158,255,0.08); border: 1px solid rgba(61,158,255,0.2);
          color: var(--accent-blue); padding: 5px 12px; border-radius: 20px;
          font-size: 12px; cursor: pointer; font-family: var(--font-display);
          font-weight: 600; transition: all 0.2s;
        }
        .shortcut-btn:hover { background: rgba(61,158,255,0.16); }
        .tester-row { display: flex; align-items: center; gap: 10px; padding: 12px 20px; border-bottom: 1px solid var(--border); }
        .method-select {
          background: rgba(255,255,255,0.05); border: 1px solid var(--border);
          color: var(--accent-amber); font-family: var(--font-mono); font-size: 12px;
          padding: 8px 10px; border-radius: 8px; outline: none; cursor: pointer; font-weight: 700;
        }
        .tester-input {
          background: rgba(255,255,255,0.04); border: 1px solid var(--border);
          color: var(--text); font-family: var(--font-mono); font-size: 13px;
          padding: 8px 12px; border-radius: 8px; outline: none; flex: 1; transition: border-color 0.2s;
        }
        .tester-input:focus { border-color: rgba(61,158,255,0.4); }
        .tester-input.full { flex: 1; }
        .send-btn {
          background: var(--accent-green); color: #0a0c10; border: none;
          padding: 8px 16px; border-radius: 8px; font-family: var(--font-display);
          font-size: 13px; font-weight: 700; cursor: pointer;
          display: flex; align-items: center; gap: 6px; transition: all 0.2s; white-space: nowrap;
        }
        .send-btn:hover { filter: brightness(1.1); }
        .send-btn:disabled { opacity: 0.5; cursor: not-allowed; }
        .spinner { width: 12px; height: 12px; border: 2px solid rgba(0,0,0,0.3); border-top-color: #000; border-radius: 50%; animation: spin 0.6s linear infinite; display: inline-block; }
        .result-box { padding: 20px; }
        .result-meta { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; }
        .status-badge { padding: 3px 10px; border-radius: 20px; font-size: 12px; font-family: var(--font-mono); font-weight: 700; }
        .status-badge.s2xx { background: rgba(0,229,160,0.15); color: var(--accent-green); }
        .status-badge.s4xx, .status-badge.s5xx { background: rgba(239,68,68,0.15); color: var(--accent-red); }
        .cache-badge { padding: 3px 10px; border-radius: 20px; font-size: 11px; font-family: var(--font-mono); font-weight: 700; }
        .cache-badge.hit { background: rgba(0,229,160,0.1); color: var(--accent-green); }
        .cache-badge.miss { background: rgba(245,158,11,0.1); color: var(--accent-amber); }
        .elapsed-badge { font-size: 11px; color: var(--muted); font-family: var(--font-mono); background: rgba(255,255,255,0.05); padding: 3px 8px; border-radius: 20px; }
        .result-body { background: rgba(0,0,0,0.3); border: 1px solid var(--border); border-radius: 8px; padding: 16px; font-family: var(--font-mono); font-size: 12px; color: var(--accent-green); overflow: auto; max-height: 320px; line-height: 1.6; white-space: pre-wrap; word-break: break-all; }

        /* ── Settings ── */
        .setting-group { padding: 20px; border-bottom: 1px solid var(--border); }
        .setting-group:last-child { border-bottom: none; }
        .setting-label { display: block; font-size: 12px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.06em; color: var(--muted); margin-bottom: 12px; }
        .setting-label.red { color: var(--accent-red); }
        .setting-hint { font-size: 12px; color: rgba(255,255,255,0.25); margin-top: 10px; }
        .danger-zone { background: rgba(239,68,68,0.03); }
        .danger-btn { background: rgba(239,68,68,0.1); border: 1px solid rgba(239,68,68,0.3); color: var(--accent-red); padding: 10px 20px; border-radius: 8px; font-family: var(--font-display); font-size: 13px; font-weight: 700; cursor: pointer; display: flex; align-items: center; gap: 8px; transition: all 0.2s; }
        .danger-btn:hover:not(:disabled) { background: rgba(239,68,68,0.2); }
        .danger-btn.cleared { background: rgba(0,229,160,0.1); border-color: rgba(0,229,160,0.3); color: var(--accent-green); }

        .security-info { display: flex; flex-direction: column; gap: 10px; }
        .sec-row { display: flex; align-items: center; gap: 10px; font-size: 13px; color: var(--muted); }

        /* ── Tester layout ── */
        .tester-layout { display: grid; grid-template-columns: 1fr 340px; gap: 20px; }
        @media(max-width: 900px) { .tester-layout { grid-template-columns: 1fr; } }

        /* ── Offline banner ── */
        .offline-banner { background: rgba(239,68,68,0.08); border: 1px solid rgba(239,68,68,0.2); border-radius: 10px; padding: 16px 20px; margin-bottom: 20px; display: flex; align-items: center; gap: 12px; font-size: 13px; color: var(--accent-red); }
        .empty { color: var(--muted); font-size: 13px; text-align: center; padding: 32px; }

        ::-webkit-scrollbar { width: 6px; height: 6px; }
        ::-webkit-scrollbar-track { background: transparent; }
        ::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.1); border-radius: 3px; }
      `}</style>

      <div className="shell">
        <header className="header">
          <div className="header-brand">
            <div className="header-logo">
              <Icon d={Icons.zap} size={16} stroke="#0a0c10" />
            </div>
            <div>
              <div className="header-title">CacheProxy</div>
              <div className="header-sub">{API_BASE}</div>
            </div>
          </div>
          <div className="header-right">
            <div className={`api-key-badge`}>
              <Icon d={Icons.key} size={11} /> Authenticated
            </div>
            <div className={`status-dot ${backendUp === false ? "down" : ""}`} />
            <span style={{ fontSize: 12, color: "var(--muted)", fontFamily: "var(--font-mono)" }}>
              {backendUp === false ? "Offline" : "Connected"}
            </span>
            <button className={`refresh-btn ${pulse ? "pulse" : ""}`} onClick={fetchStats}>
              <Icon d={Icons.refresh} size={13} /> Refresh
            </button>
          </div>
        </header>

        <nav className="nav">
          {[
            { id: "dashboard", label: "Dashboard", icon: Icons.activity },
            { id: "tester", label: "Request Tester", icon: Icons.send },
            { id: "settings", label: "Settings", icon: Icons.settings },
          ].map(t => (
            <button key={t.id} className={`nav-btn ${tab === t.id ? "active" : ""}`}
              onClick={() => setTab(t.id)}>
              <Icon d={t.icon} size={14} /> {t.label}
            </button>
          ))}
        </nav>

        <main className="main">
          {backendUp === false && (
            <div className="offline-banner">
              <Icon d={Icons.x} size={16} />
              Backend offline — start Spring Boot on port 8080 to connect.
            </div>
          )}

          {tab === "dashboard" && (
            <>
              <div className="stats-grid">
                <StatCard label="Cache Hits" icon={Icons.zap} value={fmt(stats?.hitCount)} glow="green" accent="var(--accent-green)" sub={`${fmtRate(stats?.hitRate)} hit rate`} />
                <StatCard label="Cache Misses" icon={Icons.globe} value={fmt(stats?.missCount)} glow="amber" accent="var(--accent-amber)" sub={`${fmt(stats?.totalRequests)} total req`} />
                <StatCard label="Cached Items" icon={Icons.database} value={fmt(stats?.estimatedSize)} glow="blue" accent="var(--accent-blue)" sub="Redis backend" />
                <StatCard label="Evictions" icon={Icons.layers} value={fmt(stats?.evictionCount)} accent="var(--muted)" sub="LRU evicted entries" />
              </div>

              <div className="dash-grid">
                <div className="gauge-panel">
                  <HitRateGauge rate={stats?.hitRate ?? 0} />
                  <div style={{ textAlign: "center" }}>
                    <div style={{ fontSize: 13, fontWeight: 700, color: "var(--text)" }}>Cache Efficiency</div>
                    <div style={{ fontSize: 11, color: "var(--muted)", marginTop: 4, fontFamily: "var(--font-mono)" }}>
                      {fmt(stats?.hitCount)} hits / {fmt(stats?.totalRequests)} total
                    </div>
                  </div>
                </div>

                <div className="panel">
                  <div className="panel-header"><Icon d={Icons.trendingUp} size={14} /> Top Endpoints</div>
                  <div className="panel-body" style={{ padding: "0 20px" }}>
                    {stats?.topEndpoints?.length
                      ? stats.topEndpoints.map(ep => <EndpointRow key={ep.path} ep={ep} maxCount={maxCount} />)
                      : <div className="empty">No requests yet</div>}
                  </div>
                </div>

                <div className="panel full-width">
                  <div className="panel-header">
                    <Icon d={Icons.clock} size={14} /> Recent Requests
                    <span style={{ marginLeft: "auto", fontSize: 11, fontFamily: "var(--font-mono)" }}>
                      last {stats?.recentRequests?.length ?? 0}
                    </span>
                  </div>
                  <div style={{ padding: "0 20px" }}>
                    {stats?.recentRequests?.length ? (
                      <>
                        <div className="req-row" style={{ fontSize: 10, color: "var(--muted)", fontFamily: "var(--font-display)", textTransform: "uppercase", letterSpacing: "0.06em", paddingBottom: 0 }}>
                          <span>Method</span><span>Path</span><span>Origin</span><span>Cache</span><span>Time</span><span>At</span>
                        </div>
                        {stats.recentRequests.slice(0, 20).map((req, i) => <RequestRow key={i} req={req} />)}
                      </>
                    ) : <div className="empty">Send a request via the tester to see activity here</div>}
                  </div>
                </div>
              </div>
            </>
          )}

          {tab === "tester" && (
            <div className="tester-layout">
              <ProxyTester apiKey={apiKey} />
              <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
                <div className="panel">
                  <div className="panel-header"><Icon d={Icons.shield} size={14} /> Security Status</div>
                  <div className="panel-body" style={{ display: "flex", flexDirection: "column", gap: 10 }}>
                    {[
                      ["API Key Auth", "Active", "green"],
                      ["Rate Limit", "100 req/min", "green"],
                      ["SSRF Guard", "Active", "green"],
                      ["Origin Allowlist", "Configured", "blue"],
                      ["Request Timeout", "30s", "blue"],
                      ["Cache Backend", "Redis", "blue"],
                    ].map(([label, val, color]) => (
                      <div key={label} style={{ display: "flex", justifyContent: "space-between", fontSize: 13 }}>
                        <span style={{ color: "var(--muted)" }}>{label}</span>
                        <span style={{ fontFamily: "var(--font-mono)", color: `var(--accent-${color})`, fontSize: 12 }}>{val}</span>
                      </div>
                    ))}
                  </div>
                </div>
                <div className="panel">
                  <div className="panel-header"><Icon d={Icons.server} size={14} /> How it works</div>
                  <div className="panel-body" style={{ fontSize: 12, color: "var(--muted)", lineHeight: 1.8, fontFamily: "var(--font-mono)" }}>
                    <p>1st request → <span style={{color:"var(--accent-amber)"}}>MISS</span> → forwards to origin</p>
                    <p style={{ margin: "8px 0" }}>2nd request → <span style={{color:"var(--accent-green)"}}>HIT</span> → Redis cache</p>
                    <p>TTL: <span style={{color:"var(--accent-blue)"}}>1 hour</span> · Persists restarts</p>
                  </div>
                </div>
              </div>
            </div>
          )}

          {tab === "settings" && (
            <div style={{ maxWidth: 640 }}>
              <SettingsPanel onClear={fetchStats} apiKey={apiKey} />
            </div>
          )}
        </main>
      </div>
    </>
  );
}
