const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

async function request<T = unknown>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    ...options,
  });
  if (!res.ok) {
    const error = await res.json().catch(() => ({}));
    throw new Error((error as { message?: string }).message ?? res.statusText);
  }
  return res.json() as Promise<T>;
}

// ---------- Auth ----------
export const authApi = {
  signup: (body: { email: string; nickname: string; password: string; imageUrl?: string }) =>
    request('/api/v1/auth/signup', { method: 'POST', body: JSON.stringify(body) }),

  login: (body: { email: string; password: string }) =>
    request<{ accessToken: string }>('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify(body),
    }),

  logout: () => request('/api/v1/auth/logout', { method: 'POST' }),
};

// ---------- User ----------
export const userApi = {
  getMe: () => request('/api/v1/users/me'),
  getMyTrips: () => request<unknown[]>('/api/v1/users/me/trip'),
};

// ---------- Feed ----------
export const feedApi = {
  getTopLiked: () => request<unknown[]>('/api/v1/feed/trips/top-liked'),
  getRecent: () => request<unknown[]>('/api/v1/feed/trips/recent'),
};

// ---------- Trips ----------
export const tripApi = {
  create: (body: {
    title: string;
    country: string;
    city: string;
    startDate: string;
    endDate: string;
    isPublic: boolean;
  }) => request<{ id: string }>('/api/v1/trips', { method: 'POST', body: JSON.stringify(body) }),

  getOne: (tripId: string) => request(`/api/v1/trips/${tripId}`),

  update: (
    tripId: string,
    body: Partial<{
      title: string;
      country: string;
      city: string;
      startDate: string;
      endDate: string;
      isPublic: boolean;
    }>,
  ) =>
    request(`/api/v1/trips/${tripId}`, { method: 'PATCH', body: JSON.stringify(body) }),

  delete: (tripId: string) => request(`/api/v1/trips/${tripId}`, { method: 'DELETE' }),

  uploadImages: async (tripId: string, formData: FormData) => {
    const res = await fetch(`${BASE_URL}/api/v1/trips/${tripId}/images`, {
      method: 'POST',
      credentials: 'include',
      body: formData,
    });
    if (!res.ok) throw new Error('이미지 업로드 실패');
    return res.json();
  },

  generateAutoRecords: (tripId: string) =>
    request(`/api/v1/trips/${tripId}/auto-records`, { method: 'POST' }),
};

// ---------- Likes ----------
export const likeApi = {
  like: (tripId: string) =>
    request(`/api/v1/trips/${tripId}/likes`, { method: 'POST' }),

  unlike: (tripId: string) =>
    request(`/api/v1/trips/${tripId}/likes`, { method: 'DELETE' }),
};

// ---------- Posts ----------
export const postApi = {
  getList: (tripId: string) => request<unknown[]>(`/api/v1/trips/${tripId}/posts`),

  update: (
    tripId: string,
    postId: string,
    body: Partial<{ title: string; content: string; date: string; time: string }>,
  ) =>
    request(`/api/v1/trips/${tripId}/posts/${postId}`, {
      method: 'PATCH',
      body: JSON.stringify(body),
    }),

  delete: (tripId: string, postId: string) =>
    request(`/api/v1/trips/${tripId}/posts/${postId}`, { method: 'DELETE' }),

  addImages: (tripId: string, postId: string, formData: FormData) =>
    fetch(`${BASE_URL}/api/v1/trips/${tripId}/posts/${postId}/images`, {
      method: 'POST',
      credentials: 'include',
      body: formData,
    }).then((r) => r.json()),

  deleteImage: (tripId: string, postId: string, imageId: string) =>
    request(`/api/v1/trips/${tripId}/posts/${postId}/images/${imageId}`, { method: 'DELETE' }),
};

// ---------- Markers ----------
export const markerApi = {
  update: (
    postId: string,
    markerId: string,
    body: Partial<{
      placeName: string;
      lat: number;
      lng: number;
      visitTime: string;
      source: string;
    }>,
  ) =>
    request(`/api/v1/posts/${postId}/markers/${markerId}`, {
      method: 'PATCH',
      body: JSON.stringify(body),
    }),

  delete: (postId: string, markerId: string) =>
    request(`/api/v1/posts/${postId}/markers/${markerId}`, { method: 'DELETE' }),

  // TODO: 장소 후보 조회 API 확인 필요
  getCandidates: (query: string) =>
    request<unknown[]>(`/api/v1/posts/markers/place-candidates?query=${encodeURIComponent(query)}`),
};
