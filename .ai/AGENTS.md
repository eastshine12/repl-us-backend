---
적용: 항상
---

# AGENTS.md

이 문서는 이 프로젝트에서 에이전트가 항상 따라야 하는 핵심 규칙과 상세 문서의 진입점을 정의한다.
상세 기준은 아래 문서들에 분리되어 있으며, 작업 전 해당 문서를 반드시 확인한다.

## 반드시. README.md 요구사항을 최우선으로 따른다

- 이 프로젝트의 구현 우선순위와 평가 기준은 항상 `README.md`를 기준으로 판단한다.
- 이 문서와 하위 문서의 규칙이 `README.md` 요구사항, 제약 사항, 제출 조건과 충돌하면 `README.md`를 우선한다.
- 에이전트는 과도한 추상화나 일반론보다 과제 요구사항 충족, 실행 가능성, 검증 가능성을 먼저 보장한다.

## 반드시. 항상 적용할 핵심 규칙

- 기본 구조는 전통 DDD 레이어드 아키텍처를 따르고, Clean Architecture의 의존성 역전과 인프라 격리 원칙을 유지한다.
- 백엔드는 포트폴리오용 public repository로 공개될 수 있게 작성하고, 프론트엔드는 별도 private repository로 남는 전제를 지킨다.
- 백엔드 원격 repository 이름은 서비스명 `repl.us`를 반영해 `repl-us-backend`를 기본으로 사용하고, 호스팅 환경에서 점을 안전하게 쓸 수 있으면 `repl.us-backend`도 허용한다.
- 백엔드 `README.md`는 실서비스 public repository 수준으로 영어로 작성하며, 기능 소개, 아키텍처, 실행 방법, 테스트 방법, 환경 변수, API 문서 위치를 포함한다.
- 소스 코드 의존성은 `interfaces -> application -> domain` 방향으로 흐르게 하고, `infrastructure`는 `domain`이 정의한 reader, writer, repository interface를 구현한다.
- `domain`에는 순수 도메인 모델, 정책, 서비스, 이벤트, reader/writer/repository interface만 두고 ORM, 외부 SDK, HTTP DTO, 프레임워크 타입을 넣지 않는다.
- `application`은 얇은 facade로서 트랜잭션 경계, command/result 변환, 여러 도메인 서비스 호출, 응답 조립을 담당한다.
- 도메인 서비스는 같은 도메인 안의 reader, writer, repository interface를 통해 aggregate 조회/저장을 조율할 수 있다.
- Kotlin Spring 멀티모듈을 쓰는 경우 도메인별 모듈 분리보다 `:api`, `:admin-api`, `:core` 같은 진입점 분리를 우선하고, `:api`와 `:admin-api`는 서로 직접 의존하지 않는다.
- Spring Data/JPA repository 직접 호출은 `infrastructure/persistence`의 구현체만 할 수 있다.
- 필수 조회는 `getById()`처럼 실패 의미가 드러나는 domain reader/repository 메서드로 표현하고, 조회 실패 변환 책임은 구현체 또는 도메인 서비스 경계가 진다.
- 저장소 계약 이름은 기본적으로 `Repository`를 쓰고, 조회/쓰기/추가/발행 같은 성격이 분명할 때만 `Reader`, `Writer`, `Appender`, `Publisher`처럼 역할이 드러나는 이름으로 나눈다.
- 도메인 간 협력은 `application`이 각 도메인 서비스를 호출해 조율하며, 서로 다른 도메인의 서비스끼리 직접 호출하지 않는다.
- 이벤트 기반 처리는 도메인 이벤트와 트랜잭셔널 아웃박스를 기본으로 검토하고, 브로커 직접 발행은 인프라 계층에서만 수행한다.
- 테스트 제목은 한글로 작성하고, 본문은 반드시 `// given`, `// when`, `// then` 주석으로 구분한다.
- 에러는 `ErrorCode`, `ErrorType`, `CoreException` 체계로 관리하고, HTTP 상태와 로그 레벨 매핑은 인터페이스/로깅 경계에서 처리한다.
- 요청/응답 로깅과 전역 예외 처리는 인터페이스 계층에서 일관되게 수행한다.
- 코드 수정 후 lint, test, type check와 문서 규칙 적합성을 다시 점검한다.
- 의미 있는 변경 단위가 끝나면 커밋 시점을 판단해 제안하고, 커밋 메시지는 한글로 작성한다.

## 반드시. 작업 전 참조할 문서

- 아키텍처, 계층, 의존성, 도메인 설계, 이벤트, 동시성: [.ai/agent/architecture.md]
- 테스트, 통합/E2E, 테스트 컨테이너, K6: [.ai/agent/testing.md]
- 에러 코드, `CoreException`, 전역 예외 처리: [.ai/agent/error-handling.md]
- 요청/응답 로깅, 에러 추적 로그: [.ai/agent/logging.md]
- 린트 재검토, 커밋 시점, 커밋 메시지 규칙: [.ai/agent/operations.md]

## 반드시. 문서 사용 방식

- 아키텍처를 바꾸거나 새 기능을 설계하기 전에는 `.ai/agent/architecture.md`를 먼저 확인한다.
- 테스트를 추가하거나 수정하기 전에는 `.ai/agent/testing.md`를 먼저 확인한다.
- 예외, 에러 응답, 에러 코드 체계를 건드릴 때는 `.ai/agent/error-handling.md`를 먼저 확인한다.
- 요청/응답 로그, 에러 로그, 추적 로그를 수정할 때는 `.ai/agent/logging.md`를 먼저 확인한다.
- 작업 마무리, 검증, 커밋 제안이 필요한 시점에는 `.ai/agent/operations.md`를 먼저 확인한다.
