'use client';

import { useCallback, useEffect, useMemo, useRef, useState, type CSSProperties, type RefObject } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { GoogleMap, OverlayView, useJsApiLoader } from '@react-google-maps/api';
import {
  Camera,
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
  disableDefaultUI: false,
  zoomControl: true,
  scrollwheel: true,
  mapTypeControl: false,
  streetViewControl: false,
  fullscreenControl: false,
  clickableIcons: false,
  gestureHandling: 'greedy',
  minZoom: 2,
  maxZoom: 13,
};

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
  서울: { lat: 37.5665, lng: 126.978 },
  도쿄: { lat: 35.6762, lng: 139.6503 },
  오사카: { lat: 34.6937, lng: 135.5023 },
  후쿠오카: { lat: 33.5902, lng: 130.4017 },
  런던: { lat: 51.5072, lng: -0.1276 },
  로마: { lat: 41.9028, lng: 12.4964 },
  방콕: { lat: 13.7563, lng: 100.5018 },
  한국: { lat: 36.5, lng: 127.8 },
  일본: { lat: 36.2048, lng: 138.2529 },
  프랑스: { lat: 46.2276, lng: 2.2137 },
  미국: { lat: 39.8283, lng: -98.5795 },
  그리스: { lat: 39.0742, lng: 21.8243 },
  베트남: { lat: 14.0583, lng: 108.2772 },
};

function uniqueTrips(trips: Trip[]) {
  return Array.from(new Map(trips.map((trip) => [trip.id, trip])).values());
}

function isCurrentMonthTrip(trip: Trip) {
  const now = new Date();
  const monthStart = new Date(now.getFullYear(), now.getMonth(), 1);
  const monthEnd = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59, 999);
  const start = new Date(trip.startDate);
  const end = new Date(trip.endDate || trip.startDate);

  if (Number.isNaN(start.getTime())) return false;
  if (Number.isNaN(end.getTime())) return start >= monthStart && start <= monthEnd;

  return start <= monthEnd && end >= monthStart;
}

function getTripPosition(trip: Partial<Trip>, index: number) {
  const fallback = getFallbackLatLng(trip, index);
  const lat = trip.representativeLat ?? fallback.lat;
  const lng = trip.representativeLng ?? fallback.lng;

  return {
    top: Math.min(82, Math.max(18, ((90 - lat) / 180) * 100)),
    left: Math.min(92, Math.max(8, ((lng + 180) / 360) * 100)),
  };
}

function getTripLatLng(trip: Partial<Trip>, index: number) {
  const fallback = getFallbackLatLng(trip, index);

  return {
    lat: trip.representativeLat ?? fallback.lat,
    lng: trip.representativeLng ?? fallback.lng,
  };
}

function getFeedMarkerPositions(trips: Trip[]) {
  const grouped = new Map<string, Array<{ trip: Trip; base: { lat: number; lng: number } }>>();

  trips.forEach((trip, index) => {
    const base = getTripLatLng(trip, index);
    const key = `${base.lat.toFixed(4)}:${base.lng.toFixed(4)}`;
    grouped.set(key, [...(grouped.get(key) ?? []), { trip, base }]);
  });

  const positions = new Map<string, { lat: number; lng: number }>();
  grouped.forEach((items) => {
    items.forEach(({ trip, base }, index) => {
      if (items.length === 1) {
        positions.set(trip.id, base);
        return;
      }

      const angle = (Math.PI * 2 * index) / items.length;
      const radius = 0.018 + Math.floor(index / 8) * 0.01;
      positions.set(trip.id, {
        lat: base.lat + Math.sin(angle) * radius,
        lng: base.lng + Math.cos(angle) * radius,
      });
    });
  });

  return positions;
}

