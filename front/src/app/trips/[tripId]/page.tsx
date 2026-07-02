'use client';

import { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import { ArrowLeft, Heart, MapPin, Calendar, Globe, Lock, Pencil, X, ChevronRight } from 'lucide-react';
import { tripApi, postApi, likeApi } from '@/lib/api';
import type { Trip, Post } from '@/types';

// ── 지도 placeholder ──────────────────────────────────────────────────
function TripMapPlaceholder({ posts }: { posts: Post[] }) {
  // TODO: 실제 지도 연결 - markers 표시
  return (
    <div className="relative w-full h-full bg-gradient-to-br from-blue-100 via-sky-50 to-green-50 rounded-xl overflow-hidden">
      <div className="absolute inset-0 flex items-center justify-center">
        <p className="text-xs text-gray-400">🗺 지도 영역 (TODO: 지도 라이브러리 연결)</p>
      </div>
      {posts.map((post, i) =>
        post.marker ? (
          <div
            key={post.id}
            className="absolute flex flex-col items-center"
            style={{ top: `${30 + i * 10}%`, left: `${25 + i * 12}%` }}
          >
            <MapPin size={20} className="text-green-600 fill-green-100" />
            <p className="text-[9px] font-semibold text-gray-700">{post.marker.placeName}</p>
          </div>
        ) : null,
      )}
    </div>
  );
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
          Day {i + 1}
        </button>
      ))}
    </div>
  );
}

// ── 타임라인 아이템 ───────────────────────────────────────────────────
function TimelineItem({ post }: { post: Post }) {
  return (
    <div className="flex gap-3 py-3 relative">
      {/* 세로선 */}
      <div className="flex flex-col items-center">
        <div className="w-2.5 h-2.5 rounded-full bg-green-500 mt-1 flex-shrink-0 z-10" />
        <div className="w-px flex-1 bg-gray-200 mt-1" />
      </div>

      <div className="flex-1 pb-2">
        <p className="text-xs text-gray-400 mb-0.5">{post.time ?? '시간 미정'}</p>
        <p className="font-semibold text-gray-900 text-sm">{post.title}</p>
        <p className="text-xs text-gray-500 mt-1 line-clamp-3">{post.content}</p>
        {post.marker && (
          <p className="text-xs text-gray-400 mt-1 flex items-center gap-0.5">
            <MapPin size={10} /> {post.marker.placeName}
          </p>
        )}
        {/* 이미지 그리드 */}
        {post.images.length > 0 && (
          <div className="grid grid-cols-3 gap-1 mt-2">
            {post.images.slice(0, 3).map((img) => (
              <img key={img.id} src={img.url} alt="" className="w-full h-16 object-cover rounded-md" />
            ))}
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
  const [showDetail, setShowDetail] = useState(true);

  useEffect(() => {
    Promise.all([
      tripApi.getOne(tripId),
      postApi.getList(tripId),
    ])
      .then(([t, p]) => {
        const tripData = t as Trip;
        const postData = p as Post[];
        setTrip(tripData);
        setLiked(!!tripData.liked);
        setLikeCount(tripData.likeCount ?? 0);
        setPosts(postData);
        // 날짜 목록 추출
        const days = [...new Set(postData.map((post) => post.date))].sort();
        if (days.length) setActiveDay(days[0]);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [tripId]);

  const handleLike = async () => {
    try {
      if (liked) {
        await likeApi.unlike(tripId);
        setLiked(false);
        setLikeCount((n) => n - 1);
      } else {
        await likeApi.like(tripId);
        setLiked(true);
        setLikeCount((n) => n + 1);
      }
    } catch {
      // TODO: 미로그인 시 로그인 유도
    }
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

  return (
    <div className="flex flex-col h-[calc(100vh-56px)] relative">
      {/* 지도 (배경) */}
      <div className="absolute inset-0">
        <TripMapPlaceholder posts={posts} />
      </div>

      {/* 상단 네비 */}
      <div className="relative z-10 flex items-center justify-between px-4 pt-4">
        <button onClick={() => router.back()} className="w-8 h-8 bg-white rounded-full shadow flex items-center justify-center hover:bg-gray-50 transition-colors">
          <ArrowLeft size={16} />
        </button>
        <div className="flex items-center gap-2">
          <Link href={`/trips/${tripId}/edit`} className="flex items-center gap-1 bg-white text-gray-700 text-xs font-semibold px-3 py-1.5 rounded-full shadow hover:bg-gray-50 transition-colors">
            <Pencil size={12} /> 수정/편집
          </Link>
        </div>
      </div>

      {/* 하단 상세 패널 */}
      {showDetail && (
        <div className="absolute bottom-0 left-0 right-0 z-20 bg-white rounded-t-2xl shadow-2xl" style={{ maxHeight: '70vh' }}>
          {/* 드래그 핸들 */}
          <div className="flex justify-between items-center px-5 pt-4 pb-2">
            <div className="flex gap-3 items-start">
              {/* 썸네일 placeholder */}
              <div className="w-20 h-20 rounded-xl bg-gradient-to-br from-gray-300 to-gray-400 flex-shrink-0" />
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
                    <img src={trip.author?.profileImageUrl ?? ''} alt="" className="w-4 h-4 rounded-full bg-gray-200" />
                    {trip.author?.nickname}
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
              <button onClick={() => setShowDetail(false)} className="text-gray-300 hover:text-gray-500">
                <X size={18} />
              </button>
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
          {days.length > 0 && <DayTabs days={days} active={activeDay} onSelect={setActiveDay} />}

          {/* 타임라인 */}
          <div className="overflow-y-auto px-5 pb-6" style={{ maxHeight: '40vh' }}>
            {dayPosts.length === 0 ? (
              <p className="text-center text-gray-400 text-sm py-8">이 날의 기록이 없습니다.</p>
            ) : (
              dayPosts.map((post) => <TimelineItem key={post.id} post={post} />)
            )}
          </div>
        </div>
      )}

      {/* 패널 닫혔을 때 열기 버튼 */}
      {!showDetail && (
        <button
          onClick={() => setShowDetail(true)}
          className="absolute bottom-6 left-1/2 -translate-x-1/2 z-20 bg-white shadow-lg rounded-full px-5 py-2.5 text-sm font-semibold text-gray-700 flex items-center gap-1 hover:bg-gray-50 transition-colors"
        >
          여행 정보 보기 <ChevronRight size={14} />
        </button>
      )}
    </div>
  );
}
