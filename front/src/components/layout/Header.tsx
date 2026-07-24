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
        .then((user) => {
          setLoggedIn(true);
          setProfileImageUrl(typeof user.profileImageUrl === 'string' ? user.profileImageUrl : '');
        })
        .catch(() => {
          setLoggedIn(false);
          setProfileImageUrl('');
        });
    };

    syncAuthState();
    window.addEventListener('auth-change', syncAuthState);

    return () => window.removeEventListener('auth-change', syncAuthState);
  }, [pathname]);

  return (
    <header className="fixed left-0 right-0 top-0 z-30 flex h-[56px] items-center border-b border-gray-200 bg-white/95 px-4 backdrop-blur md:left-[72px] md:h-[64px] md:px-8">
      <Link href="/" className="mr-auto text-base font-bold text-gray-900 md:text-lg">
        TripTrace
      </Link>

      <div className="flex min-w-0 items-center gap-2">
        {rightSlot}
        {loggedIn ? (
          <>
            <Link
              href="/auth/logout"
              className="hidden h-9 items-center gap-1.5 rounded-lg border border-gray-200 px-3 text-sm font-semibold text-gray-600 hover:bg-gray-50 sm:flex"
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
              <span className="hidden sm:inline">내정보</span>
            </Link>
          </>
        ) : (
          <Link
            href="/auth/login"
            className="flex h-9 items-center rounded-lg bg-emerald-600 px-3 text-sm font-semibold text-white hover:bg-emerald-700 sm:px-4"
          >
            로그인
          </Link>
        )}
      </div>
    </header>
  );
}
