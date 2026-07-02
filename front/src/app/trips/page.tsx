'use client';

import { useEffect, useState, useRef } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import {
  Plus, X, ChevronDown, Upload, Trash2,
  CheckCircle, MoreVertical, FileText, Heart,
} from 'lucide-react';
import { userApi, tripApi } from '@/lib/api';
import type { Trip, AutoRecordResult } from '@/types';

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

function Step1Basic({
  data,
  onChange,
}: {
  data: BasicInfo;
  onChange: (d: BasicInfo) => void;
}) {
  const set = (k: keyof BasicInfo, v: string | boolean) => onChange({ ...data, [k]: v });

  return (
    <div className="flex flex-col gap-4">
      <p className="text-sm text-gray-500">기본 정보 입력</p>
      <p className="text-xs text-gray-400">Trip에 필요한 기본 정보를 입력해주세요.</p>

      <div>
        <label className="block text-xs font-medium text-gray-700 mb-1">여행 제목</label>
        <input
          placeholder="예) 5박 6일 도쿄 여행"
          value={data.title}
          onChange={(e) => set('title', e.target.value)}
          className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:border-green-500 focus:ring-1 focus:ring-green-500"
        />
      </div>

      <div>
        <label className="block text-xs font-medium text-gray-700 mb-1">국가</label>
        <div className="relative">
          <select
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

      <div>
        <label className="block text-xs font-medium text-gray-700 mb-1">도시</label>
        <input
          placeholder="도시를 입력하세요"
          value={data.city}
          onChange={(e) => set('city', e.target.value)}
          className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:border-green-500 focus:ring-1 focus:ring-green-500"
        />
      </div>

      <div className="flex gap-3">
        <div className="flex-1">
          <label className="block text-xs font-medium text-gray-700 mb-1">시작일</label>
          <input
            type="date"
            value={data.startDate}
            onChange={(e) => set('startDate', e.target.value)}
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:border-green-500"
          />
        </div>
        <div className="flex-1">
          <label className="block text-xs font-medium text-gray-700 mb-1">종료일</label>
          <input
            type="date"
            value={data.endDate}
            onChange={(e) => set('endDate', e.target.value)}
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:border-green-500"
          />
        </div>
      </div>

      <div>
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
    </div>
  );
}

// ────────────────────────────────────────────────────────────────────
// Step 2: 이미지 업로드
// ────────────────────────────────────────────────────────────────────
interface UploadedFile {
  file: File;
  status: 'uploading' | 'done' | 'error';
}

