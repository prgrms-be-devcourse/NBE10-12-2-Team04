'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { ArrowLeft, Camera, CheckCircle } from 'lucide-react';
import { userApi } from '@/lib/api';
import type { User } from '@/types';

export default function UserMePage() {
  const router = useRouter();
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    userApi
      .getMe()
      .then((data) => setUser(data as User))
      .catch(() => setError('로그인이 필요합니다.'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-gray-400">불러오는 중...</div>
      </div>
    );
  }

  if (error || !user) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen gap-4">
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

  const rows = [
    { label: '아이디 (ID)', value: user.nickname },
    { label: '이메일 (Email)', value: user.email },
    { label: '닉네임', value: user.nickname },
    { label: '프로필 이미지', value: user.profileImageUrl ? '프로필 이미지' : '—' },
    {
      label: '회원 상태',
      value: (
        <span className="text-green-600 font-medium">
          {user.status === 'ACTIVE' ? '활동중' : '비활성'}
        </span>
      ),
    },
    { label: '가입 일시', value: user.createdAt },
    { label: '회원 정보 수정 일시', value: user.updatedAt },
  ];

  return (
    <div className="p-8 max-w-4xl">
      <button
        onClick={() => router.back()}
        className="flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700 mb-4"
      >
        <ArrowLeft size={16} />
        <span>사용자 정보</span>
      </button>
      <p className="text-gray-500 text-sm mb-6">내 계정 정보를 확인하고 관리할 수 있습니다.</p>

      <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-8 flex gap-10">
        {/* 왼쪽 - 프로필 */}
        <div className="flex flex-col items-center min-w-[160px]">
          <div className="relative w-28 h-28 mb-4">
            {user.profileImageUrl ? (
              <img
                src={user.profileImageUrl}
                alt="프로필"
                className="w-full h-full rounded-full object-cover"
              />
            ) : (
              <div className="w-full h-full rounded-full bg-gray-200 flex items-center justify-center text-gray-400 text-2xl font-bold">
                {user.nickname?.[0]?.toUpperCase()}
              </div>
            )}
            {/* TODO: 프로필 이미지 수정 API 연결 필요 */}
            <button className="absolute bottom-1 right-1 w-7 h-7 bg-gray-700 rounded-full flex items-center justify-center">
              <Camera size={14} className="text-white" />
            </button>
          </div>
          <p className="font-bold text-gray-900 text-base">{user.nickname}</p>
          <p className="text-sm text-gray-400 mt-1 mb-3">즐거운 여행을 기록하는 여행자 ✈️</p>
          <span className="flex items-center gap-1 text-xs text-green-600 border border-green-200 bg-green-50 px-3 py-1 rounded-full">
            <CheckCircle size={12} />
            활동중
          </span>
        </div>

        {/* 오른쪽 - 정보 테이블 */}
        <div className="flex-1">
          <table className="w-full text-sm">
            <tbody>
              {rows.map(({ label, value }) => (
                <tr key={label} className="border-b border-gray-50 last:border-0">
                  <td className="py-3 pr-6 text-gray-400 whitespace-nowrap w-40">{label}</td>
                  <td className="py-3 text-gray-800">{value}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
