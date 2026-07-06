const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

type ApiResponse<T> = {
  resultCode: string;
  msg: string;
  data: T;
};

export type ImageUploadResult = {
  fileName: string | null;
  id: number | null;
  originalFileUrl: string | null;
  thumbnailUrl: string | null;
  mimeType: string | null;
  uploadStatus: 'STORED' | 'FAILED';
};

export type ProfileImageUploadResult = {
  profileImageUrl: string;
};

function getAccessToken() {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem('accessToken');
}

export function isAuthenticated() {
  return !!getAccessToken();
}

function setAccessToken(token: string) {
  if (typeof window === 'undefined') return;
  localStorage.setItem('accessToken', token);
  window.dispatchEvent(new Event('auth-change'));
}

function clearAccessToken() {
  if (typeof window === 'undefined') return;
  localStorage.removeItem('accessToken');
  window.dispatchEvent(new Event('auth-change'));
}

function toDateInput(value: unknown) {
  return typeof value === 'string' ? value.slice(0, 10) : '';
}

function toDateTime(value?: string) {
  if (!value) return value;
  return value.includes('T') ? value : `${value}T00:00:00`;
}

function toAssetUrl(value: unknown) {
  if (typeof value !== 'string' || !value) return '';
  if (value.startsWith('http://') || value.startsWith('https://')) return value;
  if (value.startsWith('/')) return `${BASE_URL}${value}`;
  return value;
}

function normalizeTrip(trip: Record<string, unknown>) {
  const rawAuthor = trip.author as Record<string, unknown> | null | undefined;
  const ownerId = trip.ownerId ?? rawAuthor?.id;
  const author = rawAuthor
    ? {
        ...rawAuthor,
        id: String(rawAuthor.id ?? ownerId),
        nickname: String(rawAuthor.nickname ?? rawAuthor.username ?? '작성자'),
        profileImageUrl: toAssetUrl(rawAuthor.profileImageUrl),
      }
    : {
        id: ownerId != null ? String(ownerId) : '',
        nickname: String(trip.ownerNickname ?? trip.username ?? '작성자'),
        profileImageUrl: toAssetUrl(trip.ownerProfileImageUrl ?? trip.profileImageUrl),
      };

  return {
    ...trip,
    id: String(trip.id),
    ownerId: ownerId != null ? String(ownerId) : undefined,
    author,
    thumbnailUrl: toAssetUrl(trip.thumbnailUrl ?? trip.representativeImageUrl ?? trip.representativeThumbnailUrl),
    representativeLat: trip.representativeLat == null ? undefined : Number(trip.representativeLat),
    representativeLng: trip.representativeLng == null ? undefined : Number(trip.representativeLng),
    startDate: toDateInput(trip.startDate),
    endDate: toDateInput(trip.endDate),
    isPublic: typeof trip.isPublic === 'boolean' ? trip.isPublic : !!trip.visibility,
    likeCount: Number(trip.likeCount ?? 0),
    recordCount: Number(trip.recordCount ?? 0),
  };
}

function normalizeUser(user: Record<string, unknown>) {
  return {
    ...user,
    id: String(user.id),
    profileImageUrl: toAssetUrl(user.profileImageUrl),
  };
}

function toTimeInput(value: unknown) {
  if (typeof value !== 'string' || !value) return undefined;
  const timePart = value.includes('T') ? value.split('T')[1] : value;
  return timePart.slice(0, 5);
}

function normalizeMarker(marker: Record<string, unknown> | null | undefined) {
  if (!marker) return undefined;

  return {
    id: String(marker.id),
    placeName: String(marker.placeName ?? '위치 미정'),
    lat: Number(marker.lat ?? marker.centerLat),
    lng: Number(marker.lng ?? marker.centerLng),
    representativeImageUrl: toAssetUrl(marker.representativeImageUrl ?? marker.representativeThumbnailUrl ?? marker.thumbnailUrl),
    visitTime: typeof marker.visitTime === 'string' ? marker.visitTime : marker.visitedAt,
    source: typeof marker.source === 'string' ? marker.source : undefined,
  };
}

function normalizeTripImage(image: Record<string, unknown>) {
  return {
    id: String(image.id),
    url: toAssetUrl(image.originalFileUrl ?? image.url ?? image.thumbnailUrl),
    thumbnailUrl: toAssetUrl(image.thumbnailUrl ?? image.originalFileUrl ?? image.url),
    filename: String(image.fileName ?? image.filename ?? image.originalFileUrl ?? image.id),
    postId: image.postId == null ? undefined : String(image.postId),
  };
}

function normalizePost(post: Record<string, unknown>) {
  const images = Array.isArray(post.images)
    ? post.images.map((image) => {
        const item = image as Record<string, unknown>;
        return {
          id: String(item.id),
          url: toAssetUrl(item.thumbnailUrl ?? item.originalFileUrl ?? item.url),
          filename: String(item.fileName ?? item.filename ?? item.originalFileUrl ?? item.id),
        };
      })
    : [];
  const rawMarker = post.marker as Record<string, unknown> | null | undefined;
  const marker = normalizeMarker(rawMarker);

  return {
    ...post,
    id: String(post.id),
    tripId: String(post.tripId),
    content: post.content ?? post.memo ?? '',
    time: post.time ?? toTimeInput(marker?.visitTime),
    images,
    marker,
  };
}

