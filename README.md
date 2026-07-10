# TripTrace

여행 사진의 시간과 위치를 바탕으로 여행의 흐름을 기록하고, 지도 위에서 다시 돌아볼 수 있게 돕는 여행 기록 서비스입니다.

NBE 10기 12회차 2팀 04조 | 백엔드/프론트엔드 팀 프로젝트 | 2026

## 목차

- [프로젝트 소개](#프로젝트-소개)
- [팀 구성](#팀-구성)
- [서비스 흐름](#서비스-흐름)
- [핵심 기능](#핵심-기능)
- [기술 스택](#기술-스택)
- [프로젝트 구조](#프로젝트-구조)
- [시작하기](#시작하기)
- [테스트](#테스트)
- [협업 규칙](#협업-규칙)

## 프로젝트 소개

### 배경

여행을 다녀온 뒤 사진은 많이 남지만, 시간이 지나면 어떤 순서로 이동했고 어느 장소에서 무엇을 했는지 다시 정리하기가 어렵습니다. 일반적인 블로그나 게시글 방식은 사진, 날짜, 장소, 이동 흐름을 사용자가 직접 맞춰야 하므로 여행 기록을 끝까지 작성하는 데 부담이 큽니다.

### 해결 방향

TripTrace는 여행을 하나의 `Trip`으로 묶고, 그 안에서 게시글, 위치 마커, 이미지를 함께 관리합니다. 사용자가 사진을 업로드하면 이미지의 촬영 시간과 GPS 메타데이터를 분석해 여행 기록의 초안을 만들 수 있도록 설계했습니다.

| 기능 | 설명 |
| --- | --- |
| 여행 단위 기록 | 하나의 여행 안에서 게시글, 이미지, 마커를 연결해 여행 흐름을 관리 |
| 지도 기반 회고 | 마커와 대표 좌표를 바탕으로 여행 장소와 이동 흐름을 시각화 |
| 사진 메타데이터 활용 | EXIF 촬영 시간, GPS 좌표, 파일 정보를 추출해 자동 기록 생성에 활용 |
| 공개 피드 | 공개 여행기를 최신순/인기순으로 조회하고 좋아요로 반응 |

### 서비스 URL

| 환경 | URL |
| --- | --- |
| 로컬 API | http://localhost:8080 |
| 로컬 프론트엔드 | http://localhost:3000 |
| Swagger | http://localhost:8080/swagger-ui/index.html |
| H2 Console | http://localhost:8080/h2-console |
| 운영 배포 |  |

## 팀 구성

| 이름 | 역할 |
| --- | --- |
| 권유진 | 팀원, Marker 도메인 및 테스트 데이터 담당 |
| 문은지 | 팀원, Feed 및 프론트엔드 전체 담당 |
| 백승환 | 팀원, Image 도메인 및 EXIF 처리 담당, 배포 |
| 양한나 | 팀장, Trip·Post 도메인 및 지도 API 담당 |
| 최현승 | 팀원, Member 도메인 및 인증·인가 담당 |

## 서비스 흐름

```text
회원가입 / 로그인
        ↓
여행 생성
        ↓
사진 업로드 및 메타데이터 추출
        ↓
자동 기록 생성 또는 수동 게시글 작성
        ↓
Post / Marker / Image 연결
        ↓
지도 기반 여행 상세 조회
        ↓
공개 여행 피드와 좋아요
```

## 핵심 기능


### 여행 관리

- 여행 생성, 단건 조회, 목록 조회, 수정, 삭제
- 내 여행 목록 조회
- 공개 여행 목록 조회
- 대표 이미지 지정
- 공개/비공개 여부에 따른 접근 제어

### 게시물 관리

- 여행별 게시물 생성, 조회, 수정, 삭제
- 여행 날짜 기준 게시물 관리
- 공개 여행의 게시물 조회 허용
- 비공개 여행과 소유자 검증 처리

### 이미지 관리

- 여행 단위 이미지 업로드
- 게시물 단위 이미지 연결 및 삭제
- 원본 이미지 URL, 썸네일 URL, 파일 크기, MIME 타입 저장
- GPS 좌표, 촬영일, 기기 정보 등 EXIF 메타데이터 추출
- 이미지 방향 보정과 썸네일 생성

### 마커 / 지도

- 게시물별 마커 생성, 조회, 수정, 삭제
- GPS 좌표, 장소명, 방문 시간 관리
- 수동 생성 마커와 자동 생성 마커 구분
- Google Geocoding 기반 좌표 → 주소 보완
- Google Places 기반 장소 검색 및 주변 장소 후보 조회

### 자동 여행 기록

- 업로드된 이미지의 촬영 시간과 GPS 정보를 기반으로 기록 후보 생성
- 이미지 묶음을 Post와 Marker로 변환
- 자동 생성 결과를 사용자가 이후 수정할 수 있는 구조로 관리

### 피드 / 좋아요

- 공개 여행 최신순 조회
- 좋아요 많은 여행 조회
- 여행 좋아요 등록, 취소
- 내 좋아요 여부 조회
- 좋아요 수 자동 반영

## 기술 스택

### Backend

| 분류 | 기술 |
| --- | --- |
| Language / Framework | Java 25, Spring Boot 4.1.0 |
| Build | Gradle Kotlin DSL |
| Database | Spring Data JPA, H2 |
| Security | Spring Security, JWT, BCrypt |
| Image | metadata-extractor, Java ImageIO |
| Test | JUnit 5, Spring Boot Test, Spring Security Test |

### Frontend

| 분류 | 기술 |
| --- | --- |
| Framework | Next.js, React|
| Language | TypeScript |
| Styling | Tailwind CSS |
| Map | Google Maps JavaScript API |

### Infra / Collaboration

| 분류 | 기술 |
| --- | --- |
| CI | GitHub Actions Backend CI |
| Git Rule | Git hooks, PR template, issue template |
| Local DB | H2 file DB |

## 프로젝트 구조

```text
NBE10-12-2-Team04/
├── back/                         # Spring Boot API 서버
│   ├── src/main/java/com/triptrace/
│   │   ├── domain/
│   │   │   ├── auth/             # 회원가입, 로그인, JWT 재발급
│   │   │   ├── member/           # 회원 정보, 프로필 이미지
│   │   │   ├── trip/             # 여행, 피드, 좋아요, 자동 기록
│   │   │   ├── post/             # 여행 게시물
│   │   │   ├── marker/           # 지도 마커, 장소 검색
│   │   │   └── image/            # 이미지 업로드, 메타데이터, 썸네일
│   │   ├── global/               # Security, Exception, JPA, Response
│   ├── src/main/resources/       # application 설정
│   └── src/test/java/            # 백엔드 테스트
├── front/                        # Next.js 프론트엔드
│   └── src/
│       ├── app/               
│       ├── components/         
│       ├── lib/                
│       └── types/              
└──
```

## 시작하기


### Backend

```bash
cd back
./gradlew bootRun
```


Swagger:

```text
http://localhost:8080/swagger-ui/index.html
```

H2 Console:

```text
http://localhost:8080/h2-console
```

### Frontend

```bash
cd front
pnpm install
pnpm dev
```

프론트엔드는 기본적으로 다음 주소에서 실행됩니다.

```text
http://localhost:3000
```

## 테스트

### Backend 테스트

```bash
cd back
./gradlew test
```


### CI

| Workflow | 트리거 |
| --- | --- |
| Backend CI | PR 또는 push → `develop` | 

## 협업 규칙

- 작업 전 이슈를 생성합니다.
- 이슈 번호와 작업 범위가 드러나는 브랜치를 생성합니다.
- 작업 완료 후 Pull Request를 생성합니다.
- `develop` 브랜치에는 직접 push하지 않고 PR 리뷰 후 병합합니다.
- 로컬 Git hooks로 브랜치명과 커밋 메시지 규칙을 확인합니다.

