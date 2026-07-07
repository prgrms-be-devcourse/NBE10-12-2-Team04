'use client';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import { GoogleMap, Marker, OverlayView, Polyline, useJsApiLoader } from '@react-google-maps/api';
import { ArrowLeft, Heart, MapPin, Calendar, Globe, Lock, Pencil, Maximize2, Minimize2, Trash2 } from 'lucide-react';
import { isAuthenticated, tripApi, postApi, likeApi, userApi } from '@/lib/api';
import type { Trip, Post } from '@/types';

type LocatedPost = Post & {
  marker: NonNullable<Post['marker']> & {
    lat: number;
    lng: number;
  };
};

const googleMapsApiKey = process.env.NEXT_PUBLIC_GOOGLE_MAPS_API_KEY ?? '';
const GOOGLE_MAPS_SCRIPT_ID = 'triptrace-google-map-script';
const mapContainerStyle = { width: '100%', height: '100%' };
const mapOptions = {
  disableDefaultUI: true,
  zoomControl: true,
  clickableIcons: false,
  gestureHandling: 'greedy',
  minZoom: 4,
};

function isValidCoordinate(value: unknown) {
  return typeof value === 'number' && Number.isFinite(value);
}

function getMarkerPosts(posts: Post[]) {
  return posts.filter((post): post is LocatedPost =>
    !!post.marker &&
    isValidCoordinate(post.marker.lat) &&
    isValidCoordinate(post.marker.lng)
  );
}

function getMarkerPosition(index: number) {
  return {
    top: 30 + (index % 4) * 12,
    left: 22 + (index % 5) * 14,
  };
}

function getDisplayMarkerPositions(posts: LocatedPost[]) {
  const grouped = new Map<string, Array<{ post: LocatedPost; base: { lat: number; lng: number } }>>();

  posts.forEach((post) => {
    if (!post.marker) return;
    const base = { lat: post.marker.lat, lng: post.marker.lng };
    const key = `${base.lat.toFixed(5)}:${base.lng.toFixed(5)}`;
    grouped.set(key, [...(grouped.get(key) ?? []), { post, base }]);
  });

  const positions = new Map<string, { lat: number; lng: number }>();
  grouped.forEach((items) => {
    items.forEach(({ post, base }, index) => {
      if (items.length === 1) {
        positions.set(post.id, base);
        return;
      }

      const angle = (Math.PI * 2 * index) / items.length;
      const radius = 0.0009 + Math.floor(index / 8) * 0.00045;
      positions.set(post.id, {
        lat: base.lat + Math.sin(angle) * radius,
        lng: base.lng + Math.cos(angle) * radius,
      });
    });
  });

  return positions;
}

function formatDayLabel(day: string, index: number) {
  const formatted = day ? day.replaceAll('-', '.') : '';
  return `Day ${index + 1}${formatted ? ` - ${formatted}` : ''}`;
}

function getPostMarkerImageUrl(post: Post) {
  return post.marker?.representativeImageUrl || post.images?.[0]?.url || '';
}

function PostPreviewCard({ post }: { post: Post }) {
  const imageUrl = getPostMarkerImageUrl(post);

  return (
    <div className="flex w-[220px] gap-2 rounded-lg bg-white p-2 shadow-lg ring-1 ring-black/5">
      <div className="h-14 w-16 flex-shrink-0 overflow-hidden rounded-md bg-gray-100">
        {imageUrl ? (
          <img src={imageUrl} alt="" className="h-full w-full object-cover" />
        ) : (
          <div className="flex h-full w-full items-center justify-center">
            <MapPin size={16} className="text-green-600" />
          </div>
        )}
      </div>
      <div className="min-w-0 flex-1">
        <p className="line-clamp-1 text-xs font-bold text-gray-900">{post.title}</p>
        <p className="mt-0.5 line-clamp-2 text-[11px] text-gray-500">{post.content || post.marker?.placeName || '간단한 기록을 확인하세요.'}</p>
      </div>
    </div>
  );
}