function normalizeAutoRecordResult(result: Record<string, unknown>) {
  const records = Array.isArray(result.records)
    ? result.records.map((record) => {
        const item = record as Record<string, unknown>;
        return {
          ...item,
          representativeThumbnailUrl: toAssetUrl(item.representativeThumbnailUrl),
        };
      })
    : [];

  return {
    ...result,
    records,
  };
}

async function request<T = unknown>(path: string, options?: RequestInit): Promise<T> {
  const token = getAccessToken();
  const res = await fetch(`${BASE_URL}${path}`, {
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options?.headers,
    },
    ...options,
  });
  if (!res.ok) {
    const error = await res.json().catch(() => ({}));
    throw new Error((error as { msg?: string; message?: string }).msg ?? (error as { message?: string }).message ?? res.statusText);
  }
  const json = await res.json();
  if (json && typeof json === 'object' && 'data' in json) {
    return (json as ApiResponse<T>).data;
  }
  return json as T;
}

function getListData<T>(value: unknown): T[] {
  if (Array.isArray(value)) return value as T[];
  if (value && typeof value === 'object') {
    const record = value as Record<string, unknown>;
    const candidates = [record.content, record.items, record.list, record.images, record.data, record.results];
    const found = candidates.find(Array.isArray);
    if (found) return found as T[];
  }
  return [];
}

// ---------- Auth ----------
export const authApi = {
  signup: (body: { email: string; username: string; password: string; profileImageUrl?: string }) =>
    request('/api/v1/auth/signup', { method: 'POST', body: JSON.stringify(body) }),

  uploadProfileImage: async (formData: FormData) => {
    const res = await fetch(`${BASE_URL}/api/v1/profile-images`, {
      method: 'POST',
      credentials: 'include',
      body: formData,
    });

    if (!res.ok) {
      const error = await res.json().catch(() => ({}));
      throw new Error(
        (error as { msg?: string; message?: string }).msg ??
          (error as { message?: string }).message ??
          '프로필 이미지 업로드 실패',
      );
    }

    const json = await res.json();
    if (json && typeof json === 'object' && 'data' in json) {
      return (json as ApiResponse<ProfileImageUploadResult>).data;
    }
    return json as ProfileImageUploadResult;
  },

  login: async (body: { email: string; password: string }) => {
    const data = await request<{ accessToken: string }>('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify(body),
    });
    setAccessToken(data.accessToken);
    return data;
  },

  logout: async () => {
    try {
      return await request('/api/v1/auth/logout', { method: 'POST' });
    } finally {
      clearAccessToken();
    }
  },
};

// ---------- User ----------
export const userApi = {
  getMe: async () => {
    const user = await request<Record<string, unknown>>('/api/v1/users/me');
    return normalizeUser(user);
  },
  getMyTrips: async (params?: { page?: number; size?: number }) => {
    const query = params ? `?page=${params.page ?? 0}&size=${params.size ?? 12}` : '';
    const result = await request<unknown>(`/api/v1/users/me/trips${query}`);
    return getListData<Record<string, unknown>>(result).map(normalizeTrip);
  },
};

