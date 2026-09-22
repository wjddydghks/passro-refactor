# 패스로(Passro)

## 1. 프로젝트 소개

패스로는 대학생이 매일 오가는 통학 경로를 활용해 소형 택배를 대신 전달하는 P2P 전달 플랫폼입니다.

사용자가 전달 요청 등록, 통학 경로 기반 매칭, 실시간 전달 위치 및 상태 확인, 채팅, 포인트 정산 등의 핵심 기능을 편리하게 이용할 수 있도록 모바일 환경에 최적화된 웹 화면을 개발합니다.

마켓, 알림, 리뷰, 신고, 문의 기능을 제공하며 Access Token 만료 시 Refresh Token을 이용해 인증을 자동으로 갱신합니다.

## 2. 팀원 및 프론트엔드 역할 분담

| 이름 | 역할 | 담당 업무 |
|---|---|---|
| 플랫/박찬서 | Frontend | UI 구현, 페이지 라우팅, API 연동 |
| 쥬/황은주 | Frontend | UI 구현, 페이지 라우팅, API 연동 |
| 하루/유수현 | Frontend | UI 구현, 페이지 라우팅, API 연동 |
| 유뎅/최유정 | Frontend | UI 구현, 페이지 라우팅, API 연동 |

## 3. 기술 스택

### Frontend

- React 19
- TypeScript 5
- Vite 7
- React Router DOM 7
- Axios
- Tailwind CSS 3

### 협업 도구

- GitHub
- Notion
- Discord / KakaoTalk

## 4. 폴더 구조

```text
src
├── apis
│   ├── client.ts          # Axios 공통 인스턴스, 토큰 재발급, API 응답 처리
│   ├── endpoints.ts       # API 엔드포인트 중앙 관리
│   ├── authApi.ts         # 인증, 회원가입, 계정 찾기 API
│   ├── accountApi.ts      # 프로필 및 계정 API
│   ├── chatApi.ts         # 채팅방 및 메시지 API
│   ├── locationApi.ts     # 전달자 실시간 위치 API
│   ├── routeApi.ts        # 발송자·전달자 전달 경로 API
│   ├── marketApi.ts       # 포인트 마켓 API
│   ├── notificationApi.ts # 알림 API
│   ├── pointApi.ts        # 포인트 내역 API
│   ├── reportApi.ts       # 신고 API
│   └── tokenStorage.ts    # Access/Refresh Token 저장 관리
├── assets
│   ├── icons              # 공통 아이콘
│   └── images             # 화면별 이미지
├── components
│   ├── alarms             # 알림 UI
│   ├── chat               # 채팅 UI
│   ├── common             # 공통 UI 및 인증 라우트
│   ├── delivery           # 전달 요청·상태·내역
│   ├── home               # 역할별 홈 대시보드
│   ├── navbar             # 하단 내비게이션
│   ├── points             # 포인트 UI
│   ├── profile            # 마이페이지 UI
│   ├── signup             # 회원가입 폼
│   └── verification       # 학생 인증 모달
├── hooks
│   ├── useApiRequest.ts   # API 로딩·오류·데이터 상태 관리
│   ├── useDebouncedValue.ts
│   ├── useSenderRouteTracking.ts
│   └── useShipperRouteTracking.ts
├── layouts
│   └── MainLayout.tsx     # 로그인 후 화면 공통 레이아웃
├── pages                  # 라우트 단위 화면
├── routes
│   └── router.tsx         # 전체 페이지 라우팅
├── types                  # 화면 및 API 타입
├── utils                  # 인증, 검증, 상태 변환 유틸리티
├── App.tsx
└── main.tsx
```

## 5. 브랜치 전략

```text
main
└── dev
    └── feature/기능명
```

- `main`: 최종 배포용 브랜치
- `dev`: 개발 통합 브랜치
- `feature`: 기능 단위 작업 브랜치

## 6. 커밋 컨벤션

```text
feat: 새로운 기능 추가
fix: 버그 수정
docs: 문서 수정
style: 코드 포맷팅
refactor: 코드 리팩토링
chore: 기타 설정 변경
```

예시:

```text
feat: 로그인 페이지 UI 구현
fix: 라우팅 오류 수정
docs: README 실행 방법 추가
```