function PhotoMapMarker({
  post,
  position,
  index,
  selected,
  onClick,
}: {
  post: LocatedPost;
  position: { lat: number; lng: number };
  index: number;
  selected: boolean;
  onClick: () => void;
}) {
  const imageUrl = getPostMarkerImageUrl(post);

  if (!imageUrl) {
    return (
      <Marker
        position={position}
        onClick={onClick}
        label={{
          text: String(index + 1),
          color: selected ? '#ffffff' : '#064e3b',
          fontSize: '12px',
          fontWeight: '700',
        }}
        title={post.marker?.placeName ?? post.title}
      />
    );
  }

  return (
    <OverlayView
      position={position}
      mapPaneName={OverlayView.OVERLAY_MOUSE_TARGET}
    >
      <button
        type="button"
        onClick={onClick}
        className={`relative h-12 w-12 -translate-x-1/2 -translate-y-full overflow-hidden rounded-full border-[3px] bg-white shadow-lg transition-transform hover:scale-105 ${
          selected ? 'border-green-600' : 'border-white'
        }`}
        title={post.marker?.placeName ?? post.title}
      >
        <img src={imageUrl} alt="" className="h-full w-full object-cover" />
        <span className="absolute -bottom-0.5 left-1/2 h-2.5 w-2.5 -translate-x-1/2 rotate-45 bg-white" />
      </button>
    </OverlayView>
  );
}

function FallbackTripMap({
  posts,
  selectedPostId,
  onMarkerSelect,
}: {
  posts: Post[];
  selectedPostId: string | null;
  onMarkerSelect: (post: Post) => void;
}) {
  const markerPosts = getMarkerPosts(posts);
  const points = markerPosts.map((post, index) => ({
    post,
    ...getMarkerPosition(index),
  }));

  return (
    <div className="relative w-full h-full bg-gradient-to-br from-blue-100 via-sky-50 to-green-50 rounded-xl overflow-hidden">
      <div className="absolute inset-0 flex items-center justify-center">
        <p className="text-xs text-gray-400">🗺 지도 영역 (TODO: 지도 라이브러리 연결)</p>
      </div>
      {points.length > 1 && (
        <svg className="absolute inset-0 h-full w-full pointer-events-none" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true">
          <defs>
            <marker id="trip-path-arrow" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto" markerUnits="strokeWidth">
              <path d="M0,0 L8,4 L0,8 Z" fill="rgba(5,150,105,0.75)" />
            </marker>
          </defs>
          <polyline
            points={points.map((point) => `${point.left},${point.top}`).join(' ')}
            fill="none"
            stroke="rgba(5,150,105,0.45)"
            strokeWidth="1.5"
            strokeDasharray="4 4"
            markerMid="url(#trip-path-arrow)"
            markerEnd="url(#trip-path-arrow)"
            vectorEffect="non-scaling-stroke"
          />
        </svg>
      )}
      {points.map(({ post, top, left }, i) => (
          <button
            key={post.id}
            type="button"
            onClick={() => onMarkerSelect(post)}
            className="absolute flex -translate-x-1/2 -translate-y-1/2 flex-col items-center rounded-lg px-2 py-1 transition-transform hover:scale-105"
            style={{ top: `${top}%`, left: `${left}%` }}
          >
            <span className={`grid h-6 w-6 place-items-center rounded-full border-2 bg-white shadow-sm ${
              selectedPostId === post.id ? 'border-green-600' : 'border-white'
            }`}>
              <MapPin size={18} className="text-green-600 fill-green-100" />
            </span>
            <span className="mt-0.5 max-w-24 truncate text-[9px] font-semibold text-gray-700">{post.marker?.placeName}</span>
            <span className="sr-only">{i + 1}번째 기록으로 이동</span>
          </button>
      ))}
      {points.map(({ post, top, left }) => (
        selectedPostId === post.id ? (
          <div
            key={`${post.id}-preview`}
            className="absolute z-20 -translate-x-1/2 -translate-y-[115%]"
            style={{ top: `${top}%`, left: `${left}%` }}
          >
            <PostPreviewCard post={post} />
          </div>
        ) : null
      ))}
      {!googleMapsApiKey && (
        <p className="absolute bottom-3 left-3 rounded-full bg-white/90 px-3 py-1.5 text-[11px] font-medium text-gray-500 shadow">
          NEXT_PUBLIC_GOOGLE_MAPS_API_KEY가 필요합니다.
        </p>
      )}
    </div>
  );
}

