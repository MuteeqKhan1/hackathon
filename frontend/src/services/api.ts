export type UserRole = "CONTENT_OWNER" | "INSTRUCTOR" | "STUDENT";

export interface AuthIdentity {
  userId: string;
  organizationId: string;
  role: UserRole;
}

export interface MeResponse {
  userId: string;
  organizationId: string;
  role: UserRole;
  permissions: string[];
}

export interface OrganizationResponse {
  id: string;
  name: string;
  slug: string;
  status: string;
  createdAt: string;
}

export interface MembershipResponse {
  id: string;
  organizationId: string;
  userId: string;
  role: UserRole;
  createdAt: string;
}

/** Bump when mock defaults change so stale localStorage cannot leave users on empty/wrong orgs. */
const STORAGE_KEY = "alp.auth.identity.v2";

/** Org that owns “Physics Grade 10” in the local seed/dev DB (slug physics-academy). */
export const KNOWN_CONTENT_ORG_ID = "eabfcdf7-80d4-43a6-9d39-77989574ba76";

export const DEFAULT_IDENTITY: AuthIdentity = {
  userId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  organizationId: KNOWN_CONTENT_ORG_ID,
  role: "CONTENT_OWNER",
};

export function loadIdentity(): AuthIdentity {
  localStorage.removeItem("alp.auth.identity"); // discard v1 stuck org (empty duplicate)
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) return DEFAULT_IDENTITY;
  try {
    const parsed = { ...DEFAULT_IDENTITY, ...JSON.parse(raw) } as AuthIdentity;
    // Empty duplicate “Physics Academy” — no materials; force the org that owns content.
    if (parsed.organizationId === "53a72d32-a46b-415d-9c5e-1abe7e4d9a29") {
      return { ...parsed, organizationId: KNOWN_CONTENT_ORG_ID, role: "CONTENT_OWNER" };
    }
    return parsed;
  } catch {
    return DEFAULT_IDENTITY;
  }
}

export function saveIdentity(identity: AuthIdentity): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(identity));
}

function authHeaders(identity: AuthIdentity): HeadersInit {
  return {
    "X-User-Id": identity.userId,
    "X-Org-Id": identity.organizationId,
    "X-Role": identity.role,
    Accept: "application/json",
  };
}

async function parseError(response: Response): Promise<Error> {
  const body = await response.json().catch(() => ({}));
  const message = (body as { message?: string }).message ?? `HTTP ${response.status}`;
  return new Error(message);
}

export async function apiGetBlob(path: string, identity: AuthIdentity): Promise<Blob> {
  const response = await fetch(path, {
    headers: {
      "X-User-Id": identity.userId,
      "X-Org-Id": identity.organizationId,
      "X-Role": identity.role,
      Accept: "*/*",
    },
  });
  if (!response.ok) throw await parseError(response);
  return response.blob();
}

export async function apiGet<T>(path: string, identity: AuthIdentity): Promise<T> {
  const response = await fetch(path, { headers: authHeaders(identity) });
  if (!response.ok) throw await parseError(response);
  return response.json() as Promise<T>;
}

export async function apiPost<T>(path: string, identity: AuthIdentity, body: unknown): Promise<T> {
  const response = await fetch(path, {
    method: "POST",
    headers: {
      ...authHeaders(identity),
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });
  if (!response.ok) throw await parseError(response);
  return response.json() as Promise<T>;
}

export async function apiPatch<T>(path: string, identity: AuthIdentity, body: unknown): Promise<T> {
  const response = await fetch(path, {
    method: "PATCH",
    headers: {
      ...authHeaders(identity),
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });
  if (!response.ok) throw await parseError(response);
  return response.json() as Promise<T>;
}

export async function apiPut<T>(path: string, identity: AuthIdentity, body: unknown): Promise<T> {
  const response = await fetch(path, {
    method: "PUT",
    headers: {
      ...authHeaders(identity),
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });
  if (!response.ok) throw await parseError(response);
  return response.json() as Promise<T>;
}

export interface CourseResponse {
  id: string;
  organizationId: string;
  title: string;
  description: string | null;
  sourceMaterialId: string;
  currentSourceVersionId: string;
  language: string;
  audience: string | null;
  level: string | null;
  durationHours: number | null;
  status: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface CourseTopicResponse {
  id: string;
  chapterId: string;
  title: string;
  sequence: number;
  status: string;
}

export interface CourseChapterResponse {
  id: string;
  courseId: string;
  title: string;
  sequence: number;
  status: string;
  topics: CourseTopicResponse[];
}

export interface CourseDetailResponse extends CourseResponse {
  chapters: CourseChapterResponse[];
}

export interface SourceMaterialResponse {
  id: string;
  organizationId: string;
  title: string;
  description: string | null;
  subject: string | null;
  status: string;
  createdBy: string;
  createdAt: string;
}

export interface SourceVersionResponse {
  id: string;
  sourceMaterialId: string;
  versionNumber: number;
  originalFilename: string;
  fileContentHash: string;
  fileSizeBytes: number;
  status: string;
  createdAt: string;
  publishedAt: string | null;
}

export interface OperationResponse {
  operationId: string;
  type: string;
  status: string;
  progress: number;
  errorCode: string | null;
  errorMessage: string | null;
  resultJson: string | null;
  createdAt: string;
  completedAt: string | null;
}

export interface SourceSectionResponse {
  sectionId: string;
  externalReference: string;
  title: string;
  sectionType: string;
  contentHash: string;
  changeType: string;
  contentPreview: string;
  chunkCount: number;
}

export async function apiPostMultipart<T>(
  path: string,
  identity: AuthIdentity,
  formData: FormData
): Promise<T> {
  const response = await fetch(path, {
    method: "POST",
    headers: authHeaders(identity),
    body: formData,
  });
  if (!response.ok) throw await parseError(response);
  return response.json() as Promise<T>;
}
