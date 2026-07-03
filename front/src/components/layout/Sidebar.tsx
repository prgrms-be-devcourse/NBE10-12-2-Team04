'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { MapPin, Home, User, Settings, Images } from 'lucide-react';

const navItems = [
  { href: '/', icon: Home, label: '홈' },
  { href: '/trips', icon: Images, label: '내 Trip' },
  { href: '/users/me', icon: User, label: '내 정보' },
];

export default function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className="fixed left-0 top-0 h-full w-[72px] bg-white border-r border-gray-200 flex flex-col items-center py-4 z-40">
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
  );
}
