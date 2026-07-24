'use client';

import { type PointerEvent, type TouchEvent, useEffect, useMemo, useRef, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { GoogleMap, Marker as GoogleMarker, Polyline, useJsApiLoader } from '@react-google-maps/api';
import { ArrowLeft, CalendarDays, ChevronDown, ChevronLeft, ChevronRight, CircleDotDashed, Clock3, Eye, EyeOff, Grid3X3, Images, MapPin, Plus, Route, X } from 'lucide-react';
import { postApi, userApi } from '@/lib/api';
import type { AlbumPost, AlbumPostImage, Trip } from '@/types';

type AlbumView = 'trips' | 'posts' | 'photos';
type DayVisibility = 'visible' | 'dim' | 'hidden';

type AlbumPhoto = AlbumPostImage & {
  post: AlbumPost;
};

type LocatedAlbumPost = AlbumPost & {
  marker: NonNullable<AlbumPost['marker']> & {
    lat: number;
    lng: number;
  };
};

type AlbumTrip = {
  id: string;
  title: string;
  location: string;
  dateRange: string;
  coverImage: string;
  posts: AlbumPost[];
  photoCount: number;
  position?: { lat: number; lng: number };
};

type DayGroup = {
  key: string;
  label: string;
  posts: AlbumPost[];
  locatedCount: number;
  photoCount: number;
};

type MarkerDisplayItem =
  | {
      type: 'post';
      post: LocatedAlbumPost;
      position: { lat: number; lng: number };
      color: string;
      dimmed: boolean;
    }
  | {
      type: 'cluster';
      id: string;
      posts: LocatedAlbumPost[];
      position: { lat: number; lng: number };
      color: string;
      dimmed: boolean;
    };

const albumTabs: Array<{ key: AlbumView; label: string; icon: typeof Grid3X3 }> = [
  { key: 'trips', label: 'Trips', icon: Route },
  { key: 'posts', label: 'Posts', icon: Grid3X3 },
  { key: 'photos', label: 'Photos', icon: Images },
];

const googleMapsApiKey = process.env.NEXT_PUBLIC_GOOGLE_MAPS_API_KEY ?? '';
const GOOGLE_MAPS_SCRIPT_ID = 'triptrace-google-map-script';
const albumMapContainerStyle = { width: '100%', height: '100%' };
const albumMapOptions = {
  disableDefaultUI: true,
  zoomControl: true,
  clickableIcons: false,
  gestureHandling: 'greedy',
};
const dayColors = ['#1d4ed8', '#c2410c', '#7c3aed', '#be123c', '#0891b2', '#ca8a04', '#4338ca', '#db2777'];

function formatDate(value?: string) {
  if (!value) return '';
  return value.slice(0, 10).replaceAll('-', '.');
}

function getPostDayKey(post: AlbumPost) {
  return post.date || 'unknown';
}

function getDayMode(dayModes: Record<string, DayVisibility>, tripId: string, dayKey: string): DayVisibility {
  return dayModes[`${tripId}:${dayKey}`] ?? 'visible';
}

function getDayColor(index: number) {
  return dayColors[index % dayColors.length];
}

function getDayColorByKey(dayColorByKey: Record<string, string>, dayKey: string) {
  return dayColorByKey[dayKey] ?? '#2563eb';
}

function formatTripDateRange(trip?: Trip) {
  if (!trip) return '';
  const start = formatDate(trip.startDate);
  const end = formatDate(trip.endDate);
  if (!start) return '';
  return end && end !== start ? `${start} - ${end}` : start;
}

function getCoverImage(post: AlbumPost) {
  return post.marker?.representativeImageUrl || post.images[0]?.thumbnailUrl || post.images[0]?.url || '';
}

function getPostPosition(post: AlbumPost) {
  if (post.marker?.lat == null || post.marker.lng == null) return undefined;
  return { lat: post.marker.lat, lng: post.marker.lng };
}

function getLocatedPosts(posts: AlbumPost[]) {
  return posts.filter((post): post is LocatedAlbumPost => {
    const position = getPostPosition(post);
    return !!position && Number.isFinite(position.lat) && Number.isFinite(position.lng);
  });
}

function getDisplayPostPositions(posts: LocatedAlbumPost[]) {
  const grouped = new Map<string, Array<{ post: LocatedAlbumPost; base: { lat: number; lng: number } }>>();

  posts.forEach((post) => {
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

function groupPostsByDay(posts: AlbumPost[]) {
  const grouped = new Map<string, AlbumPost[]>();

  posts.forEach((post) => {
    const key = getPostDayKey(post);
    grouped.set(key, [...(grouped.get(key) ?? []), post]);
  });

  return Array.from(grouped.entries()).map(([key, dayPosts], index): DayGroup => ({
    key,
    label: `Day ${index + 1}${key === 'unknown' ? '' : ` · ${formatDate(key)}`}`,
    posts: dayPosts,
    locatedCount: getLocatedPosts(dayPosts).length,
    photoCount: dayPosts.reduce((sum, post) => sum + post.images.length, 0),
  }));
}

function getClusterBucket(position: { lat: number; lng: number }, zoom: number, crowded: boolean) {
  const step = zoom < 8 ? 0.8 : zoom < 10 ? 0.24 : zoom < 12 ? 0.08 : crowded ? 0.022 : 0.01;
  return `${Math.round(position.lat / step)}:${Math.round(position.lng / step)}`;
}

function getMarkerDisplayItems({
  posts,
  positions,
  dayModes,
  dayColorByKey,
  tripId,
  mapHeight,
  zoom,
}: {
  posts: LocatedAlbumPost[];
  positions: Map<string, { lat: number; lng: number }>;
  dayModes: Record<string, DayVisibility>;
  dayColorByKey: Record<string, string>;
  tripId?: string;
  mapHeight: number;
  zoom: number;
}) {
  const maxIndividualMarkers = Math.max(28, Math.floor(mapHeight / 14));
  const crowded = posts.length > maxIndividualMarkers;
  const shouldCluster = zoom < 10 || (zoom < 12 && crowded) || posts.length > maxIndividualMarkers * 2;

  if (!shouldCluster) {
    return posts.map((post): MarkerDisplayItem => ({
      type: 'post',
      post,
      position: positions.get(post.id) ?? { lat: post.marker.lat, lng: post.marker.lng },
      color: getDayColorByKey(dayColorByKey, getPostDayKey(post)),
      dimmed: tripId ? getDayMode(dayModes, tripId, getPostDayKey(post)) === 'dim' : false,
    }));
  }

  const buckets = new Map<string, LocatedAlbumPost[]>();
  posts.forEach((post) => {
    const position = positions.get(post.id) ?? { lat: post.marker.lat, lng: post.marker.lng };
    const bucket = getClusterBucket(position, zoom, crowded);
    buckets.set(bucket, [...(buckets.get(bucket) ?? []), post]);
  });

  return Array.from(buckets.entries()).map(([id, bucketPosts]): MarkerDisplayItem => {
    if (bucketPosts.length === 1) {
      const post = bucketPosts[0];
      return {
        type: 'post',
        post,
        position: positions.get(post.id) ?? { lat: post.marker.lat, lng: post.marker.lng },
        color: getDayColorByKey(dayColorByKey, getPostDayKey(post)),
        dimmed: tripId ? getDayMode(dayModes, tripId, getPostDayKey(post)) === 'dim' : false,
      };
    }

    const points = bucketPosts.map((post) => positions.get(post.id) ?? { lat: post.marker.lat, lng: post.marker.lng });
    const position = points.reduce(
      (sum, point) => ({ lat: sum.lat + point.lat / points.length, lng: sum.lng + point.lng / points.length }),
      { lat: 0, lng: 0 },
    );
    const dimmed = tripId
      ? bucketPosts.every((post) => getDayMode(dayModes, tripId, getPostDayKey(post)) === 'dim')
      : false;
    const clusterColors = [...new Set(bucketPosts.map((post) => getDayColorByKey(dayColorByKey, getPostDayKey(post))))];
    const color = clusterColors.length === 1 ? clusterColors[0] : '#0f172a';

    return { type: 'cluster', id, posts: bucketPosts, position, color, dimmed };
  });
}

export default function PhotosPage() {
  const router = useRouter();
  const mapRef = useRef<google.maps.Map | null>(null);
  const lastCenteredTripIdRef = useRef<string | null>(null);
  const modalDragStartRef = useRef<{ x: number; y: number } | null>(null);
  const [posts, setPosts] = useState<AlbumPost[]>([]);
  const [trips, setTrips] = useState<Trip[]>([]);
  const [activeView, setActiveView] = useState<AlbumView>('trips');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selectedPostId, setSelectedPostId] = useState<string | null>(null);
  const [selectedImageId, setSelectedImageId] = useState<string | null>(null);
  const [selectedTripId, setSelectedTripId] = useState<string | null>(null);
  const [focusedPostId, setFocusedPostId] = useState<string | null>(null);
  const [dayModes, setDayModes] = useState<Record<string, DayVisibility>>({});
  const [collapsedDays, setCollapsedDays] = useState<Record<string, boolean>>({});
  const [mapView, setMapView] = useState({ height: 420, zoom: 7 });
  const { isLoaded: isMapLoaded, loadError: mapLoadError } = useJsApiLoader({
    googleMapsApiKey,
    id: GOOGLE_MAPS_SCRIPT_ID,
  });

  useEffect(() => {
    let mounted = true;

    async function loadAlbumPosts() {
      setLoading(true);
      setError('');

      try {
        const [postResult, tripResult] = await Promise.all([
          postApi.getAlbumPosts(),
          userApi.getMyTrips({ page: 0, size: 100 }).catch(() => []),
        ]);
        if (!mounted) return;
        setPosts(postResult as AlbumPost[]);
        setTrips(tripResult as Trip[]);
      } catch (err) {
        if (!mounted) return;
        setError(err instanceof Error ? err.message : '앨범을 불러오지 못했습니다.');
      } finally {
        if (mounted) setLoading(false);
      }
    }

    loadAlbumPosts();

    return () => {
      mounted = false;
    };
  }, []);

  const selectedPost = useMemo(
    () => posts.find((post) => post.id === selectedPostId) ?? null,
    [posts, selectedPostId],
  );
  const focusedPost = useMemo(
    () => posts.find((post) => post.id === focusedPostId) ?? null,
    [focusedPostId, posts],
  );

  const selectedImage = useMemo<AlbumPostImage | null>(() => {
    if (!selectedPost) return null;
    return selectedPost.images.find((image) => image.id === selectedImageId) ?? selectedPost.images[0] ?? null;
  }, [selectedImageId, selectedPost]);

  const photos = useMemo<AlbumPhoto[]>(
    () => posts.flatMap((post) => post.images.map((image) => ({ ...image, post }))),
    [posts],
  );

  const albumTrips = useMemo<AlbumTrip[]>(() => {
    const tripMeta = new Map(trips.map((trip) => [trip.id, trip]));
    const postGroups = new Map<string, AlbumPost[]>();

    posts.forEach((post) => {
      postGroups.set(post.tripId, [...(postGroups.get(post.tripId) ?? []), post]);
    });

    return Array.from(postGroups.entries()).map(([tripId, tripPosts]) => {
      const trip = tripMeta.get(tripId);
      const firstCoverPost = tripPosts.find((post) => getCoverImage(post));
      const firstLocatedPost = tripPosts.find((post) => getPostPosition(post));
      const position = trip?.representativeLat != null && trip.representativeLng != null
        ? { lat: trip.representativeLat, lng: trip.representativeLng }
        : firstLocatedPost ? getPostPosition(firstLocatedPost) : undefined;

      return {
        id: tripId,
        title: trip?.title ?? `Trip #${tripId}`,
        location: [trip?.city, trip?.country].filter(Boolean).join(', '),
        dateRange: formatTripDateRange(trip),
        coverImage: trip?.thumbnailUrl || (firstCoverPost ? getCoverImage(firstCoverPost) : ''),
        posts: tripPosts,
        photoCount: tripPosts.reduce((sum, post) => sum + post.images.length, 0),
        position,
      };
    });
  }, [posts, trips]);

  const locatedTrips = useMemo(
    () => albumTrips.filter((trip): trip is AlbumTrip & { position: { lat: number; lng: number } } => !!trip.position),
    [albumTrips],
  );

  const selectedTrip = useMemo(
    () => albumTrips.find((trip) => trip.id === selectedTripId) ?? albumTrips[0] ?? null,
    [albumTrips, selectedTripId],
  );
  const modalPosts = useMemo(
    () => activeView === 'trips' && selectedTrip ? selectedTrip.posts : posts,
    [activeView, posts, selectedTrip],
  );
  const selectedPostIndex = useMemo(
    () => selectedPost ? modalPosts.findIndex((post) => post.id === selectedPost.id) : -1,
    [modalPosts, selectedPost],
  );
  const selectedImageIndex = useMemo(
    () => selectedPost && selectedImage ? selectedPost.images.findIndex((image) => image.id === selectedImage.id) : -1,
    [selectedImage, selectedPost],
  );

  const mapCenter = selectedTrip?.position ?? locatedTrips[0]?.position ?? { lat: 36.5, lng: 127.8 };
  const selectedTripLocatedPosts = useMemo(
    () => getLocatedPosts(selectedTrip?.posts ?? []),
    [selectedTrip],
  );
  const selectedTripPostPositions = useMemo(
    () => getDisplayPostPositions(selectedTripLocatedPosts),
    [selectedTripLocatedPosts],
  );
  const selectedTripDayGroups = useMemo(
    () => groupPostsByDay(selectedTrip?.posts ?? []),
    [selectedTrip],
  );
  const selectedTripDayColorByKey = useMemo(
    () => Object.fromEntries(selectedTripDayGroups.map((day, index) => [day.key, getDayColor(index)])),
    [selectedTripDayGroups],
  );
  const visibleSelectedTripLocatedPosts = useMemo(
    () => selectedTripLocatedPosts.filter((post) => {
      if (!selectedTrip) return true;
      return getDayMode(dayModes, selectedTrip.id, getPostDayKey(post)) !== 'hidden';
    }),
    [dayModes, selectedTrip, selectedTripLocatedPosts],
  );
  const selectedTripDayPaths = useMemo(
    () => {
      if (!selectedTrip) return [];
      return selectedTripDayGroups.map((day) => {
        const mode = getDayMode(dayModes, selectedTrip.id, day.key);
        const path = getLocatedPosts(day.posts).map((post) => selectedTripPostPositions.get(post.id) ?? {
          lat: post.marker.lat,
          lng: post.marker.lng,
        });
        return { key: day.key, mode, path, color: getDayColorByKey(selectedTripDayColorByKey, day.key) };
      }).filter((day) => day.mode !== 'hidden' && day.path.length > 1);
    },
    [dayModes, selectedTrip, selectedTripDayColorByKey, selectedTripDayGroups, selectedTripPostPositions],
  );
  const selectedTripMarkerItems = useMemo(
    () => getMarkerDisplayItems({
      posts: visibleSelectedTripLocatedPosts,
      positions: selectedTripPostPositions,
      dayModes,
      dayColorByKey: selectedTripDayColorByKey,
      tripId: selectedTrip?.id,
      mapHeight: mapView.height,
      zoom: mapView.zoom,
    }),
    [dayModes, mapView.height, mapView.zoom, selectedTrip?.id, selectedTripDayColorByKey, selectedTripPostPositions, visibleSelectedTripLocatedPosts],
  );
  const selectedTripFocusPosition = useMemo(() => {
    if (!selectedTrip) return undefined;
    if (selectedTrip.position) return selectedTrip.position;
    const firstLocatedPost = selectedTripLocatedPosts[0];
    return firstLocatedPost ? selectedTripPostPositions.get(firstLocatedPost.id) ?? getPostPosition(firstLocatedPost) : undefined;
  }, [selectedTrip, selectedTripLocatedPosts, selectedTripPostPositions]);

  useEffect(() => {
    if (!mapRef.current || !isMapLoaded || activeView !== 'trips' || !selectedTrip || !selectedTripFocusPosition) return;
    if (lastCenteredTripIdRef.current === selectedTrip.id) return;

    mapRef.current.panTo(selectedTripFocusPosition);
    mapRef.current.setZoom(12);
    lastCenteredTripIdRef.current = selectedTrip.id;
  }, [activeView, isMapLoaded, selectedTrip, selectedTripFocusPosition]);

  const imageCount = useMemo(
    () => posts.reduce((sum, post) => sum + post.images.length, 0),
    [posts],
  );

  const tripCount = useMemo(
    () => new Set(posts.map((post) => post.tripId).filter(Boolean)).size,
    [posts],
  );

  function openPost(post: AlbumPost) {
    setSelectedPostId(post.id);
    setSelectedImageId(post.images[0]?.id ?? null);
  }

  function openPhoto(photo: AlbumPhoto) {
    setSelectedPostId(photo.post.id);
    setSelectedImageId(photo.id);
  }

  function openTrip(trip: AlbumTrip) {
    setSelectedTripId(trip.id);
    const firstPostWithImage = trip.posts.find((post) => post.images.length > 0) ?? trip.posts[0];
    if (firstPostWithImage) openPost(firstPostWithImage);
  }

  function moveSelectedImage(offset: number) {
    if (!selectedPost || selectedPost.images.length < 2) return;
    const currentIndex = selectedImageIndex >= 0 ? selectedImageIndex : 0;
    const nextIndex = (currentIndex + offset + selectedPost.images.length) % selectedPost.images.length;
    setSelectedImageId(selectedPost.images[nextIndex].id);
  }

  function moveSelectedPost(offset: number) {
    if (!selectedPost || modalPosts.length < 2) return;
    const currentIndex = selectedPostIndex >= 0 ? selectedPostIndex : 0;
    const nextIndex = (currentIndex + offset + modalPosts.length) % modalPosts.length;
    openPost(modalPosts[nextIndex]);
  }

  function handleModalTouchStart(event: TouchEvent<HTMLDivElement>) {
    const touch = event.touches[0];
    modalDragStartRef.current = { x: touch.clientX, y: touch.clientY };
  }

  function handleModalTouchEnd(event: TouchEvent<HTMLDivElement>) {
    if (!modalDragStartRef.current) return;
    const touch = event.changedTouches[0];
    finishImageDrag(touch.clientX, touch.clientY);
  }

  function handleModalPointerDown(event: PointerEvent<HTMLDivElement>) {
    if (event.pointerType !== 'mouse' || event.button !== 0) return;
    modalDragStartRef.current = { x: event.clientX, y: event.clientY };
  }

  function handleModalPointerUp(event: PointerEvent<HTMLDivElement>) {
    if (event.pointerType !== 'mouse') return;
    finishImageDrag(event.clientX, event.clientY);
  }

  function finishImageDrag(clientX: number, clientY: number) {
    if (!modalDragStartRef.current) return;
    const deltaX = clientX - modalDragStartRef.current.x;
    const deltaY = clientY - modalDragStartRef.current.y;
    modalDragStartRef.current = null;

    if (Math.abs(deltaX) < 48 || Math.abs(deltaX) < Math.abs(deltaY)) return;
    moveSelectedImage(deltaX < 0 ? 1 : -1);
  }

  function setDayMode(tripId: string, dayKey: string, mode: DayVisibility) {
    setDayModes((current) => ({ ...current, [`${tripId}:${dayKey}`]: mode }));
  }

  function toggleDayCollapsed(tripId: string, dayKey: string) {
    setCollapsedDays((current) => {
      const key = `${tripId}:${dayKey}`;
      return { ...current, [key]: !current[key] };
    });
  }

  function updateMapView(map: google.maps.Map) {
    setMapView({
      height: map.getDiv().clientHeight || 420,
      zoom: map.getZoom() ?? 7,
    });
  }

  function focusCluster(postsToFocus: LocatedAlbumPost[]) {
    if (!mapRef.current || postsToFocus.length === 0) return;

    const bounds = new google.maps.LatLngBounds();
    postsToFocus.forEach((post) => {
      const position = selectedTripPostPositions.get(post.id) ?? { lat: post.marker.lat, lng: post.marker.lng };
      bounds.extend(position);
    });

    mapRef.current.fitBounds(bounds, 72);
    window.setTimeout(() => {
      if (!mapRef.current) return;
      const zoom = mapRef.current.getZoom();
      if (typeof zoom === 'number' && zoom > 15) mapRef.current.setZoom(15);
      updateMapView(mapRef.current);
    }, 0);
  }

  function focusPostMarker(post: AlbumPost) {
    const position = getPostPosition(post);
    if (!position) return;
    setFocusedPostId(post.id);
    setSelectedTripId(post.tripId);
    setDayMode(post.tripId, getPostDayKey(post), 'visible');

    if (!mapRef.current) return;
    mapRef.current.panTo(position);
    const currentZoom = mapRef.current.getZoom();
    mapRef.current.setZoom(Math.max(typeof currentZoom === 'number' ? currentZoom : 0, 15));
    updateMapView(mapRef.current);
  }

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="text-sm text-gray-400">앨범을 불러오는 중...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-4 px-6 text-center">
        <p className="text-sm text-gray-500">{error}</p>
        <button
          type="button"
          onClick={() => router.push('/auth/login')}
          className="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-700"
        >
          로그인하러 가기
        </button>
      </div>
    );
  }

  return (
    <div className="mx-auto min-h-screen max-w-6xl px-4 py-5 sm:px-6 sm:py-8">
      <button
        type="button"
        onClick={() => router.back()}
        className="mb-6 flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700"
      >
        <ArrowLeft size={16} />
        <span>앨범</span>
      </button>

      <section className="mb-5 border-b border-gray-200 sm:mb-6">
        <div className="flex flex-col gap-4 pb-5 sm:flex-row sm:items-center sm:gap-6 sm:pb-6">
          <div className="flex h-20 w-20 flex-shrink-0 items-center justify-center overflow-hidden rounded-full bg-white ring-1 ring-gray-200 sm:h-32 sm:w-32">
            {posts[0] && getCoverImage(posts[0]) ? (
              <img src={getCoverImage(posts[0])} alt="" className="h-full w-full object-cover" />
            ) : (
              <Images size={36} className="text-gray-300" />
            )}
          </div>

          <div className="min-w-0 flex-1">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
              <div>
                <h1 className="text-xl font-bold text-gray-900 sm:text-2xl">내 앨범</h1>
                <div className="mt-4 flex flex-wrap gap-x-5 gap-y-2 text-sm sm:mt-5 sm:gap-8">
                  <p><span className="font-bold text-gray-900">{posts.length}</span> 기록</p>
                  <p><span className="font-bold text-gray-900">{imageCount}</span> 사진</p>
                  <p><span className="font-bold text-gray-900">{tripCount}</span> Trip</p>
                </div>
              </div>

              <div className="flex flex-wrap gap-2">
                <Link
                  href="/trips?create=1"
                  className="inline-flex items-center gap-1.5 rounded-lg bg-emerald-600 px-3 py-2 text-sm font-semibold text-white hover:bg-emerald-700"
                >
                  <Plus size={15} />
                  Trip 만들기
                </Link>
                <Link
                  href="/trips"
                  className="inline-flex items-center rounded-lg border border-gray-200 px-3 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-50"
                >
                  관리
                </Link>
              </div>
            </div>
          </div>
        </div>

        <div className="flex items-center justify-center overflow-x-auto">
          <div className="flex w-full min-w-0 max-w-md items-center justify-between">
            {albumTabs.map(({ key, label, icon: Icon }) => {
              const active = activeView === key;
              return (
                <button
                  key={key}
                  type="button"
                  onClick={() => setActiveView(key)}
                  className={`flex min-h-11 flex-1 items-center justify-center gap-2 border-b-2 px-3 text-xs font-bold uppercase tracking-wide transition-colors ${
                    active
                      ? 'border-gray-900 text-gray-900'
                      : 'border-transparent text-gray-400 hover:border-gray-200 hover:text-gray-700'
                  }`}
                >
                  <Icon size={14} />
                  {label}
                </button>
              );
            })}
          </div>
        </div>
      </section>

      {posts.length === 0 ? (
        <div className="flex min-h-[320px] flex-col items-center justify-center rounded-xl border border-dashed border-gray-200 bg-white px-6 text-center">
          <Images size={34} className="text-gray-300" />
          <p className="mt-4 text-sm font-semibold text-gray-700">아직 앨범에 표시할 기록이 없습니다.</p>
          <p className="mt-2 text-sm text-gray-400">Trip에 사진 기록을 만들면 이곳에 Post 단위로 모입니다.</p>
          <Link
            href="/trips?create=1"
            className="mt-5 rounded-lg bg-emerald-600 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-700"
          >
            Trip 만들기
          </Link>
        </div>
      ) : activeView === 'trips' ? (
        <div className="space-y-5">
          <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_320px]">
            <div className="relative h-[360px] overflow-hidden rounded-lg border border-gray-200 bg-gray-100 sm:h-[420px]">
              {!googleMapsApiKey || mapLoadError || locatedTrips.length === 0 ? (
                <div className="flex h-full flex-col items-center justify-center px-6 text-center">
                  <MapPin size={34} className="text-gray-300" />
                  <p className="mt-4 text-sm font-semibold text-gray-700">
                    {locatedTrips.length === 0 ? '표시할 Trip 위치가 없습니다.' : '지도를 불러올 수 없습니다.'}
                  </p>
                  <p className="mt-2 text-sm text-gray-400">Trip 대표 좌표나 Post marker 좌표가 있으면 지도에 표시됩니다.</p>
                </div>
              ) : !isMapLoaded ? (
                <div className="flex h-full items-center justify-center text-sm text-gray-400">지도를 불러오는 중...</div>
              ) : (
                <GoogleMap
                  mapContainerStyle={albumMapContainerStyle}
                  options={albumMapOptions}
                  onLoad={(map) => {
                    mapRef.current = map;
                    map.setCenter(mapCenter);
                    map.setZoom(selectedTrip ? 12 : 5);
                    updateMapView(map);
                    lastCenteredTripIdRef.current = selectedTrip?.id ?? null;
                  }}
                  onIdle={() => {
                    if (mapRef.current) updateMapView(mapRef.current);
                  }}
                  onUnmount={() => {
                    mapRef.current = null;
                    lastCenteredTripIdRef.current = null;
                  }}
                >
                  {selectedTripDayPaths.map((dayPath) => (
                    <Polyline
                      key={dayPath.key}
                      path={dayPath.path}
                      options={{
                        icons: [{
                          icon: {
                            path: google.maps.SymbolPath.FORWARD_CLOSED_ARROW,
                            scale: 3.2,
                            strokeColor: dayPath.color,
                            fillColor: dayPath.color,
                            fillOpacity: 1,
                          },
                          offset: '100%',
                        }],
                        strokeColor: dayPath.color,
                        strokeOpacity: dayPath.mode === 'dim' ? 0.25 : 0.85,
                        strokeWeight: 4,
                      }}
                    />
                  ))}
                  {selectedTripMarkerItems.map((item) => {
                    if (item.type === 'cluster') {
                      return (
                        <GoogleMarker
                          key={`cluster-${item.id}`}
                          position={item.position}
                          title={`${item.posts.length} Posts`}
                          icon={{
                            path: google.maps.SymbolPath.CIRCLE,
                            scale: Math.min(22, 13 + item.posts.length * 1.4),
                            fillColor: item.color,
                            fillOpacity: item.dimmed ? 0.32 : 0.9,
                            strokeColor: '#ffffff',
                            strokeWeight: 4,
                          }}
                          label={{
                            text: String(item.posts.length),
                            color: '#ffffff',
                            fontSize: '12px',
                            fontWeight: '800',
                          }}
                          onClick={() => focusCluster(item.posts)}
                        />
                      );
                    }

                    const post = item.post;
                    const index = selectedTripLocatedPosts.findIndex((locatedPost) => locatedPost.id === post.id);
                    const focused = focusedPostId === post.id;
                    return (
                      <GoogleMarker
                        key={`post-${post.id}`}
                        position={item.position}
                        title={post.marker?.placeName ?? post.title}
                        icon={{
                          path: google.maps.SymbolPath.CIRCLE,
                          scale: focused ? 15 : 13,
                          fillColor: item.color,
                          fillOpacity: item.dimmed ? 0.35 : 1,
                          strokeColor: focused ? '#111827' : '#ffffff',
                          strokeWeight: focused ? 4 : 3,
                        }}
                        label={{
                          text: String(index + 1),
                          color: '#ffffff',
                          fontSize: '12px',
                          fontWeight: '800',
                        }}
                        onClick={() => {
                          focusPostMarker(post);
                        }}
                      />
                    );
                  })}
                </GoogleMap>
              )}
              {focusedPost && (
                <div className="absolute bottom-3 left-3 right-3 z-10 max-w-sm rounded-lg bg-white p-2 shadow-xl ring-1 ring-black/10 sm:right-auto sm:w-80">
                  <div className="flex gap-3">
                    <button
                      type="button"
                      onClick={() => openPost(focusedPost)}
                      className="h-20 w-24 flex-shrink-0 overflow-hidden rounded-md bg-gray-100"
                    >
                      {getCoverImage(focusedPost) ? (
                        <img src={getCoverImage(focusedPost)} alt="" className="h-full w-full object-cover" />
                      ) : (
                        <span className="flex h-full w-full items-center justify-center">
                          <Images size={18} className="text-gray-300" />
                        </span>
                      )}
                    </button>
                    <div className="min-w-0 flex-1 py-0.5">
                      <div className="flex items-start gap-2">
                        <button
                          type="button"
                          onClick={() => openPost(focusedPost)}
                          className="min-w-0 flex-1 text-left"
                        >
                          <p className="line-clamp-1 text-sm font-bold text-gray-900">{focusedPost.title}</p>
                          <p className="mt-1 line-clamp-2 text-xs leading-5 text-gray-500">
                            {focusedPost.content || focusedPost.marker?.placeName || '메모가 없습니다.'}
                          </p>
                        </button>
                        <button
                          type="button"
                          onClick={() => setFocusedPostId(null)}
                          className="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-full text-gray-300 hover:bg-gray-100 hover:text-gray-500"
                          aria-label="프리뷰 닫기"
                        >
                          <X size={15} />
                        </button>
                      </div>
                      <div className="mt-2 flex items-center gap-2 text-[11px] text-gray-400">
                        {focusedPost.marker?.placeName && (
                          <span className="line-clamp-1">{focusedPost.marker.placeName}</span>
                        )}
                        {focusedPost.images.length > 0 && <span>{focusedPost.images.length} Photos</span>}
                      </div>
                    </div>
                  </div>
                </div>
              )}
            </div>

            <div className="space-y-2 lg:max-h-[420px] lg:overflow-y-auto lg:pr-1">
              {albumTrips.map((trip) => (
                <div
                  key={trip.id}
                  className={`overflow-hidden rounded-lg border bg-white ${
                    selectedTrip?.id === trip.id ? 'border-emerald-300 ring-2 ring-emerald-500/10' : 'border-gray-200'
                  }`}
                >
                  <button
                    type="button"
                    onClick={() => {
                      setFocusedPostId(null);
                      setSelectedTripId(trip.id);
                    }}
                    className="flex w-full gap-3 p-3 text-left hover:bg-gray-50"
                  >
                    <div className="h-14 w-14 flex-shrink-0 overflow-hidden rounded-md bg-gray-100">
                      {trip.coverImage ? (
                        <img src={trip.coverImage} alt="" className="h-full w-full object-cover" />
                      ) : (
                        <div className="flex h-full w-full items-center justify-center">
                          <Images size={18} className="text-gray-300" />
                        </div>
                      )}
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="line-clamp-1 text-sm font-bold text-gray-900">{trip.title}</p>
                      <p className="mt-1 line-clamp-1 text-xs text-gray-500">{trip.location || '위치 정보 없음'}</p>
                      <p className="mt-1 text-xs text-gray-400">{trip.posts.length} Posts · {trip.photoCount} Photos</p>
                    </div>
                  </button>

                  {selectedTrip?.id === trip.id && (
                    <div className="border-t border-gray-100 bg-gray-50/70 p-2">
                      <div className="max-h-[52vh] space-y-2 overflow-y-auto pr-1 lg:max-h-80">
                        {selectedTripDayGroups.map((day) => {
                          const mode = getDayMode(dayModes, trip.id, day.key);
                          const collapsed = collapsedDays[`${trip.id}:${day.key}`] ?? false;
                          const dayColor = getDayColorByKey(selectedTripDayColorByKey, day.key);
                          return (
                            <section key={day.key} className="overflow-hidden rounded-md border border-gray-100 bg-white">
                              <div
                                role="button"
                                tabIndex={0}
                                onClick={() => toggleDayCollapsed(trip.id, day.key)}
                                onKeyDown={(event) => {
                                  if (event.key !== 'Enter' && event.key !== ' ') return;
                                  event.preventDefault();
                                  toggleDayCollapsed(trip.id, day.key);
                                }}
                                className="w-full border-b border-gray-100 p-2 text-left hover:bg-gray-50"
                                aria-expanded={!collapsed}
                              >
                                <div className="space-y-2">
                                  <div className="flex min-w-0 items-start gap-2">
                                    <span className="mt-0.5 flex h-5 w-5 flex-shrink-0 items-center justify-center rounded text-gray-400">
                                      <ChevronDown size={14} className={collapsed ? '-rotate-90 transition-transform' : 'transition-transform'} />
                                    </span>
                                    <span
                                      className="mt-1.5 h-2.5 w-2.5 flex-shrink-0 rounded-full"
                                      style={{ backgroundColor: dayColor }}
                                    />
                                    <p className="min-w-0 flex-1 text-xs font-bold text-gray-800">{day.label}</p>
                                  </div>
                                  <div className="flex items-center justify-between gap-2 pl-7 sm:pl-9">
                                    <div className="flex min-w-0 items-center gap-2 text-[11px] text-gray-400">
                                      <span className="inline-flex items-center gap-0.5" title="Posts">
                                        <Grid3X3 size={11} />
                                        {day.posts.length}
                                      </span>
                                      <span className="inline-flex items-center gap-0.5" title="Markers">
                                        <MapPin size={11} />
                                        {day.locatedCount}
                                      </span>
                                      <span className="inline-flex items-center gap-0.5" title="Photos">
                                        <Images size={11} />
                                        {day.photoCount}
                                      </span>
                                    </div>
                                    <div className="flex flex-shrink-0 overflow-hidden rounded-md border border-gray-200 bg-white">
                                      {([
                                        ['visible', Eye, '진하게'],
                                        ['dim', CircleDotDashed, '약하게'],
                                        ['hidden', EyeOff, '숨김'],
                                      ] as Array<[DayVisibility, typeof Eye, string]>).map(([value, Icon, label]) => (
                                        <button
                                          key={value}
                                          type="button"
                                          onClick={(event) => {
                                            event.stopPropagation();
                                            setDayMode(trip.id, day.key, value);
                                          }}
                                          className={`flex h-7 w-7 items-center justify-center ${
                                            mode === value
                                              ? value === 'hidden'
                                                ? 'bg-gray-700 text-white'
                                                : value === 'dim'
                                                  ? 'bg-blue-100 text-blue-700'
                                                  : 'bg-blue-700 text-white'
                                              : 'bg-white text-gray-400 hover:bg-gray-50'
                                          }`}
                                          title={label}
                                          aria-label={label}
                                        >
                                          <Icon size={14} />
                                        </button>
                                      ))}
                                    </div>
                                  </div>
                                </div>
                              </div>

                              {!collapsed && (
                              <div className={mode === 'hidden' ? 'opacity-45' : mode === 'dim' ? 'opacity-70' : ''}>
                                {day.posts.map((post) => {
                                  const coverImage = getCoverImage(post);
                                  const hasMarker = !!getPostPosition(post);
                                  const markerIndex = selectedTripLocatedPosts.findIndex((locatedPost) => locatedPost.id === post.id);
                                  return (
                                    <div
                                      key={post.id}
                                      className="flex w-full items-center gap-2 rounded-md p-2 text-left hover:bg-gray-50"
                                    >
                                      <button
                                        type="button"
                                        onClick={(event) => {
                                          event.stopPropagation();
                                          focusPostMarker(post);
                                        }}
                                        className={`flex h-6 w-6 flex-shrink-0 items-center justify-center rounded-full text-[11px] font-bold transition-transform hover:scale-110 ${
                                          hasMarker ? 'text-white' : 'bg-gray-200 text-gray-500'
                                        }`}
                                        style={hasMarker ? { backgroundColor: dayColor } : undefined}
                                        title={hasMarker ? '지도에서 보기' : '좌표 없음'}
                                      >
                                        {markerIndex >= 0 ? markerIndex + 1 : '-'}
                                      </button>
                                      <button
                                        type="button"
                                        onClick={() => openPost(post)}
                                        className="flex min-w-0 flex-1 items-center gap-2 text-left"
                                      >
                                        <div className="h-9 w-9 flex-shrink-0 overflow-hidden rounded bg-gray-100">
                                          {coverImage ? (
                                            <img src={coverImage} alt="" className="h-full w-full object-cover" />
                                          ) : (
                                            <div className="flex h-full w-full items-center justify-center">
                                              <Images size={14} className="text-gray-300" />
                                            </div>
                                          )}
                                        </div>
                                        <div className="min-w-0 flex-1">
                                          <p className="line-clamp-1 text-xs font-bold text-gray-800">{post.title}</p>
                                          <p className="mt-0.5 line-clamp-1 text-[11px] text-gray-400">
                                            {post.marker?.placeName || formatDate(post.date) || 'Post'}
                                          </p>
                                        </div>
                                      </button>
                                    </div>
                                  );
                                })}
                              </div>
                              )}
                            </section>
                          );
                        })}
                      </div>
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {albumTrips.map((trip) => (
              <button
                key={trip.id}
                type="button"
                onClick={() => openTrip(trip)}
                className="group overflow-hidden rounded-lg border border-gray-200 bg-white text-left shadow-sm transition hover:-translate-y-0.5 hover:shadow-md"
              >
                <div className="relative aspect-[4/3] bg-gray-100">
                  {trip.coverImage ? (
                    <img
                      src={trip.coverImage}
                      alt=""
                      className="h-full w-full object-cover transition-transform duration-200 group-hover:scale-105"
                    />
                  ) : (
                    <div className="flex h-full w-full items-center justify-center">
                      <Images size={34} className="text-gray-300" />
                    </div>
                  )}
                  <span className="absolute inset-0 bg-gradient-to-t from-black/60 via-black/0 to-black/0" />
                  <div className="absolute bottom-3 left-3 right-3 text-white">
                    <p className="line-clamp-1 text-sm font-bold">{trip.title}</p>
                    {trip.location && <p className="mt-1 line-clamp-1 text-xs text-white/85">{trip.location}</p>}
                  </div>
                </div>
                <div className="p-3">
                  <div className="flex flex-wrap gap-3 text-xs text-gray-500">
                    <span>{trip.posts.length} Posts</span>
                    <span>{trip.photoCount} Photos</span>
                  </div>
                  {trip.dateRange && <p className="mt-2 text-xs text-gray-400">{trip.dateRange}</p>}
                </div>
              </button>
            ))}
          </div>
        </div>
      ) : activeView === 'posts' ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {posts.map((post) => {
            const coverImage = getCoverImage(post);
            return (
              <button
                key={post.id}
                type="button"
                onClick={() => openPost(post)}
                className="group overflow-hidden rounded-lg border border-gray-200 bg-white text-left shadow-sm transition hover:-translate-y-0.5 hover:shadow-md"
              >
                <div className="relative aspect-square bg-gray-100">
                  {coverImage ? (
                    <img
                      src={coverImage}
                      alt=""
                      className="h-full w-full object-cover transition-transform duration-200 group-hover:scale-105"
                    />
                  ) : (
                    <div className="flex h-full w-full items-center justify-center">
                      <Images size={34} className="text-gray-300" />
                    </div>
                  )}
                  <span className="absolute inset-0 bg-gradient-to-t from-black/55 via-black/0 to-black/0" />
                  <span className="absolute right-3 top-3 rounded-full bg-black/60 px-2 py-1 text-xs font-bold text-white">
                    {post.images.length}
                  </span>
                  <div className="absolute bottom-3 left-3 right-3 text-white">
                    <p className="line-clamp-1 text-sm font-bold">{post.title}</p>
                    {post.marker?.placeName && (
                      <p className="mt-1 line-clamp-1 text-xs text-white/85">{post.marker.placeName}</p>
                    )}
                  </div>
                </div>
                <div className="p-3">
                  <p className="line-clamp-2 min-h-10 text-sm text-gray-600">{post.content || '메모가 없습니다.'}</p>
                  <div className="mt-3 flex flex-wrap gap-3 text-xs text-gray-400">
                    {post.date && <span>{formatDate(post.date)}</span>}
                    {post.time && <span>{post.time}</span>}
                  </div>
                </div>
              </button>
            );
          })}
        </div>
      ) : (
        <div className="grid grid-cols-3 gap-1 sm:gap-2 md:grid-cols-4 xl:grid-cols-5">
          {photos.map((photo) => (
            <button
              key={`${photo.post.id}-${photo.id}`}
              type="button"
              onClick={() => openPhoto(photo)}
              className="group relative aspect-square overflow-hidden bg-gray-100"
              title={photo.post.title}
            >
              <img
                src={photo.thumbnailUrl || photo.url}
                alt=""
                className="h-full w-full object-cover transition-transform duration-200 group-hover:scale-105"
              />
              <span className="absolute inset-0 bg-black/0 transition-colors group-hover:bg-black/20" />
              <span className="absolute bottom-2 left-2 right-2 line-clamp-1 text-left text-[11px] font-semibold text-white opacity-0 drop-shadow transition-opacity group-hover:opacity-100">
                {photo.post.title}
              </span>
            </button>
          ))}
        </div>
      )}

      {selectedPost && selectedImage && (
        <div className="fixed inset-0 z-[70] flex items-center justify-center bg-black/70 p-2 sm:p-4">
          {modalPosts.length > 1 && (
            <>
              <button
                type="button"
                onClick={() => moveSelectedPost(-1)}
                className="absolute left-2 top-1/2 z-20 hidden h-11 w-11 -translate-y-1/2 items-center justify-center rounded-full bg-white/90 text-gray-800 shadow-lg hover:bg-white sm:flex"
                aria-label="이전 Post"
                title="이전 Post"
              >
                <ChevronLeft size={24} />
              </button>
              <button
                type="button"
                onClick={() => moveSelectedPost(1)}
                className="absolute right-2 top-1/2 z-20 hidden h-11 w-11 -translate-y-1/2 items-center justify-center rounded-full bg-white/90 text-gray-800 shadow-lg hover:bg-white sm:flex"
                aria-label="다음 Post"
                title="다음 Post"
              >
                <ChevronRight size={24} />
              </button>
            </>
          )}
          <div className="relative flex max-h-[calc(100dvh_-_24px)] w-full max-w-6xl flex-col overflow-hidden rounded-xl bg-white shadow-2xl lg:max-h-[88vh] lg:flex-row">
            <button
              type="button"
              onClick={() => setSelectedPostId(null)}
              title="닫기"
              className="absolute right-3 top-3 z-10 flex h-9 w-9 items-center justify-center rounded-full bg-black/50 text-white hover:bg-black/70"
            >
              <X size={18} />
            </button>

            <div
              className="relative flex min-h-[280px] flex-1 touch-pan-y items-center justify-center bg-black sm:min-h-[360px] lg:min-h-[640px]"
              onTouchStart={handleModalTouchStart}
              onTouchEnd={handleModalTouchEnd}
              onPointerDown={handleModalPointerDown}
              onPointerUp={handleModalPointerUp}
            >
              {selectedPost.images.length > 1 && (
                <span className="absolute bottom-3 left-1/2 z-10 -translate-x-1/2 rounded-full bg-black/55 px-2.5 py-1 text-xs font-semibold text-white">
                  {(selectedImageIndex >= 0 ? selectedImageIndex : 0) + 1} / {selectedPost.images.length}
                </span>
              )}
              <img src={selectedImage.url} alt="" className="max-h-[62dvh] w-full object-contain lg:max-h-[88vh]" />
            </div>

            <aside className="max-h-[38dvh] w-full flex-shrink-0 overflow-y-auto border-t border-gray-100 p-4 sm:p-5 lg:max-h-none lg:w-96 lg:border-l lg:border-t-0">
              <p className="line-clamp-2 text-base font-bold text-gray-900">{selectedPost.title}</p>
              <div className="mt-4 space-y-3 text-sm text-gray-500">
                {selectedPost.marker?.placeName && (
                  <p className="flex items-center gap-2">
                    <MapPin size={15} />
                    <span>{selectedPost.marker.placeName}</span>
                  </p>
                )}
                {selectedPost.date && (
                  <p className="flex items-center gap-2">
                    <CalendarDays size={15} />
                    <span>{formatDate(selectedPost.date)}</span>
                  </p>
                )}
                {selectedPost.time && (
                  <p className="flex items-center gap-2">
                    <Clock3 size={15} />
                    <span>{selectedPost.time}</span>
                  </p>
                )}
              </div>
              <p className="mt-5 text-sm leading-6 text-gray-600">{selectedPost.content || '메모가 없습니다.'}</p>

              <div className="mt-5 grid grid-cols-4 gap-1.5">
                {selectedPost.images.map((image) => (
                  <button
                    key={image.id}
                    type="button"
                    onClick={() => setSelectedImageId(image.id)}
                    className={`aspect-square overflow-hidden rounded-md border ${
                      selectedImage.id === image.id ? 'border-emerald-500 ring-2 ring-emerald-500/20' : 'border-transparent'
                    }`}
                  >
                    <img src={image.thumbnailUrl || image.url} alt="" className="h-full w-full object-cover" />
                  </button>
                ))}
              </div>

              <Link
                href={`/trips/${selectedPost.tripId}`}
                className="mt-6 inline-flex rounded-lg bg-emerald-600 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-700"
              >
                Trip 보기
              </Link>
            </aside>
          </div>
        </div>
      )}
    </div>
  );
}
