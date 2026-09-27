export function DashboardPage() {
  return (
    <section className="page">
      <h1>Dashboard</h1>
      <p>
        Sprint S0 foundation is live. Use the role switcher to exercise mock auth, then proceed
        feature-by-feature from the PRDs.
      </p>
      <ul className="checklist">
        <li>Backend: Spring Boot + Maven + Flyway + mock RBAC</li>
        <li>Frontend: React feature folders</li>
        <li>Infra: docker-compose (Postgres, RabbitMQ, MinIO)</li>
        <li>Next: Sprint S1 — PRD-01/02 Organization APIs</li>
      </ul>
    </section>
  );
}
