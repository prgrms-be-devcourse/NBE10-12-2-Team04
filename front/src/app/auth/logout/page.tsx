'use client';

import { useEffect } from 'react';
import Link from 'next/link';
import { User } from 'lucide-react';
import { authApi } from '@/lib/api';

export default function LogoutPage() {
  useEffect(() => {
    // 페이지 진입 시 로그아웃 API 호출
    authApi.logout().catch(() => {
      // 이미 로그아웃된 경우 등 에러 무시
    });
  }, []);

  return (
    <div className="min-h-screen flex items-center justify-center">
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 w-full max-w-[380px] p-10 flex flex-col items-center text-center">
        <div className="w-16 h-16 rounded-full bg-green-50 flex items-center justify-center mb-6">
          <User size={32} className="text-green-600" />
        </div>

        <h1 className="text-xl font-bold text-gray-900 mb-2">로그아웃 되었습니다</h1>
        <p className="text-sm text-gray-500 mb-8">
          안전하게 로그아웃 되었습니다.
          <br />
          TripTrace에서 또 만나요!
        </p>

        <div className="flex flex-col gap-3 w-full">
          <Link
            href="/auth/login"
            className="w-full bg-green-600 hover:bg-green-700 text-white font-semibold py-2.5 rounded-lg text-center transition-colors"
          >
            로그인하기
          </Link>
          <Link
            href="/auth/signup"
            className="w-full border border-green-600 text-green-600 hover:bg-green-50 font-semibold py-2.5 rounded-lg text-center transition-colors"
          >
            회원가입
          </Link>
        </div>
      </div>
    </div>
  );
}