## 7. PR 컨벤션

### PR 제목 양식

```text
[type] 작업 내용 요약
```

`type`은 커밋 컨벤션의 `feat`, `fix`, `docs`, `style`, `refactor`, `chore` 중 하나를 사용합니다.

예시:

```text
[feat] 전달 요청 등록 페이지 UI 구현
[fix] 전달 상태 라우팅 오류 수정
[docs] README 화면 플로우 추가
```

### PR 본문 양식

```md
## 수정자
- 이름 또는 GitHub ID

## 작업 내용
- 구현한 기능 설명

## 변경 사항
- 수정된 주요 파일 또는 구조

## 확인 사항
- 실행 여부
- 에러 여부
- 추가 확인이 필요한 부분
```

## 8. 실행 방법

프로젝트에 필요한 패키지를 설치합니다.

```bash
npm ci
```

로컬 백엔드에 연결할 경우 `.env.local` 파일에 API 주소를 설정합니다.

```env
VITE_API_BASE_URL=http://localhost:8080
```

개발 서버를 실행합니다.

```bash
npm run dev
```

```text
http://localhost:5173
```

프로덕션 빌드는 다음 명령어로 확인합니다.

```bash
npm run build
```

## 9. 화면 목록 및 흐름

### 화면 목록

- 로그인 페이지
- 아이디·비밀번호 찾기 페이지
- 회원가입 및 이메일 인증 페이지
- 발송자·전달자 역할 선택 페이지
- 학생 인증 팝업
- 역할별 홈 페이지
- 전달 요청 등록 페이지
- 전달 매칭 페이지
- 전달자용 실시간 전달 추적 페이지
- 발송자용 전달 상태 페이지
- 채팅 목록 페이지
- 채팅 페이지
- 알림 팝업
- 포인트 마켓 페이지
- 마이페이지
- 프로필 수정 페이지
- 비밀번호 변경 페이지
- 포인트 내역 페이지
- 전달 내역 페이지
- 문의 페이지
- 리뷰 작성 페이지
- 신고 페이지
- 로딩·오류·404 페이지

### 현재 프론트 화면 흐름

```text
로그인 또는 회원가입
└── 역할 선택
    ├── 발송자
    │   └── 발송자 홈
    │       ├── 전달 요청 약관 동의
    │       │   └── 전달 요청 정보 입력
    │       │       └── 전달 요청 등록 완료
    │       ├── 매칭된 전달자 및 진행 상태 확인
    │       │   ├── 실시간 위치 확인
    │       │   ├── 채팅
    │       │   └── 신고
    │       └── 최근 전달 내역 확인
    └── 전달자
        └── 학생 이메일 인증
            └── 전달자 홈
                ├── 매칭 요청 상세 확인
                │   └── 매칭 수락
                └── 물품 인수
                    └── 실시간 위치 공유 및 전달 진행
                        ├── 채팅
                        ├── 신고
                        └── 전달 완료 요청

하단 내비게이션
├── 홈
├── 포인트 마켓
├── 채팅 목록
└── 마이페이지

홈 프로필 이미지
└── 마이페이지
    ├── 프로필 수정
    ├── 비밀번호 변경
    ├── 포인트 내역
    ├── 전달 내역
    ├── 문의하기
    └── 로그아웃

전달 완료
├── 포인트 정산
└── 리뷰 및 피드백 작성
```

### 목표 MVP 흐름

1. 발송자는 출발지, 도착지, 물품 크기, 사진을 포함해 전달 요청을 등록합니다.
2. 시스템은 전달 요청과 전달자의 통학 경로를 비교해 유사도가 높은 전달자를 탐색합니다.
3. 전달자는 자신의 통학 경로와 일치하는 요청을 확인하고 매칭을 수락합니다.
4. 매칭이 확정되면 양측은 알림과 채팅을 통해 전달 내용을 확인합니다.
5. 전달자는 픽업, 이동 중, 전달 완료 상태를 업데이트하고 실시간 위치를 공유합니다.
6. 전달 완료가 확인되면 포인트가 자동 정산됩니다.
7. 전달 완료 후 발송자는 리뷰와 평점을 남길 수 있으며, 문제 발생 시 신고 또는 문의할 수 있습니다.