function GoogleTripMap({
  posts,
  selectedPostId,
  onMarkerSelect,
  sheetTop,
  dayKey,
}: {
  posts: Post[];
  selectedPostId: string | null;
  onMarkerSelect: (post: Post) => void;
  sheetTop: number;
  dayKey: string;
}) {
  const markerPosts = useMemo(() => getMarkerPosts(posts), [posts]);
  const displayPositions = useMemo(() => getDisplayMarkerPositions(markerPosts), [markerPosts]);
  const path = useMemo(() => markerPosts.map((post) => ({
    postId: post.id,
    ...(displayPositions.get(post.id) ?? { lat: post.marker.lat, lng: post.marker.lng }),
  })), [displayPositions, markerPosts]);
  const pathKey = `${dayKey}|${path.map((point) => `${point.postId}:${point.lat},${point.lng}`).join('|')}`;
  const mapRef = useRef<google.maps.Map | null>(null);
  const fittedPathRef = useRef('');
  const idleFitPathRef = useRef('');
  const latestFitRef = useRef<(map: google.maps.Map, fit: boolean) => void>(() => {});
  const { isLoaded, loadError } = useJsApiLoader({
    googleMapsApiKey,
    id: GOOGLE_MAPS_SCRIPT_ID,
  });

  const moveMapToCurrentDay = useCallback((map: google.maps.Map, fit: boolean) => {
    if (path.length === 0) return;
    const bottomPadding = Math.max(160, window.innerHeight - sheetTop + 64);
    const bounds = new google.maps.LatLngBounds();
    path.forEach((point) => bounds.extend({ lat: point.lat, lng: point.lng }));
    if (path.length === 1) {
      bounds.extend({ lat: path[0].lat + 0.01, lng: path[0].lng + 0.01 });
      bounds.extend({ lat: path[0].lat - 0.01, lng: path[0].lng - 0.01 });
    }
    const padding = {
      top: 96,
      right: 72,
      bottom: bottomPadding,
      left: 72,
    };

    if (fit) {
      map.fitBounds(bounds, padding);
    } else {
      if (path.length === 1) {
        map.panTo(path[0]);
      } else {
        map.panTo(bounds.getCenter());
      }
    }
  }, [path, sheetTop]);

  useEffect(() => {
    latestFitRef.current = moveMapToCurrentDay;
  }, [moveMapToCurrentDay]);

  useEffect(() => {
    idleFitPathRef.current = '';
  }, [pathKey]);

  useEffect(() => {
    if (!isLoaded || !mapRef.current || path.length === 0) return;
    const isFirstFit = !fittedPathRef.current;
    const isDayChanged = fittedPathRef.current !== pathKey;

    if (isFirstFit || isDayChanged) {
      moveMapToCurrentDay(mapRef.current, isFirstFit);
    }

    fittedPathRef.current = pathKey;
  }, [isLoaded, moveMapToCurrentDay, path.length, pathKey]);

  useEffect(() => {
    if (!isLoaded || !mapRef.current || path.length === 0) return;
    const map = mapRef.current;
    const timers = [
      window.setTimeout(() => latestFitRef.current(map, true), 120),
      window.setTimeout(() => latestFitRef.current(map, true), 320),
    ];

    return () => timers.forEach((timer) => window.clearTimeout(timer));
  }, [isLoaded, path.length, pathKey]);

  if (loadError) {
    return <FallbackTripMap posts={posts} selectedPostId={selectedPostId} onMarkerSelect={onMarkerSelect} />;
  }

  if (!isLoaded) {
    return (
      <div className="flex h-full w-full items-center justify-center bg-gradient-to-br from-blue-100 via-sky-50 to-green-50">
        <p className="text-xs text-gray-400">지도를 불러오는 중...</p>
      </div>
    );
  }

  return (
    <GoogleMap
      mapContainerStyle={mapContainerStyle}
      options={mapOptions}
      onLoad={(map) => {
        mapRef.current = map;
        moveMapToCurrentDay(map, true);
        fittedPathRef.current = pathKey;
      }}
      onIdle={() => {
        if (!mapRef.current || path.length === 0 || idleFitPathRef.current === pathKey) return;
        idleFitPathRef.current = pathKey;
        latestFitRef.current(mapRef.current, true);
      }}
      onUnmount={() => {
        mapRef.current = null;
      }}
    >
      {path.length > 1 && (
        <Polyline
          key={pathKey}
          path={path.map(({ lat, lng }) => ({ lat, lng }))}
          options={{
            icons: [{
              icon: {
                path: google.maps.SymbolPath.FORWARD_CLOSED_ARROW,
                scale: 3,
                strokeColor: '#059669',
                fillColor: '#059669',
                fillOpacity: 1,
              },
              offset: '100%',
              repeat: '72px',
            }],
            strokeColor: '#059669',
            strokeOpacity: 0.75,
            strokeWeight: 4,
          }}
        />
      )}
      {markerPosts.map((post, index) => (
        <PhotoMapMarker
          key={post.id}
          post={post}
          position={displayPositions.get(post.id) ?? { lat: post.marker!.lat, lng: post.marker!.lng }}
          index={index}
          selected={selectedPostId === post.id}
          onClick={() => onMarkerSelect(post)}
        />
      ))}
      {markerPosts.map((post) => (
        selectedPostId === post.id ? (
          <OverlayView
            key={`${post.id}-preview`}
            position={displayPositions.get(post.id) ?? { lat: post.marker.lat, lng: post.marker.lng }}
            mapPaneName={OverlayView.OVERLAY_MOUSE_TARGET}
          >
            <div className="pointer-events-auto -translate-x-1/2 -translate-y-[calc(100%+58px)]">
              <PostPreviewCard post={post} />
            </div>
          </OverlayView>
        ) : null
      ))}
    </GoogleMap>
  );
}

