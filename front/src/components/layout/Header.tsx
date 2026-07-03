'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { LogOut, User } from 'lucide-react';
import { isAuthenticated, userApi } from '@/lib/api';

interface HeaderProps {
  rightSlot?: React.ReactNode;
}

export default function Header({ rightSlot }: HeaderProps) {
  const pathname = usePathname();
  const [loggedIn, setLoggedIn] = useState(false);
  const [profileImageUrl, setProfileImageUrl] = useState('');

  useEffect(() => {
    const syncAuthState = () => {
      const authed = isAuthenticated();
      setLoggedIn(authed);
      if (!authed) {
        setProfileImageUrl('');
        return;
      }

      userApi
        .getMe()
        .then((user) => setProfileImageUrl(typeof user.profileImageUrl === 'string' ? user.profileImageUrl : ''))
        .catch(() => setProfileImageUrl(''));
    };

    syncAuthState();
    window.addEventListener('auth-change', syncAuthState);

    return () => window.removeEventListener('auth-change', syncAuthState);
  }, [pathname]);

  return (
    <header className="fixed top-0 left-[72px] right-0 h-[64px] bg-white/95 backdrop-blur border-b border-gray-200 flex items-center px-8 z-30">
      <Link href="/" className="text-lg font-bold text-gray-900 mr-auto">
        TripTrace
      </Link>

      <div className="flex items-center gap-2">
        {rightSlot}
        {loggedIn ? (
          <>
            <Link
              href="/auth/logout"
              className="flex h-9 items-center gap-1.5 rounded-lg border border-gray-200 px-3 text-sm font-semibold text-gray-600 hover:bg-gray-50"
            >
              <LogOut size={15} />
              로그아웃
            </Link>
            <Link
              href="/users/me"
              className="flex h-9 items-center gap-1.5 rounded-lg bg-emerald-600 px-3 text-sm font-semibold text-white hover:bg-emerald-700"
            >
              {profileImageUrl ? (
                <img src={profileImageUrl} alt="" className="h-5 w-5 rounded-full object-cover bg-white/20" />
              ) : (
                <User size={15} />
              )}
              내정보
            </Link>
          </>
        ) : (
          <Link
            href="/auth/login"
            className="flex h-9 items-center rounded-lg bg-emerald-600 px-4 text-sm font-semibold text-white hover:bg-emerald-700"
          >
            로그인
          </Link>
        )}
      </div>
    </header>
  );
}
