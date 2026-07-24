'use client';

import { useEffect, useRef, useState, type ChangeEvent } from 'react';
import { useRouter } from 'next/navigation';
import { ArrowLeft, Camera, CheckCircle } from 'lucide-react';
import { authApi, userApi } from '@/lib/api';
import type { User } from '@/types';

function formatDateOnly(value?: string) {
  if (!value) return '—';
  return value.slice(0, 10).replaceAll('-', '.');
}

// 업로드 직후 받은 상대 경로(/images/profile/...)를 미리보기용 절대 URL로 변환한다.
function toAbsoluteUrl(value?: string) {
  if (!value) return '';
  if (value.startsWith('http://') || value.startsWith('https://')) return value;
  const base = process.env.NEXT_PUBLIC_API_URL ?? '';
  return value.startsWith('/') ? (base ? `${base}${value}` : value) : value;
}

export default function UserMePage() {
  const router = useRouter();
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [editing, setEditing] = useState(false);
  const [nickname, setNickname] = useState('');
  const [intro, setIntro] = useState('');
  const [pendingImageUrl, setPendingImageUrl] = useState<string | undefined>(undefined);
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [formError, setFormError] = useState('');
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    userApi
      .getMe()
      .then((data) => setUser(data as User))
      .catch(() => setError('로그인이 필요합니다.'))
      .finally(() => setLoading(false));
  }, []);

  function startEdit() {
    if (!user) return;
    setNickname(user.nickname ?? '');
    setIntro(user.intro ?? '');
    setPendingImageUrl(undefined);
    setFormError('');
    setEditing(true);
  }

  function cancelEdit() {
    setEditing(false);
    setFormError('');
    setPendingImageUrl(undefined);
  }

  async function handleImageChange(e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true);
    setFormError('');
    try {
      const formData = new FormData();
      formData.append('image', file);
      const result = await authApi.uploadProfileImage(formData);
      setPendingImageUrl(result.profileImageUrl);
    } catch (err) {
      setFormError(err instanceof Error ? err.message : '이미지 업로드에 실패했습니다.');
    } finally {
      setUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  }

  async function handleSave() {
    if (!nickname.trim()) {
      setFormError('닉네임을 입력해주세요.');
      return;
    }
    setSaving(true);
    setFormError('');
    try {
      const updated = await userApi.updateMe({
        username: nickname.trim(),
        intro,
        ...(pendingImageUrl ? { profileImageUrl: pendingImageUrl } : {}),
      });
      setUser(updated as User);
      setEditing(false);
      setPendingImageUrl(undefined);
    } catch (err) {
      setFormError(err instanceof Error ? err.message : '회원 정보 수정에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-gray-400">불러오는 중...</div>
      </div>
    );
  }

  if (error || !user) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-4 px-4 text-center">
        <p className="text-gray-500">{error || '사용자 정보를 불러올 수 없습니다.'}</p>
        <button
          onClick={() => router.push('/auth/login')}
          className="bg-green-600 text-white px-4 py-2 rounded-lg text-sm"
        >
          로그인하러 가기
        </button>
      </div>
    );
  }

  const displayImageUrl = editing && pendingImageUrl ? toAbsoluteUrl(pendingImageUrl) : user.profileImageUrl;
  const displayNickname = editing ? nickname : user.nickname;

  return (
    <div className="mx-auto max-w-4xl px-4 py-5 sm:p-8">
      <button
        onClick={() => router.back()}
        className="flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700 mb-4"
      >
        <ArrowLeft size={16} />
        <span>사용자 정보</span>
      </button>

      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <p className="text-gray-500 text-sm">내 계정 정보를 확인하고 관리할 수 있습니다.</p>
        {editing ? (
          <div className="flex gap-2">
            <button
              onClick={cancelEdit}
              disabled={saving}
              className="px-4 py-2 rounded-lg text-sm border border-gray-200 text-gray-600 hover:bg-gray-50 disabled:opacity-50"
            >
              취소
            </button>
            <button
              onClick={handleSave}
              disabled={saving || uploading}
              className="px-4 py-2 rounded-lg text-sm bg-green-600 text-white hover:bg-green-700 disabled:opacity-50"
            >
              {saving ? '저장 중...' : '저장'}
            </button>
          </div>
        ) : (
          <button
            onClick={startEdit}
            className="px-4 py-2 rounded-lg text-sm border border-gray-200 text-gray-700 hover:bg-gray-50"
          >
            정보 수정
          </button>
        )}
      </div>

      {formError && (
        <p className="mb-4 text-sm text-red-500 bg-red-50 border border-red-100 rounded-lg px-4 py-2">
          {formError}
        </p>
      )}

      <div className="flex flex-col gap-8 rounded-2xl border border-gray-100 bg-white p-5 shadow-sm sm:p-8 md:flex-row md:gap-10">
        {/* 왼쪽 - 프로필 */}
        <div className="flex min-w-0 flex-col items-center md:min-w-[160px]">
          <div className="relative w-28 h-28 mb-4">
            {displayImageUrl ? (
              <img
                src={displayImageUrl}
                alt="프로필"
                className="w-full h-full rounded-full object-cover"
              />
            ) : (
              <div className="w-full h-full rounded-full bg-gray-200 flex items-center justify-center text-gray-400 text-2xl font-bold">
                {displayNickname?.[0]?.toUpperCase()}
              </div>
            )}
            {editing && (
              <>
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  onChange={handleImageChange}
                  className="hidden"
                />
                <button
                  onClick={() => fileInputRef.current?.click()}
                  disabled={uploading}
                  title="프로필 이미지 변경"
                  className="absolute bottom-1 right-1 w-7 h-7 bg-gray-700 rounded-full flex items-center justify-center disabled:opacity-50"
                >
                  <Camera size={14} className="text-white" />
                </button>
              </>
            )}
          </div>
          <p className="font-bold text-gray-900 text-base">{displayNickname}</p>
          {editing ? (
            <input
              value={intro}
              onChange={(e) => setIntro(e.target.value)}
              maxLength={100}
              placeholder="소개를 입력해보세요"
              className="mt-2 mb-3 w-full text-sm text-center text-gray-600 border border-gray-200 rounded-lg px-2 py-1 focus:outline-none focus:ring-1 focus:ring-green-500"
            />
          ) : (
            <p className="text-sm text-gray-400 mt-1 mb-3 text-center">
              {user.intro || '소개를 입력해보세요'}
            </p>
          )}
          <span className="flex items-center gap-1 text-xs text-green-600 border border-green-200 bg-green-50 px-3 py-1 rounded-full">
            <CheckCircle size={12} />
            활동중
          </span>
        </div>

        {/* 오른쪽 - 정보 테이블 */}
        <div className="min-w-0 flex-1 overflow-x-auto">
          <table className="w-full min-w-[520px] text-sm">
            <tbody>
              <tr className="border-b border-gray-50">
                <td className="py-3 pr-6 text-gray-400 whitespace-nowrap w-40">아이디 (ID)</td>
                <td className="py-3 text-gray-800">{user.nickname}</td>
              </tr>
              <tr className="border-b border-gray-50">
                <td className="py-3 pr-6 text-gray-400 whitespace-nowrap w-40">이메일 (Email)</td>
                <td className="py-3 text-gray-800">{user.email}</td>
              </tr>
              <tr className="border-b border-gray-50">
                <td className="py-3 pr-6 text-gray-400 whitespace-nowrap w-40">닉네임</td>
                <td className="py-3 text-gray-800">
                  {editing ? (
                    <input
                      value={nickname}
                      onChange={(e) => setNickname(e.target.value)}
                      minLength={2}
                      maxLength={50}
                      className="w-full max-w-xs border border-gray-200 rounded-lg px-2 py-1 focus:outline-none focus:ring-1 focus:ring-green-500"
                    />
                  ) : (
                    user.nickname
                  )}
                </td>
              </tr>
              <tr className="border-b border-gray-50">
                <td className="py-3 pr-6 text-gray-400 whitespace-nowrap w-40">소개</td>
                <td className="py-3 text-gray-800">
                  {editing ? (
                    <input
                      value={intro}
                      onChange={(e) => setIntro(e.target.value)}
                      maxLength={100}
                      placeholder="소개를 입력해보세요"
                      className="w-full border border-gray-200 rounded-lg px-2 py-1 focus:outline-none focus:ring-1 focus:ring-green-500"
                    />
                  ) : (
                    user.intro || '—'
                  )}
                </td>
              </tr>
              <tr className="border-b border-gray-50">
                <td className="py-3 pr-6 text-gray-400 whitespace-nowrap w-40">프로필 이미지</td>
                <td className="py-3 text-gray-800">{user.profileImageUrl ? '프로필 이미지' : '—'}</td>
              </tr>
              <tr className="border-b border-gray-50">
                <td className="py-3 pr-6 text-gray-400 whitespace-nowrap w-40">회원 상태</td>
                <td className="py-3 text-gray-800">
                  <span className="text-green-600 font-medium">
                    {user.status === 'ACTIVE' ? '활동중' : '비활성'}
                  </span>
                </td>
              </tr>
              <tr className="border-b border-gray-50">
                <td className="py-3 pr-6 text-gray-400 whitespace-nowrap w-40">가입 일시</td>
                <td className="py-3 text-gray-800">{formatDateOnly(user.createdAt)}</td>
              </tr>
              <tr className="last:border-0">
                <td className="py-3 pr-6 text-gray-400 whitespace-nowrap w-40">회원 정보 수정 일시</td>
                <td className="py-3 text-gray-800">{formatDateOnly(user.updatedAt)}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
