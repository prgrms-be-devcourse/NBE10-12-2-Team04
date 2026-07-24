import type { Metadata } from 'next';
import './globals.css';
import Sidebar from '@/components/layout/Sidebar';
import Header from '@/components/layout/Header';

export const metadata: Metadata = {
  title: 'TripTrace',
  description: '나만의 여행 기록을 시작해보세요',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <body className="bg-gray-50 antialiased">
        <Sidebar />
        <Header />
        <main className="min-h-dvh pb-[calc(72px_+_env(safe-area-inset-bottom))] pt-[56px] md:ml-[72px] md:mt-[64px] md:min-h-screen md:pb-0 md:pt-0">
          {children}
        </main>
      </body>
    </html>
  );
}
