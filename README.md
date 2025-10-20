# 🎵 Music App

## 1) 기획 의도
한 번의 검색으로 사운드클라우드와 유튜브를 동시에 조회해, 
플랫폼마다 따로 올라간 음원·영상을 한 화면에서 찾아주는
메타 검색 엔진을 만들어보고자 하였습니다.

문제: 사운드클라우드와 유튜브는 업로드가 분리되어 있어, A에는 있는데 B에는 없는 곡/영상이 자주 발생함.
해결: 두 서비스의 API를 동시 연동하여 한 번의 검색으로 양쪽 결과를 통합 제공 , 관련검색어를 활용하여 사용자가 찾는 음악,영상을 더많이 제공 
기대효과: 사용자는 플랫폼을 오가며 중복 검색할 필요 없이, 더 빠르게 원하는 트랙/영상을 발견하고 대체/유사 콘텐츠도 쉽게 찾을 수 있음.

--------------------------------------------------------------------------------------------------------------

## 2) 핵심 기능
-  **CRUD**:  1. Playlist : 등록/목록펼침/수정/삭제   2. Board /등록/목록 펼침/수정/완료 
-  **SPA (React Router)**  : Home(main page) | Discover | Board | Search | 
-  **가상 스크롤**: 대량 데이터 효율 렌더링
-  **코드 스플리팅**: `React.lazy` + `<Suspense />` | Discover | Board | Search | 적용
-  **외부 데이터 연동**: YouTube Data API(frontend) , SoundCloud API(backend)  
                           ㄴ 영상 재생(YouTube modal)                     ㄴ 음원 재생(PlayBar)

--------------------------------------------------------------------------------------------------------------

## 3) 화면 구성 / Flow
- **페이지**: **Home**, Discover, Board, Library, Search , Playlist

### Home
중심이 되는 페이지 
YouTube Data API 를 사용하여 실시간 인기 동영상을 보여줌

Header , Footer , SideBar를 고정시킴 - Header 부분의 search(검색창)는 다른 페이지를 들어가도 항상 보이게설정 (항상 접근 가능)
                                    - footer 부분의 PlayerBar(재생바)                    ''
                                    - SideBar 부분의  PlayList(플레이 리스트)             ''        - Library page로 연결되어 상세 트랙을 볼 수 있음
                                         

### Discover
SoundCloud API를 받아와 실기간 인기차트를 불러옴
Lazy + Suspense 를 사용해 사이트를 처음 들어갈때 로딩을 하지않음 -> Discover page를 들어와야 데이터 불러옴


### Board
게시판
CRUD : 등록 / 조회(목록 펼침) / 수정(제목 + 내용수정) / 삭제


### Library
나의 재생목록 (나의 플레이리스트 - 플리 상세 확인 가능)
전역객체(Store)를 만들어 본인이 원하는 YouTube 영상 SoundCloud 음원을 어디서든 저장 할 수 있음


### Playlist 
플레이 리스트 상세영역
Library page에서 플레이 리스트를 선택하면 
하나의 플레이 리스트를 상세화면으로 볼 수 있음 

                            
--------------------------------------------------------------------------------------------------------------

## 전체 시스템 구성도
<img width="2175" height="1738" alt="1" src="https://github.com/user-attachments/assets/2d3dec2a-a5f9-4be5-ac63-d0eeea1448d7" />

## 프론트엔드 다이어그램
<img width="8369" height="5386" alt="2" src="https://github.com/user-attachments/assets/1d95a774-a578-4d11-9807-78a62b543326" />

## 백엔드 다이어그램
<img width="1880" height="1050" alt="3" src="https://github.com/user-attachments/assets/dfc6ac9d-ff77-4cbb-939c-c1a408a3fa60" />

## api 명세서
[api.pdf](https://github.com/user-attachments/files/22996616/api.pdf)
