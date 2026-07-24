'use client';

import { useEffect, useState, useRef } from 'react';
import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import { GoogleMap, Marker as GoogleMarker, useJsApiLoader } from '@react-google-maps/api';
import {
  Plus, Trash2, Save, Eye, ChevronDown,
  MapPin, X, Upload, ImageIcon, PanelLeftClose, PanelLeftOpen,
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
    time: toTimeInput(post.marker?.visitTime ?? post.time),
  };
}

function withMarkerPostTime(post: Post, marker: Marker | null) {
  return {
    ...post,
    marker: marker ?? undefined,
    time: toTimeInput(marker?.visitTime) ?? toTimeInput(post.time),
  };
}

function getDefaultVisitTime(post: Post) {
  if (post.marker?.visitTime) return post.marker.visitTime;
  if (post.time) return post.time.includes('T') ? post.time : `${post.date}T${post.time}`;
  return post.date ? `${post.date}T00:00` : undefined;
}

function combinePostDateAndTime(post: Post, time?: string) {
  if (!post.date || !time) return undefined;
  return `${post.date}T${time}`;
}

function alignMarkerVisitDate(post: Post, visitTime?: string) {
  const time = toTimeInput(visitTime);
  return combinePostDateAndTime(post, time);
}

function formatDateLabel(date: string) {
  return date ? date.replaceAll('-', '. ') : '날짜 미정';
}

function getPostTimeLabel(post: Post) {
  return toTimeInput(post.marker?.visitTime) ?? post.time ?? '시간 미정';
}

function getPostSortValue(post: Post) {
  return `${post.date || '9999-12-31'}T${toTimeInput(post.marker?.visitTime) || post.time || '99:99'}`;
}

