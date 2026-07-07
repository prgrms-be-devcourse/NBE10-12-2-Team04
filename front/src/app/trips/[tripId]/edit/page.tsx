'use client';

import { useEffect, useState, useRef } from 'react';
import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import { GoogleMap, Marker as GoogleMarker, useJsApiLoader } from '@react-google-maps/api';
import {
  Plus, Trash2, Save, Eye, ChevronDown,
  MapPin, X, Upload, ImageIcon,
} from 'lucide-react';
import { tripApi, postApi, markerApi, placeApi } from '@/lib/api';
import type { Trip, Post, Marker, TripImage, PlaceCandidate } from '@/types';

const googleMapsApiKey = process.env.NEXT_PUBLIC_GOOGLE_MAPS_API_KEY ?? '';
const GOOGLE_MAPS_SCRIPT_ID = 'triptrace-google-map-script';
const markerMapContainerStyle = { width: '100%', height: '100%' };
const markerMapOptions = {
  disableDefaultUI: true,
  zoomControl: true,
  clickableIcons: false,
  gestureHandling: 'greedy',
};

function toTimeInput(value: unknown) {
  if (typeof value !== 'string' || !value) return undefined;
  const timePart = value.includes('T') ? value.split('T')[1] : value;
  return timePart.slice(0, 5);
}

function withDerivedPostTime(post: Post) {
  return {
    ...post,
    time: post.time ?? toTimeInput(post.marker?.visitTime),
  };
}

function getDefaultVisitTime(post: Post) {
  if (post.marker?.visitTime) return post.marker.visitTime;
  if (post.time) return post.time.includes('T') ? post.time : `${post.date}T${post.time}`;
  return post.date ? `${post.date}T00:00` : undefined;
}

function getTripImagesFromPosts(posts: Post[]) {
  const images = new Map<string, TripImage>();

  posts.forEach((post) => {
    post.images?.forEach((image) => {
      if (!images.has(image.id)) {
        images.set(image.id, {
          id: image.id,
          url: image.url,
          thumbnailUrl: image.url,
          filename: image.filename,
          postId: post.id,
        });
      }
    });
  });

  return Array.from(images.values());
}

function toTripImage(image: Post['images'][number], postId?: string): TripImage {
  return {
    id: image.id,
    url: image.url,
    thumbnailUrl: image.url,
    filename: image.filename,
    postId,
  };
}

type ToastState = {
  message: string;
  tone: 'success' | 'error';
};