function TripMap({
  posts,
  selectedPostId,
  onMarkerSelect,
  sheetTop,
  dayKey,
}: {
  posts: Post[];
  selectedPostId: string | null;
  onMarkerSelect: (post: Post) => void;
  sheetTop: number;
  dayKey: string;
}) {
  if (!googleMapsApiKey || getMarkerPosts(posts).length === 0) {
    return <FallbackTripMap posts={posts} selectedPostId={selectedPostId} onMarkerSelect={onMarkerSelect} />;
  }

  return <GoogleTripMap posts={posts} selectedPostId={selectedPostId} onMarkerSelect={onMarkerSelect} sheetTop={sheetTop} dayKey={dayKey} />;
}

// ── Day 탭 ────────────────────────────────────────────────────────────
function DayTabs({
  days,
  active,
  onSelect,
}: {
  days: string[];
  active: string;
  onSelect: (d: string) => void;
}) {
  return (
    <div className="flex gap-1 border-b border-gray-100 px-4">
      {days.map((day, i) => (
        <button
          key={day}
          onClick={() => onSelect(day)}
          className={`px-3 py-2 text-xs font-semibold transition-colors border-b-2 ${
            active === day
              ? 'border-green-600 text-green-700'
              : 'border-transparent text-gray-400 hover:text-gray-600'
          }`}
        >
          {formatDayLabel(day, i)}
        </button>
      ))}
    </div>
  );
}