function Step2Images({
  tripId,
  files,
  onChange,
}: {
  tripId: string;
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
        await tripApi.uploadImages(tripId, fd);
        uf.status = 'done';
      } catch {
        uf.status = 'error';
      }
    }
    onChange([...files, ...added]);
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

  return (
    <div className="flex flex-col gap-4">
      <div className="bg-gray-50 rounded-xl p-4 text-sm">
        <p className="font-semibold text-gray-700 mb-3">자동 기록 생성 결과</p>
        <div className="grid grid-cols-4 gap-2">
          {[
            { label: '생성된 기록', value: result.totalRecords, icon: FileText },
            { label: '생성된 마커', value: result.totalMarkers, icon: CheckCircle },
            { label: '사용 이미지', value: result.usedImages, icon: Upload },
            { label: '제외 이미지', value: result.excludedImages, icon: X },
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
          {result.records.map((rec, i) => (
            <div key={i} className="flex items-center gap-3 border border-gray-100 rounded-lg p-3 bg-white">
              <div className="w-12 h-12 bg-gray-200 rounded-md flex-shrink-0" />
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 text-xs text-gray-400 mb-0.5">
                  <span>{rec.date} ({rec.dayOfWeek})</span>
                </div>
                <p className="text-sm font-semibold text-gray-900 truncate">{rec.title}</p>
                <p className="text-xs text-gray-400 flex items-center gap-1">
                  📍 {rec.location}
                </p>
              </div>
              <span className="text-xs text-gray-400 flex-shrink-0">사용 이미지 {rec.imageCount}장</span>
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
  const [step, setStep] = useState<1 | 2 | 3>(1);
  const [basicInfo, setBasicInfo] = useState<BasicInfo>({
    title: '', country: '', city: '', startDate: '', endDate: '', isPublic: true,
  });
  const [createdTripId, setCreatedTripId] = useState<string | null>(null);
  const [uploadedFiles, setUploadedFiles] = useState<UploadedFile[]>([]);
  const [autoResult, setAutoResult] = useState<AutoRecordResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const stepLabels = ['기본 정보', '이미지 업로드', '자동 기록 생성'];

  const handleStep1Next = async () => {
    if (!basicInfo.title || !basicInfo.country || !basicInfo.city || !basicInfo.startDate || !basicInfo.endDate) {
      setError('모든 항목을 입력해주세요.');
      return;
    }
    setError('');
    setLoading(true);
    try {
      const res = await tripApi.create(basicInfo) as { id: string };
      setCreatedTripId(res.id);
      setStep(2);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Trip 생성에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleStep2Next = async () => {
    if (!createdTripId) return;
    setLoading(true);
    try {
      const res = await tripApi.generateAutoRecords(createdTripId);
      setAutoResult(res as AutoRecordResult);
      setStep(3);
    } catch (e) {
      setError(e instanceof Error ? e.message : '자동 기록 생성에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleFinish = () => {
    if (createdTripId) router.push(`/trips/${createdTripId}/edit`);
    else onClose();
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
          {step === 1 && <Step1Basic data={basicInfo} onChange={setBasicInfo} />}
          {step === 2 && createdTripId && (
            <Step2Images tripId={createdTripId} files={uploadedFiles} onChange={setUploadedFiles} />
          )}
          {step === 3 && <Step3AutoRecord result={autoResult} />}

          {error && <p className="text-sm text-red-500 mt-3">{error}</p>}
        </div>

        {/* 푸터 버튼 */}
        <div className="flex justify-between items-center px-6 py-4 border-t border-gray-100">
          {step === 1 ? (
            <button onClick={onClose} className="text-sm text-gray-500 hover:text-gray-700 px-4 py-2 rounded-lg border border-gray-200 hover:bg-gray-50">
              취소
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
            <button onClick={handleStep2Next} disabled={loading} className="bg-green-600 hover:bg-green-700 disabled:opacity-60 text-white text-sm font-semibold px-5 py-2 rounded-lg transition-colors">
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

// ────────────────────────────────────────────────────────────────────
// 내 Trip 목록 페이지
// ────────────────────────────────────────────────────────────────────
export default function TripsPage() {
  const [trips, setTrips] = useState<Trip[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);

  useEffect(() => {
    userApi
      .getMyTrips()
      .then((d) => setTrips(d as Trip[]))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="p-8 max-w-[900px]">
      <div className="flex items-center justify-between mb-2">
        <div>
          <h1 className="text-xl font-bold text-gray-900">내 Trip</h1>
          <p className="text-sm text-gray-400 mt-0.5">내가 다녀온 여행을 기록하고 관리하세요.</p>
        </div>
        <button
          onClick={() => setShowModal(true)}
          className="flex items-center gap-1.5 bg-green-600 hover:bg-green-700 text-white text-sm font-semibold px-4 py-2 rounded-lg transition-colors"
        >
          <Plus size={16} /> 새 Trip 만들기
        </button>
      </div>

      <div className="mt-6 grid grid-cols-2 gap-5">
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
            <TripCard key={trip.id} trip={trip} onDeleted={() => setTrips((prev) => prev.filter((t) => t.id !== trip.id))} />
          ))
        )}
      </div>

      {showModal && <CreateTripModal onClose={() => setShowModal(false)} />}
    </div>
  );
}

// ── Trip 카드 ─────────────────────────────────────────────────────────
function TripCard({ trip, onDeleted }: { trip: Trip; onDeleted: () => void }) {
  const [menuOpen, setMenuOpen] = useState(false);

  const handleDelete = async () => {
    if (!confirm('이 Trip을 삭제하시겠습니까?')) return;
    try {
      await tripApi.delete(trip.id);
      onDeleted();
    } catch {
      alert('삭제에 실패했습니다.');
    }
  };

  return (
    <div className="bg-white rounded-2xl overflow-hidden border border-gray-100 shadow-sm hover:shadow-md transition-shadow">
      {/* 썸네일 placeholder (thumbnailUrl 없음) */}
      <Link href={`/trips/${trip.id}`} className="block h-[160px] bg-gradient-to-br from-gray-300 to-gray-400 hover:opacity-90 transition-opacity" />

      <div className="p-4">
        <div className="flex items-start justify-between">
          <div>
            <Link href={`/trips/${trip.id}`} className="font-bold text-gray-900 hover:text-green-700 transition-colors">
              {trip.city}, {trip.country}
            </Link>
            <p className="text-xs text-gray-400 mt-0.5">{trip.startDate} ~ {trip.endDate}</p>
          </div>
          <div className="relative">
            <button onClick={() => setMenuOpen(!menuOpen)} className="text-gray-400 hover:text-gray-600 p-1">
              <MoreVertical size={16} />
            </button>
            {menuOpen && (
              <div className="absolute right-0 top-7 bg-white border border-gray-100 rounded-lg shadow-lg z-10 min-w-[100px]">
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
