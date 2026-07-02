'use client';

import Link from 'next/link';
import { Search, Bell, User } from 'lucide-react';

interface HeaderProps {
  rightSlot?: React.ReactNode;
}

export default function Header({ rightSlot }: HeaderProps) {
  return (
    <header className="fixed top-0 left-[60px] right-0 h-[56px] bg-white border-b border-gray-200 flex items-center px-6 z-30">
      <Link href="/" className="text-lg font-bold text-gray-900 mr-auto">
        TripTrace
      </Link>

      <div className="flex items-center gap-2">
        {rightSlot}
        {/* TODO: 검색 기능 1차에서는 placeholder */}
        <button
          className="w-9 h-9 flex items-center justify-center rounded-full text-gray-500 hover:bg-gray-100 transition-colors"
          title="검색"
        >
          <Search size={20} />
        </button>
        {/* TODO: 알림 기능 1차에서는 placeholder */}
        <button
          className="w-9 h-9 flex items-center justify-center rounded-full text-gray-500 hover:bg-gray-100 transition-colors"
          title="알림"
        >
          <Bell size={20} />
        </button>
        <Link
          href="/users/me"
          className="w-9 h-9 flex items-center justify-center rounded-full bg-gray-200 text-gray-600 hover:bg-gray-300 transition-colors overflow-hidden"
        >
          <User size={18} />
        </Link>
      </div>
    </header>
  );
}
