'use client';

import { useEffect, useState, useRef } from 'react';
import Link from 'next/link';
import { Heart, ChevronLeft, ChevronRight, MapPin, Plus, Clock } from 'lucide-react';
import { feedApi, likeApi } from '@/lib/api';
import type { Trip } from '@/types';

// ── 지도 placeholder ──────────────────────────────────────────────────
function MapPlaceholder() {
  // TODO: 실제 지도 연결 (Kakao Map / Leaflet 등) - 1차에서는 placeholder
  return (
    <div className="relative w-full h-[200px] bg-gradient-to-br from-blue-100 via-sky-50 to-green-50 overflow-hidden">
      <div className="absolute inset-0 flex items-center justify-center">
        <p className="text-xs text-gray-400">🗺 지도 영역 (TODO: 지도 라이브러리 연결)</p>
      </div>
      {[
        { label: '파리', count: 12, top: '30%', left: '38%' },
        { label: '나고야', count: 4, top: '40%', left: '68%' },
        { label: '뉴욕', count: 15, top: '35%', left: '20%' },
      ].map((pin) => (
        <div key={pin.label} className="absolute flex flex-col items-center" style={{ top: pin.top, left: pin.left }}>
          <div className="relative">
            <div className="w-9 h-9 rounded-full bg-white border-2 border-green-500 shadow-md overflow-hidden">
              <div className="w-full h-full bg-gray-200" />
            </div>
            <span className="absolute -top-1 -right-1 w-4 h-4 bg-green-600 text-white text-[9px] rounded-full flex items-center justify-center font-bold">
              {pin.count}
            </span>
          </div>
          <p className="text-[10px] font-semibold text-gray-800 mt-0.5 drop-shadow-sm">{pin.label}</p>
        </div>
      ))}
    </div>
  );
}

// ── Top 10 캐러셀 ─────────────────────────────────────────────────────
function TopLikedCarousel({ trips }: { trips: Trip[] }) {
  const scrollRef = useRef<HTMLDivElement>(null);
  const scroll = (dir: 'left' | 'right') =>
    scrollRef.current?.scrollBy({ left: dir === 'left' ? -220 : 220, behavior: 'smooth' });

  const items = trips.length > 0 ? trips : Array.from({ length: 4 });

  return (
    <section className="px-6 py-5">
      <div className="flex items-center justify-between mb-3">
        <h2 className="flex items-center gap-1.5 font-bold text-gray-900 text-sm">
          👍 좋아요 Top 10
        </h2>
        <div className="flex gap-1">
          <button onClick={() => scroll('left')} className="w-7 h-7 rounded-full border border-gray-200 flex items-center justify-center hover:bg-gray-50 transition-colors">
            <ChevronLeft size={13} />
          </button>
          <button onClick={() => scroll('right')} className="w-7 h-7 rounded-full border border-gray-200 flex items-center justify-center hover:bg-gray-50 transition-colors">
            <ChevronRight size={13} />
          </button>
        </div>
      </div>

      <div ref={scrollRef} className="flex gap-3 overflow-x-auto pb-1" style={{ scrollbarWidth: 'none' }}>
        {items.map((trip, i) =>
          trip ? (
            <Link
              key={(trip as Trip).id}
              href={`/trips/${(trip as Trip).id}`}
              className="relative flex-shrink-0 w-[170px] h-[115px] rounded-xl overflow-hidden group"
            >
              {/* thumbnailUrl 없으므로 placeholder 처리 */}
              <div className="w-full h-full bg-gradient-to-br from-gray-300 to-gray-500" />
              <div className="absolute inset-0 bg-black/30 group-hover:bg-black/40 transition-colors" />
              <span className="absolute top-2 left-2 text-white text-xs font-bold">{i + 1}위</span>
              <div className="absolute bottom-2 left-2 right-2">
                <p className="text-white text-xs font-semibold truncate">{(trip as Trip).city}, {(trip as Trip).country}</p>
                <p className="text-white/80 text-[10px] flex items-center gap-0.5 mt-0.5">
                  <Heart size={9} fill="white" /> {(trip as Trip).likeCount?.toLocaleString()}
                </p>
              </div>
            </Link>
          ) : (
            <div key={i} className="flex-shrink-0 w-[170px] h-[115px] rounded-xl bg-gray-200 animate-pulse" />
          ),
        )}
      </div>
    </section>
  );
}