// ────────────────────────────────────────────────────────────────────
// 컬럼 1: Post 목록
// ────────────────────────────────────────────────────────────────────
function PostList({
  posts,
  selectedId,
  onSelect,
  onDelete,
  onCreate,
}: {
  posts: Post[];
  selectedId: string | null;
  onSelect: (p: Post) => void;
  onDelete: (id: string) => void;
  onCreate: () => void;
}) {
  return (
    <div className="flex flex-col h-full overflow-hidden">
      <div className="flex items-center justify-between px-4 py-3 border-b border-gray-100">
        <h3 className="font-bold text-gray-900 text-sm flex items-center gap-1">
          <span className="w-5 h-5 rounded-full bg-green-600 text-white text-[10px] flex items-center justify-center font-bold">1</span>
          Post 목록
        </h3>
        <button onClick={onCreate} className="flex items-center gap-1 text-xs text-green-600 hover:text-green-700 font-medium">
          <Plus size={13} /> 새 Post 추가
        </button>
      </div>

      <div className="flex-1 overflow-y-auto divide-y divide-gray-50">
        {posts.length === 0 ? (
          <p className="text-center text-gray-400 text-xs py-8">기록이 없습니다.</p>
        ) : (
          posts.map((post) => (
            <div
              key={post.id}
              role="button"
              tabIndex={0}
              onClick={() => onSelect(post)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault();
                  onSelect(post);
                }
              }}
              className={`w-full text-left p-3 hover:bg-gray-50 transition-colors ${selectedId === post.id ? 'bg-green-50 border-l-2 border-green-500' : ''}`}
            >
              <div className="flex gap-2">
                {/* 이미지 thumbnail */}
                {post.images?.[0]?.url ? (
                  <img src={post.images[0].url} alt="" className="w-12 h-12 rounded-lg object-cover flex-shrink-0" />
                ) : (
                  <div className="w-12 h-12 rounded-lg bg-gray-200 flex-shrink-0 flex items-center justify-center">
                    <ImageIcon size={16} className="text-gray-400" />
                  </div>
                )}
                <div className="flex-1 min-w-0">
                  <p className="text-xs text-gray-400">{post.date} {post.time}</p>
                  <p className="text-sm font-semibold text-gray-800 line-clamp-1 mt-0.5">{post.title}</p>
                  <p className="text-xs text-gray-400 line-clamp-2 mt-0.5">{post.content ?? ''}</p>
                </div>
              </div>
              <div className="flex gap-2 mt-2">
                <span className="text-xs text-green-600 border border-green-200 rounded px-2 py-0.5">수정</span>
                <button
                  onClick={(e) => { e.stopPropagation(); onDelete(post.id); }}
                  className="text-xs text-red-400 border border-red-100 rounded px-2 py-0.5 hover:bg-red-50"
                >
                  삭제
                </button>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}

// ────────────────────────────────────────────────────────────────────
// 컬럼 2: Post 상세 편집
// ────────────────────────────────────────────────────────────────────
function PostEditor({
  tripId,
  post,
  onChange,
  onToast,
}: {
  tripId: string;
  post: Post;
  onChange: (updated: Post) => void;
  onToast: (message: string, tone?: ToastState['tone']) => void;
}) {
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const fileRef = useRef<HTMLInputElement>(null);
  const images = post.images ?? [];
  const content = post.content ?? '';

  const set = (key: keyof Post, value: unknown) => onChange({ ...post, [key]: value });

  const handleSave = async () => {
    setSaving(true);
    try {
      await postApi.update(tripId, post.id, {
        title: post.title,
        content,
        date: post.date,
        time: post.time,
      });
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
      onToast('Post가 저장되었습니다.');
    } catch {
      onToast('저장에 실패했습니다.', 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteImage = async (imageId: string) => {
    try {
      await postApi.deleteImage(tripId, post.id, imageId);
      onChange({ ...post, images: images.filter((img) => img.id !== imageId) });
      onToast('이미지가 삭제되었습니다.');
    } catch {
      onToast('이미지 삭제에 실패했습니다.', 'error');
    }
  };

  const handleAddImages = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files ?? []);
    if (!files.length) return;
    const fd = new FormData();
    files.forEach((f) => fd.append('images', f));
    try {
      const res = await postApi.addImages(tripId, post.id, fd) as { images: Post['images'] };
      onChange({ ...post, images: [...images, ...(res.images ?? [])] });
      onToast('이미지가 추가되었습니다.');
    } catch {
      onToast('이미지 업로드에 실패했습니다.', 'error');
    } finally {
      if (fileRef.current) {
        fileRef.current.value = '';
      }
    }
  };

  return (
    <div className="flex flex-col h-full overflow-hidden">
      <div className="flex items-center justify-between px-4 py-3 border-b border-gray-100">
        <h3 className="font-bold text-gray-900 text-sm flex items-center gap-1">
          <span className="w-5 h-5 rounded-full bg-green-600 text-white text-[10px] flex items-center justify-center font-bold">2</span>
          Post 상세 편집
        </h3>
        <button
          onClick={handleSave}
          disabled={saving}
          className={`flex items-center gap-1 text-xs font-semibold px-3 py-1.5 rounded-lg transition-colors ${
            saved ? 'bg-green-100 text-green-700' : 'bg-green-600 hover:bg-green-700 text-white'
          }`}
        >
          <Save size={12} /> {saving ? '저장 중...' : saved ? '저장됨' : '저장'}
        </button>
      </div>

      <div className="flex-1 overflow-y-auto p-3 flex flex-col gap-3">
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="block text-xs text-gray-500 mb-1">날짜</label>
            <input
              type="date"
              value={post.date}
              onChange={(e) => set('date', e.target.value)}
              className="w-full border border-gray-200 rounded-lg px-3 py-2 text-xs outline-none focus:border-green-500"
            />
          </div>
          <div>
            <label className="block text-xs text-gray-500 mb-1">시간</label>
            <input
              type="time"
              value={post.time ?? ''}
              onChange={(e) => set('time', e.target.value)}
              className="w-full border border-gray-200 rounded-lg px-3 py-2 text-xs outline-none focus:border-green-500"
            />
          </div>
        </div>

        <div>
          <label className="block text-xs text-gray-500 mb-1">제목</label>
          <input
            value={post.title}
            onChange={(e) => set('title', e.target.value)}
            className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm outline-none focus:border-green-500"
          />
        </div>

        <div>
          <div className="flex justify-between items-center mb-1">
            <label className="text-xs text-gray-500">내용</label>
            <span className="text-[10px] text-gray-400">{content.length} / 1000</span>
          </div>
          <textarea
            value={content}
            onChange={(e) => set('content', e.target.value)}
            maxLength={1000}
            rows={4}
            className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm outline-none focus:border-green-500 resize-none"
          />
        </div>

        {/* 이미지 */}
        <div>
          <div className="flex justify-between items-center mb-2">
            <label className="text-xs text-gray-500">연결된 이미지 ({images.length})</label>
            <button
              onClick={() => fileRef.current?.click()}
              className="text-xs text-green-600 flex items-center gap-0.5 hover:text-green-700"
            >
              <Plus size={11} /> 이미지 추가
            </button>
            <input ref={fileRef} type="file" multiple accept="image/*" className="hidden" onChange={handleAddImages} />
          </div>
          <div className="flex h-24 gap-2 overflow-x-auto pb-1">
            {images.map((img) =>
              img.url ? (
                <div key={img.id} className="relative group h-full flex-shrink-0">
                  <img src={img.url} alt="" className="h-full w-auto max-w-none object-cover rounded-lg" />
                  <button
                    onClick={() => handleDeleteImage(img.id)}
                    className="absolute top-1 right-1 w-5 h-5 bg-black/50 rounded-full hidden group-hover:flex items-center justify-center"
                  >
                    <X size={10} className="text-white" />
                  </button>
                </div>
              ) : null,
            )}
            <button
              onClick={() => fileRef.current?.click()}
              className="h-full w-24 flex-shrink-0 border-2 border-dashed border-gray-200 rounded-lg flex items-center justify-center hover:border-green-300 transition-colors"
            >
              <Upload size={16} className="text-gray-300" />
            </button>
          </div>
          <p className="text-[10px] text-gray-400 mt-1">
            이미지 변경/제거 API: PATCH /api/v1/trips/{'{tripId}'}/posts/{'{postId}'}/images
          </p>
        </div>
      </div>
    </div>
  );
}

// ────────────────────────────────────────────────────────────────────
// 컬럼 3: Marker 편집
// ────────────────────────────────────────────────────────────────────
function MarkerEditor({
  post,
  onMarkerUpdated,
  onToast,
}: {
  post: Post;
  onMarkerUpdated: (marker: Marker | null) => void;
  onToast: (message: string, tone?: ToastState['tone']) => void;
}) {
  const [marker, setMarker] = useState<Marker | null>(post.marker ?? null);
  const [saving, setSaving] = useState(false);
  const [candidatesOpen, setCandidatesOpen] = useState(false);
  const [candidates, setCandidates] = useState<PlaceCandidate[]>([]);
  const [candidateMode, setCandidateMode] = useState<'nearby' | 'search' | null>(null);
  const [candidatesLoading, setCandidatesLoading] = useState(false);
  const [candidatesError, setCandidatesError] = useState('');
  const [searchKeyword, setSearchKeyword] = useState(post.marker?.placeName ?? '');
  const { isLoaded, loadError } = useJsApiLoader({
    googleMapsApiKey,
    id: GOOGLE_MAPS_SCRIPT_ID,
  });

  const markerPosition = marker && Number.isFinite(Number(marker.lat)) && Number.isFinite(Number(marker.lng))
    ? { lat: Number(marker.lat), lng: Number(marker.lng) }
    : null;

  const setM = (k: keyof Marker, v: unknown) => {
    if (!marker) return;
    const nextMarker = { ...marker, [k]: v };
    setMarker(nextMarker);
    if (nextMarker.id) {
      onMarkerUpdated(nextMarker);
    }
  };

  const loadCandidates = async () => {
    if (!marker) return;

    if (candidatesOpen && candidateMode === 'nearby') {
      setCandidatesOpen(false);
      return;
    }

    setCandidatesOpen(true);
    setCandidateMode('nearby');

    setCandidatesLoading(true);
    setCandidatesError('');
    try {
      const data = await placeApi.nearby(marker.lat, marker.lng);
      setCandidates(data);
    } catch (error) {
      setCandidatesError(error instanceof Error ? error.message : '장소 후보 조회에 실패했습니다.');
    } finally {
      setCandidatesLoading(false);
    }
  };

  const searchPlaces = async () => {
    const keyword = searchKeyword.trim();
    if (!keyword) {
      setCandidatesError('검색어를 입력해주세요.');
      setCandidatesOpen(true);
      return;
    }

    setCandidatesOpen(true);
    setCandidateMode('search');
    setCandidatesLoading(true);
    setCandidatesError('');
    try {
      const data = await placeApi.search(keyword);
      setCandidates(data);
    } catch (error) {
      setCandidatesError(error instanceof Error ? error.message : '장소 검색에 실패했습니다.');
    } finally {
      setCandidatesLoading(false);
    }
  };

  const selectCandidate = (candidate: PlaceCandidate) => {
    const nextMarker = {
      id: marker?.id ?? '',
      placeName: candidate.name,
      lat: Number(candidate.latitude),
      lng: Number(candidate.longitude),
      visitTime: marker?.visitTime ?? getDefaultVisitTime(post),
      source: 'MANUAL',
    };

    setMarker(nextMarker);
    if (nextMarker.id) {
      onMarkerUpdated(nextMarker);
    }
    setCandidates([]);
    setCandidateMode(null);
    setCandidatesOpen(false);
  };

  const handleSave = async () => {
    if (!marker) return;
    setSaving(true);
    try {
      const markerPayload = {
        placeName: marker.placeName,
        lat: marker.lat,
        lng: marker.lng,
        visitTime: marker.visitTime,
        source: marker.source ?? (marker.id ? 'AUTO' : 'MANUAL'),
      };
      const updatedMarker = marker.id
        ? await markerApi.update(post.id, marker.id, markerPayload)
        : await markerApi.create(post.id, markerPayload);
      const nextMarker = (updatedMarker ?? marker) as Marker;
      setMarker(nextMarker);
      onMarkerUpdated(nextMarker);
      onToast(marker.id ? '마커가 수정되었습니다.' : '마커가 추가되었습니다.');
    } catch (error) {
      onToast(error instanceof Error ? error.message : '마커 저장에 실패했습니다.', 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!marker?.id || !confirm('마커를 삭제하시겠습니까?')) return;
    try {
      await markerApi.delete(post.id, marker.id);
      setMarker(null);
      onMarkerUpdated(null);
      onToast('마커가 삭제되었습니다.');
    } catch {
      onToast('마커 삭제에 실패했습니다.', 'error');
    }
  };

  const candidateList = (
    <div className="mt-2 overflow-hidden rounded-lg border border-gray-100 bg-white">
      {candidatesLoading ? (
        <p className="px-3 py-3 text-xs text-gray-400">장소 후보를 불러오는 중...</p>
      ) : candidatesError ? (
        <p className="px-3 py-3 text-xs text-red-400">{candidatesError}</p>
      ) : candidates.length === 0 ? (
        <p className="px-3 py-3 text-xs text-gray-400">표시할 장소 후보가 없습니다.</p>
      ) : (
        <div className="max-h-44 overflow-y-auto divide-y divide-gray-50">
          {candidates.map((candidate) => (
            <button
              type="button"
              key={candidate.placeId ?? `${candidate.name}-${candidate.latitude}-${candidate.longitude}`}
              onClick={() => selectCandidate(candidate)}
              className="block w-full px-3 py-2 text-left hover:bg-green-50"
            >
              <p className="text-xs font-semibold text-gray-800">{candidate.name}</p>
              {candidate.address && (
                <p className="mt-0.5 line-clamp-1 text-[11px] text-gray-400">{candidate.address}</p>
              )}
            </button>
          ))}
        </div>
      )}
    </div>
  );

  return (
    <div className="flex flex-col h-full overflow-hidden">
      <div className="flex items-center px-4 py-3 border-b border-gray-100">
        <h3 className="font-bold text-gray-900 text-sm flex items-center gap-1">
          <span className="w-5 h-5 rounded-full bg-green-600 text-white text-[10px] flex items-center justify-center font-bold">3</span>
          Marker 편집
        </h3>
      </div>

      <div className="flex-1 overflow-y-auto p-3 pl-4 flex flex-col gap-3">
        <div className="relative h-[320px] min-h-[260px] w-full overflow-hidden rounded-xl bg-gradient-to-br from-blue-100 to-green-50">
          {googleMapsApiKey && !loadError && isLoaded && markerPosition ? (
            <GoogleMap
              mapContainerStyle={markerMapContainerStyle}
              center={markerPosition}
              zoom={14}
              options={markerMapOptions}
            >
              <GoogleMarker
                position={markerPosition}
                draggable
                onDragEnd={(event) => {
                  const lat = event.latLng?.lat();
                  const lng = event.latLng?.lng();
                  if (typeof lat === 'number' && typeof lng === 'number') {
                    setM('lat', Number(lat.toFixed(6)));
                    setM('lng', Number(lng.toFixed(6)));
                  }
                }}
              />
            </GoogleMap>
          ) : (
            <div className="flex h-full w-full flex-col items-center justify-center">
              <MapPin size={24} className="mb-1 text-green-600" />
              <p className="text-xs text-gray-400">
                {marker ? '지도 로딩 대기 중' : '마커 좌표가 없습니다'}
              </p>
            </div>
          )}
        </div>

        {marker ? (
          <>
            <div>
              <label className="block text-xs text-gray-500 mb-1">선택된 마커 1</label>
              <div className="mb-3 flex gap-2">
                <input
                  value={searchKeyword}
                  onChange={(e) => setSearchKeyword(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') {
                      e.preventDefault();
                      searchPlaces();
                    }
                  }}
                  placeholder="장소명 또는 주소 검색"
                  className="min-w-0 flex-1 border border-gray-200 rounded-lg px-3 py-2 text-xs outline-none focus:border-green-500"
                />
                <button
                  type="button"
                  onClick={searchPlaces}
                  disabled={candidatesLoading}
                  className="rounded-lg bg-green-600 px-3 py-2 text-xs font-semibold text-white hover:bg-green-700 disabled:opacity-60"
                >
                  검색
                </button>
              </div>
              <p className="text-xs text-gray-400 mb-2">장소명</p>
              <input
                value={marker.placeName}
                onChange={(e) => setM('placeName', e.target.value)}
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm outline-none focus:border-green-500"
              />
              <button
                type="button"
                onClick={loadCandidates}
                className="mt-2 flex w-full items-center justify-between rounded-lg border border-gray-200 px-3 py-2 text-xs font-medium text-gray-600 hover:bg-gray-50"
              >
                <span>장소 후보 펼치기</span>
                <ChevronDown
                  size={14}
                  className={`text-gray-400 transition-transform ${candidatesOpen ? 'rotate-180' : ''}`}
                />
              </button>
              {candidatesOpen && candidateList}
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs text-gray-500 mb-1">위도</label>
                <input
                  type="number"
                  value={marker.lat}
                  onChange={(e) => setM('lat', parseFloat(e.target.value))}
                  step="0.0001"
                  className="w-full border border-gray-200 rounded-lg px-3 py-2 text-xs outline-none focus:border-green-500"
                />
              </div>
              <div>
                <label className="block text-xs text-gray-500 mb-1">경도</label>
                <input
                  type="number"
                  value={marker.lng}
                  onChange={(e) => setM('lng', parseFloat(e.target.value))}
                  step="0.0001"
                  className="w-full border border-gray-200 rounded-lg px-3 py-2 text-xs outline-none focus:border-green-500"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs text-gray-500 mb-1">방문 시간</label>
              <input
                type="datetime-local"
                value={marker.visitTime ?? ''}
                onChange={(e) => setM('visitTime', e.target.value)}
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-xs outline-none focus:border-green-500"
              />
            </div>

            <div className="flex gap-2">
              <button
                onClick={handleSave}
                disabled={saving}
                className="flex-1 bg-green-600 hover:bg-green-700 disabled:opacity-60 text-white text-xs font-semibold py-2 rounded-lg transition-colors"
              >
                {saving ? '저장 중...' : marker.id ? '마커 수정' : '마커 추가'}
              </button>
              <button
                onClick={handleDelete}
                disabled={!marker.id}
                className="px-4 py-2 border border-red-200 text-red-500 text-xs font-semibold rounded-lg hover:bg-red-50 transition-colors"
              >
                마커 삭제
              </button>
            </div>

          </>
        ) : (
          <div className="rounded-xl border border-dashed border-gray-200 p-4">
            <div className="mb-4 flex flex-col items-center justify-center py-6 text-gray-400">
              <MapPin size={28} className="mb-2" />
              <p className="text-sm">마커가 없습니다.</p>
              <p className="text-xs mt-1">장소를 검색해 수동으로 마커를 추가할 수 있습니다.</p>
            </div>
            <div className="flex gap-2">
              <input
                value={searchKeyword}
                onChange={(e) => setSearchKeyword(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault();
                    searchPlaces();
                  }
                }}
                placeholder="장소명 또는 주소 검색"
                className="min-w-0 flex-1 border border-gray-200 rounded-lg px-3 py-2 text-xs outline-none focus:border-green-500"
              />
              <button
                type="button"
                onClick={searchPlaces}
                disabled={candidatesLoading}
                className="rounded-lg bg-green-600 px-3 py-2 text-xs font-semibold text-white hover:bg-green-700 disabled:opacity-60"
              >
                검색
              </button>
            </div>
            {candidatesOpen && candidateList}
          </div>
        )}
      </div>
    </div>
  );
}

// ────────────────────────────────────────────────────────────────────
// Trip 수정/편집 페이지
// ────────────────────────────────────────────────────────────────────
export default function TripEditPage() {
  const { tripId } = useParams<{ tripId: string }>();
  const router = useRouter();

  const [trip, setTrip] = useState<Trip | null>(null);
  const [posts, setPosts] = useState<Post[]>([]);
  const [tripImages, setTripImages] = useState<TripImage[]>([]);
  const [selectedPost, setSelectedPost] = useState<Post | null>(null);
  const [saving, setSaving] = useState(false);
  const [representativeSaving, setRepresentativeSaving] = useState(false);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState<ToastState | null>(null);
  const [markerPanelWidth, setMarkerPanelWidth] = useState(380);
  const markerResizeRef = useRef<{ x: number; width: number } | null>(null);

  // Trip 기본 정보 편집 상태
  const [tripForm, setTripForm] = useState({
    title: '', country: '', city: '', startDate: '', endDate: '', isPublic: true,
  });

  const showToast = (message: string, tone: ToastState['tone'] = 'success') => {
    setToast({ message, tone });
    window.setTimeout(() => setToast(null), 2400);
  };

  const startMarkerPanelResize = (event: React.MouseEvent<HTMLDivElement>) => {
    markerResizeRef.current = { x: event.clientX, width: markerPanelWidth };
    window.addEventListener('mousemove', moveMarkerPanelResize);
    window.addEventListener('mouseup', endMarkerPanelResize);
  };

  const moveMarkerPanelResize = (event: MouseEvent) => {
    if (!markerResizeRef.current) return;
    const nextWidth = markerResizeRef.current.width + markerResizeRef.current.x - event.clientX;
    setMarkerPanelWidth(Math.max(300, Math.min(560, nextWidth)));
  };

  const endMarkerPanelResize = () => {
    markerResizeRef.current = null;
    window.removeEventListener('mousemove', moveMarkerPanelResize);
    window.removeEventListener('mouseup', endMarkerPanelResize);
  };

  useEffect(() => {
    Promise.all([tripApi.getOne(tripId), postApi.getList(tripId), tripApi.getImages(tripId).catch(() => [])])
      .then(([t, p, images]) => {
        const tripData = t as Trip;
        const postData = (p as Post[]).map(withDerivedPostTime);
        const loadedImages = images as TripImage[];
        setTrip(tripData);
        setTripForm({
          title: tripData.title,
          country: tripData.country,
          city: tripData.city,
          startDate: tripData.startDate,
          endDate: tripData.endDate,
          isPublic: tripData.isPublic,
        });
        setTripImages(loadedImages.length ? loadedImages : getTripImagesFromPosts(postData));
        setPosts(postData);
        if (postData.length) setSelectedPost(postData[0]);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [tripId]);

  const handleSaveTrip = async () => {
    setSaving(true);
    try {
      await Promise.all([
        ...posts.map((post) => postApi.update(tripId, post.id, {
          title: post.title,
          content: post.content,
          date: post.date,
          time: post.time,
        })),
        ...posts
          .filter((post) => post.marker)
          .map((post) => markerApi.update(post.id, post.marker!.id, {
            placeName: post.marker!.placeName,
            lat: post.marker!.lat,
            lng: post.marker!.lng,
            visitTime: post.marker!.visitTime,
            source: post.marker!.source ?? 'AUTO',
          })),
      ]);
      await tripApi.update(tripId, tripForm);
      showToast('Trip, Post, Marker 정보가 모두 저장되었습니다.');
      return true;
    } catch (error) {
      showToast(error instanceof Error ? error.message : '저장에 실패했습니다.', 'error');
      return false;
    } finally {
      setSaving(false);
    }
  };

  const handleDeletePost = async (postId: string) => {
    if (!confirm('이 Post를 삭제하시겠습니까?')) return;
    const deletedPost = posts.find((post) => post.id === postId);
    try {
      await postApi.delete(tripId, postId);
      const nextPosts = posts.filter((post) => post.id !== postId);
      setPosts(nextPosts);
      if (selectedPost?.id === postId) setSelectedPost(nextPosts[0] ?? null);
      setTripImages((prev) => prev.map((image) => (
        deletedPost?.images.some((postImage) => postImage.id === image.id)
          ? { ...image, postId: undefined }
          : image
      )));
      showToast('Post가 삭제되었습니다.');
    } catch {
      showToast('삭제에 실패했습니다.', 'error');
    }
  };

  const handleCreatePost = async () => {
    const today = tripForm.startDate || new Date().toISOString().slice(0, 10);
    try {
      const created = await postApi.create(tripId, {
        title: '새 Post',
        content: '',
        date: today,
      }) as Post;
      const nextPost = withDerivedPostTime(created);
      setPosts((prev) => [nextPost, ...prev]);
      setSelectedPost(nextPost);
      showToast('새 Post가 생성되었습니다.');
    } catch (error) {
      showToast(error instanceof Error ? error.message : 'Post 생성에 실패했습니다.', 'error');
    }
  };

  const handleRepresentativeImageChange = async (imageId: string) => {
    setRepresentativeSaving(true);
    try {
      const updatedTrip = await tripApi.updateRepresentativeImage(tripId, imageId) as Trip;
      setTrip(updatedTrip);
      showToast('대표이미지가 변경되었습니다.');
    } catch (error) {
      showToast(error instanceof Error ? error.message : '대표이미지 변경에 실패했습니다.', 'error');
    } finally {
      setRepresentativeSaving(false);
    }
  };

  const handleDeleteTrip = async () => {
    if (!confirm('이 Trip을 삭제하시겠습니까?')) return;
    try {
      await tripApi.delete(tripId);
      router.push('/trips');
    } catch (error) {
      showToast(error instanceof Error ? error.message : 'Trip 삭제에 실패했습니다.', 'error');
    }
  };

  const handlePublish = async () => {
    const saved = await handleSaveTrip();
    if (saved) {
      router.push(`/trips/${tripId}`);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <p className="text-gray-400 text-sm">불러오는 중...</p>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-[calc(100vh-56px)]">
      {toast && (
        <div className={`fixed right-6 top-20 z-50 rounded-lg px-4 py-2 text-sm font-semibold shadow-lg ${
          toast.tone === 'success' ? 'bg-gray-900 text-white' : 'bg-red-50 text-red-600 ring-1 ring-red-100'
        }`}>
          {toast.message}
        </div>
      )}

      {/* 상단 바 */}
      <div className="flex items-center justify-between px-6 py-3 bg-white border-b border-gray-200 flex-shrink-0">
        <div>
          <h1 className="font-bold text-gray-900 text-base">Trip 수정/편집</h1>
          <p className="text-xs text-gray-400">자동 생성된 기록을 확인하고 편집한 내용을 수정하세요.</p>
        </div>
        <div className="flex items-center gap-2">
          <Link
            href={`/trips/${tripId}`}
            className="flex items-center gap-1 text-sm text-gray-600 border border-gray-200 px-3 py-1.5 rounded-lg hover:bg-gray-50 transition-colors"
          >
            <Eye size={14} /> 미리보기
          </Link>
          <button
            onClick={handleDeleteTrip}
            className="flex items-center gap-1 text-sm text-red-500 border border-red-100 px-3 py-1.5 rounded-lg hover:bg-red-50 transition-colors"
          >
            <Trash2 size={14} /> 삭제
          </button>
          <button
            onClick={handlePublish}
            disabled={saving}
            className="flex items-center gap-1 text-sm bg-green-600 hover:bg-green-700 disabled:opacity-60 text-white font-semibold px-4 py-1.5 rounded-lg transition-colors"
          >
            <Save size={14} /> {saving ? '저장 중...' : '공개/저장'}
          </button>
        </div>
      </div>

      {/* Trip 기본 정보 바 */}
      <div className="flex items-center gap-3 px-6 py-3 bg-white border-b border-gray-100 flex-shrink-0 flex-wrap">
        <div className="flex items-center gap-2 rounded-lg border border-gray-100 bg-gray-50 px-2 py-1.5">
          <div className="h-10 w-10 overflow-hidden rounded-md bg-gray-200">
            {trip?.thumbnailUrl ? (
              <img src={trip.thumbnailUrl} alt="" className="h-full w-full object-cover" />
            ) : (
              <div className="flex h-full w-full items-center justify-center">
                <ImageIcon size={14} className="text-gray-400" />
              </div>
            )}
          </div>
          <div>
            <p className="text-[10px] font-medium text-gray-400">대표 이미지</p>
            <p className="max-w-24 truncate text-xs font-semibold text-gray-700">
              {trip?.thumbnailUrl ? '자동 설정됨' : '없음'}
            </p>
          </div>
        </div>
        <div className="relative">
          <select
            value={tripImages.find((image) => image.thumbnailUrl === trip?.thumbnailUrl || image.url === trip?.thumbnailUrl)?.id ?? ''}
            onChange={(event) => {
              if (event.target.value) void handleRepresentativeImageChange(event.target.value);
            }}
            disabled={representativeSaving || tripImages.length === 0}
            className="max-w-[180px] rounded-lg border border-gray-200 bg-white px-3 py-1.5 pr-7 text-sm outline-none focus:border-green-500 disabled:opacity-60"
          >
            <option value="">{tripImages.length ? '대표이미지 선택' : '이미지 없음'}</option>
            {tripImages.map((image) => (
              <option key={image.id} value={image.id}>
                {image.postId ? `Post 이미지 ${image.id}` : `미연결 이미지 ${image.id}`}
              </option>
            ))}
          </select>
          <ChevronDown size={12} className="absolute right-2 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
        </div>
        {tripImages.length > 0 && (
          <div className="flex max-w-[220px] gap-1 overflow-x-auto">
            {tripImages.slice(0, 8).map((image) => (
              <button
                key={image.id}
                type="button"
                onClick={() => handleRepresentativeImageChange(image.id)}
                disabled={representativeSaving}
                className={`h-10 w-10 flex-shrink-0 overflow-hidden rounded-md border-2 bg-gray-100 ${
                  image.thumbnailUrl === trip?.thumbnailUrl || image.url === trip?.thumbnailUrl ? 'border-green-600' : 'border-white'
                }`}
                title={image.postId ? 'Post 이미지' : '미연결 이미지'}
              >
                <img src={image.thumbnailUrl || image.url} alt="" className="h-full w-full object-cover" />
              </button>
            ))}
          </div>
        )}
        <input
          value={tripForm.title}
          onChange={(e) => setTripForm({ ...tripForm, title: e.target.value })}
          placeholder="여행 제목"
          className="border border-gray-200 rounded-lg px-3 py-1.5 text-sm outline-none focus:border-green-500 min-w-[180px]"
        />
        <div className="relative">
          <select
            value={tripForm.country}
            onChange={(e) => setTripForm({ ...tripForm, country: e.target.value })}
            className="border border-gray-200 rounded-lg px-3 py-1.5 text-sm appearance-none outline-none focus:border-green-500 pr-7"
          >
            <option value="">국가</option>
            <option value="한국">한국</option>
            <option value="일본">일본</option>
            <option value="프랑스">프랑스</option>
            <option value="미국">미국</option>
            <option value="그리스">그리스</option>
            <option value="베트남">베트남</option>
            <option value="스페인">스페인</option>
          </select>
          <ChevronDown size={12} className="absolute right-2 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
        </div>
        <input
          value={tripForm.city}
          onChange={(e) => setTripForm({ ...tripForm, city: e.target.value })}
          placeholder="도시"
          className="border border-gray-200 rounded-lg px-3 py-1.5 text-sm outline-none focus:border-green-500 min-w-[120px]"
        />
        <input
          type="date"
          value={tripForm.startDate}
          onChange={(e) => setTripForm({ ...tripForm, startDate: e.target.value })}
          className="border border-gray-200 rounded-lg px-3 py-1.5 text-sm outline-none focus:border-green-500"
        />
        <input
          type="date"
          value={tripForm.endDate}
          onChange={(e) => setTripForm({ ...tripForm, endDate: e.target.value })}
          className="border border-gray-200 rounded-lg px-3 py-1.5 text-sm outline-none focus:border-green-500"
        />
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => setTripForm({ ...tripForm, isPublic: !tripForm.isPublic })}
            className={`relative w-10 h-5 rounded-full transition-colors ${tripForm.isPublic ? 'bg-green-600' : 'bg-gray-300'}`}
          >
            <span className={`absolute top-0.5 left-0.5 w-4 h-4 bg-white rounded-full shadow transition-transform ${tripForm.isPublic ? 'translate-x-5' : ''}`} />
          </button>
          <span className="text-xs text-gray-600">{tripForm.isPublic ? '공개' : '비공개'}</span>
        </div>
        <button
          onClick={handleSaveTrip}
          disabled={saving}
          className="bg-green-600 hover:bg-green-700 disabled:opacity-60 text-white text-xs font-semibold px-3 py-1.5 rounded-lg transition-colors"
        >
          전체 저장
        </button>
      </div>

      {/* 3컬럼 편집 영역 */}
      <div className="flex flex-1 overflow-hidden">
        {/* 컬럼 1 - Post 목록 */}
        <div className="w-[240px] border-r border-gray-100 bg-white flex-shrink-0 overflow-hidden">
          <PostList
            posts={posts}
            selectedId={selectedPost?.id ?? null}
            onSelect={(post) => setSelectedPost(withDerivedPostTime(post))}
            onDelete={handleDeletePost}
            onCreate={handleCreatePost}
          />
        </div>

        {/* 컬럼 2 - Post 편집 */}
        <div className="flex-1 border-r border-gray-100 bg-white overflow-hidden">
          {selectedPost ? (
            <PostEditor
              tripId={tripId}
              post={selectedPost}
              onToast={showToast}
              onChange={(updated) => {
                const nextPost = withDerivedPostTime(updated);
                setSelectedPost(nextPost);
                setPosts((prev) => prev.map((p) => (p.id === nextPost.id ? nextPost : p)));
                setTripImages((prev) => {
                  const nextImages = new Map(prev.map((image) => [image.id, image]));
                  nextPost.images?.forEach((image) => {
                    nextImages.set(image.id, toTripImage(image, nextPost.id));
                  });

                  prev.forEach((image) => {
                    const wasLinkedToPost = image.postId === nextPost.id;
                    const stillLinkedToPost = nextPost.images?.some((postImage) => postImage.id === image.id);
                    if (wasLinkedToPost && !stillLinkedToPost) {
                      nextImages.set(image.id, { ...image, postId: undefined });
                    }
                  });

                  return Array.from(nextImages.values());
                });
              }}
            />
          ) : (
            <div className="flex items-center justify-center h-full text-gray-400 text-sm">
              왼쪽에서 Post를 선택하세요.
            </div>
          )}
        </div>

        {/* 컬럼 3 - Marker 편집 */}
        <div
          className="relative bg-white flex-shrink-0 overflow-hidden"
          style={{ width: markerPanelWidth }}
        >
          <div
            onMouseDown={startMarkerPanelResize}
            className="absolute left-0 top-0 z-20 h-full w-2 cursor-col-resize bg-transparent hover:bg-green-100"
            title="마커 편집 패널 크기 조절"
          >
            <span className="absolute left-0 top-1/2 h-10 w-1 -translate-y-1/2 rounded-full bg-gray-200" />
          </div>
          {selectedPost ? (
            <MarkerEditor
              key={selectedPost.id}
              post={selectedPost}
              onToast={showToast}
              onMarkerUpdated={(marker) => {
                setSelectedPost((p) => (p ? withDerivedPostTime({ ...p, marker: marker ?? undefined }) : p));
                setPosts((prev) => prev.map((p) => (
                  p.id === selectedPost.id ? withDerivedPostTime({ ...p, marker: marker ?? undefined }) : p
                )));
              }}
            />
          ) : (
            <div className="flex items-center justify-center h-full text-gray-400 text-sm p-4 text-center">
              Post를 선택하면 마커를 편집할 수 있습니다.
            </div>
          )}
        </div>
      </div>

      {/* 하단 안내 */}
      <div className="px-6 py-2.5 bg-amber-50 border-t border-amber-100 flex-shrink-0">
        <p className="text-xs text-amber-700">
          변경 사항은 저장(공개/저장) 시 반영됩니다. 저장하지 않고 이동하면 최후 화면으로 되돌아옵니다.
        </p>
      </div>
    </div>
  );
}