function sortPosts(posts: Post[]) {
  return [...posts].sort((a, b) => {
    const byDateTime = getPostSortValue(a).localeCompare(getPostSortValue(b));
    if (byDateTime !== 0) return byDateTime;
    const numericA = Number(a.id);
    const numericB = Number(b.id);
    if (Number.isFinite(numericA) && Number.isFinite(numericB)) {
      return numericA - numericB;
    }
    return a.id.localeCompare(b.id);
  });
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

function getRepresentativeImageId(images: TripImage[], thumbnailUrl?: string) {
  if (!thumbnailUrl) return '';
  return images.find((image) => image.thumbnailUrl === thumbnailUrl || image.url === thumbnailUrl)?.id ?? '';
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

function UnassignedImageShelf({
  images,
  onAssign,
  compact = false,
}: {
  images: TripImage[];
  onAssign: (imageId: string) => void;
  compact?: boolean;
}) {
  if (images.length === 0) {
    return (
      <div className="rounded-xl border border-gray-100 bg-gray-50 px-3 py-4 text-center text-xs text-gray-400">
        배치할 이미지가 없습니다.
      </div>
    );
  }

  return (
    <div className={`grid gap-2 overflow-y-auto rounded-xl border border-gray-100 bg-white p-2 ${
      compact ? 'max-h-[calc(100vh_-_260px)] grid-cols-2' : 'max-h-28 grid-flow-col auto-cols-[5rem] overflow-x-auto'
    }`}>
      {images.map((image) => (
        <button
          key={image.id}
          type="button"
          draggable
          onDragStart={(event) => {
            event.dataTransfer.setData('application/triptrace-image-id', image.id);
            event.dataTransfer.setData('text/plain', image.id);
            event.dataTransfer.effectAllowed = 'move';
          }}
          onClick={() => onAssign(image.id)}
          className={`group relative overflow-hidden rounded-lg bg-gray-100 ring-1 ring-gray-100 transition hover:ring-green-300 ${
            compact ? 'aspect-square' : 'h-20 w-20'
          }`}
          title="클릭하거나 드래그해서 현재 Post에 배치"
        >
          <img src={image.thumbnailUrl || image.url} alt="" className="h-full w-full object-cover" />
          <span className="absolute inset-x-1 bottom-1 rounded bg-black/45 px-1 py-0.5 text-[10px] font-medium text-white opacity-0 transition group-hover:opacity-100">
            배치
          </span>
        </button>
      ))}
    </div>
  );
}

function renderInlineMarkdown(text: string) {
  return text.split(/(\*\*[^*]+\*\*)/g).map((part, index) => {
    if (part.startsWith('**') && part.endsWith('**')) {
      return <strong key={`${part}-${index}`}>{part.slice(2, -2)}</strong>;
    }
    return part;
  });
}

function renderMarkdownPreview(markdown: string) {
  const lines = markdown.split('\n');
  const blocks: React.ReactNode[] = [];
  let listItems: string[] = [];

  const flushList = () => {
    if (!listItems.length) return;
    blocks.push(
      <ul key={`list-${blocks.length}`} className="my-3 list-disc space-y-1 pl-5 text-gray-700">
        {listItems.map((item, index) => (
          <li key={`${item}-${index}`}>{renderInlineMarkdown(item)}</li>
        ))}
      </ul>,
    );
    listItems = [];
  };

  lines.forEach((line, index) => {
    const trimmed = line.trim();
    if (!trimmed) {
      flushList();
      blocks.push(<div key={`space-${index}`} className="h-3" />);
      return;
    }
    if (trimmed.startsWith('- ')) {
      listItems.push(trimmed.slice(2));
      return;
    }

    flushList();
    if (trimmed.startsWith('### ')) {
      blocks.push(<h3 key={index} className="mt-5 text-lg font-bold text-gray-900">{renderInlineMarkdown(trimmed.slice(4))}</h3>);
    } else if (trimmed.startsWith('## ')) {
      blocks.push(<h2 key={index} className="mt-6 text-xl font-bold text-gray-900">{renderInlineMarkdown(trimmed.slice(3))}</h2>);
    } else if (trimmed.startsWith('# ')) {
      blocks.push(<h1 key={index} className="mt-6 text-2xl font-bold text-gray-900">{renderInlineMarkdown(trimmed.slice(2))}</h1>);
    } else {
      blocks.push(<p key={index} className="leading-7 text-gray-700">{renderInlineMarkdown(trimmed)}</p>);
    }
  });

  flushList();
  return blocks;
}

// ────────────────────────────────────────────────────────────────────
// 컬럼 1: Post 목록
// ────────────────────────────────────────────────────────────────────
function PostList({
  posts,
  selectedId,
  onSelect,
  onDelete,
  onCreate,
  onCollapse,
}: {
  posts: Post[];
  selectedId: string | null;
  onSelect: (p: Post) => void;
  onDelete: (id: string) => void;
  onCreate: () => void;
  onCollapse?: () => void;
}) {
  const groupedPosts = posts.reduce<Array<{ date: string; posts: Post[] }>>((groups, post) => {
    const lastGroup = groups[groups.length - 1];
    if (lastGroup?.date === post.date) {
      lastGroup.posts.push(post);
    } else {
      groups.push({ date: post.date, posts: [post] });
    }
    return groups;
  }, []);

  return (
    <div className="flex flex-col h-full overflow-hidden">
      <div className="flex items-center justify-between px-4 py-3 border-b border-gray-100">
        <h3 className="font-bold text-gray-900 text-sm flex items-center gap-1">
          Post 목록
        </h3>
        <div className="flex items-center gap-2">
          <button onClick={onCreate} className="flex items-center gap-1 text-xs text-green-600 hover:text-green-700 font-medium">
            <Plus size={13} /> 새 Post 추가
          </button>
          {onCollapse && (
            <button
              type="button"
              onClick={onCollapse}
              className="rounded-md p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
              title="Post 목록 접기"
            >
              <PanelLeftClose size={14} />
            </button>
          )}
        </div>
      </div>

      <div className="flex-1 overflow-y-auto divide-y divide-gray-50">
        {posts.length === 0 ? (
          <p className="text-center text-gray-400 text-xs py-8">기록이 없습니다.</p>
        ) : (
          groupedPosts.map((group) => (
            <section key={group.date || 'unknown'} className="border-b border-gray-100 last:border-b-0">
              <div className="sticky top-0 z-10 bg-gray-50/95 px-3 py-2 text-[11px] font-bold text-gray-500 backdrop-blur">
                {formatDateLabel(group.date)}
              </div>
              {group.posts.map((post) => (
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
                    <div className="w-14 flex-shrink-0">
                      <span className={`block rounded-md px-2 py-1 text-center text-xs font-bold ${
                        getPostTimeLabel(post) === '시간 미정'
                          ? 'bg-gray-100 text-gray-400'
                          : 'bg-green-100 text-green-700'
                      }`}>
                        {getPostTimeLabel(post)}
                      </span>
                    </div>
                    {post.images?.[0]?.url ? (
                      <img src={post.images[0].url} alt="" className="w-12 h-12 rounded-lg object-cover flex-shrink-0" />
                    ) : (
                      <div className="w-12 h-12 rounded-lg bg-gray-200 flex-shrink-0 flex items-center justify-center">
                        <ImageIcon size={16} className="text-gray-400" />
                      </div>
                    )}
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-semibold text-gray-800 line-clamp-1">{post.title}</p>
                      <p className="text-xs text-gray-400 line-clamp-2 mt-0.5">{post.content ?? ''}</p>
                      <p className="mt-1 text-[11px] text-gray-400">{post.marker?.placeName ?? '위치 미정'}</p>
                    </div>
                  </div>
                  <div className="flex gap-2 mt-2 pl-16">
                    <button
                      onClick={(e) => { e.stopPropagation(); onDelete(post.id); }}
                      className="text-xs text-red-400 border border-red-100 rounded px-2 py-0.5 hover:bg-red-50"
                    >
                      삭제
                    </button>
                  </div>
                </div>
              ))}
            </section>
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
  unassignedImages,
  onChange,
  onAssignTripImage,
  onUnassignPostImage,
  onToast,
  showUnassignedImages = true,
}: {
  tripId: string;
  post: Post;
  unassignedImages: TripImage[];
  onChange: (updated: Post) => void;
  onAssignTripImage: (imageId: string) => void;
  onUnassignPostImage: (imageId: string) => void;
  onToast: (message: string, tone?: ToastState['tone']) => void;
  showUnassignedImages?: boolean;
}) {
  const [draggingOver, setDraggingOver] = useState(false);
  const [contentMode, setContentMode] = useState<'write' | 'preview'>('write');
  const fileRef = useRef<HTMLInputElement>(null);
  const images = post.images ?? [];
  const content = post.content ?? '';

  const set = (key: keyof Post, value: unknown) => {
    if (key === 'date' && typeof value === 'string') {
      const nextPost = { ...post, date: value };
      onChange({
        ...nextPost,
        marker: post.marker
          ? { ...post.marker, visitTime: alignMarkerVisitDate(nextPost, post.marker.visitTime) }
          : post.marker,
      });
      return;
    }

    onChange({ ...post, [key]: value });
  };

  const uploadFiles = async (files: File[]) => {
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

  const handleAddImages = async (e: React.ChangeEvent<HTMLInputElement>) => {
    await uploadFiles(Array.from(e.target.files ?? []));
  };

  const handleImageDrop = async (event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    setDraggingOver(false);

    const files = Array.from(event.dataTransfer.files ?? []).filter((file) => file.type.startsWith('image/'));
    if (files.length) {
      await uploadFiles(files);
      return;
    }

    const imageId = event.dataTransfer.getData('application/triptrace-image-id') || event.dataTransfer.getData('text/plain');
    if (imageId) {
      onAssignTripImage(imageId);
    }
  };

  return (
    <div className="flex flex-col h-full overflow-hidden">
      <div className="flex items-center justify-between px-4 py-3 border-b border-gray-100">
        <h3 className="font-bold text-gray-900 text-sm flex items-center gap-1">
          Post 작성
        </h3>
        <span className="rounded-full bg-gray-100 px-2 py-1 text-[11px] font-medium text-gray-500">편집 중</span>
      </div>

      <div className="flex-1 overflow-y-auto">
        <article className="mx-auto flex w-full max-w-3xl flex-col gap-5 px-4 py-5 md:px-6">
          <header className="border-b border-gray-100 pb-4">
            <div className="mb-3 flex flex-wrap items-center gap-2 text-xs text-gray-400">
              <input
                type="date"
                value={post.date}
                onChange={(e) => set('date', e.target.value)}
                className="rounded-md border border-transparent bg-gray-50 px-2 py-1 font-medium text-gray-600 outline-none hover:border-gray-200 focus:border-green-500"
              />
              <span>{getPostTimeLabel(post)}</span>
              <span>·</span>
              <span className="truncate">{post.marker?.placeName ?? '위치 미정'}</span>
            </div>
            <input
              value={post.title}
              onChange={(e) => set('title', e.target.value)}
              placeholder="Post 제목"
              className="w-full border-0 bg-transparent px-0 text-3xl font-bold leading-tight text-gray-950 outline-none placeholder:text-gray-300 md:text-4xl"
            />
          </header>

          <section>
            <div className="mb-2 flex items-center justify-between">
              <div className="flex rounded-lg bg-gray-100 p-0.5">
                <button
                  type="button"
                  onClick={() => setContentMode('write')}
                  className={`rounded-md px-3 py-1 text-xs font-semibold transition ${
                    contentMode === 'write' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500'
                  }`}
                >
                  작성
                </button>
                <button
                  type="button"
                  onClick={() => setContentMode('preview')}
                  className={`rounded-md px-3 py-1 text-xs font-semibold transition ${
                    contentMode === 'preview' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500'
                  }`}
                >
                  미리보기
                </button>
              </div>
              <span className="text-[10px] text-gray-400">{content.length} / 1000</span>
            </div>
            {contentMode === 'write' ? (
              <textarea
                value={content}
                onChange={(e) => set('content', e.target.value)}
                maxLength={1000}
                rows={12}
                placeholder="여행의 장면을 적어보세요. # 제목, ## 소제목, - 목록, **강조**를 사용할 수 있습니다."
                className="min-h-[320px] w-full resize-none rounded-xl border border-gray-100 bg-white px-4 py-4 text-base leading-7 text-gray-800 outline-none placeholder:text-gray-300 focus:border-green-500"
              />
            ) : (
              <div className="min-h-[320px] rounded-xl border border-gray-100 bg-white px-4 py-4">
                {content.trim() ? (
                  <div className="prose max-w-none">{renderMarkdownPreview(content)}</div>
                ) : (
                  <p className="text-sm text-gray-400">미리보기 할 본문이 없습니다.</p>
                )}
              </div>
            )}
          </section>

        {/* 이미지 */}
        <section>
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
          <div
            onDragOver={(event) => {
              event.preventDefault();
              setDraggingOver(true);
            }}
            onDragLeave={() => setDraggingOver(false)}
            onDrop={handleImageDrop}
            className={`rounded-xl border border-dashed p-2 transition-colors ${
              draggingOver ? 'border-green-400 bg-green-50' : 'border-gray-200 bg-gray-50/40'
            }`}
          >
          <div className="flex h-24 gap-2 overflow-x-auto pb-1">
            {images.map((img) =>
              img.url ? (
                <div key={img.id} className="relative group h-full flex-shrink-0">
                  <img
                    src={img.url}
                    alt=""
                    draggable
                    onDragStart={(event) => {
                      event.dataTransfer.setData('application/triptrace-image-id', img.id);
                      event.dataTransfer.setData('text/plain', img.id);
                      event.dataTransfer.effectAllowed = 'copyMove';
                    }}
                    className="h-full w-auto max-w-none cursor-grab object-cover rounded-lg active:cursor-grabbing"
                  />
                  <button
                    onClick={() => onUnassignPostImage(img.id)}
                    className="absolute top-1 right-1 w-5 h-5 bg-black/50 rounded-full hidden group-hover:flex items-center justify-center"
                    title="Post에서 빼기"
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
          <p className="mt-1 text-[10px] text-gray-400">이미지를 끌어오거나 파일을 떨어뜨려 현재 Post에 배치할 수 있습니다.</p>
          </div>
        </section>

        {showUnassignedImages && (
        <section>
          <div className="mb-2 flex items-center justify-between">
            <label className="text-xs text-gray-500">미배치 이미지 ({unassignedImages.length})</label>
            <span className="text-[10px] text-gray-400">드래그해서 위로 배치</span>
          </div>
          <UnassignedImageShelf images={unassignedImages} onAssign={onAssignTripImage} />
        </section>
        )}
        </article>
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
  onMarkerUpdated: (marker: Marker | null) => void;
}) {
  const [marker, setMarker] = useState<Marker | null>(post.marker ?? null);
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

  const geocodeKeyword = async (keyword: string): Promise<PlaceCandidate[]> => {
    if (!googleMapsApiKey || !isLoaded || typeof google === 'undefined' || !google.maps.Geocoder) {
      return [];
    }

    const geocoder = new google.maps.Geocoder();
    const response = await geocoder.geocode({ address: keyword, region: 'kr' });

    return response.results.slice(0, 5).map((result) => ({
      placeId: result.place_id,
      name: result.address_components[0]?.long_name ?? result.formatted_address ?? keyword,
      address: result.formatted_address,
      latitude: result.geometry.location.lat(),
      longitude: result.geometry.location.lng(),
      types: result.types,
    }));
  };

  const setM = (k: keyof Marker, v: unknown) => {
    if (!marker) return;
    const nextMarker = { ...marker, [k]: v };
    setMarker(nextMarker);
    onMarkerUpdated(nextMarker);
  };

  const loadSearchCandidates = async (keyword: string) => {
    setCandidatesOpen(true);
    setCandidateMode('search');
    setCandidatesLoading(true);
    setCandidatesError('');
    try {
      let data = await placeApi.search(keyword);
      if (data.length === 0) {
        data = await geocodeKeyword(keyword);
      }
      setCandidates(data);
    } catch (error) {
      setCandidatesError(error instanceof Error ? error.message : '장소 검색에 실패했습니다.');
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

    await loadSearchCandidates(keyword);
  };

  const loadCandidates = async () => {
    if (!marker) return;
    if (marker.lat == null || marker.lng == null) {
      const keyword = searchKeyword.trim() || marker.placeName?.trim();
      if (keyword) {
        setSearchKeyword(keyword);
        await loadSearchCandidates(keyword);
        return;
      }

      setCandidatesError('좌표가 없어 주변 장소를 조회할 수 없습니다. 장소명 또는 주소로 검색해주세요.');
      setCandidatesOpen(true);
      setCandidateMode('nearby');
      return;
    }

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
    onMarkerUpdated(nextMarker);
    setCandidates([]);
    setCandidateMode(null);
    setCandidatesOpen(false);
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
                {markerPosition ? '지도 로딩 대기 중' : '마커 위치가 아직 지정되지 않았습니다'}
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
                  value={marker.lat ?? ''}
                  onChange={(e) => setM('lat', e.target.value === '' ? undefined : parseFloat(e.target.value))}
                  step="0.0001"
                  className="w-full border border-gray-200 rounded-lg px-3 py-2 text-xs outline-none focus:border-green-500"
                />
              </div>
              <div>
                <label className="block text-xs text-gray-500 mb-1">경도</label>
                <input
                  type="number"
                  value={marker.lng ?? ''}
                  onChange={(e) => setM('lng', e.target.value === '' ? undefined : parseFloat(e.target.value))}
                  step="0.0001"
                  className="w-full border border-gray-200 rounded-lg px-3 py-2 text-xs outline-none focus:border-green-500"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs text-gray-500 mb-1">방문 시간</label>
              <input
                type="time"
                value={toTimeInput(marker.visitTime) ?? ''}
                onChange={(e) => setM('visitTime', combinePostDateAndTime(post, e.target.value))}
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-xs outline-none focus:border-green-500"
              />
            </div>

            <div className="flex gap-2">
              <span className="flex-1 rounded-lg bg-gray-100 px-3 py-2 text-center text-xs font-medium text-gray-500">
                {marker.id ? '마커 편집 중' : '새 마커 편집 중'}
              </span>
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
  const [representativeImageId, setRepresentativeImageId] = useState('');
  const [savedRepresentativeImageId, setSavedRepresentativeImageId] = useState('');
  const [selectedPostId, setSelectedPostId] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState<ToastState | null>(null);
  const [mobileTab, setMobileTab] = useState<'posts' | 'edit' | 'location'>('edit');
  const [representativeDragOver, setRepresentativeDragOver] = useState(false);
  const [postListOpen, setPostListOpen] = useState(true);
  const [unassignedPanelOpen, setUnassignedPanelOpen] = useState(true);
  const [markerPanelOpen, setMarkerPanelOpen] = useState(false);
  const [markerPanelWidth, setMarkerPanelWidth] = useState(380);
  const markerResizeRef = useRef<{ x: number; width: number } | null>(null);

  // Trip 기본 정보 편집 상태
  const [tripForm, setTripForm] = useState({
    title: '', country: '', city: '', startDate: '', endDate: '', isPublic: true,
  });
  const selectedPost = selectedPostId
    ? posts.find((post) => post.id === selectedPostId) ?? null
    : null;

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
        const postData = sortPosts((p as Post[]).map(withDerivedPostTime));
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
        const normalizedImages = loadedImages.length ? loadedImages : getTripImagesFromPosts(postData);
        const initialRepresentativeImageId = getRepresentativeImageId(normalizedImages, tripData.thumbnailUrl);
        setTripImages(normalizedImages);
        setRepresentativeImageId(initialRepresentativeImageId);
        setSavedRepresentativeImageId(initialRepresentativeImageId);
        setPosts(postData);
        if (postData.length) setSelectedPostId(postData[0].id);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [tripId]);

  const handleSaveTrip = async () => {
    setSaving(true);
    try {
      const updatedPosts = await Promise.all(posts.map(async (post) => {
        const updatedPost = await postApi.update(tripId, post.id, {
          title: post.title,
          content: post.content,
          date: post.date,
        }) as Post;

        if (!post.marker) {
          return withDerivedPostTime(updatedPost);
        }

        const markerPayload = {
          placeName: post.marker.placeName,
          lat: post.marker.lat,
          lng: post.marker.lng,
          visitTime: alignMarkerVisitDate(post, post.marker.visitTime),
          source: post.marker.source ?? (post.marker.id ? 'AUTO' : 'MANUAL'),
        };
        const updatedMarker = post.marker.id
          ? await markerApi.update(post.id, post.marker.id, markerPayload)
          : await markerApi.create(post.id, markerPayload);

        return withMarkerPostTime(withDerivedPostTime(updatedPost), updatedMarker as Marker);
      }));

      await tripApi.update(tripId, tripForm);

      if (representativeImageId && representativeImageId !== savedRepresentativeImageId) {
        const nextTrip = await tripApi.updateRepresentativeImage(tripId, representativeImageId) as Trip;
        setTrip(nextTrip);
        setSavedRepresentativeImageId(representativeImageId);
      }

      setPosts(sortPosts(updatedPosts));
      showToast('변경사항이 저장되었습니다.');
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
      const nextPosts = sortPosts(posts.filter((post) => post.id !== postId));
      setPosts(nextPosts);
      if (selectedPostId === postId) setSelectedPostId(nextPosts[0]?.id ?? null);
      setTripImages((prev) => {
        const nextImages = prev.map((image) => (
          deletedPost?.images.some((postImage) => postImage.id === image.id)
            ? { ...image, postId: undefined }
            : image
        ));
        if (deletedPost?.images.some((postImage) => postImage.id === representativeImageId)) {
          setRepresentativeImageId('');
        }
        return nextImages;
      });
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
      setPosts((prev) => sortPosts([...prev, nextPost]));
      setSelectedPostId(nextPost.id);
      showToast('새 Post가 생성되었습니다.');
    } catch (error) {
      showToast(error instanceof Error ? error.message : 'Post 생성에 실패했습니다.', 'error');
    }
  };

  const selectedRepresentativeImage = tripImages.find((image) => image.id === representativeImageId);

  const handleDeleteTrip = async () => {
    if (!confirm('이 Trip을 삭제하시겠습니까?')) return;
    try {
      await tripApi.delete(tripId);
      router.push('/trips');
    } catch (error) {
      showToast(error instanceof Error ? error.message : 'Trip 삭제에 실패했습니다.', 'error');
    }
  };

  const unassignedImages = tripImages.filter((image) => !image.postId);

  const handleAssignTripImage = async (imageId: string) => {
    if (!selectedPost || selectedPost.images?.some((image) => image.id === imageId)) return;
    try {
      const image = await postApi.assignTripImage(tripId, selectedPost.id, imageId);
      setPosts((prev) => sortPosts(prev.map((post) => (
        post.id === selectedPost.id
          ? { ...post, images: [...(post.images ?? []), image] }
          : post
      ))));
      setTripImages((prev) => prev.map((tripImage) => (
        tripImage.id === imageId ? { ...tripImage, postId: selectedPost.id } : tripImage
      )));
      showToast('이미지를 Post에 배치했습니다.');
    } catch (error) {
      showToast(error instanceof Error ? error.message : '이미지 배치에 실패했습니다.', 'error');
    }
  };

  const handleUnassignPostImage = async (imageId: string) => {
    if (!selectedPost) return;
    const targetImage = selectedPost.images?.find((image) => image.id === imageId);
    try {
      const unassignedImage = await postApi.unassignTripImage(tripId, imageId);
      setPosts((prev) => sortPosts(prev.map((post) => (
        post.id === selectedPost.id
          ? { ...post, images: (post.images ?? []).filter((image) => image.id !== imageId) }
          : post
      ))));
      setTripImages((prev) => {
        const exists = prev.some((image) => image.id === imageId);
        if (!exists && targetImage) {
          return [...prev, { ...toTripImage(targetImage), postId: undefined }];
        }
        return prev.map((image) => (
          image.id === imageId ? { ...unassignedImage, postId: undefined } : image
        ));
      });
      showToast('이미지를 미배치 상태로 옮겼습니다.');
    } catch (error) {
      showToast(error instanceof Error ? error.message : '이미지 연결 해제에 실패했습니다.', 'error');
    }
  };

  const handleRepresentativeImageDrop = (event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    setRepresentativeDragOver(false);

    const imageId = event.dataTransfer.getData('application/triptrace-image-id') || event.dataTransfer.getData('text/plain');
    if (!imageId) return;

    const existsInTrip = tripImages.some((image) => image.id === imageId)
      || posts.some((post) => post.images?.some((image) => image.id === imageId));
    if (!existsInTrip) {
      showToast('Trip에 속한 이미지만 대표 이미지로 지정할 수 있습니다.', 'error');
      return;
    }

    const postImage = posts
      .flatMap((post) => post.images?.map((image) => ({ image, postId: post.id })) ?? [])
      .find(({ image }) => image.id === imageId);
    if (postImage && !tripImages.some((image) => image.id === imageId)) {
      setTripImages((prev) => [...prev, toTripImage(postImage.image, postImage.postId)]);
    }

    setRepresentativeImageId(imageId);
    showToast('대표 이미지로 선택했습니다. 저장 버튼을 누르면 반영됩니다.');
  };

  const handlePostChange = (updated: Post) => {
    const nextPost = withDerivedPostTime(updated);
    setSelectedPostId(nextPost.id);
    setPosts((prev) => sortPosts(prev.map((p) => (p.id === nextPost.id ? nextPost : p))));
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
  };

  const handleMarkerUpdated = (marker: Marker | null) => {
    if (!selectedPost) return;
    setPosts((prev) => sortPosts(prev.map((p) => (
      p.id === selectedPost.id ? withMarkerPostTime(p, marker) : p
    ))));
  };

  const renderPostEditor = (showUnassignedImages = false) => (
    selectedPost ? (
      <PostEditor
        tripId={tripId}
        post={selectedPost}
        unassignedImages={unassignedImages}
        onToast={showToast}
        onAssignTripImage={handleAssignTripImage}
        onUnassignPostImage={handleUnassignPostImage}
        onChange={handlePostChange}
        showUnassignedImages={showUnassignedImages}
      />
    ) : (
      <div className="flex items-center justify-center h-full text-gray-400 text-sm">
        Post를 선택하세요.
      </div>
    )
  );

  const renderMarkerEditor = () => (
    selectedPost ? (
      <MarkerEditor
        key={selectedPost.id}
        post={selectedPost}
        onMarkerUpdated={handleMarkerUpdated}
      />
    ) : (
      <div className="flex items-center justify-center h-full text-gray-400 text-sm p-4 text-center">
        Post를 선택하면 마커를 편집할 수 있습니다.
      </div>
    )
  );

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <p className="text-gray-400 text-sm">불러오는 중...</p>
      </div>
    );
  }

  return (
    <div className="flex h-[calc(100dvh_-_56px_-_72px_-_env(safe-area-inset-bottom))] flex-col overflow-hidden md:h-[calc(100vh_-_64px)]">
      {toast && (
        <div className={`fixed right-6 top-20 z-50 rounded-lg px-4 py-2 text-sm font-semibold shadow-lg ${
          toast.tone === 'success' ? 'bg-gray-900 text-white' : 'bg-red-50 text-red-600 ring-1 ring-red-100'
        }`}>
          {toast.message}
        </div>
      )}

      {/* Trip 편집 헤더 */}
      <div className="flex-shrink-0 border-b border-gray-200 bg-white px-4 py-3 md:px-6">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
          <div className="flex min-w-0 flex-1 items-start gap-2 sm:gap-3">
          <div
            onDragOver={(event) => {
              event.preventDefault();
              setRepresentativeDragOver(true);
            }}
            onDragLeave={() => setRepresentativeDragOver(false)}
            onDrop={handleRepresentativeImageDrop}
            className={`flex flex-shrink-0 items-center gap-2 rounded-lg border px-2 py-1.5 transition-colors ${
              representativeDragOver ? 'border-green-400 bg-green-50' : 'border-gray-100 bg-gray-50'
            }`}
            title="이미지를 끌어 대표 이미지로 지정"
          >
            <div className="h-11 w-11 overflow-hidden rounded-md bg-gray-200">
              {selectedRepresentativeImage?.thumbnailUrl || trip?.thumbnailUrl ? (
                <img src={selectedRepresentativeImage?.thumbnailUrl ?? trip?.thumbnailUrl} alt="" className="h-full w-full object-cover" />
              ) : (
                <div className="flex h-full w-full items-center justify-center">
                  <ImageIcon size={14} className="text-gray-400" />
                </div>
              )}
            </div>
            <div className="min-w-0">
              <p className="text-[10px] font-medium text-gray-400">대표 이미지</p>
              <p className="max-w-20 truncate text-xs font-semibold text-gray-700 sm:max-w-24">
                {representativeDragOver ? '여기에 놓기' : selectedRepresentativeImage ? '선택됨' : trip?.thumbnailUrl ? '자동 설정됨' : '이미지 드롭'}
              </p>
            </div>
          </div>
          <div className="min-w-0">
            <p className="text-[11px] font-semibold text-gray-400">Trip 수정/편집</p>
            <input
              value={tripForm.title}
              onChange={(e) => setTripForm({ ...tripForm, title: e.target.value })}
              placeholder="여행 제목"
              className="mt-0.5 w-full min-w-0 border-0 border-b border-transparent bg-transparent px-0 py-0 text-lg font-bold text-gray-900 outline-none placeholder:text-gray-300 focus:border-green-500 sm:text-xl md:text-2xl"
            />
          </div>
        </div>
          <div className="flex flex-shrink-0 items-center justify-end gap-2">
            <Link
              href={`/trips/${tripId}`}
              className="flex h-9 w-9 items-center justify-center rounded-lg border border-gray-200 text-gray-600 transition-colors hover:bg-gray-50 md:h-auto md:w-auto md:gap-1 md:px-3 md:py-1.5 md:text-sm"
              title="미리보기"
            >
              <Eye size={14} /> <span className="hidden md:inline">미리보기</span>
            </Link>
            <button
              onClick={handleDeleteTrip}
              className="flex h-9 w-9 items-center justify-center rounded-lg border border-red-100 text-red-500 transition-colors hover:bg-red-50 md:h-auto md:w-auto md:gap-1 md:px-3 md:py-1.5 md:text-sm"
              title="삭제"
            >
              <Trash2 size={14} /> <span className="hidden md:inline">삭제</span>
            </button>
            <button
              onClick={handleSaveTrip}
              disabled={saving}
              className="flex items-center gap-1 rounded-lg bg-green-600 px-3 py-2 text-sm font-semibold text-white transition-colors hover:bg-green-700 disabled:opacity-60 md:px-4 md:py-1.5"
            >
              <Save size={14} /> {saving ? '저장 중...' : '저장'}
            </button>
          </div>
        </div>

        <div className="mt-3 grid grid-cols-1 gap-2 md:flex md:items-center md:justify-between">
          <div className="grid min-w-0 grid-cols-2 gap-2 md:flex md:items-center">
          <div className="relative min-w-0 md:w-[104px] md:flex-shrink-0">
            <select
              value={tripForm.country}
              onChange={(e) => setTripForm({ ...tripForm, country: e.target.value })}
              className="w-full border border-gray-200 rounded-lg px-3 py-1.5 text-sm appearance-none outline-none focus:border-green-500 pr-7"
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
            className="min-w-0 border border-gray-200 rounded-lg px-3 py-1.5 text-sm outline-none focus:border-green-500 md:w-[104px] md:flex-shrink-0"
          />
          <div className="col-span-2 flex min-w-0 items-center gap-1 overflow-x-auto rounded-lg border border-gray-200 bg-white px-2 py-1.5 md:flex-shrink-0">
            <span className="text-[11px] font-medium text-gray-400">기간</span>
            <input
              type="date"
              value={tripForm.startDate}
              onChange={(e) => setTripForm({ ...tripForm, startDate: e.target.value })}
              className="min-w-0 flex-1 border-0 bg-transparent px-1 text-xs outline-none md:w-[112px] md:flex-none"
            />
            <span className="text-xs text-gray-300">-</span>
            <input
              type="date"
              value={tripForm.endDate}
              onChange={(e) => setTripForm({ ...tripForm, endDate: e.target.value })}
              className="min-w-0 flex-1 border-0 bg-transparent px-1 text-xs outline-none md:w-[112px] md:flex-none"
            />
          </div>
          </div>
          <div className="flex items-center gap-2 md:col-span-1">
            <button
              type="button"
              onClick={() => setTripForm({ ...tripForm, isPublic: !tripForm.isPublic })}
              className={`relative w-10 h-5 rounded-full transition-colors ${tripForm.isPublic ? 'bg-green-600' : 'bg-gray-300'}`}
            >
              <span className={`absolute top-0.5 left-0.5 w-4 h-4 bg-white rounded-full shadow transition-transform ${tripForm.isPublic ? 'translate-x-5' : ''}`} />
            </button>
            <span className="text-xs text-gray-600">{tripForm.isPublic ? '공개' : '비공개'}</span>
          </div>
        </div>
      </div>

      <div className="flex border-b border-gray-100 bg-white px-4 md:hidden">
        {[
          { id: 'posts', label: '기록' },
          { id: 'edit', label: '작성' },
          { id: 'location', label: '위치' },
        ].map((tab) => (
          <button
            key={tab.id}
            type="button"
            onClick={() => setMobileTab(tab.id as typeof mobileTab)}
            className={`flex-1 border-b-2 px-3 py-2 text-sm font-semibold transition-colors ${
              mobileTab === tab.id
                ? 'border-green-600 text-green-700'
                : 'border-transparent text-gray-400'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <div className="flex-1 overflow-hidden md:hidden">
        {mobileTab === 'posts' && (
          <PostList
            posts={posts}
            selectedId={selectedPostId}
            onSelect={(post) => {
              setSelectedPostId(post.id);
              setMobileTab('edit');
            }}
            onDelete={handleDeletePost}
            onCreate={handleCreatePost}
          />
        )}
        {mobileTab === 'edit' && renderPostEditor(true)}
        {mobileTab === 'location' && renderMarkerEditor()}
      </div>

      {/* 3컬럼 편집 영역 */}
      <div className="hidden flex-1 overflow-hidden md:flex">
        {/* 컬럼 1 - Post 목록 */}
        {postListOpen ? (
          <div className="w-[260px] border-r border-gray-100 bg-white flex-shrink-0 overflow-hidden">
            <PostList
              posts={posts}
              selectedId={selectedPostId}
              onSelect={(post) => setSelectedPostId(post.id)}
              onDelete={handleDeletePost}
              onCreate={handleCreatePost}
              onCollapse={() => setPostListOpen(false)}
            />
          </div>
        ) : (
          <div className="flex w-14 flex-shrink-0 flex-col items-center border-r border-gray-100 bg-white py-3">
            <button
              type="button"
              onClick={() => setPostListOpen(true)}
              className="flex h-9 w-9 items-center justify-center rounded-lg border border-gray-200 text-gray-500 hover:bg-gray-50"
              title="Post 목록 열기"
            >
              <PanelLeftOpen size={16} />
            </button>
            <span className="mt-4 [writing-mode:vertical-rl] text-[11px] font-semibold text-gray-400">
            Post 목록
          </span>
          </div>
        )}

        {unassignedPanelOpen ? (
          <div className="w-[220px] flex-shrink-0 overflow-hidden border-r border-gray-100 bg-gray-50/40">
            <div className="flex items-center justify-between border-b border-gray-100 bg-white px-4 py-3">
              <div>
                <h3 className="text-sm font-bold text-gray-900">미배치 이미지</h3>
                <p className="text-[11px] text-gray-400">{unassignedImages.length}장 대기 중</p>
              </div>
              <button
                type="button"
                onClick={() => setUnassignedPanelOpen(false)}
                className="rounded-md p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
                title="미배치 이미지 접기"
              >
                <PanelLeftClose size={14} />
              </button>
            </div>
            <div className="p-3">
              <UnassignedImageShelf images={unassignedImages} onAssign={handleAssignTripImage} compact />
            </div>
          </div>
        ) : (
          <div className="flex w-14 flex-shrink-0 flex-col items-center border-r border-gray-100 bg-white py-3">
            <button
              type="button"
              onClick={() => setUnassignedPanelOpen(true)}
              className="flex h-9 w-9 items-center justify-center rounded-lg border border-gray-200 text-gray-500 hover:bg-gray-50"
              title="미배치 이미지 열기"
            >
              <ImageIcon size={16} />
            </button>
            <span className="mt-4 [writing-mode:vertical-rl] text-[11px] font-semibold text-gray-400">
              미배치 이미지
            </span>
          </div>
        )}

        {/* 컬럼 2 - Post 편집 */}
        <div className="flex-1 border-r border-gray-100 bg-white overflow-hidden">
          {renderPostEditor()}
        </div>

        {/* 컬럼 3 - Marker 편집 */}
        <div
          className="relative flex-shrink-0 overflow-hidden border-l border-gray-100 bg-white transition-[width] duration-200"
          style={{ width: markerPanelOpen ? markerPanelWidth : 56 }}
        >
          {markerPanelOpen && (
            <div
              onMouseDown={startMarkerPanelResize}
              className="absolute left-0 top-0 z-20 h-full w-2 cursor-col-resize bg-transparent hover:bg-green-100"
              title="마커 편집 패널 크기 조절"
            >
              <span className="absolute left-0 top-1/2 h-10 w-1 -translate-y-1/2 rounded-full bg-gray-200" />
            </div>
          )}
          <button
            type="button"
            onClick={() => setMarkerPanelOpen((open) => !open)}
            className={`absolute right-3 top-3 z-30 flex h-8 w-8 items-center justify-center rounded-lg border border-gray-200 bg-white text-gray-500 shadow-sm hover:bg-gray-50 ${
              markerPanelOpen ? '' : 'left-1/2 right-auto -translate-x-1/2'
            }`}
            title={markerPanelOpen ? '위치 패널 닫기' : '위치 패널 열기'}
          >
            <MapPin size={15} />
          </button>
          {!markerPanelOpen ? (
            <div className="flex h-full flex-col items-center justify-center gap-2 px-2 text-gray-400">
              <MapPin size={18} className={selectedPost?.marker ? 'text-green-600' : 'text-gray-300'} />
              <span className="[writing-mode:vertical-rl] text-[11px] font-medium tracking-normal">
                위치
              </span>
            </div>
          ) : renderMarkerEditor()}
        </div>
      </div>
    </div>
  );
}
