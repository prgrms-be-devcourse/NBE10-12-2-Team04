'use client';

import { useEffect, useMemo, useRef, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { GoogleMap, Marker, useJsApiLoader } from '@react-google-maps/api';
import {
  Camera,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  X,
  Clock3,
  Heart,
  Maximize2,
  Minimize2,
  MapPin,
  Plus,
} from 'lucide-react';
import { feedApi, isAuthenticated, likeApi } from '@/lib/api';
import type { Trip } from '@/types';

const googleMapsApiKey = process.env.NEXT_PUBLIC_GOOGLE_MAPS_API_KEY ?? '';
const GOOGLE_MAPS_SCRIPT_ID = 'triptrace-google-map-script';
const feedMapContainerStyle = { width: '100%', height: '100%' };
const feedMapOptions = {
  disableDefaultUI: true,
  zoomControl: true,
  clickableIcons: false,
  gestureHandling: 'greedy',
};
const FEED_CLUSTER_ZOOM_THRESHOLD = 8;

const fallbackPlaces = [
  { city: '다낭', country: '베트남', tone: 'from-cyan-400 via-sky-500 to-emerald-600' },
  { city: '파리', country: '프랑스', tone: 'from-slate-400 via-sky-500 to-blue-700' },
  { city: '산토리니', country: '그리스', tone: 'from-blue-400 via-sky-500 to-white' },
  { city: '뉴욕', country: '미국', tone: 'from-slate-600 via-gray-700 to-zinc-900' },
];

const fallbackCoords: Record<string, { lat: number; lng: number }> = {
  다낭: { lat: 16.0544, lng: 108.2022 },
  파리: { lat: 48.8566, lng: 2.3522 },
  산토리니: { lat: 36.3932, lng: 25.4615 },
  뉴욕: { lat: 40.7128, lng: -74.006 },
  부산: { lat: 35.1796, lng: 129.0756 },
  교토: { lat: 35.0116, lng: 135.7681 },
};

function uniqueTrips(trips: Trip[]) {
  return Array.from(new Map(trips.map((trip) => [trip.id, trip])).values());
}

function getTripPosition(trip: Partial<Trip>, index: number) {
  const fallback = fallbackCoords[trip.city ?? ''] ?? fallbackCoords[fallbackPlaces[index % fallbackPlaces.length].city];
  const lat = trip.representativeLat ?? fallback.lat;
  const lng = trip.representativeLng ?? fallback.lng;

  return {
    top: Math.min(82, Math.max(18, ((90 - lat) / 180) * 100)),
    left: Math.min(92, Math.max(8, ((lng + 180) / 360) * 100)),
  };
}

function getTripLatLng(trip: Partial<Trip>, index: number) {
  const fallback = fallbackCoords[trip.city ?? ''] ?? fallbackCoords[fallbackPlaces[index % fallbackPlaces.length].city];

  return {
    lat: trip.representativeLat ?? fallback.lat,
    lng: trip.representativeLng ?? fallback.lng,
  };
}

function getFeedClusterCellSize(zoom: number) {
  if (zoom < 4) return 90;
  if (zoom < 6) return 35;
  if (zoom < FEED_CLUSTER_ZOOM_THRESHOLD) return 12;
  return 1;
}

function TripVisual({ trip, index, className = '' }: { trip?: Partial<Trip>; index: number; className?: string }) {
  const tone = fallbackPlaces[index % fallbackPlaces.length].tone;

  return (
    <div className={`relative overflow-hidden bg-gradient-to-br ${tone} ${className}`}>
      {trip?.thumbnailUrl && (
        <img src={trip.thumbnailUrl} alt="" className="absolute inset-0 h-full w-full object-cover" />
      )}
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_20%_20%,rgba(255,255,255,0.45),transparent_24%),linear-gradient(to_top,rgba(0,0,0,0.55),transparent_55%)]" />
      <div className="absolute bottom-0 left-0 right-0 h-12 bg-black/20" />
      {trip && (
        <div className="absolute bottom-3 left-3 right-3 text-white">
          <p className="text-sm font-bold truncate">{trip.city}, {trip.country}</p>
          <p className="mt-1 flex items-center gap-1 text-xs text-white/90">
            <Heart size={12} fill="white" /> {trip.likeCount ?? 0}
          </p>
        </div>
      )}
    </div>
  );
}

