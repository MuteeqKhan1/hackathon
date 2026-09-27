import { useCallback, useEffect, useState, type FormEvent } from "react";
import { useAuth } from "../../services/auth-context";
import {
  apiGet,
  apiPost,
  apiPostMultipart,
  type AuthIdentity,
  type OperationResponse,
  type SourceMaterialResponse,
  type SourceSectionResponse,
  type SourceVersionResponse,
} from "../../services/api";

export function SourceMaterialPage() {
  const { identity, setRole, setOrganizationId } = useAuth();
  const [materials, setMaterials] = useState<SourceMaterialResponse[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [versions, setVersions] = useState<SourceVersionResponse[]>([]);
  const [sections, setSections] = useState<SourceSectionResponse[]>([]);
  const [selectedVersionId, setSelectedVersionId] = useState<string | null>(null);
  const [title, setTitle] = useState("");
  const [subject, setSubject] = useState("Physics");
  const [file, setFile] = useState<File | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [operation, setOperation] = useState<OperationResponse | null>(null);

  /** Upload/publish as CONTENT_OWNER in the material's org (or active org). */
  function ownerAuthForMaterial(materialId: string | null): AuthIdentity {
    const material = materials.find((m) => m.id === materialId);
    const organizationId = material?.organizationId ?? identity.organizationId;
    setRole("CONTENT_OWNER");
    if (organizationId !== identity.organizationId) {
      setOrganizationId(organizationId);
    }
    return {
      userId: identity.userId,
      organizationId,
      role: "CONTENT_OWNER",
    };
  }

  useEffect(() => {
    setSelectedId(null);
    setVersions([]);
    setSections([]);
    setSelectedVersionId(null);
  }, [identity.organizationId]);

  const loadMaterials = useCallback(async () => {
    try {
      const data = await apiGet<SourceMaterialResponse[]>("/api/v1/source-materials", identity);
      setMaterials(data);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load materials");
    }
  }, [identity]);

  const loadVersions = useCallback(
    async (materialId: string, auth: AuthIdentity = identity) => {
      try {
        const data = await apiGet<SourceVersionResponse[]>(
          `/api/v1/source-materials/${materialId}/versions`,
          auth
        );
        setVersions(data);
        const published = [...data].reverse().find((v) => v.status === "PUBLISHED");
        setSelectedVersionId(published?.id ?? null);
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to load versions");
      }
    },
    [identity]
  );

  const loadSections = useCallback(
    async (materialId: string, versionId: string) => {
      try {
        const data = await apiGet<SourceSectionResponse[]>(
          `/api/v1/source-materials/${materialId}/versions/${versionId}/sections`,
          identity
        );
        setSections(data);
      } catch {
        setSections([]);
      }
    },
    [identity]
  );

  useEffect(() => {
    void loadMaterials();
  }, [loadMaterials]);

  useEffect(() => {
    if (selectedId) void loadVersions(selectedId);
  }, [selectedId, loadVersions]);

  useEffect(() => {
    if (selectedId && selectedVersionId) {
      void loadSections(selectedId, selectedVersionId);
    } else {
      setSections([]);
    }
  }, [selectedId, selectedVersionId, loadSections]);

  async function onCreate(e: FormEvent) {
    e.preventDefault();
    setMessage(null);
    setError(null);
    const auth = ownerAuthForMaterial(null);
    try {
      const created = await apiPost<SourceMaterialResponse>("/api/v1/source-materials", auth, {
        title,
        subject,
        description: "",
      });
      setTitle("");
      setSelectedId(created.id);
      setMessage(`Created material ${created.title}`);
      await loadMaterials();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Create failed");
    }
  }

  async function onUpload(e: FormEvent) {
    e.preventDefault();
    if (!selectedId || !file) {
      setError("Select a material and a PDF file");
      return;
    }
    const auth = ownerAuthForMaterial(selectedId);
    setMessage(null);
    setError(null);
    try {
      const form = new FormData();
      form.append("file", file);
      const version = await apiPostMultipart<SourceVersionResponse>(
        `/api/v1/source-materials/${selectedId}/versions`,
        auth,
        form
      );
      setFile(null);
      setMessage(`Uploaded version ${version.versionNumber} (DRAFT)`);
      await loadVersions(selectedId, auth);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Upload failed");
    }
  }

  async function onPublish(versionId: string) {
    if (!selectedId) return;
    const auth = ownerAuthForMaterial(selectedId);
    setMessage(null);
    setError(null);
    try {
      const accepted = await apiPost<{ operationId: string; status: string; progress: number }>(
        `/api/v1/source-materials/${selectedId}/versions/${versionId}/publish`,
        auth,
        {}
      );
      setMessage(`Publish started: operation ${accepted.operationId}`);
      await pollOperation(accepted.operationId, auth);
      await loadVersions(selectedId, auth);
      setSelectedVersionId(versionId);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Publish failed");
    }
  }

  async function pollOperation(operationId: string, auth: AuthIdentity) {
    for (let i = 0; i < 40; i++) {
      const op = await apiGet<OperationResponse>(`/api/v1/operations/${operationId}`, auth);
      setOperation(op);
      if (op.status === "COMPLETED" || op.status === "FAILED") {
        setMessage(
          op.status === "COMPLETED"
            ? `Publish completed — sections parsed (operation ${operationId})`
            : `Publish failed: ${op.errorMessage ?? op.errorCode}`
        );
        return;
      }
      await new Promise((r) => setTimeout(r, 500));
    }
    setMessage(`Publish still running — refresh versions in a few seconds (operation ${operationId})`);
  }

  function resetMockAuth() {
    setRole("CONTENT_OWNER");
    setMessage(`Role set to CONTENT_OWNER · keep Org ${identity.organizationId.slice(0, 8)}… selected.`);
    setError(null);
  }

  return (
    <section className="page">
      <h1>Source Materials</h1>
      <p className="muted">PRD-03/04 — PDF upload, publish, section fingerprints &amp; chunks.</p>
      <p className="muted small">
        Active role: <strong>{identity.role}</strong> · Org:{" "}
        <strong>{identity.organizationId.slice(0, 8)}…</strong>
        {" · "}
        <button type="button" className="linkish" onClick={resetMockAuth}>
          Reset mock auth
        </button>
      </p>
      {materials.length === 0 && (
        <div className="banner error">
          No materials in this org yet. Create one below, or switch <strong>Org</strong> in the top bar
          to the org where you created the material (<code>{identity.organizationId.slice(0, 8)}…</code>).
        </div>
      )}
      {message && <div className="banner ok">{message}</div>}
      {error && <div className="banner error">{error}</div>}
      {operation && (
        <div className="banner ok">
          Operation {operation.operationId.slice(0, 8)}… · {operation.status} · {operation.progress}%
          {operation.resultJson ? ` · ${operation.resultJson}` : ""}
        </div>
      )}

      <div className="panel-grid">
        <form className="panel" onSubmit={onCreate}>
          <h2>Create material</h2>
          <label>
            Title
            <input value={title} onChange={(e) => setTitle(e.target.value)} required />
          </label>
          <label>
            Subject
            <input value={subject} onChange={(e) => setSubject(e.target.value)} />
          </label>
          <button type="submit">Create</button>
        </form>

        <form className="panel" onSubmit={onUpload}>
          <h2>Upload PDF version</h2>
          <p className="muted small">
            Selected: {selectedId ? selectedId.slice(0, 8) + "…" : "none"}
          </p>
          <label>
            PDF file
            <input
              type="file"
              accept="application/pdf,.pdf"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            />
          </label>
          <button type="submit" disabled={!selectedId || !file}>
            Upload version
          </button>
        </form>
      </div>

      <div className="panel">
        <h2>Materials</h2>
        <ul className="list">
          {materials.map((m) => (
            <li key={m.id}>
              <button type="button" className="linkish" onClick={() => setSelectedId(m.id)}>
                {m.title}
              </button>
              <span className="muted">
                {" "}
                · {m.subject ?? "—"} · {m.status}
              </span>
              {selectedId === m.id && <span className="tag">selected</span>}
            </li>
          ))}
        </ul>
      </div>

      <div className="panel">
        <h2>Versions</h2>
        <ul className="list">
          {versions.map((v) => (
            <li key={v.id}>
              <button type="button" className="linkish" onClick={() => setSelectedVersionId(v.id)}>
                v{v.versionNumber}
              </button>{" "}
              · {v.status} · {v.originalFilename}
              {v.status === "DRAFT" && (
                <>
                  {" "}
                  <button type="button" onClick={() => void onPublish(v.id)}>
                    Publish
                  </button>
                </>
              )}
              {selectedVersionId === v.id && <span className="tag">sections</span>}
            </li>
          ))}
        </ul>
      </div>

      <div className="panel">
        <h2>Parsed sections</h2>
        <ul className="list">
          {sections.map((s) => (
            <li key={s.sectionId}>
              <strong>{s.externalReference}</strong> · {s.title} · {s.changeType} · chunks{" "}
              {s.chunkCount}
              <div className="muted small">{s.contentPreview}</div>
            </li>
          ))}
          {selectedVersionId && sections.length === 0 && (
            <li className="muted">No sections yet — publish a version with text PDF content.</li>
          )}
        </ul>
      </div>
    </section>
  );
}
