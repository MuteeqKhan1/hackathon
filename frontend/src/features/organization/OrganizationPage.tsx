import { useCallback, useEffect, useState, type FormEvent } from "react";
import {
  apiGet,
  apiPost,
  type MembershipResponse,
  type OrganizationResponse,
  type UserRole,
} from "../../services/api";
import { useAuth } from "../../services/auth-context";

export function OrganizationPage() {
  const { identity, setOrganizationId } = useAuth();
  const [orgs, setOrgs] = useState<OrganizationResponse[]>([]);
  const [members, setMembers] = useState<MembershipResponse[]>([]);
  const [name, setName] = useState("");
  const [memberEmail, setMemberEmail] = useState("");
  const [memberRole, setMemberRole] = useState<UserRole>("INSTRUCTOR");
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const loadOrgs = useCallback(async () => {
    try {
      const data = await apiGet<OrganizationResponse[]>("/api/v1/organizations", identity);
      setOrgs(data);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load organizations");
    }
  }, [identity]);

  const loadMembers = useCallback(async () => {
    try {
      const data = await apiGet<MembershipResponse[]>(
        `/api/v1/organizations/${identity.organizationId}/users`,
        identity
      );
      setMembers(data);
    } catch {
      setMembers([]);
    }
  }, [identity]);

  useEffect(() => {
    void loadOrgs();
    void loadMembers();
  }, [loadOrgs, loadMembers]);

  async function onCreate(e: FormEvent) {
    e.preventDefault();
    setMessage(null);
    setError(null);
    try {
      const created = await apiPost<OrganizationResponse>("/api/v1/organizations", identity, {
        name,
      });
      setOrganizationId(created.id);
      setName("");
      setMessage(`Created ${created.name}. Active org header switched to ${created.id}.`);
      await loadOrgs();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Create failed");
    }
  }

  async function onAddMember(e: FormEvent) {
    e.preventDefault();
    setMessage(null);
    setError(null);
    try {
      await apiPost<MembershipResponse>(
        `/api/v1/organizations/${identity.organizationId}/users`,
        identity,
        { email: memberEmail, displayName: memberEmail, role: memberRole }
      );
      setMemberEmail("");
      setMessage("Member added.");
      await loadMembers();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Add member failed");
    }
  }

  return (
    <section className="page">
      <h1>Organizations</h1>
      <p className="muted">PRD-02 — create tenant orgs and attach users with roles.</p>

      {message && <div className="banner ok">{message}</div>}
      {error && <div className="banner error">{error}</div>}

      <div className="panel-grid">
        <form className="panel" onSubmit={onCreate}>
          <h2>Create organization</h2>
          <label>
            Name
            <input value={name} onChange={(e) => setName(e.target.value)} required />
          </label>
          <button type="submit">Create</button>
        </form>

        <form className="panel" onSubmit={onAddMember}>
          <h2>Add member to active org</h2>
          <p className="muted small">Org: {identity.organizationId}</p>
          <label>
            Email
            <input
              type="email"
              value={memberEmail}
              onChange={(e) => setMemberEmail(e.target.value)}
              required
            />
          </label>
          <label>
            Role
            <select
              value={memberRole}
              onChange={(e) => setMemberRole(e.target.value as UserRole)}
            >
              <option value="CONTENT_OWNER">CONTENT_OWNER</option>
              <option value="INSTRUCTOR">INSTRUCTOR</option>
              <option value="STUDENT">STUDENT</option>
            </select>
          </label>
          <button type="submit">Add member</button>
        </form>
      </div>

      <div className="panel">
        <h2>My organizations</h2>
        <ul className="list">
          {orgs.map((org) => (
            <li key={org.id}>
              <button type="button" className="linkish" onClick={() => setOrganizationId(org.id)}>
                {org.name}
              </button>
              <span className="muted">
                {" "}
                · {org.slug} · {org.id.slice(0, 8)}… · {org.status}
              </span>
              {org.id === identity.organizationId && <span className="tag">active</span>}
            </li>
          ))}
          {orgs.length === 0 && <li className="muted">No memberships yet — create an org.</li>}
        </ul>
      </div>

      <div className="panel">
        <h2>Members (active org)</h2>
        <ul className="list">
          {members.map((m) => (
            <li key={m.id}>
              {m.userId.slice(0, 8)}… · {m.role}
            </li>
          ))}
          {members.length === 0 && <li className="muted">Switch to an org you belong to.</li>}
        </ul>
      </div>
    </section>
  );
}
