'use client';

import { useState, useRef } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Camera, Plus } from 'lucide-react';
import { authApi } from '@/lib/api';

export default function SignupPage() {
  const router = useRouter();
  const fileRef = useRef<HTMLInputElement>(null);

  const [form, setForm] = useState({
    email: '',
    username: '',
    password: '',
    passwordConfirm: '',
  });
  const [profilePreview, setProfilePreview] = useState<string | null>(null);
  const [profileFile, setProfileFile] = useState<File | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const url = URL.createObjectURL(file);
    setProfilePreview(url);
    setProfileFile(file);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (form.password !== form.passwordConfirm) {
      setError('비밀번호가 일치하지 않습니다.');
      return;
    }

    setLoading(true);
    try {
      let profileImageUrl: string | undefined;
      if (profileFile) {
        const formData = new FormData();
        formData.append('image', profileFile);
        const uploaded = await authApi.uploadProfileImage(formData);
        profileImageUrl = uploaded.profileImageUrl;
      }

      await authApi.signup({
        email: form.email,
        username: form.username,
        password: form.password,
        profileImageUrl,
      });
      router.push('/auth/login');
    } catch (err) {
      setError(err instanceof Error ? err.message : '회원가입에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-[calc(100dvh_-_56px_-_72px_-_env(safe-area-inset-bottom))] items-center justify-center px-4 py-6 md:min-h-screen md:py-12">
      <div className="w-full max-w-[420px] rounded-2xl border border-gray-100 bg-white p-5 shadow-sm sm:p-8">
        <p className="text-xs text-gray-400 mb-1">TripTrace</p>
        <h1 className="text-2xl font-bold text-gray-900 mb-1">회원가입</h1>
        <p className="text-sm text-gray-500 mb-6">나만의 여행 기록을 시작해보세요.</p>

        {/* 프로필 이미지 */}
        <div className="flex flex-col items-center mb-6">
          <button
            type="button"
            onClick={() => fileRef.current?.click()}
            className="relative w-20 h-20 rounded-full bg-gray-100 flex items-center justify-center hover:bg-gray-200 transition-colors"
          >
            {profilePreview ? (
              <img src={profilePreview} alt="프로필" className="w-full h-full rounded-full object-cover" />
            ) : (
              <Camera size={24} className="text-gray-400" />
            )}
            <span className="absolute bottom-0 right-0 w-6 h-6 bg-green-600 rounded-full flex items-center justify-center">
              <Plus size={14} className="text-white" />
            </span>
          </button>
          <p className="text-xs text-gray-400 mt-2">프로필 이미지 등록</p>
          <input ref={fileRef} type="file" accept="image/*" className="hidden" onChange={handleFileChange} />
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">이메일</label>
            <input
              type="email"
              placeholder="name@example.com"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              required
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:border-green-500 focus:ring-1 focus:ring-green-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">사용자 이름</label>
            <input
              type="text"
              placeholder="Traveler_shb"
              value={form.username}
              onChange={(e) => setForm({ ...form, username: e.target.value })}
              required
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:border-green-500 focus:ring-1 focus:ring-green-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">비밀번호</label>
            <input
              type="password"
              placeholder="••••••••"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              required
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:border-green-500 focus:ring-1 focus:ring-green-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">비밀번호 확인</label>
            <input
              type="password"
              placeholder="••••••••"
              value={form.passwordConfirm}
              onChange={(e) => setForm({ ...form, passwordConfirm: e.target.value })}
              required
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:border-green-500 focus:ring-1 focus:ring-green-500"
            />
          </div>

          {error && <p className="text-sm text-red-500">{error}</p>}

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-green-600 hover:bg-green-700 disabled:opacity-60 text-white font-semibold py-2.5 rounded-lg transition-colors"
          >
            {loading ? '처리 중...' : '가입하기'}
          </button>
        </form>

        <p className="text-center text-sm text-gray-500 mt-4">
          이미 계정이 있으신가요?{' '}
          <Link href="/auth/login" className="text-green-600 font-medium hover:underline">
            로그인
          </Link>
        </p>
      </div>
    </div>
  );
}