// ── 최신 여행기 ───────────────────────────────────────────────────────
function RecentTripsList({
  trips,
  onLike,
}: {
  trips: Trip[];
  onLike: (id: string, liked: boolean) => void;
}) {
  const [expanded, setExpanded] = useState(false);
  const visible = expanded ? trips : trips.slice(0, 4);

  return (
    <section className="px-6 py-4">
      <h2 className="flex items-center gap-1.5 font-bold text-gray-900 text-sm mb-2">
        <Clock size={15} className="text-green-600" /> 최신 여행기
      </h2>

      <div className="flex flex-col divide-y divide-gray-100">
        {visible.length > 0
          ? visible.map((trip) => (
              <div key={trip.id} className="flex items-center gap-4 py-3">
                {/* thumbnailUrl 없으므로 placeholder */}
                <Link href={`/trips/${trip.id}`} className="flex-shrink-0 w-16 h-12 rounded-lg bg-gray-200 overflow-hidden hover:opacity-80 transition-opacity">
                  <div className="w-full h-full bg-gradient-to-br from-gray-300 to-gray-400" />
                </Link>
                <div className="flex-1 min-w-0">
                  <Link href={`/trips/${trip.id}`} className="font-semibold text-gray-900 text-sm hover:text-green-700 line-clamp-1">
                    {trip.title}
                  </Link>
                  <p className="text-xs text-gray-500 line-clamp-1 mt-0.5">{trip.city}, {trip.country}</p>
                  <div className="flex items-center gap-1.5 mt-1 text-xs text-gray-400">
                    <MapPin size={10} />
                    <span>{trip.author?.nickname}</span>
                    <span className="text-gray-300">|</span>
                    <span>{trip.startDate}</span>
                  </div>
                </div>
                <button
                  onClick={() => onLike(trip.id, !!trip.liked)}
                  className="flex items-center gap-1 text-xs text-gray-400 hover:text-red-500 transition-colors flex-shrink-0"
                >
                  <Heart size={14} className={trip.liked ? 'fill-red-500 text-red-500' : ''} />
                  {trip.likeCount}
                </button>
              </div>
            ))
          : Array.from({ length: 4 }).map((_, i) => (
              <div key={i} className="flex gap-4 py-3 animate-pulse">
                <div className="w-16 h-12 rounded-lg bg-gray-200" />
                <div className="flex-1 space-y-2 py-1">
                  <div className="h-3 bg-gray-200 rounded w-3/4" />
                  <div className="h-3 bg-gray-200 rounded w-1/2" />
                </div>
              </div>
            ))}
      </div>

      {trips.length > 4 && (
        <button
          onClick={() => setExpanded(!expanded)}
          className="w-full mt-3 py-2 text-sm text-gray-500 border border-gray-200 rounded-lg hover:bg-gray-50 flex items-center justify-center gap-1 transition-colors"
        >
          {expanded ? '접기 ∧' : '더보기 ∨'}
        </button>
      )}
    </section>
  );
}

// ── 홈 페이지 ─────────────────────────────────────────────────────────
export default function HomePage() {
  const [topLiked, setTopLiked] = useState<Trip[]>([]);
  const [recent, setRecent] = useState<Trip[]>([]);

  useEffect(() => {
    feedApi.getTopLiked().then((d) => setTopLiked(d as Trip[])).catch(() => {});
    feedApi.getRecent().then((d) => setRecent(d as Trip[])).catch(() => {});
  }, []);

  const handleLike = async (tripId: string, currentlyLiked: boolean) => {
    try {
      if (currentlyLiked) await likeApi.unlike(tripId);
      else await likeApi.like(tripId);

      setRecent((prev) =>
        prev.map((t) =>
          t.id === tripId
            ? { ...t, liked: !currentlyLiked, likeCount: t.likeCount + (currentlyLiked ? -1 : 1) }
            : t,
        ),
      );
    } catch {
      // TODO: 미로그인 시 로그인 유도
    }
  };

  return (
    <div className="max-w-[820px] relative">
      {/* 헤더 트립생성 버튼 */}
      <div className="fixed top-0 right-0 z-50 flex items-center pr-6 h-[56px]">
        <Link
          href="/trips"
          className="flex items-center gap-1.5 bg-green-600 hover:bg-green-700 text-white text-sm font-semibold px-4 py-2 rounded-lg transition-colors"
        >
          <Plus size={15} />
          트립생성
        </Link>
      </div>

      <MapPlaceholder />
      <div className="border-b border-gray-100" />
      <TopLikedCarousel trips={topLiked} />
      <div className="border-b border-gray-100 mx-6" />
      <RecentTripsList trips={recent} onLike={handleLike} />
    </div>
  );
}
