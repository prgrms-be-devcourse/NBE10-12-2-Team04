'use client';

import { useEffect, useState, useRef } from 'react';
import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import {
  Plus, Trash2, Save, Eye, ChevronDown,
  MapPin, X, Upload, ImageIcon,
} from 'lucide-react';
import { tripApi, postApi, markerApi } from '@/lib/api';
import type { Trip, Post, Marker } from '@/types';

// ────────────────────────────────────────────────────────────────────
// 컬럼 1: Post 목록
// ────────────────────────────────────────────────────────────────────
function PostList({
  posts,
  selectedId,
  onSelect,
  onDelete,
}: {
  posts: Post[];
  selectedId: string | null;
  onSelect: (p: Post) => void;
  onDelete: (id: string) => void;
}) {
  return (
    <div className="flex flex-col h-full overflow-hidden">
      <div className="flex items-center justify-between px-4 py-3 border-b border-gray-100">
        <h3 className="font-bold text-gray-900 text-sm flex items-center gap-1">
          <span className="w-5 h-5 rounded-full bg-green-600 text-white text-[10px] flex items-center justify-center font-bold">1</span>
          Post 목록
        </h3>
        {/* TODO: Post 직접 추가 API 확인 필요 */}
        <button className="flex items-center gap-1 text-xs text-green-600 hover:text-green-700 font-medium">
          <Plus size={13} /> 새 Post 추가
        </button>
      </div>

      <div className="flex-1 overflow-y-auto divide-y divide-gray-50">
        {posts.length === 0 ? (
          <p className="text-center text-gray-400 text-xs py-8">기록이 없습니다.</p>
        ) : (
          posts.map((post) => (
            <button
              key={post.id}
              onClick={() => onSelect(post)}
              className={`w-full text-left p-3 hover:bg-gray-50 transition-colors ${selectedId === post.id ? 'bg-green-50 border-l-2 border-green-500' : ''}`}
            >
              <div className="flex gap-2">
                {/* 이미지 thumbnail */}
                {post.images[0] ? (
                  <img src={post.images[0].url} alt="" className="w-12 h-12 rounded-lg object-cover flex-shrink-0" />
                ) : (
                  <div className="w-12 h-12 rounded-lg bg-gray-200 flex-shrink-0 flex items-center justify-center">
                    <ImageIcon size={16} className="text-gray-400" />
                  </div>
                )}
                <div className="flex-1 min-w-0">
                  <p className="text-xs text-gray-400">{post.date} {post.time}</p>
                  <p className="text-sm font-semibold text-gray-800 line-clamp-1 mt-0.5">{post.title}</p>
                  <p className="text-xs text-gray-400 line-clamp-2 mt-0.5">{post.content}</p>
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
            </button>
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
}: {
  tripId: string;
  post: Post;
  onChange: (updated: Post) => void;
}) {
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const fileRef = useRef<HTMLInputElement>(null);

  const set = (key: keyof Post, value: unknown) => onChange({ ...post, [key]: value });

  const handleSave = async () => {
    setSaving(true);
    try {
      await postApi.update(tripId, post.id, {
        title: post.title,
        content: post.content,
        date: post.date,
        time: post.time,
      });
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
    } catch {
      alert('저장에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteImage = async (imageId: string) => {
    try {
      await postApi.deleteImage(tripId, post.id, imageId);
      onChange({ ...post, images: post.images.filter((img) => img.id !== imageId) });
    } catch {
      alert('이미지 삭제에 실패했습니다.');
    }
  };

  const handleAddImages = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files ?? []);
    if (!files.length) return;
    const fd = new FormData();
    files.forEach((f) => fd.append('images', f));
    try {
      const res = await postApi.addImages(tripId, post.id, fd) as { images: Post['images'] };
      onChange({ ...post, images: [...post.images, ...(res.images ?? [])] });
    } catch {
      alert('이미지 업로드에 실패했습니다.');
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

      <div className="flex-1 overflow-y-auto p-4 flex flex-col gap-4">
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
            <span className="text-[10px] text-gray-400">{post.content.length} / 1000</span>
          </div>
          <textarea
            value={post.content}
            onChange={(e) => set('content', e.target.value)}
            maxLength={1000}
            rows={5}
            className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm outline-none focus:border-green-500 resize-none"
          />
        </div>

        {/* 이미지 */}
        <div>
          <div className="flex justify-between items-center mb-2">
            <label className="text-xs text-gray-500">연결된 이미지 ({post.images.length})</label>
            <button
              onClick={() => fileRef.current?.click()}
              className="text-xs text-green-600 flex items-center gap-0.5 hover:text-green-700"
            >
              <Plus size={11} /> 이미지 추가
            </button>
            <input ref={fileRef} type="file" multiple accept="image/*" className="hidden" onChange={handleAddImages} />
          </div>
          <div className="grid grid-cols-3 gap-2">
            {post.images.map((img) => (
              <div key={img.id} className="relative group">
                <img src={img.url} alt="" className="w-full h-16 object-cover rounded-lg" />
                <button
                  onClick={() => handleDeleteImage(img.id)}
                  className="absolute top-1 right-1 w-5 h-5 bg-black/50 rounded-full hidden group-hover:flex items-center justify-center"
                >
                  <X size={10} className="text-white" />
                </button>
              </div>
            ))}
            <button
              onClick={() => fileRef.current?.click()}
              className="h-16 border-2 border-dashed border-gray-200 rounded-lg flex items-center justify-center hover:border-green-300 transition-colors"
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
}: {
  post: Post;
  onMarkerUpdated: (marker: Marker) => void;
}) {
  const [marker, setMarker] = useState<Marker | null>(post.marker ?? null);
  const [saving, setSaving] = useState(false);
  const [query, setQuery] = useState('');

  const setM = (k: keyof Marker, v: unknown) => setMarker((m) => m ? { ...m, [k]: v } : m);

  const handleSave = async () => {
    if (!marker) return;
    setSaving(true);
    try {
      await markerApi.update(post.id, marker.id, {
        placeName: marker.placeName,
        lat: marker.lat,
        lng: marker.lng,
        visitTime: marker.visitTime,
        source: marker.source,
      });
      onMarkerUpdated(marker);
    } catch {
      alert('마커 저장에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!marker || !confirm('마커를 삭제하시겠습니까?')) return;
    try {
      await markerApi.delete(post.id, marker.id);
      setMarker(null);
    } catch {
      alert('마커 삭제에 실패했습니다.');
    }
  };

  return (
    <div className="flex flex-col h-full overflow-hidden">
      <div className="flex items-center px-4 py-3 border-b border-gray-100">
        <h3 className="font-bold text-gray-900 text-sm flex items-center gap-1">
          <span className="w-5 h-5 rounded-full bg-green-600 text-white text-[10px] flex items-center justify-center font-bold">3</span>
          Marker 편집
        </h3>
      </div>

      <div className="flex-1 overflow-y-auto p-4 flex flex-col gap-4">
        {/* 지도 placeholder */}
        <div className="w-full h-[120px] bg-gradient-to-br from-blue-100 to-green-50 rounded-xl flex items-center justify-center relative">
          <p className="text-xs text-gray-400">🗺 지도 placeholder</p>
          {marker && (
            <div className="absolute inset-0 flex items-center justify-center">
              <MapPin size={24} className="text-green-600" />
            </div>
          )}
        </div>

        {marker ? (
          <>
            <div>
              <label className="block text-xs text-gray-500 mb-1">선택된 마커 1</label>
              <p className="text-xs text-gray-400 mb-2">장소명</p>
              <input
                value={marker.placeName}
                onChange={(e) => setM('placeName', e.target.value)}
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm outline-none focus:border-green-500"
              />
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

            <div>
              <label className="block text-xs text-gray-500 mb-1">출처 (source)</label>
              <div className="relative">
                <select
                  value={marker.source ?? ''}
                  onChange={(e) => setM('source', e.target.value)}
                  className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm appearance-none outline-none focus:border-green-500"
                >
                  <option value="">자동 설정</option>
                  <option value="MANUAL">수동 입력</option>
                  <option value="AI">AI 추출</option>
                </select>
                <ChevronDown size={13} className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
              </div>
            </div>

            <p className="text-[10px] text-gray-400">
              장소 후보 조회로 정확한 장소를 선택할 수 있습니다.
            </p>

            <div className="flex gap-2">
              <button
                onClick={handleSave}
                disabled={saving}
                className="flex-1 bg-green-600 hover:bg-green-700 disabled:opacity-60 text-white text-xs font-semibold py-2 rounded-lg transition-colors"
              >
                {saving ? '저장 중...' : '마커 수정'}
              </button>
              <button
                onClick={handleDelete}
                className="px-4 py-2 border border-red-200 text-red-500 text-xs font-semibold rounded-lg hover:bg-red-50 transition-colors"
              >
                마커 삭제
              </button>
            </div>

            {/* TODO: 장소 후보 조회 */}
            <div className="border-t border-gray-100 pt-3">
              <label className="block text-xs text-gray-500 mb-1">장소 후보 조회</label>
              <div className="flex gap-2">
                <input
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  placeholder="장소명 검색..."
                  className="flex-1 border border-gray-200 rounded-lg px-3 py-1.5 text-xs outline-none focus:border-green-500"
                />
                <button className="px-3 py-1.5 bg-gray-100 text-gray-600 text-xs rounded-lg hover:bg-gray-200">
                  검색
                </button>
              </div>
              {/* TODO: 장소 후보 목록 표시 */}
            </div>
          </>
        ) : (
          <div className="flex flex-col items-center justify-center py-10 text-gray-400">
            <MapPin size={28} className="mb-2" />
            <p className="text-sm">마커가 없습니다.</p>
            <p className="text-xs mt-1">자동 기록 생성 시 마커가 추가됩니다.</p>
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
  const [selectedPost, setSelectedPost] = useState<Post | null>(null);
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);

  // Trip 기본 정보 편집 상태
  const [tripForm, setTripForm] = useState({
    title: '', country: '', city: '', startDate: '', endDate: '', isPublic: true,
  });

  useEffect(() => {
    Promise.all([tripApi.getOne(tripId), postApi.getList(tripId)])
      .then(([t, p]) => {
        const tripData = t as Trip;
        setTrip(tripData);
        setTripForm({
          title: tripData.title,
          country: tripData.country,
          city: tripData.city,
          startDate: tripData.startDate,
          endDate: tripData.endDate,
          isPublic: tripData.isPublic,
        });
        const postData = p as Post[];
        setPosts(postData);
        if (postData.length) setSelectedPost(postData[0]);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [tripId]);

  const handleSaveTrip = async () => {
    setSaving(true);
    try {
      await tripApi.update(tripId, tripForm);
    } catch {
      alert('Trip 정보 저장에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  };

  const handleDeletePost = async (postId: string) => {
    if (!confirm('이 Post를 삭제하시겠습니까?')) return;
    try {
      await postApi.delete(tripId, postId);
      setPosts((prev) => prev.filter((p) => p.id !== postId));
      if (selectedPost?.id === postId) setSelectedPost(null);
    } catch {
      alert('삭제에 실패했습니다.');
    }
  };

  const handlePublish = async () => {
    await handleSaveTrip();
    router.push(`/trips/${tripId}`);
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
          Trip 정보 저장
        </button>
      </div>

      {/* 3컬럼 편집 영역 */}
      <div className="flex flex-1 overflow-hidden">
        {/* 컬럼 1 - Post 목록 */}
        <div className="w-[240px] border-r border-gray-100 bg-white flex-shrink-0 overflow-hidden">
          <PostList
            posts={posts}
            selectedId={selectedPost?.id ?? null}
            onSelect={setSelectedPost}
            onDelete={handleDeletePost}
          />
        </div>

        {/* 컬럼 2 - Post 편집 */}
        <div className="flex-1 border-r border-gray-100 bg-white overflow-hidden">
          {selectedPost ? (
            <PostEditor
              tripId={tripId}
              post={selectedPost}
              onChange={(updated) => {
                setSelectedPost(updated);
                setPosts((prev) => prev.map((p) => (p.id === updated.id ? updated : p)));
              }}
            />
          ) : (
            <div className="flex items-center justify-center h-full text-gray-400 text-sm">
              왼쪽에서 Post를 선택하세요.
            </div>
          )}
        </div>

        {/* 컬럼 3 - Marker 편집 */}
        <div className="w-[280px] bg-white flex-shrink-0 overflow-hidden">
          {selectedPost ? (
            <MarkerEditor
              post={selectedPost}
              onMarkerUpdated={(marker) =>
                setSelectedPost((p) => (p ? { ...p, marker } : p))
              }
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
