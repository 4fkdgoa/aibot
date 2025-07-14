# AUTO_CRM AI봇 프로젝트 문서

> 🤖 딜러시스템과 API서버를 연동한 AI 기반 고객관리 챗봇 시스템

## 📋 프로젝트 개요
- **목적**: CRM 시스템에 AI 챗봇 도입으로 업무 효율성 향상
- **환경**: Windows Server, MSSQL, Spring Framework/Boot, MyBatis, EclipseLink  
- **AI**: Spring AI + OpenAI GPT-4o-mini

## 🏗️ 시스템 구조
```
딜러시스템 (Spring Framework) ↔ API서버 (Spring Boot + AI) ↔ MSSQL DB
```

## 📁 실제 프로젝트 위치
- **딜러시스템**: `C:\jexer\eclipse-workspace-2024\AutoCRM_Samchully`
- **API서버**: `C:\jexer\workspace-spring-tool-suite-4-4.22\API_for_wdms`
- **버전관리**: SVN (회사 내부)
- **이 GitHub**: 문서 및 진행상황 추적 전용

## 🎯 구현 목표
1. **메뉴 연결**: 권한별 접근 가능 메뉴 버튼 제공
2. **판매현황**: AI가 자연어를 SQL로 변환하여 차트/표 제공  
3. **음성인식**: 웹 기반 음성으로 AI와 대화

## 📊 진행 상황

### Phase 1: 기본 채팅 인터페이스 ⏳
- [x] 시스템 설계 완료
- [x] 기술 스펙 정의 완료
- [ ] AIChatController.java 구현
- [ ] ai-chat.jsp UI 구현
- [ ] 기본 통신 테스트

### Phase 2: 메뉴 연결 기능 ❌
- [ ] 권한별 메뉴 매핑
- [ ] 동적 메뉴 버튼 생성
- [ ] 페이지 이동 기능

### Phase 3: 데이터 분석 기능 ❌  
- [ ] AI 쿼리 생성기
- [ ] 차트 렌더링
- [ ] 엑셀 데이터 출력

### Phase 4: 고급 기능 ❌
- [ ] 음성인식 지원
- [ ] 대화 컨텍스트 관리
- [ ] 성능 최적화

## 📝 현재 작업 상황

**📅 마지막 업데이트**: 2025-01-13  
**👤 작업자**: 개발팀  
**🔄 다음 단계**: AIChatController.java 기본 구조 구현

### 현재 진행중
- **파일**: AIChatController.java
- **위치**: `API_for_wdms/src/main/java/net/autocrm/api/controller/`
- **상태**: 미시작
- **목표**: 간단한 "Hello AI" 응답 구현

### 준비 사항
- [ ] OpenAI API 키 발급
- [x] 개발 환경 세팅 완료
- [x] 프로젝트 구조 파악 완료

## 🔧 기술 스택
- **Backend**: Spring Boot 3.5.0, Spring AI 1.0.0, MyBatis, EclipseLink
- **Frontend**: JSP, JavaScript, WebSocket
- **Database**: MSSQL Server  
- **AI**: OpenAI GPT-4o-mini
- **IDE**: Eclipse, Spring Tool Suite

## 📖 상세 문서
- [📋 진행상황 상세](docs/PROGRESS.md)
- [🔧 기술 명세서](docs/TECHNICAL_SPECS.md)  
- [🚀 빠른 시작 가이드](docs/QUICK_START.md)

## 💬 다음 대화 시 참고 템플릿

```
[AUTO_CRM AI봇 프로젝트 계속]
GitHub: https://github.com/4fkdgoa/autocrm-ai-bot

현재 상황:
- 완료: [GitHub 상태 확인]
- 진행중: [현재 작업 파일]
- 이슈: [발생한 문제]

요청: 구현 - [구체적 요청사항]
```

---
**🏢 회사 프로젝트**: 실제 소스코드는 내부 SVN에서 관리  
**📚 GitHub 용도**: 문서화, 진행상황 추적, 코드 예제 보관  
**🔄 업데이트**: 각 Phase 완료시마다 예제 코드 추가