function getFallbackLatLng(trip: Partial<Trip>, index: number) {
  const direct = fallbackCoords[trip.city ?? ''] ?? fallbackCoords[trip.country ?? ''];
  if (direct) return direct;

  const seed = `${trip.country ?? ''}${trip.city ?? ''}${trip.title ?? ''}` || String(index);
  const hash = Array.from(seed).reduce((sum, char) => sum + char.charCodeAt(0), 0);
  const base = fallbackCoords[fallbackPlaces[index % fallbackPlaces.length].city];

  return {
    lat: base.lat + ((hash % 120) - 60) / 100,
    lng: base.lng + (((hash / 7) % 120) - 60) / 100,
  };
}

function TripVisual({
  trip,
  index,
  className = '',
  showMeta = true,
}: {
  trip?: Partial<Trip>;
  index: number;
  className?: string;
  showMeta?: boolean;
}) {
  const tone = fallbackPlaces[index % fallbackPlaces.length].tone;

  return (
    <div className={`relative overflow-hidden bg-gradient-to-br ${tone} ${className}`}>
      {trip?.thumbnailUrl && (
        <img src={trip.thumbnailUrl} alt="" className="absolute inset-0 h-full w-full object-cover" />
      )}
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_20%_20%,rgba(255,255,255,0.45),transparent_24%),linear-gradient(to_top,rgba(0,0,0,0.55),transparent_55%)]" />
      <div className="absolute bottom-0 left-0 right-0 h-12 bg-black/20" />
      {trip && showMeta && (
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

function TripPhotoMapMarker({
  trip,
  index,
  position,
  selected,
  onClick,
}: {
  trip: Trip;
  index: number;
  position: { lat: number; lng: number };
  selected: boolean;
  onClick: () => void;
}) {
  const sizeClass = selected ? 'h-14 w-14' : 'h-12 w-12';

  return (
    <OverlayView position={position} mapPaneName={OverlayView.OVERLAY_MOUSE_TARGET}>
      <button
        type="button"
        onClick={onClick}
        className="group relative -translate-x-1/2 -translate-y-full pb-2 transition-transform hover:scale-105"
        title={trip.title}
      >
        <span className="absolute bottom-1 left-1/2 h-3 w-3 -translate-x-1/2 rotate-45 border-b border-r border-white bg-white shadow-md" />
        <span className={`relative z-10 block overflow-hidden rounded-full border-[3px] bg-white shadow-lg ${
          selected ? 'border-emerald-600 ring-4 ring-emerald-500/20' : 'border-white'
        } ${sizeClass}`}>
          <TripVisual trip={trip} index={index} showMeta={false} className="h-full w-full rounded-full" />
        </span>
      </button>
    </OverlayView>
  );
}

function TripMapPreviewCard({
  trip,
  index,
  style,
  onClose,
}: {
  trip: Trip;
  index: number;
  style: CSSProperties;
  onClose: () => void;
}) {
  return (
    <div
      className="pointer-events-auto absolute z-30 w-[260px] rounded-xl bg-white p-3 shadow-xl ring-1 ring-black/10"
      style={style}
    >
      <div className="flex gap-3">
        <TripVisual trip={trip} index={index} className="h-20 w-24 flex-shrink-0 rounded-lg" />
        <div className="min-w-0 flex-1">
          <div className="flex items-start justify-between gap-2">
            <p className="line-clamp-1 text-sm font-bold text-gray-900">{trip.title}</p>
            <button type="button" onClick={onClose} className="text-gray-300 hover:text-gray-500">
              <X size={14} />
            </button>
          </div>
          <p className="mt-1 text-xs text-gray-400">{trip.city}, {trip.country}</p>
          <p className="mt-2 flex items-center gap-1 text-xs font-semibold text-red-500">
            <Heart size={12} className="fill-red-500" /> {trip.likeCount ?? 0}
          </p>
          {!trip.id.startsWith('map-fallback') && (
            <Link href={`/trips/${trip.id}`} className="mt-2 inline-flex text-xs font-bold text-emerald-600 hover:text-emerald-700">
              상세보기
            </Link>
          )}
        </div>
      </div>
    </div>
  );
}

function DecorativeMapBand({ trips, fillHeight = false }: { trips: Trip[]; fillHeight?: boolean }) {
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
      className={`relative overflow-hidden bg-[#dcefdc] transition-[height] duration-300 ${fillHeight ? 'h-full' : expanded ? 'h-[420px]' : 'h-[236px]'} ${dragStart ? 'cursor-grabbing' : 'cursor-grab'}`}
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
      {!fillHeight && (
        <button
          type="button"
          onClick={() => setExpanded((value) => !value)}
          className="absolute right-6 top-5 z-20 flex items-center gap-1 rounded-full bg-white/95 px-3 py-1.5 text-xs font-semibold text-gray-600 shadow hover:bg-white"
        >
          {expanded ? <Minimize2 size={13} /> : <Maximize2 size={13} />}
          {expanded ? '지도 접기' : '지도 펼치기'}
        </button>
      )}
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

function MapBand({
  trips,
  fillHeight = false,
  bottomInset = 0,
}: {
  trips: Trip[];
  fillHeight?: boolean;
  bottomInset?: number;
}) {
  const [selectedTrip, setSelectedTrip] = useState<Trip | null>(null);
  const [selectedPreview, setSelectedPreview] = useState<{ index: number; style: CSSProperties } | null>(null);
  const [mapViewVersion, setMapViewVersion] = useState(0);
  const [expanded, setExpanded] = useState(false);
  const [mapZoom, setMapZoom] = useState(4);
  const mapRef = useRef<google.maps.Map | null>(null);
  const idleFitRef = useRef('');
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
  const markerPositions = useMemo(() => getFeedMarkerPositions(source), [source]);
  const sourceKey = useMemo(() => source.map((trip, index) => {
    const position = markerPositions.get(trip.id) ?? getTripLatLng(trip, index);
    return `${trip.id}:${position.lat}:${position.lng}`;
  }).join('|'), [markerPositions, source]);
  const { isLoaded, loadError } = useJsApiLoader({
    googleMapsApiKey,
    id: GOOGLE_MAPS_SCRIPT_ID,
  });

  const getVisibleMapPadding = useCallback((map: google.maps.Map) => {
    const mapHeight = map.getDiv().clientHeight || (typeof window === 'undefined' ? 720 : window.innerHeight);
    const topPadding = 96;
    const visibleHeight = fillHeight ? Math.max(160, mapHeight - bottomInset) : mapHeight;
    const maxBottomPadding = Math.max(96, mapHeight - topPadding - Math.max(120, visibleHeight * 0.45));
    const bottomPadding = fillHeight
      ? Math.max(120, Math.min(maxBottomPadding, bottomInset + topPadding))
      : 96;

    return {
      top: topPadding,
      right: 72,
      bottom: bottomPadding,
      left: 72,
    };
  }, [bottomInset, fillHeight]);

  const fitMapToPositions = useCallback((map: google.maps.Map, positions: Array<{ lat: number; lng: number }>) => {
    if (positions.length === 0) return;
    const bounds = new google.maps.LatLngBounds();
    positions.forEach((position) => bounds.extend(position));
    if (positions.length === 1) {
      const point = positions[0];
      bounds.extend({ lat: point.lat + 0.02, lng: point.lng + 0.02 });
      bounds.extend({ lat: point.lat - 0.02, lng: point.lng - 0.02 });
    }

    map.fitBounds(bounds, getVisibleMapPadding(map));
  }, [getVisibleMapPadding]);

  const fitMapToSource = useCallback((map: google.maps.Map) => {
    const positions = source.map((trip, index) => markerPositions.get(trip.id) ?? getTripLatLng(trip, index));
    fitMapToPositions(map, positions);
  }, [fitMapToPositions, markerPositions, source]);

  const getPreviewStyle = useCallback((position: { lat: number; lng: number }): CSSProperties | null => {
    const map = mapRef.current;
    const bounds = map?.getBounds();
    if (!map || !bounds) return null;

    const north = bounds.getNorthEast().lat();
    const south = bounds.getSouthWest().lat();
    let east = bounds.getNorthEast().lng();
    const west = bounds.getSouthWest().lng();
    if (east < west) east += 360;
    const normalizedLng = position.lng < west ? position.lng + 360 : position.lng;
    const latRange = Math.max(0.0001, north - south);
    const lngRange = Math.max(0.0001, east - west);
    const mapRect = map.getDiv().getBoundingClientRect();
    const markerX = ((normalizedLng - west) / lngRange) * mapRect.width;
    const markerY = ((north - position.lat) / latRange) * mapRect.height;
    const cardWidth = 260;
    const cardHeight = 116;
    const sidePadding = 16;
    const topLimit = fillHeight ? 16 : 88;
    const bottomLimit = fillHeight
      ? Math.max(topLimit + cardHeight, mapRect.height - bottomInset - 24)
      : mapRect.height - 16;

    const clamp = (value: number, min: number, max: number) => Math.min(Math.max(value, min), Math.max(min, max));
    const preferredLeft = markerX - cardWidth / 2;
    const preferredTop = markerY - cardHeight - 76;
    const left = clamp(preferredLeft, sidePadding, mapRect.width - cardWidth - sidePadding);
    const top = clamp(preferredTop, topLimit, bottomLimit - cardHeight);

    return { left, top };
  }, [bottomInset, fillHeight]);

  const updateSelectedPreview = useCallback(() => {
    if (!selectedTrip) return null;
    const selectedIndex = source.findIndex((trip) => trip.id === selectedTrip.id);
    const position = markerPositions.get(selectedTrip.id) ?? getTripLatLng(selectedTrip, Math.max(0, selectedIndex));
    const style = getPreviewStyle(position);
    if (!style) {
      setSelectedPreview(null);
      return;
    }

    setSelectedPreview({
      index: Math.max(0, selectedIndex),
      style,
    });
  }, [getPreviewStyle, markerPositions, selectedTrip, source]);

  useEffect(() => {
    if (!isLoaded || !mapRef.current) return;
    fitMapToSource(mapRef.current);
  }, [fitMapToSource, isLoaded]);

  useEffect(() => {
    idleFitRef.current = '';
  }, [bottomInset, sourceKey]);

  useEffect(() => {
    const frame = window.requestAnimationFrame(updateSelectedPreview);
    return () => window.cancelAnimationFrame(frame);
  }, [mapViewVersion, updateSelectedPreview]);

  const resetMap = () => {
    setSelectedTrip(null);
    setSelectedPreview(null);
    if (mapRef.current) fitMapToSource(mapRef.current);
  };

  const changeZoom = (delta: number) => {
    const currentZoom = mapRef.current?.getZoom() ?? mapZoom;
    const nextZoom = Math.max(4, Math.min(13, currentZoom + delta));
    mapRef.current?.setZoom(nextZoom);
    setMapZoom(nextZoom);
  };

  if (!googleMapsApiKey || loadError) {
    return <DecorativeMapBand trips={trips} fillHeight={fillHeight} />;
  }

  return (
    <div className={`relative overflow-hidden bg-[#dcefdc] transition-[height] duration-300 ${fillHeight ? 'h-full' : expanded ? 'h-[420px]' : 'h-[236px]'}`}>
      {!isLoaded ? (
        <div className="flex h-full w-full items-center justify-center bg-gradient-to-br from-blue-100 via-sky-50 to-green-50">
          <p className="text-xs text-gray-400">지도를 불러오는 중...</p>
        </div>
      ) : (
        <GoogleMap
          mapContainerStyle={feedMapContainerStyle}
          options={feedMapOptions}
          onZoomChanged={() => {
            const nextZoom = mapRef.current?.getZoom();
            if (typeof nextZoom === 'number') {
              setMapZoom(nextZoom);
              setMapViewVersion((value) => value + 1);
            }
          }}
          onLoad={(map) => {
            mapRef.current = map;
            map.setOptions({ scrollwheel: true, gestureHandling: 'greedy' });
            setMapZoom(map.getZoom() ?? 5);
            setMapViewVersion((value) => value + 1);
            fitMapToSource(map);
          }}
          onIdle={() => {
            setMapViewVersion((value) => value + 1);
            const idleKey = `${sourceKey}:${Math.round(bottomInset)}`;
            if (!mapRef.current || idleFitRef.current === idleKey) return;
            idleFitRef.current = idleKey;
            fitMapToSource(mapRef.current);
          }}
          onUnmount={() => {
            mapRef.current = null;
          }}
        >
          {source.map((trip, index) => {
            const position = markerPositions.get(trip.id) ?? getTripLatLng(trip, index);

            return (
              <TripPhotoMapMarker
                key={trip.id}
                trip={trip}
                index={index}
                position={position}
                selected={selectedTrip?.id === trip.id}
                onClick={() => {
                  setMapViewVersion((value) => value + 1);
                  setSelectedTrip(trip);
                }}
              />
            );
          })}
        </GoogleMap>
      )}
      {selectedTrip && selectedPreview && (
        <TripMapPreviewCard
          trip={selectedTrip}
          index={selectedPreview.index}
          style={selectedPreview.style}
          onClose={() => {
            setSelectedTrip(null);
            setSelectedPreview(null);
          }}
        />
      )}
      {isLoaded && (
        <div className="absolute right-5 top-5 z-20 overflow-hidden rounded-lg bg-white shadow ring-1 ring-black/10">
          <button
            type="button"
            onClick={() => changeZoom(1)}
            className="grid h-9 w-9 place-items-center border-b border-gray-100 text-xl font-semibold leading-none text-gray-700 hover:bg-gray-50"
            aria-label="지도 확대"
          >
            +
          </button>
          <button
            type="button"
            onClick={() => changeZoom(-1)}
            className="grid h-9 w-9 place-items-center text-xl font-semibold leading-none text-gray-700 hover:bg-gray-50"
            aria-label="지도 축소"
          >
            -
          </button>
        </div>
      )}
      {!fillHeight && (
        <button
          type="button"
          onClick={() => setExpanded((value) => !value)}
          className="absolute right-6 top-5 z-20 flex items-center gap-1 rounded-full bg-white/95 px-3 py-1.5 text-xs font-semibold text-gray-600 shadow hover:bg-white"
        >
          {expanded ? <Minimize2 size={13} /> : <Maximize2 size={13} />}
          {expanded ? '지도 접기' : '지도 펼치기'}
        </button>
      )}
      {selectedTrip && (
        <button
          type="button"
          onClick={resetMap}
          className="absolute right-32 top-5 z-20 rounded-full bg-white/95 px-3 py-1.5 text-xs font-semibold text-gray-600 shadow hover:bg-white"
        >
          전체 보기
        </button>
      )}
    </div>
  );
}

function TopLikedCarousel({ trips }: { trips: Trip[] }) {
  const scrollRef = useRef<HTMLDivElement>(null);
  const items = useMemo(() => trips.slice(0, 10), [trips]);

  const scroll = (direction: -1 | 1) => {
    scrollRef.current?.scrollBy({ left: direction * 300, behavior: 'smooth' });
  };

  return (
    <section className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
      <div className="mb-5 flex items-center justify-between">
        <h2 className="flex items-center gap-2 text-lg font-bold text-gray-900">
          <Heart size={20} className="fill-emerald-500 text-emerald-500" /> 월간 인기 여행 Top 10
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
      {items.length === 0 ? (
        <div className="rounded-lg border border-dashed border-gray-200 py-10 text-center text-sm text-gray-400">
          이번 달 인기 여행기가 아직 없습니다.
        </div>
      ) : (
        <div ref={scrollRef} className="flex gap-5 overflow-x-auto pb-1 [scrollbar-width:none]">
          {items.map((trip, index) => (
            <Link key={trip.id} href={`/trips/${trip.id}`} className="group relative h-[168px] w-[250px] shrink-0 overflow-hidden rounded-lg shadow-sm">
              <TripVisual trip={trip} index={index} showMeta={false} className="h-full w-full transition-transform group-hover:scale-105" />
              <span className="absolute left-3 top-3 rounded-md bg-white/95 px-2 py-1 text-sm font-bold text-gray-800">월간 {index + 1}위</span>
              <div className="absolute inset-x-0 bottom-0 z-10 bg-gradient-to-t from-black/80 via-black/50 to-transparent px-3 pb-3 pt-10 text-white">
                <p className="line-clamp-1 text-sm font-bold">{trip.title}</p>
                <p className="mt-1 flex items-center justify-between gap-2 text-xs text-white/85">
                  <span className="truncate">{trip.city}, {trip.country}</span>
                  <span className="flex shrink-0 items-center gap-1">
                    <Heart size={12} fill="white" /> {trip.likeCount ?? 0}
                  </span>
                </p>
              </div>
            </Link>
          ))}
        </div>
      )}
    </section>
  );
}

function RecentTripsList({
  trips,
  onLike,
  hasMore,
  loadingMore,
  sentinelRef,
}: {
  trips: Trip[];
  onLike: (trip: Trip) => void;
  hasMore: boolean;
  loadingMore: boolean;
  sentinelRef: RefObject<HTMLDivElement | null>;
}) {
  const visible = trips;

  return (
    <section className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
      <h2 className="mb-4 flex items-center gap-2 text-lg font-bold text-gray-900">
        <Clock3 size={21} className="text-emerald-500" /> 최신 여행기
      </h2>
      <div className="divide-y divide-gray-100">
        {visible.map((trip, index) => (
          <div key={trip.id} className="flex items-center gap-5 py-4 first:pt-0">
            <Link href={trip.id.startsWith('recent') ? '#' : `/trips/${trip.id}`} className="block h-[92px] w-[220px] shrink-0 overflow-hidden rounded-lg">
              <TripVisual trip={trip} index={index} className="h-full w-full" />
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
      {(trips.length > 0 || hasMore || loadingMore) && (
        <div ref={sentinelRef} className="flex min-h-10 items-center justify-center pt-4 text-sm text-gray-400">
          {loadingMore ? '불러오는 중...' : hasMore ? null : '마지막 여행기입니다.'}
        </div>
      )}
    </section>
  );
}

export default function HomePage() {
  const router = useRouter();
  const [topLiked, setTopLiked] = useState<Trip[]>([]);
  const [recent, setRecent] = useState<Trip[]>([]);
  const [recentPage, setRecentPage] = useState(0);
  const [recentHasMore, setRecentHasMore] = useState(true);
  const [recentLoadingMore, setRecentLoadingMore] = useState(false);
  const [sheetHeight, setSheetHeight] = useState(430);
  const dragRef = useRef<{ y: number; height: number } | null>(null);
  const sheetScrollRef = useRef<HTMLDivElement | null>(null);
  const recentSentinelRef = useRef<HTMLDivElement | null>(null);
  const mapTrips = useMemo(() => uniqueTrips([...recent, ...topLiked]), [recent, topLiked]);

  const attachLikedStatus = useCallback(async (trips: Trip[]) => {
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
  }, []);

  const loadFeeds = useCallback(async () => {
    const [topLikedResult, recentResult, monthlyCandidateResult] = await Promise.allSettled([
      feedApi.getTopLiked(),
      feedApi.getRecent({ page: 0, size: 6 }),
      feedApi.getRecent({ page: 0, size: 50 }),
    ]);

    if (topLikedResult.status === 'fulfilled') {
      const monthlyCandidates = monthlyCandidateResult.status === 'fulfilled'
        ? monthlyCandidateResult.value as Trip[]
        : [];
      const normalizedTopLiked = await attachLikedStatus(uniqueTrips([
        ...(topLikedResult.value as Trip[]),
        ...monthlyCandidates,
      ]));
      const nextTopLiked = uniqueTrips(normalizedTopLiked)
        .filter(isCurrentMonthTrip)
        .sort((a, b) => (b.likeCount ?? 0) - (a.likeCount ?? 0))
        .slice(0, 10);
      setTopLiked(nextTopLiked);
    }

    if (recentResult.status === 'fulfilled') {
      const normalizedRecent = await attachLikedStatus(recentResult.value as Trip[]);
      setRecent(normalizedRecent);
      setRecentHasMore(normalizedRecent.length >= 6);
      setRecentPage(0);
    }
  }, [attachLikedStatus]);

  useEffect(() => {
    const id = window.setTimeout(() => {
      void loadFeeds();
    }, 0);

    const handlePageShow = () => {
      void loadFeeds();
    };

    window.addEventListener('pageshow', handlePageShow);
    return () => {
      window.clearTimeout(id);
      window.removeEventListener('pageshow', handlePageShow);
    };
  }, [loadFeeds]);

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

  const loadMoreRecent = useCallback(async () => {
    if (recentLoadingMore || !recentHasMore) return;
    setRecentLoadingMore(true);
    try {
      const nextPage = recentPage + 1;
      const nextTrips = await feedApi.getRecent({ page: nextPage, size: 6 }) as Trip[];
      const withLiked = isAuthenticated()
        ? await Promise.all(nextTrips.map(async (trip) => {
            try {
              const status = await likeApi.getMine(trip.id);
              return { ...trip, liked: status.liked };
            } catch {
              return { ...trip, liked: false };
            }
          }))
        : nextTrips.map((trip) => ({ ...trip, liked: false }));
      setRecent((prev) => uniqueTrips([...prev, ...withLiked]));
      setRecentPage(nextPage);
      setRecentHasMore(nextTrips.length >= 6);
    } finally {
      setRecentLoadingMore(false);
    }
  }, [recentHasMore, recentLoadingMore, recentPage]);

  useEffect(() => {
    const root = sheetScrollRef.current;
    const target = recentSentinelRef.current;
    if (!root || !target || !recentHasMore) return;

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          void loadMoreRecent();
        }
      },
      { root, rootMargin: '160px 0px 160px 0px', threshold: 0.01 },
    );

    observer.observe(target);
    return () => observer.disconnect();
  }, [loadMoreRecent, recentHasMore, recent.length]);

  const startSheetDrag = (event: React.MouseEvent<HTMLDivElement>) => {
    dragRef.current = { y: event.clientY, height: sheetHeight };
    window.addEventListener('mousemove', moveSheetDrag);
    window.addEventListener('mouseup', endSheetDrag);
  };

  const moveSheetDrag = (event: MouseEvent) => {
    if (!dragRef.current) return;
    const next = dragRef.current.height + dragRef.current.y - event.clientY;
    setSheetHeight(Math.max(260, Math.min(window.innerHeight - 110, next)));
  };

  const endSheetDrag = () => {
    dragRef.current = null;
    window.removeEventListener('mousemove', moveSheetDrag);
    window.removeEventListener('mouseup', endSheetDrag);
  };

  return (
    <div className="relative h-[calc(100vh-64px)] overflow-hidden bg-gray-50">
      <div className="absolute inset-0 z-0">
        <MapBand trips={mapTrips} fillHeight bottomInset={sheetHeight} />
      </div>
      <div
        className="absolute bottom-0 left-0 right-0 z-20 overflow-hidden rounded-t-2xl bg-gray-50 shadow-2xl"
        style={{ height: sheetHeight }}
      >
        <div onMouseDown={startSheetDrag} className="flex cursor-grab justify-center px-8 py-3 active:cursor-grabbing">
          <span className="h-1.5 w-12 rounded-full bg-gray-300" />
        </div>
      <div
        ref={sheetScrollRef}
        className="mx-auto flex max-w-[1180px] flex-col gap-5 overflow-y-auto px-8 pb-10"
        style={{ height: sheetHeight - 38 }}
      >
        <div className="flex justify-end">
          <button onClick={handleCreateTrip} className="flex items-center gap-2 rounded-lg bg-emerald-600 px-4 py-2.5 text-sm font-bold text-white shadow-sm hover:bg-emerald-700">
            <Plus size={18} /> 트립생성
          </button>
        </div>
        <TopLikedCarousel trips={topLiked} />
        <RecentTripsList
          trips={recent}
          onLike={handleLike}
          hasMore={recentHasMore}
          loadingMore={recentLoadingMore}
          sentinelRef={recentSentinelRef}
        />
      </div>
      </div>
    </div>
  );
}
