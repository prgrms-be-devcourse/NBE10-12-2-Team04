export interface User {
  id: string;
  email: string;
  nickname: string;
  profileImageUrl?: string;
  bio?: string;
  status: 'ACTIVE' | 'INACTIVE';
  createdAt: string;
  updatedAt: string;
}

export interface Trip {
  id: string;
  title: string;
  country: string;
  city: string;
  startDate: string;
  endDate: string;
  isPublic: boolean;
  thumbnailUrl?: string; // TODO: 백엔드 TripResponse에 추가 필요
  recordCount: number;
  likeCount: number;
  liked?: boolean;
  author: {
    id: string;
    nickname: string;
    profileImageUrl?: string;
  };
  createdAt: string;
}

export interface Post {
  id: string;
  tripId: string;
  title: string;
  content: string;
  date: string;
  time?: string;
  images: PostImage[];
  marker?: Marker;
}

export interface PostImage {
  id: string;
  url: string;
  filename: string;
}

export interface Marker {
  id: string;
  placeName: string;
  lat: number;
  lng: number;
  visitTime?: string;
  source?: string;
}

export interface AutoRecord {
  date: string;
  dayOfWeek: string;
  title: string;
  location: string;
  imageCount: number;
}

export interface AutoRecordResult {
  totalRecords: number;
  totalMarkers: number;
  usedImages: number;
  excludedImages: number;
  records: AutoRecord[];
}