// ---------- Feed ----------
export const feedApi = {
  getTopLiked: async () => {
    const trips = await request<Record<string, unknown>[]>('/api/v1/feed/trips/top-liked');
    return trips.map(normalizeTrip);
  },
  getRecent: async (params?: { page?: number; size?: number }) => {
    const query = params ? `?page=${params.page ?? 0}&size=${params.size ?? 10}` : '';
    const result = await request<unknown>(`/api/v1/feed/trips/recent${query}`);
    return getListData<Record<string, unknown>>(result).map(normalizeTrip);
  },
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
  }) =>
    request<Record<string, unknown>>('/api/v1/trips', {
      method: 'POST',
      body: JSON.stringify({
        title: body.title,
        country: body.country,
        city: body.city,
        startDate: toDateTime(body.startDate),
        endDate: toDateTime(body.endDate),
        visibility: body.isPublic,
      }),
    }).then(normalizeTrip),

  getOne: async (tripId: string) => {
    const trip = await request<Record<string, unknown>>(`/api/v1/trips/${tripId}`);
    return normalizeTrip(trip);
  },

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
    request(`/api/v1/trips/${tripId}`, {
      method: 'PATCH',
      body: JSON.stringify({
        title: body.title,
        country: body.country,
        city: body.city,
        startDate: toDateTime(body.startDate),
        endDate: toDateTime(body.endDate),
        visibility: body.isPublic,
      }),
    }),

  delete: (tripId: string) => request(`/api/v1/trips/${tripId}`, { method: 'DELETE' }),

  getImages: async (tripId: string) => {
    const result = await request<unknown>(`/api/v1/trips/${tripId}/images`);
    return getListData<Record<string, unknown>>(result).map(normalizeTripImage);
  },

  updateRepresentativeImage: (tripId: string, imageId: string) =>
    request<Record<string, unknown>>(`/api/v1/trips/${tripId}/representative-image`, {
      method: 'PATCH',
      body: JSON.stringify({ imageId: Number(imageId) }),
    }).then(normalizeTrip),

  uploadImages: async (tripId: string, ownerId: string, formData: FormData) => {
    const token = getAccessToken();
    const res = await fetch(`${BASE_URL}/api/v1/trips/${tripId}/images?ownerId=${ownerId}`, {
      method: 'POST',
      credentials: 'include',
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: formData,
    });
    if (!res.ok) {
      const contentType = res.headers.get('content-type') ?? '';
      if (contentType.includes('application/json')) {
        const error = await res.json().catch(() => ({}));
        throw new Error(
          (error as { msg?: string; message?: string; error?: string }).msg ??
            (error as { message?: string }).message ??
            (error as { error?: string }).error ??
            '이미지 업로드 실패',
        );
      }

      const errorText = await res.text().catch(() => '');
      throw new Error(errorText || '이미지 업로드 실패');
    }

    const json = await res.json();
    if (json && typeof json === 'object' && 'data' in json) {
      return (json as ApiResponse<ImageUploadResult[]>).data;
    }
    return json as ImageUploadResult[];
  },

  generateAutoRecords: async (tripId: string) => {
    const result = await request<Record<string, unknown>>(`/api/v1/trips/${tripId}/auto-records`, { method: 'POST' });
    return normalizeAutoRecordResult(result);
  },
};

// ---------- Likes ----------
export const likeApi = {
  like: (tripId: string) =>
    request(`/api/v1/trips/${tripId}/likes`, { method: 'POST' }),

  unlike: (tripId: string) =>
    request(`/api/v1/trips/${tripId}/likes`, { method: 'DELETE' }),

  getMine: (tripId: string) =>
    request<{ liked: boolean }>(`/api/v1/trips/${tripId}/likes/me`),
};

// ---------- Posts ----------
export const postApi = {
  getList: async (tripId: string) => {
    const posts = await request<Record<string, unknown>[]>(`/api/v1/trips/${tripId}/posts`);
    return posts.map(normalizePost);
  },

  create: (
    tripId: string,
    body: Partial<{ title: string; content: string; date: string; time: string }>,
  ) =>
    request<Record<string, unknown>>(`/api/v1/trips/${tripId}/posts`, {
      method: 'POST',
      body: JSON.stringify({
        date: body.date,
        title: body.title,
        memo: body.content,
      }),
    }).then(normalizePost),

  update: (
    _tripId: string,
    postId: string,
    body: Partial<{ title: string; content: string; date: string; time: string }>,
  ) =>
    request(`/api/v1/posts/${postId}`, {
      method: 'PATCH',
      body: JSON.stringify({
        date: body.date,
        title: body.title,
        memo: body.content,
      }),
    }),

  delete: (_tripId: string, postId: string) =>
    request(`/api/v1/posts/${postId}`, { method: 'DELETE' }),

  addImages: async (tripId: string, postId: string, formData: FormData) => {
    const token = getAccessToken();
    const res = await fetch(`${BASE_URL}/api/v1/trips/${tripId}/posts/${postId}/images`, {
      method: 'POST',
      credentials: 'include',
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: formData,
    });

    if (!res.ok) {
      const error = await res.json().catch(() => ({}));
      throw new Error(
        (error as { msg?: string; message?: string }).msg ??
          (error as { message?: string }).message ??
          '이미지 업로드 실패',
      );
    }

    const json = await res.json();
    if (json && typeof json === 'object' && 'data' in json) {
      return (json as ApiResponse<{ images?: Array<{ id: string; url: string; filename: string }> }>).data;
    }
    return json as { images?: Array<{ id: string; url: string; filename: string }> };
  },

  deleteImage: (tripId: string, postId: string, imageId: string) =>
    request(`/api/v1/trips/${tripId}/posts/${postId}/images/${imageId}`, { method: 'DELETE' }),
};

// ---------- Markers ----------
export const markerApi = {
  update: (
    _postId: string,
    markerId: string,
    body: Partial<{
      placeName: string;
      lat: number;
      lng: number;
      visitTime: string;
      source: string;
    }>,
  ) =>
    request<Record<string, unknown>>(`/api/v1/posts/markers/${markerId}`, {
      method: 'PATCH',
      body: JSON.stringify({
        centerLat: body.lat,
        centerLng: body.lng,
        placeName: body.placeName,
        visitedAt: body.visitTime,
        source: body.source ?? 'AUTO',
      }),
    }).then(normalizeMarker),

  delete: (_postId: string, markerId: string) =>
    request(`/api/v1/posts/markers/${markerId}`, { method: 'DELETE' }),

  getCandidates: (markerId: string) =>
    request<unknown[]>(`/api/v1/posts/markers/${markerId}/place-candidates`),
};