// ── 타임라인 아이템 ───────────────────────────────────────────────────
function TimelineItem({ post, active }: { post: Post; active: boolean }) {
  const images = post.images ?? [];
  const content = post.content ?? '';

  return (
    <div className={`flex gap-3 py-3 relative rounded-xl transition-colors ${active ? 'bg-green-50/70 px-2' : ''}`}>
      {/* 세로선 */}
      <div className="flex flex-col items-center">
        <div className="w-2.5 h-2.5 rounded-full bg-green-500 mt-1 flex-shrink-0 z-10" />
        <div className="w-px flex-1 bg-gray-200 mt-1" />
      </div>

      <div className="flex-1 pb-2">
        <p className="text-xs text-gray-400 mb-0.5">{post.time ?? '시간 미정'}</p>
        <p className="font-semibold text-gray-900 text-sm">{post.title}</p>
        <p className="text-xs text-gray-500 mt-1 line-clamp-3">{content}</p>
        {post.marker && (
          <p className="text-xs text-gray-400 mt-1 flex items-center gap-0.5">
            <MapPin size={10} /> {post.marker.placeName}
          </p>
        )}
        {/* 이미지 그리드 */}
        {images.length > 0 && (
          <div className="mt-2 flex h-44 gap-2 overflow-x-auto pb-1">
            {images.map((img) =>
              img.url ? (
                <img key={img.id} src={img.url} alt="" className="h-full w-auto max-w-none rounded-md object-cover" />
              ) : null,
            )}
          </div>
        )}
      </div>
    </div>
  );
}

