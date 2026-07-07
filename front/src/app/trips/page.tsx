'use client';

import { Dispatch, SetStateAction, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { GoogleMap, Marker, useJsApiLoader } from '@react-google-maps/api';
import {
  Plus, X, ChevronDown, Upload, Trash2,
  CheckCircle, MoreVertical, FileText, Heart, Map as MapIcon,
} from 'lucide-react';
import { isAuthenticated, userApi, tripApi } from '@/lib/api';
import type { Trip, AutoRecordResult } from '@/types';

const googleMapsApiKey = process.env.NEXT_PUBLIC_GOOGLE_MAPS_API_KEY ?? '';
const GOOGLE_MAPS_SCRIPT_ID = 'triptrace-google-map-script';
const tripsMapContainerStyle = { width: '100%', height: '100%' };
const tripsMapOptions = {
  disableDefaultUI: true,
  zoomControl: true,
  clickableIcons: false,
  gestureHandling: 'greedy',
};

const fallbackCoords: Record<string, { lat: number; lng: number }> = {
  한국: { lat: 37.5665, lng: 126.978 },
  서울: { lat: 37.5665, lng: 126.978 },
  부산: { lat: 35.1796, lng: 129.0756 },
  제주: { lat: 33.4996, lng: 126.5312 },
  일본: { lat: 35.6762, lng: 139.6503 },
  도쿄: { lat: 35.6762, lng: 139.6503 },
  오사카: { lat: 34.6937, lng: 135.5023 },
  교토: { lat: 35.0116, lng: 135.7681 },
  프랑스: { lat: 48.8566, lng: 2.3522 },
  파리: { lat: 48.8566, lng: 2.3522 },
  미국: { lat: 40.7128, lng: -74.006 },
  뉴욕: { lat: 40.7128, lng: -74.006 },
  그리스: { lat: 37.9838, lng: 23.7275 },
  산토리니: { lat: 36.3932, lng: 25.4615 },
  베트남: { lat: 16.0544, lng: 108.2022 },
  다낭: { lat: 16.0544, lng: 108.2022 },
  스페인: { lat: 40.4168, lng: -3.7038 },
  마드리드: { lat: 40.4168, lng: -3.7038 },
};

// ────────────────────────────────────────────────────────────────────
// Step 1: 기본 정보 입력
// ────────────────────────────────────────────────────────────────────
interface BasicInfo {
  title: string;
  country: string;
  city: string;
  startDate: string;
  endDate: string;
  isPublic: boolean;
}

function getTodayDateInput() {
  const today = new Date();
  const year = today.getFullYear();
  const month = String(today.getMonth() + 1).padStart(2, '0');
  const day = String(today.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function Step1Basic({
  data,
  onChange,
  formRef,
}: {
  data: BasicInfo;
  onChange: Dispatch<SetStateAction<BasicInfo>>;
  formRef: { current: HTMLFormElement | null };
}) {
  const set = (k: keyof BasicInfo, v: string | boolean) => onChange((prev) => ({ ...prev, [k]: v }));

  return (
    <form ref={formRef} className="flex flex-col gap-4">
      <p className="text-sm text-gray-500">기본 정보 입력</p>
      <p className="text-xs text-gray-400">Trip에 필요한 기본 정보를 입력해주세요.</p>

      <div>
        <label className="block text-xs font-medium text-gray-700 mb-1">여행 제목</label>
        <input
          name="title"
          placeholder="예) 5박 6일 도쿄 여행"
          value={data.title}
          onChange={(e) => set('title', e.target.value)}
          className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:border-green-500 focus:ring-1 focus:ring-green-500"
        />
      </div>

      <div className="sr-only">
        <label className="block text-xs font-medium text-gray-700 mb-1">국가</label>
        <div className="relative">
          <select
            name="country"
            value={data.country}
            onChange={(e) => set('country', e.target.value)}
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:border-green-500 appearance-none bg-white"
          >
            <option value="">국가를 선택하세요</option>
            <option value="한국">한국</option>
            <option value="일본">일본</option>
            <option value="프랑스">프랑스</option>
            <option value="미국">미국</option>
            <option value="그리스">그리스</option>
            <option value="베트남">베트남</option>
            <option value="스페인">스페인</option>
          </select>
          <ChevronDown size={14} className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
        </div>
      </div>

      <div className="sr-only">
        <label className="block text-xs font-medium text-gray-700 mb-1">도시</label>
        <input
          name="city"
          placeholder="도시를 입력하세요"
          value={data.city}
          onChange={(e) => set('city', e.target.value)}
          className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:border-green-500 focus:ring-1 focus:ring-green-500"
        />
      </div>

      <div className="sr-only flex gap-3">
        <div className="flex-1">
          <label className="block text-xs font-medium text-gray-700 mb-1">시작일</label>
          <input
            name="startDate"
            type="date"
            value={data.startDate}
            onChange={(e) => set('startDate', e.target.value)}
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:border-green-500"
          />
        </div>
        <div className="flex-1">
          <label className="block text-xs font-medium text-gray-700 mb-1">종료일</label>
          <input
            name="endDate"
            type="date"
            value={data.endDate}
            onChange={(e) => set('endDate', e.target.value)}
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:border-green-500"
          />
        </div>
      </div>

      <div className="rounded-lg bg-gray-50 px-3 py-2 text-xs text-gray-500">
        여행지와 날짜는 기본값으로 시작하고, 편집 화면에서 필요할 때 바꿀 수 있습니다.
      </div>

      <div className="hidden">
        <label className="block text-xs font-medium text-gray-700 mb-2">공개 여부</label>
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={() => set('isPublic', !data.isPublic)}
            className={`relative w-11 h-6 rounded-full transition-colors ${data.isPublic ? 'bg-green-600' : 'bg-gray-300'}`}
          >
            <span
              className={`absolute top-0.5 left-0.5 w-5 h-5 bg-white rounded-full shadow transition-transform ${data.isPublic ? 'translate-x-5' : ''}`}
            />
          </button>
          <span className="text-sm text-gray-600">{data.isPublic ? '공개' : '비공개'}</span>
        </div>
        <p className="text-xs text-gray-400 mt-1">다른 사용자에게 공개할지 선택하세요.</p>
      </div>
    </form>
  );
}

// ────────────────────────────────────────────────────────────────────
// Step 2: 이미지 업로드
// ────────────────────────────────────────────────────────────────────
interface UploadedFile {
  file: File;
  status: 'uploading' | 'done' | 'error';
  errorMessage?: string;
}

function Step2Images({
  tripId,
  ownerId,
  files,
  onChange,
}: {
  tripId: string;
  ownerId: string;
  files: UploadedFile[];
  onChange: (f: UploadedFile[]) => void;
}) {
  const inputRef = useRef<HTMLInputElement>(null);

  const handleAdd = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const newFiles = Array.from(e.target.files ?? []);
    if (!newFiles.length) return;

    const added: UploadedFile[] = newFiles.map((f) => ({ file: f, status: 'uploading' }));
    onChange([...files, ...added]);

    // 업로드 진행
    for (const uf of added) {
      const fd = new FormData();
      fd.append('images', uf.file);
      try {
        const result = await tripApi.uploadImages(tripId, ownerId, fd);
        const uploaded = result[0];
        if (!uploaded || uploaded.uploadStatus === 'FAILED') {
          uf.status = 'error';
          uf.errorMessage = 'JPEG 파일이 아니거나 이미지 메타데이터/저장 처리에 실패했습니다.';
        } else {
          uf.status = 'done';
          uf.errorMessage = undefined;
        }
      } catch (e) {
        uf.status = 'error';
        uf.errorMessage = e instanceof Error ? e.message : '이미지 업로드에 실패했습니다.';
      }
    }
    onChange([...files, ...added]);
    e.target.value = '';
  };

  const remove = (i: number) => onChange(files.filter((_, idx) => idx !== i));

  const formatSize = (bytes: number) =>
    bytes > 1024 * 1024 ? `${(bytes / 1024 / 1024).toFixed(1)}MB` : `${Math.round(bytes / 1024)}KB`;

  return (
    <div className="flex flex-col gap-5">
      {/* Trip 정보 요약 */}
      <div className="bg-gray-50 rounded-xl p-4">
        <p className="text-xs font-semibold text-gray-500 mb-2">Trip 정보</p>
        <p className="text-sm font-bold text-gray-900">이미지를 업로드하여 기록을 생성하세요.</p>
      </div>

      <div className="flex gap-4">
        {/* 업로드 영역 */}
        <button
          type="button"
          onClick={() => inputRef.current?.click()}
          className="flex-1 border-2 border-dashed border-gray-300 rounded-xl p-6 flex flex-col items-center justify-center gap-2 hover:border-green-400 hover:bg-green-50 transition-colors min-h-[160px]"
        >
          <Upload size={28} className="text-gray-400" />
          <p className="text-sm text-gray-500 font-medium">이미지를 드래그하거나</p>
          <p className="text-sm text-gray-500">클릭해서 업로드하세요</p>
          <p className="text-xs text-gray-400">JPG 이미지 업로드</p>
        </button>
        <input ref={inputRef} type="file" multiple accept="image/jpeg,image/jpg" className="hidden" onChange={handleAdd} />

        {/* 업로드된 파일 목록 */}
        <div className="flex-1 flex flex-col gap-2 max-h-[200px] overflow-y-auto">
          {files.length === 0 ? (
            <p className="text-xs text-gray-400 text-center mt-8">업로드된 이미지가 없습니다</p>
          ) : (
            files.map((uf, i) => (
              <div key={i} className="flex items-center gap-3 border border-gray-100 rounded-lg p-2.5 bg-white">
                <div className="w-10 h-10 rounded-md bg-gray-200 flex-shrink-0 overflow-hidden">
                  <img src={URL.createObjectURL(uf.file)} alt="" className="w-full h-full object-cover" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-xs font-medium text-gray-700 truncate">{uf.file.name}</p>
                  <p className="text-xs text-gray-400">{formatSize(uf.file.size)}</p>
                  <span className={`text-[10px] px-1.5 py-0.5 rounded-full font-medium ${
                    uf.status === 'done' ? 'bg-green-100 text-green-700' :
                    uf.status === 'error' ? 'bg-red-100 text-red-600' :
                    'bg-gray-100 text-gray-500'
                  }`}>
                    {uf.status === 'done' ? '업로드 완료' : uf.status === 'error' ? '실패' : '업로드 중...'}
                  </span>
                  {uf.status === 'error' && uf.errorMessage && (
                    <p className="mt-1 line-clamp-2 text-[11px] text-red-500">{uf.errorMessage}</p>
                  )}
                </div>
                <button onClick={() => remove(i)} className="text-gray-300 hover:text-red-400 transition-colors">
                  <Trash2 size={14} />
                </button>
              </div>
            ))
          )}
        </div>
      </div>

      <p className="text-xs text-gray-400">여러 장 선택 가능 (최대 50장, 장당 10MB)</p>
      {files.some((f) => f.status === 'done') && (
        <p className="text-xs text-green-600 flex items-center gap-1">
          <CheckCircle size={12} /> 이미지 업로드가 완료되면 자동 기록 생성을 진행할 수 있습니다.
        </p>
      )}
    </div>
  );
}

// ────────────────────────────────────────────────────────────────────
// Step 3: 자동 기록 생성 결과
// ────────────────────────────────────────────────────────────────────
function Step3AutoRecord({ result }: { result: AutoRecordResult | null }) {
  if (!result) return (
    <div className="flex items-center justify-center min-h-[200px]">
      <p className="text-sm text-gray-400">자동 기록을 생성하는 중입니다...</p>
    </div>
  );

  const generatedPostCount = result.totalRecords ?? result.generatedPostCount ?? 0;
  const generatedMarkerCount = result.totalMarkers ?? result.generatedMarkerCount ?? 0;
  const usedImageCount = result.usedImages ?? result.usedImageCount ?? 0;
  const skippedImageCount = result.excludedImages ?? result.skippedImageCount ?? 0;
  const records = result.records ?? [];

  return (
    <div className="flex flex-col gap-4">
      <div className="bg-gray-50 rounded-xl p-4 text-sm">
        <p className="font-semibold text-gray-700 mb-3">자동 기록 생성 결과</p>
        <div className="grid grid-cols-4 gap-2">
          {[
            { label: '생성된 기록', value: generatedPostCount, icon: FileText },
            { label: '생성된 마커', value: generatedMarkerCount, icon: CheckCircle },
            { label: '사용 이미지', value: usedImageCount, icon: Upload },
            { label: '제외 이미지', value: skippedImageCount, icon: X },
          ].map(({ label, value, icon: Icon }) => (
            <div key={label} className="bg-white rounded-lg p-3 flex flex-col items-center gap-1">
              <Icon size={16} className="text-green-600" />
              <p className="text-lg font-bold text-gray-900">{value}개</p>
              <p className="text-[10px] text-gray-400 text-center">{label}</p>
            </div>
          ))}
        </div>
      </div>

      <div>
        <p className="text-xs font-semibold text-gray-500 mb-2">생성된 기록 목록</p>
        <div className="flex flex-col gap-2 max-h-[240px] overflow-y-auto">
          {records.map((rec, i) => (
            <div key={i} className="flex items-center gap-3 border border-gray-100 rounded-lg p-3 bg-white">
              <div className="aspect-square w-12 overflow-hidden rounded-md bg-gray-200 flex-shrink-0">
                {rec.representativeThumbnailUrl && (
                  <img src={rec.representativeThumbnailUrl} alt="" className="h-full w-full object-cover" />
                )}
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 text-xs text-gray-400 mb-0.5">
                  <span>{rec.date} {rec.dayOfWeek ? `(${rec.dayOfWeek})` : ''}</span>
                </div>
                <p className="text-sm font-semibold text-gray-900 truncate">{rec.title ?? `Post #${rec.postId ?? i + 1}`}</p>
                <p className="text-xs text-gray-400 flex items-center gap-1">
                  📍 {rec.location ?? `${rec.centerLat ?? '-'}, ${rec.centerLng ?? '-'}`}
                </p>
              </div>
              <span className="text-xs text-gray-400 flex-shrink-0">사용 이미지 {rec.imageCount ?? rec.imageIds?.length ?? 0}장</span>
            </div>
          ))}
        </div>
      </div>

      <p className="text-xs text-green-600 flex items-center gap-1">
        <CheckCircle size={12} /> 자동 기록 생성이 완료되었습니다. 수정/편집 화면에서 내용을 확인하고 편집할 수 있습니다.
      </p>
    </div>
  );
}

// ────────────────────────────────────────────────────────────────────
// 생성 모달
// ────────────────────────────────────────────────────────────────────
function CreateTripModal({ onClose }: { onClose: () => void }) {
  const router = useRouter();
  const today = getTodayDateInput();
  const [step, setStep] = useState<1 | 2 | 3>(() => {
    if (typeof window === 'undefined') return 1;
    if (sessionStorage.getItem('triptrace:createAutoResult')) return 3;
    if (sessionStorage.getItem('triptrace:createDraftTripId')) return 2;
    return 1;
  });
  const [basicInfo, setBasicInfo] = useState<BasicInfo>({
    title: '', country: '미정', city: '미정', startDate: today, endDate: today, isPublic: false,
  });
  const [createdTripId, setCreatedTripId] = useState<string | null>(() => (
    typeof window === 'undefined' ? null : sessionStorage.getItem('triptrace:createDraftTripId')
  ));
  const [createdOwnerId, setCreatedOwnerId] = useState<string | null>(() => (
    typeof window === 'undefined' ? null : sessionStorage.getItem('triptrace:createDraftOwnerId')
  ));
  const [uploadedFiles, setUploadedFiles] = useState<UploadedFile[]>([]);
  const [autoResult, setAutoResult] = useState<AutoRecordResult | null>(() => {
    if (typeof window === 'undefined') return null;
    const storedResult = sessionStorage.getItem('triptrace:createAutoResult');
    return storedResult ? JSON.parse(storedResult) as AutoRecordResult : null;
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const basicFormRef = useRef<HTMLFormElement>(null);

  const stepLabels = ['기본 정보', '이미지 업로드', '자동 기록 생성'];

  const handleStep1Next = async () => {
    const formInfo = {
      ...basicInfo,
      title: basicInfo.title.trim(),
      country: basicInfo.country.trim(),
      city: basicInfo.city.trim(),
      startDate: basicInfo.startDate.trim(),
      endDate: basicInfo.endDate.trim(),
    };

    setBasicInfo(formInfo);

    if (!formInfo.title || !formInfo.country || !formInfo.city || !formInfo.startDate || !formInfo.endDate) {
      setError('여행 제목을 입력해주세요.');
      return;
    }
    setError('');
    setLoading(true);
    try {
      const res = await tripApi.create(formInfo) as Trip;
      setCreatedTripId(res.id);
      setCreatedOwnerId(res.ownerId ?? '');
      if (typeof window !== 'undefined') {
        sessionStorage.setItem('triptrace:createDraftTripId', res.id);
        sessionStorage.setItem('triptrace:createDraftOwnerId', res.ownerId ?? '');
      }
      setStep(2);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Trip 생성에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleStep2Next = async () => {
    if (!createdTripId) return;
    if (!uploadedFiles.some((file) => file.status === 'done')) {
      setError('자동 기록 생성을 위해 이미지를 1장 이상 업로드해주세요.');
      return;
    }
    const storedResult = sessionStorage.getItem('triptrace:createAutoResult');
    if (storedResult) {
      setAutoResult(JSON.parse(storedResult) as AutoRecordResult);
      setStep(3);
      return;
    }
    setError('');
    setLoading(true);
    try {
      const res = await tripApi.generateAutoRecords(createdTripId);
      setAutoResult(res as AutoRecordResult);
      sessionStorage.setItem('triptrace:createAutoResult', JSON.stringify(res));
      setStep(3);
    } catch (e) {
      setError(e instanceof Error ? e.message : '자동 기록 생성에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleFinish = () => {
    if (typeof window !== 'undefined') {
      sessionStorage.removeItem('triptrace:createDraftTripId');
      sessionStorage.removeItem('triptrace:createDraftOwnerId');
      sessionStorage.removeItem('triptrace:createAutoResult');
    }
    if (createdTripId) router.push(`/trips/${createdTripId}/edit`);
    else onClose();
  };

  const handleManualStart = async () => {
    if (createdTripId) {
      handleFinish();
      return;
    }
    const title = basicInfo.title.trim() || '새 여행';
    setLoading(true);
    setError('');
    try {
      const res = await tripApi.create({ ...basicInfo, title }) as Trip;
      router.push(`/trips/${res.id}/edit`);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Trip 생성에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-[680px] max-h-[90vh] overflow-y-auto mx-4">
        {/* 모달 헤더 */}
        <div className="flex items-center justify-between px-6 pt-6 pb-4 border-b border-gray-100">
          <h2 className="text-lg font-bold text-gray-900">새 Trip 만들기</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 transition-colors">
            <X size={20} />
          </button>
        </div>

        {/* 스텝 표시 */}
        <div className="flex items-center gap-0 px-6 pt-5 pb-4">
          {stepLabels.map((label, i) => {
            const s = (i + 1) as 1 | 2 | 3;
            const isDone = step > s;
            const isCurrent = step === s;
            return (
              <div key={s} className="flex items-center">
                <div className="flex items-center gap-1.5">
                  <div className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold ${
                    isDone ? 'bg-green-600 text-white' :
                    isCurrent ? 'bg-green-600 text-white' : 'bg-gray-200 text-gray-500'
                  }`}>
                    {isDone ? <CheckCircle size={14} /> : s}
                  </div>
                  <span className={`text-xs font-medium ${isCurrent ? 'text-green-700' : 'text-gray-400'}`}>{label}</span>
                </div>
                {i < stepLabels.length - 1 && (
                  <div className={`w-12 h-px mx-2 ${step > s ? 'bg-green-400' : 'bg-gray-200'}`} />
                )}
              </div>
            );
          })}
        </div>

        {/* 스텝 콘텐츠 */}
        <div className="px-6 pb-4">
          {step === 1 && <Step1Basic data={basicInfo} onChange={setBasicInfo} formRef={basicFormRef} />}
          {step === 2 && createdTripId && createdOwnerId && (
            <Step2Images tripId={createdTripId} ownerId={createdOwnerId} files={uploadedFiles} onChange={setUploadedFiles} />
          )}
          {step === 3 && <Step3AutoRecord result={autoResult} />}

          {error && <p className="text-sm text-red-500 mt-3">{error}</p>}
        </div>

        {/* 푸터 버튼 */}
        <div className="flex justify-between items-center px-6 py-4 border-t border-gray-100">
          {step === 1 ? (
            <button onClick={handleManualStart} disabled={loading} className="text-sm text-gray-600 hover:text-gray-800 px-4 py-2 rounded-lg border border-gray-200 hover:bg-gray-50 disabled:opacity-60">
              수동으로 시작
            </button>
          ) : (
            <button
              onClick={() => setStep((s) => (s - 1) as 1 | 2 | 3)}
              className="text-sm text-gray-500 hover:text-gray-700 px-4 py-2 rounded-lg border border-gray-200 hover:bg-gray-50"
            >
              이전
            </button>
          )}

          {step === 1 && (
            <button onClick={handleStep1Next} disabled={loading} className="bg-green-600 hover:bg-green-700 disabled:opacity-60 text-white text-sm font-semibold px-5 py-2 rounded-lg transition-colors">
              {loading ? '생성 중...' : 'Trip 생성 후 이미지 업로드'}
            </button>
          )}
          {step === 2 && (
            <button onClick={handleStep2Next} disabled={loading || !uploadedFiles.some((file) => file.status === 'done')} className="bg-green-600 hover:bg-green-700 disabled:opacity-60 text-white text-sm font-semibold px-5 py-2 rounded-lg transition-colors">
              {loading ? '생성 중...' : '자동 기록 생성하기'}
            </button>
          )}
          {step === 3 && (
            <button onClick={handleFinish} className="bg-green-600 hover:bg-green-700 text-white text-sm font-semibold px-5 py-2 rounded-lg transition-colors">
              수정/편집 화면으로 이동
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

function getTripMapPosition(trip: Trip) {
  const fallback = fallbackCoords[trip.city] ?? fallbackCoords[trip.country] ?? fallbackCoords['한국'];

  return {
    lat: trip.representativeLat ?? fallback.lat,
    lng: trip.representativeLng ?? fallback.lng,
  };
}

function getTripMapPositions(trips: Trip[]) {
  const grouped = new Map<string, Array<{ trip: Trip; base: { lat: number; lng: number } }>>();

  trips.forEach((trip) => {
    const base = getTripMapPosition(trip);
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
      const radius = 0.035 + Math.floor(index / 8) * 0.018;
      positions.set(trip.id, {
        lat: base.lat + Math.sin(angle) * radius,
        lng: base.lng + Math.cos(angle) * radius,
      });
    });
  });

  return positions;
}

function TripsFallbackMapView({ trips }: { trips: Trip[] }) {
  return (
    <div className="rounded-2xl border border-gray-100 bg-white p-4 shadow-sm">
      <div className="relative h-[360px] overflow-hidden rounded-xl bg-gradient-to-br from-blue-100 via-sky-50 to-green-50">
        <div className="absolute inset-0 opacity-70 bg-[linear-gradient(25deg,transparent_0_28%,rgba(255,255,255,0.65)_28%_30%,transparent_30%_100%),linear-gradient(145deg,transparent_0_48%,rgba(255,255,255,0.75)_48%_50%,transparent_50%_100%)]" />
        {trips.map((trip, index) => {
          const top = 24 + (index % 4) * 16;
          const left = 18 + (index % 5) * 15;
          return (
            <Link
              key={trip.id}
              href={`/trips/${trip.id}`}
              className="absolute -translate-x-1/2 -translate-y-1/2 rounded-full bg-white px-3 py-2 text-xs font-semibold text-gray-700 shadow hover:text-green-700"
              style={{ top: `${top}%`, left: `${left}%` }}
            >
              <MapIcon size={13} className="mr-1 inline text-green-600" />
              {trip.city || trip.country || trip.title}
            </Link>
          );
        })}
      </div>
    </div>
  );
}

function TripsMapView({ trips }: { trips: Trip[] }) {
  const router = useRouter();
  const mapRef = useRef<google.maps.Map | null>(null);
  const positions = useMemo(() => getTripMapPositions(trips), [trips]);
  const { isLoaded, loadError } = useJsApiLoader({
    googleMapsApiKey,
    id: GOOGLE_MAPS_SCRIPT_ID,
  });

  const fitTripsMap = useCallback((map: google.maps.Map) => {
    if (trips.length === 0) {
      map.setCenter(fallbackCoords['한국']);
      map.setZoom(6);
      return;
    }

    const bounds = new google.maps.LatLngBounds();
    trips.forEach((trip) => bounds.extend(positions.get(trip.id) ?? getTripMapPosition(trip)));
    if (trips.length === 1) {
      map.setCenter(positions.get(trips[0].id) ?? getTripMapPosition(trips[0]));
      map.setZoom(7);
      return;
    }
    map.fitBounds(bounds, 72);
  }, [positions, trips]);

  useEffect(() => {
    if (!isLoaded || !mapRef.current) return;
    fitTripsMap(mapRef.current);
  }, [fitTripsMap, isLoaded]);

  if (!googleMapsApiKey || loadError) {
    return <TripsFallbackMapView trips={trips} />;
  }

  return (
    <div className="rounded-2xl border border-gray-100 bg-white p-4 shadow-sm">
      <div className="relative h-[420px] overflow-hidden rounded-xl bg-gradient-to-br from-blue-100 via-sky-50 to-green-50">
        {!isLoaded ? (
          <div className="flex h-full w-full items-center justify-center">
            <p className="text-xs text-gray-400">지도를 불러오는 중...</p>
          </div>
        ) : (
          <GoogleMap
            mapContainerStyle={tripsMapContainerStyle}
            center={trips[0] ? positions.get(trips[0].id) ?? getTripMapPosition(trips[0]) : fallbackCoords['한국']}
            zoom={6}
            options={tripsMapOptions}
            onLoad={(map) => {
              mapRef.current = map;
              fitTripsMap(map);
            }}
            onUnmount={() => {
              mapRef.current = null;
            }}
          >
            {trips.map((trip) => (
              <Marker
                key={trip.id}
                position={positions.get(trip.id) ?? getTripMapPosition(trip)}
                title={`${trip.title} - ${trip.city || trip.country}`}
                onClick={() => router.push(`/trips/${trip.id}`)}
                label={{
                  text: trip.city || trip.country || 'Trip',
                  color: '#064e3b',
                  fontSize: '11px',
                  fontWeight: '700',
                }}
              />
            ))}
          </GoogleMap>
        )}
      </div>
    </div>
  );
}

// ────────────────────────────────────────────────────────────────────
// 내 Trip 목록 페이지
// ────────────────────────────────────────────────────────────────────
export default function TripsPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [trips, setTrips] = useState<Trip[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const [page, setPage] = useState(0);
  const [message, setMessage] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [viewMode, setViewMode] = useState<'list' | 'map'>('list');
  const [mounted, setMounted] = useState(false);
  const shouldOpenFromQuery = searchParams.get('create') === '1';
  const shouldShowCreateModal = showModal || (mounted && shouldOpenFromQuery && isAuthenticated());

  useEffect(() => {
    const id = window.setTimeout(() => setMounted(true), 0);
    return () => window.clearTimeout(id);
  }, []);

  useEffect(() => {
    userApi
      .getMyTrips({ page: 0, size: 8 })
      .then((d) => {
        const nextTrips = d as Trip[];
        setTrips(nextTrips);
        setHasMore(nextTrips.length >= 8);
        setPage(0);
      })
      .catch(() => setMessage('내 Trip 목록을 불러오지 못했습니다.'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (!mounted) return;
    if (!shouldOpenFromQuery) return;

    if (!isAuthenticated()) {
      router.replace('/auth/login');
    }
  }, [mounted, router, shouldOpenFromQuery]);

  const handleOpenCreateModal = () => {
    if (!isAuthenticated()) {
      router.push('/auth/login');
      return;
    }

    setShowModal(true);
  };

  const handleCloseCreateModal = () => {
    setShowModal(false);
    if (shouldOpenFromQuery) {
      router.replace('/trips');
    }
  };

  const loadMoreTrips = async () => {
    if (loadingMore || !hasMore) return;
    setLoadingMore(true);
    setMessage('');
    try {
      const nextPage = page + 1;
      const nextTrips = await userApi.getMyTrips({ page: nextPage, size: 8 }) as Trip[];
      setTrips((prev) => Array.from(new Map([...prev, ...nextTrips].map((trip) => [trip.id, trip])).values()));
      setHasMore(nextTrips.length >= 8);
      setPage(nextPage);
    } catch {
      setMessage('내 Trip 목록을 더 불러오지 못했습니다.');
    } finally {
      setLoadingMore(false);
    }
  };

  return (
    <div className="mx-auto max-w-[1040px] p-8">
      <div className="flex items-center justify-between mb-2">
        <div>
          <h1 className="text-xl font-bold text-gray-900">내 Trip</h1>
          <p className="text-sm text-gray-400 mt-0.5">내가 다녀온 여행을 기록하고 관리하세요.</p>
        </div>
        <button
          onClick={handleOpenCreateModal}
          className="flex items-center gap-1.5 bg-green-600 hover:bg-green-700 text-white text-sm font-semibold px-4 py-2 rounded-lg transition-colors"
        >
          <Plus size={16} /> 새 Trip 만들기
        </button>
      </div>

      <div className="mt-6 flex gap-2 border-b border-gray-100">
        {(['list', 'map'] as const).map((mode) => (
          <button
            key={mode}
            type="button"
            onClick={() => setViewMode(mode)}
            className={`px-4 py-2 text-sm font-semibold border-b-2 ${viewMode === mode ? 'border-green-600 text-green-700' : 'border-transparent text-gray-400'}`}
          >
            {mode === 'map' ? '지도 뷰' : '목록 뷰'}
          </button>
        ))}
      </div>

      {message && (
        <p className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-500">{message}</p>
      )}

      {viewMode === 'map' && !loading && trips.length > 0 && (
        <div className="mt-5">
          <TripsMapView trips={trips} />
        </div>
      )}

      <div className={`${viewMode === 'list' ? 'mt-5 grid' : 'mt-5 hidden'} grid-cols-2 gap-5`}>
        {loading ? (
          Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="h-[260px] rounded-2xl bg-gray-200 animate-pulse" />
          ))
        ) : trips.length === 0 ? (
          <div className="col-span-2 flex flex-col items-center justify-center py-20 text-gray-400">
            <p className="text-lg font-semibold mb-2">아직 Trip이 없어요</p>
            <p className="text-sm">+ 새 Trip 만들기를 눌러 첫 여행을 기록해보세요!</p>
          </div>
        ) : (
          trips.map((trip) => (
            <TripCard
              key={trip.id}
              trip={trip}
              onDeleted={() => setTrips((prev) => prev.filter((t) => t.id !== trip.id))}
              onDeleteError={(errorMessage) => setMessage(errorMessage)}
            />
          ))
        )}
      </div>

      {!loading && trips.length > 0 && hasMore && (
        <button
          onClick={loadMoreTrips}
          disabled={loadingMore}
          className="mx-auto mt-6 flex rounded-full border border-gray-200 px-8 py-2 text-sm font-semibold text-gray-600 hover:bg-gray-50 disabled:opacity-60"
        >
          {loadingMore ? '불러오는 중...' : '더보기'}
        </button>
      )}

      {shouldShowCreateModal && <CreateTripModal onClose={handleCloseCreateModal} />}
    </div>
  );
}

// ── Trip 카드 ─────────────────────────────────────────────────────────
function TripCard({
  trip,
  onDeleted,
  onDeleteError,
}: {
  trip: Trip;
  onDeleted: () => void;
  onDeleteError: (message: string) => void;
}) {
  const [menuOpen, setMenuOpen] = useState(false);

  const handleDelete = async () => {
    if (!confirm('이 Trip을 삭제하시겠습니까?')) return;
    try {
      await tripApi.delete(trip.id);
      onDeleted();
    } catch (error) {
      onDeleteError(error instanceof Error ? error.message : '삭제에 실패했습니다.');
    }
  };

  return (
    <div className="relative overflow-visible rounded-2xl border border-gray-100 bg-white shadow-sm transition-shadow hover:shadow-md">
      <Link href={`/trips/${trip.id}`} className="block h-[160px] overflow-hidden bg-gradient-to-br from-gray-300 to-gray-400 hover:opacity-90 transition-opacity">
        {trip.thumbnailUrl && (
          <img src={trip.thumbnailUrl} alt="" className="h-full w-full object-cover" />
        )}
      </Link>

      <div className="p-4">
        <div className="flex items-start justify-between">
          <div>
            <Link href={`/trips/${trip.id}`} className="font-bold text-gray-900 hover:text-green-700 transition-colors">
              {trip.city}, {trip.country}
            </Link>
            <p className="text-xs text-gray-400 mt-0.5">{trip.startDate} ~ {trip.endDate}</p>
          </div>
          <div className="relative z-20">
            <button onClick={() => setMenuOpen(!menuOpen)} className="text-gray-400 hover:text-gray-600 p-1">
              <MoreVertical size={16} />
            </button>
            {menuOpen && (
              <div className="absolute right-0 top-7 z-30 min-w-[100px] rounded-lg border border-gray-100 bg-white shadow-lg">
                <Link href={`/trips/${trip.id}/edit`} className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-50">편집</Link>
                <button onClick={handleDelete} className="block w-full text-left px-4 py-2 text-sm text-red-500 hover:bg-red-50">삭제</button>
              </div>
            )}
          </div>
        </div>

        <div className="flex items-center gap-3 mt-3 text-xs text-gray-400">
          <span className="flex items-center gap-1">
            <FileText size={12} /> 기록 {trip.recordCount ?? 0}개
          </span>
          <span className="flex items-center gap-1">
            <Heart size={12} /> {trip.likeCount ?? 0}
          </span>
        </div>
      </div>
    </div>
  );
}
