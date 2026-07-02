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
  ownerId?: string;
  title: string;
  country: string;
  city: string;
  startDate: string;
  endDate: string;
  isPublic: boolean;
  thumbnailUrl?: string;
  representativeLat?: number;
  representativeLng?: number;
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
  postId?: string | number;
  markerId?: string | number;
  date: string;
  dayOfWeek?: string;
  title?: string;
  location?: string;
  representativeThumbnailUrl?: string;
  imageCount?: number;
  imageIds?: Array<string | number>;
  centerLat?: string | number;
  centerLng?: string | number;
}

export interface AutoRecordResult {
  totalRecords?: number;
  totalMarkers?: number;
  usedImages?: number;
  excludedImages?: number;
  generatedPostCount?: number;
  generatedMarkerCount?: number;
  usedImageCount?: number;
  skippedImageCount?: number;
  records: AutoRecord[];
}
