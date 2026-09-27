import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import {
  apiGet,
  loadIdentity,
  saveIdentity,
  type AuthIdentity,
  type MeResponse,
  type UserRole,
} from "./api";

interface AuthContextValue {
  identity: AuthIdentity;
  me: MeResponse | null;
  error: string | null;
  setRole: (role: UserRole) => void;
  setOrganizationId: (organizationId: string) => void;
  refreshMe: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [identity, setIdentity] = useState<AuthIdentity>(() => loadIdentity());
  const [me, setMe] = useState<MeResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Re-apply storage migration after HMR / first paint (useState init may have used old module).
  useEffect(() => {
    const fixed = loadIdentity();
    setIdentity((prev) =>
      prev.organizationId === fixed.organizationId && prev.role === fixed.role && prev.userId === fixed.userId
        ? prev
        : fixed
    );
  }, []);

  const refreshMe = useCallback(async () => {
    try {
      const result = await apiGet<MeResponse>("/api/v1/me", identity);
      setMe(result);
      setError(null);
    } catch (err) {
      setMe(null);
      setError(err instanceof Error ? err.message : "Failed to call /api/v1/me");
    }
  }, [identity]);

  useEffect(() => {
    saveIdentity(identity);
    void refreshMe();
  }, [identity, refreshMe]);

  const setRole = useCallback((role: UserRole) => {
    setIdentity((prev) => ({ ...prev, role }));
  }, []);

  const setOrganizationId = useCallback((organizationId: string) => {
    setIdentity((prev) => ({ ...prev, organizationId }));
  }, []);

  const value = useMemo(
    () => ({ identity, me, error, setRole, setOrganizationId, refreshMe }),
    [identity, me, error, setRole, setOrganizationId, refreshMe]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
