'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { MapPin, Home, User, Settings, Image } from 'lucide-react';

const navItems = [
  { href: '/', icon: Home, label: '홈' },
  { href: '/photos', icon: Image, label: '앨범' },
  { href: '/users/me', icon: User, label: '내 정보' },
];

export default function Sidebar() {
  const pathname = usePathname();

  return (
    <>
    <aside className="fixed left-0 top-0 z-40 hidden h-full w-[72px] flex-col items-center border-r border-gray-200 bg-white py-4 md:flex">
      <Link href="/" className="mb-6">
        <div className="w-10 h-10 flex items-center justify-center rounded-2xl border border-gray-200 text-gray-900 shadow-sm">
          <MapPin size={22} strokeWidth={2.3} />
        </div>
      </Link>

      <nav className="flex flex-col items-center gap-1 flex-1">
        {navItems.map(({ href, icon: Icon, label }) => {
          const isActive =
            href === '/' ? pathname === '/' : pathname.startsWith(href);
          return (
            <Link
              key={href}
              href={href}
              title={label}
              className={`w-11 h-11 flex items-center justify-center rounded-2xl transition-colors ${
                isActive
                  ? 'bg-emerald-50 text-emerald-600'
                  : 'text-gray-400 hover:bg-gray-100 hover:text-gray-600'
              }`}
            >
              <Icon size={20} />
            </Link>
          );
        })}
      </nav>

      <Link
        href="/settings"
        title="설정"
        className="w-11 h-11 flex items-center justify-center rounded-2xl text-gray-400 hover:bg-gray-100 hover:text-gray-600 transition-colors"
      >
        <Settings size={20} />
      </Link>
    </aside>
    <nav className="fixed inset-x-0 bottom-0 z-50 border-t border-gray-200 bg-white/95 px-3 pb-[env(safe-area-inset-bottom)] pt-2 backdrop-blur md:hidden">
      <div className="mx-auto grid max-w-md grid-cols-3 gap-1">
        {navItems.map(({ href, icon: Icon, label }) => {
          const isActive =
            href === '/' ? pathname === '/' : pathname.startsWith(href);
          return (
            <Link
              key={href}
              href={href}
              className={`flex min-h-14 flex-col items-center justify-center gap-1 rounded-xl text-[11px] font-semibold transition-colors ${
                isActive
                  ? 'bg-emerald-50 text-emerald-600'
                  : 'text-gray-400 active:bg-gray-100'
              }`}
            >
              <Icon size={20} />
              {label}
            </Link>
          );
        })}
      </div>
    </nav>
    </>
  );
}