// ── Trip 단건 조회 페이지 ─────────────────────────────────────────────
export default function TripDetailPage() {
  const { tripId } = useParams<{ tripId: string }>();
  const router = useRouter();

  const [trip, setTrip] = useState<Trip | null>(null);
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeDay, setActiveDay] = useState('');
  const [liked, setLiked] = useState(false);
  const [likeCount, setLikeCount] = useState(0);
  const [mapExpanded, setMapExpanded] = useState(false);
  const [sheetTop, setSheetTop] = useState(420);
  const [message, setMessage] = useState('');
  const [currentUserId, setCurrentUserId] = useState<string | null>(null);
  const [focusedPostId, setFocusedPostId] = useState<string | null>(null);
  const postRefs = useRef<Record<string, HTMLDivElement | null>>({});
  const timelineRef = useRef<HTMLDivElement | null>(null);
  const dragRef = useRef<{ y: number; top: number } | null>(null);

  useEffect(() => {
    async function loadTripDetail() {
      try {
        const [t, p] = await Promise.all([
          tripApi.getOne(tripId),
          postApi.getList(tripId),
        ]);

        const tripData = t as Trip;
        const postData = p as Post[];
        setTrip(tripData);
        setLiked(false);
        setLikeCount(tripData.likeCount ?? 0);
        setPosts(postData);
        // 날짜 목록 추출
        const days = [...new Set(postData.map((post) => post.date))].sort();
        if (days.length) setActiveDay(days[0]);

        if (isAuthenticated()) {
          try {
            const me = await userApi.getMe() as { id?: string | number };
            setCurrentUserId(me.id != null ? String(me.id) : null);
          } catch {
            setCurrentUserId(null);
          }

          try {
            const status = await likeApi.getMine(tripId);
            setLiked(status.liked);
          } catch {
            setLiked(false);
          }
        } else {
          setCurrentUserId(null);
          setLiked(false);
        }
      } catch {
        setTrip(null);
      } finally {
        setLoading(false);
      }
    }

    loadTripDetail();
  }, [tripId]);

  useEffect(() => {
    const setInitialSheetPosition = () => {
      setSheetTop(Math.max(340, window.innerHeight - 360));
    };

    setInitialSheetPosition();
    window.addEventListener('resize', setInitialSheetPosition);
    return () => window.removeEventListener('resize', setInitialSheetPosition);
  }, []);

  const handleSelectDay = (day: string) => {
    setActiveDay(day);
    setFocusedPostId(null);
  };

  const toggleMapExpanded = () => {
    const nextExpanded = !mapExpanded;
    setMapExpanded(nextExpanded);
    setSheetTop(nextExpanded ? window.innerHeight - 220 : Math.max(340, window.innerHeight - 360));
  };

  const handleLike = async () => {
    if (!isAuthenticated()) {
      router.push('/auth/login');
      return;
    }

    try {
      if (liked) {
        await likeApi.unlike(tripId);
        setLiked(false);
        setLikeCount((n) => Math.max(0, n - 1));
      } else {
        await likeApi.like(tripId);
        setLiked(true);
        setLikeCount((n) => n + 1);
      }
    } catch {
      try {
        const status = await likeApi.getMine(tripId);
        setLiked(status.liked);
      } catch {
        // 좋아요 상태 동기화 실패 시 현재 화면 상태를 유지합니다.
      }
    }
  };

  const handleDeleteTrip = async () => {
    if (!confirm('이 Trip을 삭제하시겠습니까?')) return;
    try {
      await tripApi.delete(tripId);
      router.push('/trips');
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Trip 삭제에 실패했습니다.');
    }
  };

  const focusPost = (post: Post) => {
    if (post.date !== activeDay) setActiveDay(post.date);
    setFocusedPostId(post.id);
    window.setTimeout(() => {
      const container = timelineRef.current;
      const target = postRefs.current[post.id];
      if (!container || !target) return;

      container.scrollTo({
        top: target.offsetTop - container.clientHeight / 2 + target.clientHeight / 2,
        behavior: 'smooth',
      });
    }, 80);
  };

  const startSheetDrag = (event: React.MouseEvent<HTMLDivElement>) => {
    dragRef.current = { y: event.clientY, top: sheetTop };
    window.addEventListener('mousemove', moveSheetDrag);
    window.addEventListener('mouseup', endSheetDrag);
  };

  const moveSheetDrag = (event: MouseEvent) => {
    if (!dragRef.current) return;
    const minTop = mapExpanded ? 96 : 160;
    const maxTop = window.innerHeight - 220;
    const next = dragRef.current.top + event.clientY - dragRef.current.y;
    setSheetTop(Math.max(minTop, Math.min(maxTop, next)));
  };

  const endSheetDrag = () => {
    dragRef.current = null;
    window.removeEventListener('mousemove', moveSheetDrag);
    window.removeEventListener('mouseup', endSheetDrag);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <p className="text-gray-400 text-sm">불러오는 중...</p>
      </div>
    );
  }

  if (!trip) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen gap-3">
        <p className="text-gray-500">Trip을 찾을 수 없습니다.</p>
        <Link href="/trips" className="text-green-600 text-sm hover:underline">목록으로</Link>
      </div>
    );
  }

  // 날짜별 posts 그룹
  const days = [...new Set(posts.map((p) => p.date))].sort();
  const dayPosts = posts.filter((p) => p.date === activeDay);
  const isOwner = !!currentUserId && (
    trip.ownerId === currentUserId ||
    trip.author?.id === currentUserId
  );

  return (
    <div className="flex flex-col h-[calc(100vh-64px)] relative overflow-hidden bg-gray-50">
      {/* 지도 (배경) */}
      <div className="absolute inset-0 z-0">
        <TripMap key={activeDay} posts={dayPosts} selectedPostId={focusedPostId} onMarkerSelect={focusPost} sheetTop={sheetTop} dayKey={activeDay} />
      </div>

      {/* 상단 네비 */}
      <div className="relative z-40 flex items-center justify-between px-5 pt-5">
        <button onClick={() => router.back()} className="w-8 h-8 bg-white rounded-full shadow flex items-center justify-center hover:bg-gray-50 transition-colors">
          <ArrowLeft size={16} />
        </button>
        <div className="flex items-center gap-2">
          <button
            onClick={toggleMapExpanded}
            className="flex items-center gap-1 bg-white text-gray-700 text-xs font-semibold px-3 py-1.5 rounded-full shadow hover:bg-gray-50 transition-colors"
          >
            {mapExpanded ? <Minimize2 size={12} /> : <Maximize2 size={12} />}
            {mapExpanded ? '지도 줄이기' : '지도 펼치기'}
          </button>
          {isOwner && (
            <>
              <Link href={`/trips/${tripId}/edit`} className="flex items-center gap-1 bg-white text-gray-700 text-xs font-semibold px-3 py-1.5 rounded-full shadow hover:bg-gray-50 transition-colors">
                <Pencil size={12} /> 수정/편집
              </Link>
              <button onClick={handleDeleteTrip} className="flex items-center gap-1 bg-white text-red-500 text-xs font-semibold px-3 py-1.5 rounded-full shadow hover:bg-red-50 transition-colors">
                <Trash2 size={12} /> 삭제
              </button>
            </>
          )}
        </div>
      </div>
      {message && (
        <p className="absolute left-1/2 top-16 z-[70] -translate-x-1/2 rounded-lg bg-red-50 px-4 py-2 text-sm font-semibold text-red-500 shadow">
          {message}
        </p>
      )}

      {/* 하단 상세 패널 */}
      <div
        className="absolute bottom-0 left-0 right-0 z-[60] flex flex-col overflow-hidden rounded-t-2xl bg-white shadow-2xl"
        style={{ top: sheetTop }}
      >
          {/* 드래그 핸들 */}
          <div onMouseDown={startSheetDrag} className="relative z-10 flex shrink-0 cursor-grab justify-center bg-white px-5 pt-3 active:cursor-grabbing">
            <span className="h-1.5 w-12 rounded-full bg-gray-300" />
          </div>
          <div className="flex justify-between items-center px-5 pt-3 pb-2">
            <div className="flex gap-3 items-start">
              <div className="w-20 h-20 overflow-hidden rounded-xl bg-gradient-to-br from-gray-300 to-gray-400 flex-shrink-0">
                {trip.thumbnailUrl && (
                  <img src={trip.thumbnailUrl} alt="" className="h-full w-full object-cover" />
                )}
              </div>
              <div>
                <h2 className="font-bold text-gray-900 text-base leading-tight">{trip.title}</h2>
                <p className="text-xs text-gray-400 flex items-center gap-1 mt-1">
                  <MapPin size={10} /> {trip.city}, {trip.country}
                </p>
                <p className="text-xs text-gray-400 flex items-center gap-1 mt-0.5">
                  <Calendar size={10} /> {trip.startDate} ~ {trip.endDate}
                </p>
                <div className="flex items-center gap-2 mt-1.5">
                  <span className="flex items-center gap-0.5 text-xs text-gray-400">
                    {trip.author?.profileImageUrl ? (
                      <img src={trip.author.profileImageUrl} alt="" className="w-4 h-4 rounded-full bg-gray-200" />
                    ) : (
                      <span className="w-4 h-4 rounded-full bg-gray-200 inline-block" />
                    )}
                    {trip.author?.nickname ?? '작성자'}
                  </span>
                  {trip.isPublic ? (
                    <span className="flex items-center gap-0.5 text-xs text-green-600"><Globe size={10} /> 공개</span>
                  ) : (
                    <span className="flex items-center gap-0.5 text-xs text-gray-400"><Lock size={10} /> 비공개</span>
                  )}
                </div>
              </div>
            </div>

            <div className="flex flex-col items-end gap-2">
              <button
                onClick={handleLike}
                className={`flex items-center gap-1 text-sm font-medium px-3 py-1.5 rounded-full border transition-colors ${
                  liked ? 'bg-red-50 border-red-200 text-red-500' : 'border-gray-200 text-gray-500 hover:border-red-200 hover:text-red-400'
                }`}
              >
                <Heart size={14} className={liked ? 'fill-red-500' : ''} />
                {likeCount}
              </button>
            </div>
          </div>

          {/* Day 탭 */}
          {days.length > 0 && <DayTabs days={days} active={activeDay} onSelect={handleSelectDay} />}

          {/* 타임라인 */}
          <div ref={timelineRef} className="flex-1 overflow-y-auto px-5 pb-6 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
            {dayPosts.length === 0 ? (
              <p className="text-center text-gray-400 text-sm py-8">이 날의 기록이 없습니다.</p>
            ) : (
              dayPosts.map((post) => (
                <div
                  key={post.id}
                  ref={(node) => {
                    postRefs.current[post.id] = node;
                  }}
                >
                  <TimelineItem post={post} active={focusedPostId === post.id} />
                </div>
              ))
            )}
          </div>
      </div>
    </div>
  );
}