function DecorativeMapBand({ trips }: { trips: Trip[] }) {
  const [zoomedCluster, setZoomedCluster] = useState<string | null>(null);
  const [selectedTrip, setSelectedTrip] = useState<Trip | null>(null);
  const [expanded, setExpanded] = useState(false);
  const [panX, setPanX] = useState(0);
  const [dragStart, setDragStart] = useState<{ x: number; panX: number } | null>(null);
  const source = useMemo(() => trips.length ? uniqueTrips(trips) : fallbackPlaces.map((place, index) => ({
    id: `map-fallback-${index}`,
    title: `${place.city} 여행기`,
    city: place.city,
    country: place.country,
    startDate: '',
    endDate: '',
    isPublic: true,
    thumbnailUrl: '',
    likeCount: [12345, 9876, 8765, 7654][index],
    recordCount: 0,
    author: { id: '', nickname: 'Traveler' },
    createdAt: '',
  } as Trip)), [trips]);
  const clusters = useMemo(() => {
    const grouped = new Map<string, Trip[]>();
    source.forEach((trip) => {
      const key = `${trip.country}-${trip.city}`;
      grouped.set(key, [...(grouped.get(key) ?? []), trip]);
    });
    return Array.from(grouped.entries()).map(([key, items], index) => {
      const position = getTripPosition(items[0], index);
      return {
        key,
        label: items[0].city || items[0].country || '여행지',
        trips: items,
        ...position,
      };
    });
  }, [source]);
  const activeCluster = clusters.find((cluster) => cluster.key === zoomedCluster);
  const visibleTrips = activeCluster?.trips ?? [];

  const selectCluster = (key: string) => {
    setZoomedCluster(key);
    setSelectedTrip(null);
  };

  const resetMap = () => {
    setZoomedCluster(null);
    setSelectedTrip(null);
  };

  const startDrag = (event: React.MouseEvent<HTMLDivElement>) => {
    if ((event.target as HTMLElement).closest('button, a')) return;
    setDragStart({ x: event.clientX, panX });
  };

  const moveDrag = (event: React.MouseEvent<HTMLDivElement>) => {
    if (!dragStart) return;
    const next = dragStart.panX + event.clientX - dragStart.x;
    setPanX(Math.max(-220, Math.min(220, next)));
  };

  const endDrag = () => {
    setDragStart(null);
  };

  return (
    <div
      className={`relative overflow-hidden bg-[#dcefdc] transition-[height] duration-300 ${expanded ? 'h-[420px]' : 'h-[236px]'} ${dragStart ? 'cursor-grabbing' : 'cursor-grab'}`}
      onMouseDown={startDrag}
      onMouseMove={moveDrag}
      onMouseUp={endDrag}
      onMouseLeave={endDrag}
    >
      <div
        className="absolute inset-y-0 -left-[15%] w-[130%]"
        style={{ transform: `translateX(${panX}px)` }}
      >
        <div className="absolute inset-0 opacity-80 bg-[linear-gradient(25deg,transparent_0_22%,rgba(255,255,255,0.55)_22%_24%,transparent_24%_100%),linear-gradient(145deg,transparent_0_35%,rgba(255,255,255,0.6)_35%_37%,transparent_37%_100%),radial-gradient(circle_at_18%_75%,#b7d7ee_0_18%,transparent_18%),radial-gradient(circle_at_92%_35%,#b7d7ee_0_22%,transparent_22%)]" />
        <div className="absolute left-[18%] top-[14%] h-40 w-40 rounded-full bg-sky-200/50 blur-sm" />
        <div className="absolute right-[8%] top-[6%] h-48 w-48 rounded-full bg-sky-200/60 blur-sm" />
        <div className="absolute left-[38%] top-0 h-full w-3 rotate-45 bg-white/35" />
        <div className="absolute left-[20%] top-0 h-full w-3 -rotate-[62deg] bg-white/35" />
        {!zoomedCluster && clusters.map((cluster, i) => (
          <button
            type="button"
            key={cluster.key}
            onClick={() => selectCluster(cluster.key)}
            className="absolute -translate-x-1/2 flex items-center gap-2 rounded-full bg-white/95 px-2 py-1 text-left shadow-md ring-1 ring-black/5 transition-transform hover:scale-105"
            style={{ top: `${cluster.top}%`, left: `${cluster.left}%` }}
          >
            <TripVisual trip={cluster.trips[0]} index={i} className="h-10 w-10 rounded-full border-2 border-white shadow" />
            <div className="pr-1">
              <p className="text-xs font-bold text-gray-800 leading-none">{cluster.label}</p>
              <p className="mt-1 text-[11px] font-bold text-emerald-600">{cluster.trips.length}</p>
            </div>
            <MapPin size={14} className="absolute -top-3 left-1/2 -translate-x-1/2 fill-emerald-500 text-emerald-500" />
          </button>
        ))}
        {zoomedCluster && visibleTrips.map((trip, index) => {
          const base = activeCluster ? { top: activeCluster.top, left: activeCluster.left } : getTripPosition(trip, index);
          const angle = (Math.PI * 2 * index) / Math.max(1, visibleTrips.length);
          const radius = visibleTrips.length === 1 ? 0 : 16;
          const top = Math.min(84, Math.max(16, base.top + Math.sin(angle) * radius));
          const left = Math.min(92, Math.max(8, base.left + Math.cos(angle) * radius));

          return (
            <button
              type="button"
              key={trip.id}
              onClick={() => setSelectedTrip(trip)}
              className="absolute -translate-x-1/2 -translate-y-1/2 rounded-full transition-transform hover:scale-110"
              style={{ top: `${top}%`, left: `${left}%` }}
            >
              <TripVisual trip={trip} index={index} className={`h-12 w-12 rounded-full border-[3px] shadow-lg ${selectedTrip?.id === trip.id ? 'border-emerald-500' : 'border-white'}`} />
            </button>
          );
        })}
      </div>
      <div className="absolute left-6 top-5 z-20 rounded-full bg-white/90 px-3 py-1.5 text-xs font-semibold text-gray-500 shadow">
        마우스로 지도를 좌우로 이동
      </div>
      <button
        type="button"
        onClick={() => setExpanded((value) => !value)}
        className="absolute right-6 top-5 z-20 flex items-center gap-1 rounded-full bg-white/95 px-3 py-1.5 text-xs font-semibold text-gray-600 shadow hover:bg-white"
      >
        {expanded ? <Minimize2 size={13} /> : <Maximize2 size={13} />}
        {expanded ? '지도 접기' : '지도 펼치기'}
      </button>
      {zoomedCluster && (
        <button
          type="button"
          onClick={resetMap}
          className="absolute right-32 top-5 z-20 rounded-full bg-white/95 px-3 py-1.5 text-xs font-semibold text-gray-600 shadow hover:bg-white"
        >
          전체 보기
        </button>
      )}
      {selectedTrip && (
        <div className="absolute bottom-5 left-1/2 z-30 flex w-[320px] -translate-x-1/2 gap-3 rounded-xl bg-white p-3 shadow-xl ring-1 ring-black/5">
          <TripVisual trip={selectedTrip} index={0} className="h-20 w-24 flex-shrink-0 rounded-lg" />
          <div className="min-w-0 flex-1">
            <div className="flex items-start justify-between gap-2">
              <p className="line-clamp-1 text-sm font-bold text-gray-900">{selectedTrip.title}</p>
              <button type="button" onClick={() => setSelectedTrip(null)} className="text-gray-300 hover:text-gray-500">
                <X size={14} />
              </button>
            </div>
            <p className="mt-1 text-xs text-gray-400">{selectedTrip.city}, {selectedTrip.country}</p>
            <p className="mt-2 flex items-center gap-1 text-xs font-semibold text-red-500">
              <Heart size={12} className="fill-red-500" /> {selectedTrip.likeCount ?? 0}
            </p>
            {!selectedTrip.id.startsWith('map-fallback') && (
              <Link href={`/trips/${selectedTrip.id}`} className="mt-2 inline-flex text-xs font-bold text-emerald-600 hover:text-emerald-700">
                상세보기
              </Link>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function MapBand({ trips }: { trips: Trip[] }) {
  const [zoomedCluster, setZoomedCluster] = useState<string | null>(null);
  const [focusedTrips, setFocusedTrips] = useState<Trip[] | null>(null);
  const [selectedTrip, setSelectedTrip] = useState<Trip | null>(null);
  const [expanded, setExpanded] = useState(false);
  const [mapZoom, setMapZoom] = useState(5);
  const mapRef = useRef<google.maps.Map | null>(null);
  const source = useMemo(() => trips.length ? uniqueTrips(trips) : fallbackPlaces.map((place, index) => ({
    id: `map-fallback-${index}`,
    title: `${place.city} 여행기`,
    city: place.city,
    country: place.country,
    startDate: '',
    endDate: '',
    isPublic: true,
    thumbnailUrl: '',
    likeCount: [12345, 9876, 8765, 7654][index],
    recordCount: 0,
    author: { id: '', nickname: 'Traveler' },
    createdAt: '',
  } as Trip)), [trips]);
  const clusters = useMemo(() => {
    const cellSize = getFeedClusterCellSize(mapZoom);
    const grouped = new Map<string, Array<{ trip: Trip; position: { lat: number; lng: number } }>>();

    source.forEach((trip, index) => {
      const position = getTripLatLng(trip, index);
      const cellLat = Math.floor(position.lat / cellSize);
      const cellLng = Math.floor(position.lng / cellSize);
      const key = `${cellLat}:${cellLng}`;
      grouped.set(key, [...(grouped.get(key) ?? []), { trip, position }]);
    });

    return Array.from(grouped.entries()).map(([key, items]) => {
      const lat = items.reduce((sum, item) => sum + item.position.lat, 0) / items.length;
      const lng = items.reduce((sum, item) => sum + item.position.lng, 0) / items.length;
      const firstTrip = items[0].trip;

      return {
        key,
        label: items.length > 1
          ? `${firstTrip.city || firstTrip.country || '여행지'} 외 ${items.length - 1}`
          : firstTrip.city || firstTrip.country || '여행지',
        trips: items.map((item) => item.trip),
        position: { lat, lng },
      };
    });
  }, [mapZoom, source]);
  const showClusters = mapZoom < FEED_CLUSTER_ZOOM_THRESHOLD;
  const visibleTrips = showClusters
    ? []
    : focusedTrips ?? source;
  const { isLoaded, loadError } = useJsApiLoader({
    googleMapsApiKey,
    id: GOOGLE_MAPS_SCRIPT_ID,
  });

  useEffect(() => {
    if (!isLoaded || !mapRef.current || source.length === 0 || zoomedCluster) return;
    const bounds = new google.maps.LatLngBounds();
    source.forEach((trip, index) => bounds.extend(getTripLatLng(trip, index)));
    if (source.length === 1) {
      mapRef.current.setCenter(getTripLatLng(source[0], 0));
      mapRef.current.setZoom(8);
      return;
    }
    mapRef.current.fitBounds(bounds, 48);
  }, [isLoaded, source, zoomedCluster]);

  const selectCluster = (clusterKey: string) => {
    const cluster = clusters.find((item) => item.key === clusterKey);
    setZoomedCluster(clusterKey);
    setFocusedTrips(cluster?.trips ?? null);
    setSelectedTrip(null);
    if (cluster && mapRef.current) {
      mapRef.current.panTo(cluster.position);
      mapRef.current.setZoom(FEED_CLUSTER_ZOOM_THRESHOLD + 1);
    }
  };

  const resetMap = () => {
    setZoomedCluster(null);
    setFocusedTrips(null);
    setSelectedTrip(null);
    mapRef.current?.setZoom(5);
  };

  if (!googleMapsApiKey || loadError) {
    return <DecorativeMapBand trips={trips} />;
  }

  return (
    <div className={`relative overflow-hidden bg-[#dcefdc] transition-[height] duration-300 ${expanded ? 'h-[420px]' : 'h-[236px]'}`}>
      {!isLoaded ? (
        <div className="flex h-full w-full items-center justify-center bg-gradient-to-br from-blue-100 via-sky-50 to-green-50">
          <p className="text-xs text-gray-400">지도를 불러오는 중...</p>
        </div>
      ) : (
        <GoogleMap
          mapContainerStyle={feedMapContainerStyle}
          center={clusters[0]?.position ?? { lat: 37.5665, lng: 126.978 }}
          zoom={5}
          options={feedMapOptions}
          onZoomChanged={() => {
            const nextZoom = mapRef.current?.getZoom();
            if (typeof nextZoom === 'number') {
              setMapZoom(nextZoom);
              if (nextZoom < FEED_CLUSTER_ZOOM_THRESHOLD) {
                setZoomedCluster(null);
                setFocusedTrips(null);
                setSelectedTrip(null);
              }
            }
          }}
          onLoad={(map) => {
            mapRef.current = map;
            setMapZoom(map.getZoom() ?? 5);
          }}
          onUnmount={() => {
            mapRef.current = null;
          }}
        >
          {showClusters && clusters.map((cluster) => (
            <Marker
              key={cluster.key}
              position={cluster.position}
              onClick={() => selectCluster(cluster.key)}
              label={{
                text: String(cluster.trips.length),
                color: '#ffffff',
                fontSize: '12px',
                fontWeight: '700',
              }}
              title={`${cluster.label} ${cluster.trips.length}개`}
            />
          ))}
          {!showClusters && visibleTrips.map((trip, index) => {
            const base = getTripLatLng(trip, index);
            const offset = visibleTrips.length === 1 ? 0 : (index - (visibleTrips.length - 1) / 2) * 0.004;

            return (
              <Marker
                key={trip.id}
                position={{ lat: base.lat + offset, lng: base.lng + offset }}
                onClick={() => setSelectedTrip(trip)}
                title={trip.title}
              />
            );
          })}
        </GoogleMap>
      )}
      <button
        type="button"
        onClick={() => setExpanded((value) => !value)}
        className="absolute right-6 top-5 z-20 flex items-center gap-1 rounded-full bg-white/95 px-3 py-1.5 text-xs font-semibold text-gray-600 shadow hover:bg-white"
      >
        {expanded ? <Minimize2 size={13} /> : <Maximize2 size={13} />}
        {expanded ? '지도 접기' : '지도 펼치기'}
      </button>
      {!showClusters && focusedTrips && (
        <button
          type="button"
          onClick={resetMap}
          className="absolute right-32 top-5 z-20 rounded-full bg-white/95 px-3 py-1.5 text-xs font-semibold text-gray-600 shadow hover:bg-white"
        >
          전체 보기
        </button>
      )}
      {selectedTrip && (
        <div className="absolute bottom-5 left-1/2 z-30 flex w-[320px] -translate-x-1/2 gap-3 rounded-xl bg-white p-3 shadow-xl ring-1 ring-black/5">
          <TripVisual trip={selectedTrip} index={0} className="h-20 w-24 flex-shrink-0 rounded-lg" />
          <div className="min-w-0 flex-1">
            <div className="flex items-start justify-between gap-2">
              <p className="line-clamp-1 text-sm font-bold text-gray-900">{selectedTrip.title}</p>
              <button type="button" onClick={() => setSelectedTrip(null)} className="text-gray-300 hover:text-gray-500">
                <X size={14} />
              </button>
            </div>
            <p className="mt-1 text-xs text-gray-400">{selectedTrip.city}, {selectedTrip.country}</p>
            <p className="mt-2 flex items-center gap-1 text-xs font-semibold text-red-500">
              <Heart size={12} className="fill-red-500" /> {selectedTrip.likeCount ?? 0}
            </p>
            {!selectedTrip.id.startsWith('map-fallback') && (
              <Link href={`/trips/${selectedTrip.id}`} className="mt-2 inline-flex text-xs font-bold text-emerald-600 hover:text-emerald-700">
                상세보기
              </Link>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function TopLikedCarousel({ trips }: { trips: Trip[] }) {
  const scrollRef = useRef<HTMLDivElement>(null);
  const items = useMemo(() => trips.length ? trips.slice(0, 10) : fallbackPlaces.map((p, i) => ({
    id: `fallback-${i}`,
    title: `${p.city} 여행기`,
    city: p.city,
    country: p.country,
    startDate: '',
    endDate: '',
    isPublic: true,
    likeCount: [12345, 9876, 8765, 7654][i],
    recordCount: 0,
    author: { id: '', nickname: 'Traveler' },
    createdAt: '',
  } as Trip)), [trips]);

  const scroll = (direction: -1 | 1) => {
    scrollRef.current?.scrollBy({ left: direction * 300, behavior: 'smooth' });
  };

  return (
    <section className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
      <div className="mb-5 flex items-center justify-between">
        <h2 className="flex items-center gap-2 text-lg font-bold text-gray-900">
          <Heart size={20} className="fill-emerald-500 text-emerald-500" /> 좋아요 Top 10
          <Camera size={18} className="text-emerald-500" />
        </h2>
        <div className="flex gap-2">
          <button onClick={() => scroll(-1)} className="grid h-8 w-8 place-items-center rounded-full border border-gray-200 text-gray-500 hover:bg-gray-50">
            <ChevronLeft size={16} />
          </button>
          <button onClick={() => scroll(1)} className="grid h-8 w-8 place-items-center rounded-full border border-gray-200 text-gray-500 hover:bg-gray-50">
            <ChevronRight size={16} />
          </button>
        </div>
      </div>
      <div ref={scrollRef} className="flex gap-5 overflow-x-auto pb-1 [scrollbar-width:none]">
        {items.map((trip, index) => (
          <Link key={trip.id} href={trip.id.startsWith('fallback') ? '#' : `/trips/${trip.id}`} className="group relative h-[168px] w-[250px] shrink-0 overflow-hidden rounded-lg shadow-sm">
            <TripVisual trip={trip} index={index} className="h-full w-full transition-transform group-hover:scale-105" />
            <span className="absolute left-3 top-3 rounded-md bg-white/95 px-2 py-1 text-sm font-bold text-gray-800">{index + 1}위</span>
          </Link>
        ))}
      </div>
    </section>
  );
}

function RecentTripsList({ trips, onLike }: { trips: Trip[]; onLike: (trip: Trip) => void }) {
  const [expanded, setExpanded] = useState(false);
  const fallback = fallbackPlaces.map((p, i) => ({
    id: `recent-${i}`,
    title: `${p.city}, ${p.country} 여행 일정 공유`,
    city: p.city,
    country: p.country,
    startDate: `2024.05.${String(20 + i).padStart(2, '0')}`,
    endDate: '',
    isPublic: true,
    likeCount: [123, 98, 87, 76][i],
    recordCount: 0,
    author: { id: '', nickname: 'Traveler_shb' },
    createdAt: '',
  } as Trip));
  const source = trips.length ? trips : fallback;
  const visible = expanded ? source : source.slice(0, 4);

  return (
    <section className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
      <h2 className="mb-4 flex items-center gap-2 text-lg font-bold text-gray-900">
        <Clock3 size={21} className="text-emerald-500" /> 최신 여행기
      </h2>
      <div className="divide-y divide-gray-100">
        {visible.map((trip, index) => (
          <div key={trip.id} className="flex items-center gap-5 py-4 first:pt-0">
            <Link href={trip.id.startsWith('recent') ? '#' : `/trips/${trip.id}`} className="block h-[92px] w-[220px] shrink-0 overflow-hidden rounded-lg">
              <TripVisual index={index} className="h-full w-full" />
            </Link>
            <div className="min-w-0 flex-1">
              <Link href={trip.id.startsWith('recent') ? '#' : `/trips/${trip.id}`} className="line-clamp-1 text-base font-bold text-gray-900 hover:text-emerald-600">
                {trip.title}
              </Link>
              <p className="mt-1 line-clamp-1 text-sm text-gray-500">{trip.city}의 숨겨진 명소와 맛집, 일정까지 자세히 정리했습니다.</p>
              <div className="mt-3 flex items-center gap-3 text-xs text-gray-400">
                <span className="grid h-6 w-6 place-items-center rounded-full bg-emerald-500 text-white">T</span>
                <span className="font-medium text-gray-600">{trip.author?.nickname ?? 'Traveler'}</span>
                <span>|</span>
                <span>{trip.startDate}</span>
              </div>
            </div>
            <button onClick={() => onLike(trip)} className="flex items-center gap-2 px-3 text-gray-500 hover:text-red-500">
              <Heart size={22} className={trip.liked ? 'fill-red-500 text-red-500' : ''} />
              <span className="text-sm font-medium">{trip.likeCount ?? 0}</span>
            </button>
          </div>
        ))}
      </div>
      {source.length > 4 && (
        <button onClick={() => setExpanded((v) => !v)} className="mx-auto mt-5 flex items-center gap-1 rounded-full border border-gray-200 px-8 py-2 text-sm font-semibold text-gray-600 hover:bg-gray-50">
          {expanded ? '접기' : '더보기'} <ChevronDown size={15} className={expanded ? 'rotate-180' : ''} />
        </button>
      )}
    </section>
  );
}

export default function HomePage() {
  const router = useRouter();
  const [topLiked, setTopLiked] = useState<Trip[]>([]);
  const [recent, setRecent] = useState<Trip[]>([]);

  useEffect(() => {
    const attachLikedStatus = async (trips: Trip[]) => {
      if (!isAuthenticated()) return trips.map((trip) => ({ ...trip, liked: false }));

      return Promise.all(
        trips.map(async (trip) => {
          if (trip.id.startsWith('fallback') || trip.id.startsWith('recent')) return trip;

          try {
            const status = await likeApi.getMine(trip.id);
            return { ...trip, liked: status.liked };
          } catch {
            return { ...trip, liked: false };
          }
        }),
      );
    };

    async function loadFeeds() {
      const [topLikedTrips, recentTrips] = await Promise.all([
        feedApi.getTopLiked(),
        feedApi.getRecent(),
      ]);

      setTopLiked(await attachLikedStatus(topLikedTrips as Trip[]));
      setRecent(await attachLikedStatus(recentTrips as Trip[]));
    }

    loadFeeds().catch(() => {});
  }, []);

  const handleLike = async (trip: Trip) => {
    if (trip.id.startsWith('recent')) return;
    if (!isAuthenticated()) {
      router.push('/auth/login');
      return;
    }

    const nextLiked = !trip.liked;
    const delta = nextLiked ? 1 : -1;
    const applyLikeState = (items: Trip[]) =>
      items.map((item) =>
        item.id === trip.id
          ? { ...item, liked: nextLiked, likeCount: Math.max(0, (item.likeCount ?? 0) + delta) }
          : item,
      );

    try {
      if (trip.liked) await likeApi.unlike(trip.id);
      else await likeApi.like(trip.id);

      setRecent(applyLikeState);
      setTopLiked(applyLikeState);
    } catch {
      try {
        const status = await likeApi.getMine(trip.id);
        const syncLikeState = (items: Trip[]) =>
          items.map((item) => (item.id === trip.id ? { ...item, liked: status.liked } : item));
        setRecent(syncLikeState);
        setTopLiked(syncLikeState);
      } catch {
        // 좋아요 상태 동기화 실패 시 현재 화면 상태를 유지합니다.
      }
    }
  };

  const handleCreateTrip = () => {
    router.push(isAuthenticated() ? '/trips?create=1' : '/auth/login');
  };

  return (
    <div className="relative min-h-[calc(100vh-64px)] bg-gray-50">
      <MapBand trips={uniqueTrips([...recent, ...topLiked])} />
      <div className="mx-auto mt-6 flex max-w-[1180px] flex-col gap-5 px-8 pb-10">
        <div className="flex justify-end">
          <button onClick={handleCreateTrip} className="flex items-center gap-2 rounded-lg bg-emerald-600 px-4 py-2.5 text-sm font-bold text-white shadow-sm hover:bg-emerald-700">
            <Plus size={18} /> 트립생성
          </button>
        </div>
        <TopLikedCarousel trips={topLiked} />
        <RecentTripsList trips={recent} onLike={handleLike} />
      </div>
    </div>
  );
}
