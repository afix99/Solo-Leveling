/**
 * There is no web UI by design — the Android app is the only client. This page
 * exists so hitting the domain in a browser explains what the service is
 * rather than returning a 404 that looks like a broken deployment.
 */
export default function Home() {
  return (
    <main style={{ fontFamily: "system-ui, sans-serif", padding: "3rem 1.5rem", maxWidth: 640 }}>
      <p style={{ letterSpacing: "0.2em", fontSize: 12, color: "#7c5cff" }}>AM SYSTEM</p>
      <h1 style={{ fontSize: 28, margin: "0.4rem 0 1rem" }}>Backup and analytics service</h1>
      <p style={{ color: "#9aa0ae", lineHeight: 1.6 }}>
        This is the private backend for the ASCEND habit app. It stores encrypted-at-rest
        backups keyed to a Hunter Key that never leaves your phone in readable form, and
        computes the history analytics the in-app coach reads.
      </p>
      <p style={{ color: "#9aa0ae", lineHeight: 1.6 }}>
        There is nothing to see here without the app. Endpoints:{" "}
        <code>/api/health</code>, <code>/api/sync</code>, <code>/api/report</code>.
      </p>
    </main>
  );
}
