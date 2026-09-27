import { useEffect, useMemo, useState } from "react";
import { useAuth } from "../../services/auth-context";
import { apiGet, apiPost } from "../../services/api";

type SyncEvent = {
  id: string;
  sourceMaterialId: string;
  oldVersionId: string;
  newVersionId: string;
  status: string;
  idempotencyKey: string;
  createdAt: string;
};

type Impact = {
  id: string;
  courseId: string;
  contentAssetId: string;
  sourceSectionId: string | null;
  impactLevel: string;
  reason: string;
  recommendedAction: string;
  status: string;
  changeSignature: string | null;
};

type ActionResult = {
  id: string;
  action: string;
  status: string;
  newAssetVersionId: string | null;
  detail: string | null;
};

export function SyncPage() {
  const { identity, setRole } = useAuth();
  const [events, setEvents] = useState<SyncEvent[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [impacts, setImpacts] = useState<Impact[]>([]);
  const [busyAssetId, setBusyAssetId] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (identity.role !== "INSTRUCTOR") setRole("INSTRUCTOR");
  }, [identity.role, setRole]);

  async function loadEvents() {
    setError(null);
    try {
      const list = await apiGet<SyncEvent[]>("/api/v1/sync-events", identity);
      setEvents(list);
      if (list.length > 0 && !selectedId) {
        setSelectedId(list[0].id);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load sync events");
    }
  }

  async function loadImpact(eventId: string) {
    setError(null);
    try {
      const list = await apiGet<Impact[]>(`/api/v1/sync-events/${eventId}/impact`, identity);
      setImpacts(list);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load impact");
    }
  }

  useEffect(() => {
    void loadEvents();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [identity.organizationId, identity.role]);

  useEffect(() => {
    if (selectedId) void loadImpact(selectedId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedId]);

  const openImpacts = useMemo(() => impacts.filter((i) => i.status === "OPEN"), [impacts]);
  const closedImpacts = useMemo(() => impacts.filter((i) => i.status !== "OPEN"), [impacts]);

  async function act(assetId: string, action: "REGENERATE" | "IGNORE" | "ACCEPT" | "REJECT") {
    if (!selectedId) return;
    setMessage(null);
    setError(null);
    setBusyAssetId(assetId);
    try {
      const result = await apiPost<ActionResult>(
        `/api/v1/sync-events/${selectedId}/actions`,
        identity,
        { assetId, action }
      );
      setMessage(
        `${result.action} → ${result.status}${result.detail ? ` · ${result.detail}` : ""}. ` +
          (action === "REGENERATE"
            ? "Open Course Authoring → Approve the new draft, then students see it."
            : "")
      );
      await loadImpact(selectedId);
      await loadEvents();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Action failed");
    } finally {
      setBusyAssetId(null);
    }
  }

  async function regenerateAllOpen() {
    if (!selectedId || openImpacts.length === 0) return;
    setMessage(`Regenerating ${openImpacts.length} asset(s)…`);
    for (const impact of openImpacts) {
      await act(impact.contentAssetId, "REGENERATE");
    }
  }

  return (
    <section className="page">
      <h1>Synchronization</h1>
      <p className="muted">
        After a new PDF version is published, review each affected lesson asset (one row per asset).
      </p>
      <p className="muted small">
        Role <strong>INSTRUCTOR</strong>. Recommended: <strong>Regenerate</strong> → Course Authoring →{" "}
        <strong>Approve</strong> → students see updates on the same course.
      </p>
      {message && <div className="banner ok">{message}</div>}
      {error && <div className="banner error">{error}</div>}

      <div className="panel-grid">
        <div className="panel">
          <h2>Sync events</h2>
          <button type="button" onClick={() => void loadEvents()}>
            Refresh
          </button>
          <ul className="list">
            {events.length === 0 && <li className="muted">No sync events yet.</li>}
            {events.map((e) => (
              <li key={e.id}>
                <button
                  type="button"
                  className={selectedId === e.id ? "link selected" : "link"}
                  onClick={() => setSelectedId(e.id)}
                >
                  {e.status} · {e.id.slice(0, 8)}… · {e.createdAt.slice(0, 19)}
                </button>
              </li>
            ))}
          </ul>
        </div>

        <div className="panel">
          <h2>Impact (unique assets)</h2>
          {!selectedId && <p className="muted">Select a sync event.</p>}
          {selectedId && (
            <p className="muted small">
              {openImpacts.length} open · {closedImpacts.length} closed
            </p>
          )}
          {selectedId && openImpacts.length > 0 && (
            <p style={{ marginBottom: "0.75rem" }}>
              <button type="button" onClick={() => void regenerateAllOpen()} disabled={busyAssetId != null}>
                Regenerate all open ({openImpacts.length})
              </button>
            </p>
          )}
          {selectedId && impacts.length === 0 && (
            <p className="muted">No impacts (unchanged hashes or no lineage).</p>
          )}
          {openImpacts.map((i) => (
            <div key={i.contentAssetId} className="impact-card">
              <p>
                <strong>{i.impactLevel}</strong> · {i.recommendedAction} · {i.status}
              </p>
              <p className="muted small">{i.reason}</p>
              <p className="muted small">
                asset {i.contentAssetId.slice(0, 8)}… · course {i.courseId.slice(0, 8)}…
              </p>
              <div style={{ display: "flex", gap: "0.5rem", flexWrap: "wrap", marginTop: "0.5rem" }}>
                <button
                  type="button"
                  disabled={busyAssetId === i.contentAssetId}
                  onClick={() => void act(i.contentAssetId, "REGENERATE")}
                >
                  {busyAssetId === i.contentAssetId ? "Working…" : "Regenerate"}
                </button>
                <button type="button" onClick={() => void act(i.contentAssetId, "IGNORE")}>
                  Ignore
                </button>
                <button type="button" onClick={() => void act(i.contentAssetId, "ACCEPT")}>
                  Accept (keep current)
                </button>
                <button type="button" onClick={() => void act(i.contentAssetId, "REJECT")}>
                  Reject
                </button>
              </div>
            </div>
          ))}
          {closedImpacts.length > 0 && (
            <details style={{ marginTop: "1rem" }}>
              <summary className="muted">Closed impacts ({closedImpacts.length})</summary>
              {closedImpacts.map((i) => (
                <p key={i.id} className="muted small">
                  {i.impactLevel} · {i.status} · asset {i.contentAssetId.slice(0, 8)}…
                </p>
              ))}
            </details>
          )}
        </div>
      </div>
    </section>
  );
}
