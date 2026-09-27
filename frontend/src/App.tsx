import { useEffect, useState } from "react";
import { Link, Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "./services/auth-context";
import { apiGet, type OrganizationResponse } from "./services/api";
import { DashboardPage } from "./features/notifications/DashboardPage";
import { SourceMaterialPage } from "./features/source-material/SourceMaterialPage";
import { CourseAuthoringPage } from "./features/course-authoring/CourseAuthoringPage";
import { SyncPage } from "./features/synchronization/SyncPage";
import { StudentLearningPage } from "./features/student-learning/StudentLearningPage";
import { OrganizationPage } from "./features/organization/OrganizationPage";

export function App() {
  const { identity, setRole, setOrganizationId, me, error, refreshMe } = useAuth();
  const [orgs, setOrgs] = useState<OrganizationResponse[]>([]);

  useEffect(() => {
    void apiGet<OrganizationResponse[]>("/api/v1/organizations", identity)
      .then((list) => {
        setOrgs(list);
        if (list.length === 0) return;
        const stillValid = list.some((o) => o.id === identity.organizationId);
        if (!stillValid && list[0]) {
          setOrganizationId(list[0].id);
        }
      })
      .catch(() => setOrgs([]));
  }, [identity, setOrganizationId]);

  const activeOrg = orgs.find((o) => o.id === identity.organizationId);

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand">
          <span className="brand-mark">ALP</span>
          <div>
            <p className="brand-name">Adaptive Learning Platform</p>
            <p className="brand-sub">Source-synced course authoring</p>
          </div>
        </div>
        <div className="auth-controls">
          <label>
            Org
            <select
              value={identity.organizationId}
              onChange={(e) => setOrganizationId(e.target.value)}
            >
              {orgs.length === 0 && (
                <option value={identity.organizationId}>
                  {identity.organizationId.slice(0, 8)}… (create an org)
                </option>
              )}
              {orgs.map((org) => (
                <option key={org.id} value={org.id}>
                  {org.name} ({org.id.slice(0, 8)}…)
                </option>
              ))}
            </select>
          </label>
          <label>
            Role
            <select
              value={identity.role}
              onChange={(e) => setRole(e.target.value as typeof identity.role)}
            >
              <option value="CONTENT_OWNER">CONTENT_OWNER</option>
              <option value="INSTRUCTOR">INSTRUCTOR</option>
              <option value="STUDENT">STUDENT</option>
            </select>
          </label>
          <button type="button" onClick={() => void refreshMe()}>
            Refresh /me
          </button>
        </div>
      </header>

      <div className="layout">
        <nav className="sidebar">
          <Link to="/">Dashboard</Link>
          <Link to="/organizations">Organizations</Link>
          <Link to="/source-materials">Source Materials</Link>
          <Link to="/courses">Course Authoring</Link>
          <Link to="/sync">Synchronization</Link>
          <Link to="/learn">Student Learning</Link>
        </nav>

        <main className="content">
          {error && <div className="banner error">{error}</div>}
          {me && (
            <div className="banner ok">
              Authenticated as {me.role} · {me.permissions.length} permissions ·{" "}
              {activeOrg ? `${activeOrg.name} (${activeOrg.id.slice(0, 8)}…)` : me.organizationId}
            </div>
          )}
          <p className="muted small" style={{ marginTop: "-0.5rem" }}>
            Source upload/publish: role <strong>CONTENT_OWNER</strong>. Course authoring:{" "}
            <strong>INSTRUCTOR</strong>. Keep the same Org in the top bar for the whole flow.
          </p>
          <Routes>
            <Route path="/" element={<DashboardPage />} />
            <Route path="/organizations" element={<OrganizationPage />} />
            <Route path="/source-materials" element={<SourceMaterialPage />} />
            <Route path="/courses" element={<CourseAuthoringPage />} />
            <Route path="/sync" element={<SyncPage />} />
            <Route path="/learn" element={<StudentLearningPage />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </main>
      </div>
    </div>
  );
}
