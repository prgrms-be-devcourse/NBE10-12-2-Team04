'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { MapPin, Home, Compass, Bookmark, Heart, User, Settings } from 'lucide-react';

const navItems = [
  { href: '/', icon: Home, label: '홈' },
  { href: '/explore', icon: Compass, label: '탐색' },
  { href: '/bookmarks', icon: Bookmark, label: '북마크' },
  { href: '/likes', icon: Heart, label: '좋아요' },
  { href: '/users/me', icon: User, label: '내 정보' },
];

export default function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className="fixed left-0 top-0 h-full w-[60px] bg-white border-r border-gray-200 flex flex-col items-center py-4 z-40">
      <Link href="/" className="mb-6">
        <div className="w-9 h-9 flex items-center justify-center text-green-600">
          <MapPin size={22} strokeWidth={2.5} />
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
              className={`w-10 h-10 flex items-center justify-center rounded-xl transition-colors ${
                isActive
                  ? 'bg-green-600 text-white'
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
        className="w-10 h-10 flex items-center justify-center rounded-xl text-gray-400 hover:bg-gray-100 hover:text-gray-600 transition-colors"
      >
        <Settings size={20} />
      </Link>
    </aside>
  );
}
