---
적용: 항상
---

# 아키텍처 규칙

## 반드시. 새로운 기능과 설계는 확장 가능해야 한다

지금 필요한 요구사항만 맞추는 구조로 끝내면 기능이 늘어날수록 수정 범위가 급격히 커진다.
새로운 기능을 붙이거나 설계할 때는 이후 정책 추가, 저장소 교체, 외부 시스템 확장, 이벤트 발행, 조회 모델 분리를 감당할 수 있게 설계한다.
조건 분기가 늘어날 가능성이 보이면 역할별 객체 분리, 추상화, 조합 가능한 인터페이스를 먼저 검토한다.

## 반드시. 추상화는 변경 가능성이 큰 지점을 감싸야 한다

모든 것을 추상화하는 것이 아니라, 교체 가능성이 높고 도메인에 영향을 주면 안 되는 지점을 추상화해야 한다.
특히 저장소, ORM, 메시지 브로커, 외부 API, 캐시, 락, 이벤트 발행기 같은 인프라 요소는 계약 뒤로 숨긴다.
추상화가 생기면 이름이 구현 기술이 아니라 역할과 책임을 드러내야 한다.

## 반드시. 계층 의존성을 단방향으로 유지한다

계층 의존성은 소스 코드 import 기준으로 판단한다.
기본 구조는 전통 DDD 레이어드 아키텍처를 따르고, Clean Architecture의 의존성 역전과 인프라 격리 원칙을 함께 유지한다.
진입점 쪽 코드는 `interfaces -> application -> domain` 순서로 안쪽을 향해 의존하고, `infrastructure`는 바깥 구현 계층으로서 `domain`이 정의한 reader, writer, repository interface를 구현한다.
`domain`은 어떤 경우에도 `application`, `interfaces`, `infrastructure`를 참조하지 않는다.
`application`도 `interfaces`와 `infrastructure` 구현체를 직접 참조하지 않고, 필요한 도메인 서비스들을 호출해 유스케이스를 실행한다.
의존성 주입, import, 호출 흐름, 모듈 참조 어디에서도 계층 역전이 발생하면 안 된다.

## 반드시. 도메인 기준으로 디렉터리를 나눈다

기능이 늘어날수록 기술 기준 분류보다 도메인 기준 분리가 유지보수성과 변경 안정성을 높인다.
최상위는 도메인별 디렉터리로 나누고, 각 도메인 아래를 `application`, `domain`, `infrastructure`, `interfaces`로 분리한다.
공통 모듈은 정말 여러 도메인에서 반복 검증된 경우에만 분리한다.

예시 구조:

```text
src
└── room
    ├── application
    │   ├── facade
    │   ├── command
    │   ├── query
    │   └── result
    ├── domain
    │   ├── model
    │   ├── service
    │   ├── policy
    │   ├── validator
    │   ├── repository
    │   ├── event
    │   └── type
    ├── infrastructure
    │   ├── persistence
    │   │   ├── entity
    │   │   ├── jpa
    │   │   └── repository
    │   └── external
    └── interfaces
        └── rest
            └── dto
```

`domain/type`은 도메인 의미가 있는 식별자, 상태, 기간, 수량 같은 타입을 둘 때만 사용한다.
값 객체를 만든다는 이유만으로 `valueobject`나 `vo` 패키지를 강제하지 않는다.
도메인 모델 안에서 의미가 더 선명하면 `domain/model`에 함께 두고, 축약어 `vo`는 사용하지 않는다.
`domain/repository`에는 `RoomReader`, `RoomWriter`, `RoomRepository`처럼 도메인 서비스가 필요로 하는 조회/저장 계약을 둔다.
처음부터 reader와 writer를 억지로 나누지 말고, 역할이 실제로 갈라질 때만 분리한다.

## 권장. Kotlin Spring 멀티모듈은 진입점 분리에만 신중히 사용한다

처음부터 모든 프로젝트를 멀티모듈로 과하게 쪼개지 않는다.
단일 API로 충분한 초기 단계에서는 하나의 Spring Boot 앱 안에서 도메인별 패키지 구조를 먼저 명확히 유지한다.
멀티모듈을 쓰더라도 도메인별 Gradle 모듈을 먼저 나누지 않는다.
도메인, application, infrastructure는 `:core` 안에서 도메인별 패키지 구조로 유지하고, public API와 internal admin처럼 실행 진입점과 보안/라우팅 경계가 달라질 때 `:api`, `:admin-api` 같은 진입점 모듈만 분리한다.
관리자가 쓰는 internal admin은 유저에게 제공하는 기능이 아니므로 public API의 controller, dto, route, security 설정에 기대지 않고 `:admin-api` 안에 별도 controller, dto, security 설정을 둔다.
멀티모듈은 프로세스 분리를 뜻하지 않는다.
별도 Spring Boot 실행 모듈을 따로 실행할 때만 별도 JVM 프로세스로 운영된다.

권장 모듈 의존 방향:

```text
:api       -> :core
:admin-api -> :core
```

금지 의존 방향:

```text
:admin-api -> :api
:api       -> :admin-api
:core      -> :api
:core      -> :admin-api
```

멀티모듈에서도 핵심 원칙은 같다.
`:api`와 `:admin-api`는 서로 직접 의존하지 않고 필요한 기능은 `:core`의 application facade나 도메인 서비스를 통해 호출한다.
`:core`는 public/admin controller, request/response dto, route, security 설정을 알면 안 된다.
admin 전용 업무 흐름이 필요하면 `:admin-api` controller가 public API를 재사용하지 않고, `:core` 안의 application/admin 패키지나 별도 admin facade를 호출한다.
Gradle 의존성에서 순환 참조가 생기면 설계가 잘못된 것으로 보고 모듈 경계를 다시 잡는다.

## 반드시. 객체의 역할과 책임을 계층에 맞게 분리한다

하나의 클래스가 검증, 오케스트레이션, 상태 변경, 외부 연동을 동시에 가지면 변경 이유가 섞이고 테스트가 무거워진다.
`interfaces`는 요청 해석과 응답 변환만 담당하고, `application`은 유스케이스 조합과 트랜잭션 경계 관리에 집중하고, `domain`은 핵심 규칙과 상태 변경을 담당하고, `infrastructure`는 저장소 및 외부 시스템 연동만 담당한다.

## 반드시. application 레이어는 유스케이스 조합에만 집중한다

`application`은 외부 요청을 유스케이스 단위로 받고 하나 이상의 도메인 서비스를 호출해 시나리오를 완성하는 얇은 facade 계층이다.
facade, command handler, query handler에는 비즈니스 핵심 규칙을 새로 만들지 말고 도메인 서비스와 도메인 객체에 위임한다.
외부 요청 포맷, ORM entity, Spring Data repository, 외부 SDK 세부사항도 여기로 끌고 오지 않는다.
입력은 `command` 또는 `query`, 출력은 `info` 또는 `result`로 명확히 구분한다.
유스케이스 클래스는 `application` 바로 아래의 facade, command handler, query handler로 표현해도 된다.
`usecase` 패키지는 클래스가 많아져 별도 묶음이 실제 가독성을 높일 때만 만든다.
트랜잭션 경계, command/result 변환, 도메인 서비스 호출, 응답 조립은 application의 책임이다.

## 반드시. domain 레이어는 핵심 규칙의 소유권을 가진다

시스템에서 가장 오래 살아남는 것은 프레임워크 코드가 아니라 도메인 규칙이다.
상태 변경 조건, 불변식, 금지 규칙, 계산 규칙은 `domain`에 둔다.
단순 CRUD 조합만으로 설명되지 않는 규칙은 `policy`, `service`, `validator` 같은 명시적 객체로 분리한다.
도메인 서비스는 같은 도메인 안의 reader, writer, repository interface를 통해 aggregate 조회와 저장을 조율할 수 있다.
단, Spring Data repository, JPA entity, 외부 SDK, HTTP DTO 같은 구현 세부사항은 절대 참조하지 않는다.
도메인 서비스는 생성자 주입이 가능한 순수 클래스로 작성하고, Spring Bean 등록 같은 프레임워크 조립은 바깥 계층에서 처리한다.

## 반드시. domain 엔티티는 순수해야 한다

도메인 레이어에는 순수 도메인 엔티티가 존재해야 한다.
도메인 엔티티에 ORM 어노테이션, 영속성 전용 상속 구조, 프레임워크 전용 타입, 저장소 구현 세부사항을 넣지 않는다.
ORM이나 매핑 기술은 교체 가능한 인프라 세부사항으로 보고, 필요하면 인프라 전용 모델과 매퍼를 분리한다.

## 반드시. infrastructure는 구현 세부사항을 캡슐화한다

저장소, 캐시, 메시지 브로커, 외부 API는 언제든 교체될 수 있는 바깥 요소다.
`infrastructure/persistence`에는 ORM entity, Spring Data/JPA repository, domain repository 구현체를 두고, 조회 최적화와 저장 전략을 이 계층 안에 가둔다.
repository 구현체는 `domain/repository`의 reader, writer, repository interface를 구현하고 내부에서 JPA repository를 호출한다.
도메인이나 애플리케이션 계층에 ORM, SQL, 외부 SDK 타입이 새어 나오지 않게 한다.

## 반드시. 도메인 저장소 계약과 구현체를 구분한다

도메인 레이어에는 reader, writer, repository interface 같은 저장소 계약만 둔다.
도메인 서비스는 이 저장소 계약을 통해 aggregate를 조회하거나 저장할 수 있다.
Spring Data/JPA repository 직접 호출은 `infrastructure/persistence`의 domain 저장소 계약 구현체만 할 수 있다.
데이터 접근 전략, ORM 매핑, 조회 최적화, 저장 세부사항은 인프라 레이어가 책임진다.

## 반드시. 필수 조회는 domain reader/repository interface에서 의미를 드러낸다

도메인 서비스에서 반드시 필요한 데이터는 `findById()` 같은 선택적 조회보다 `getById()`처럼 실패 의미가 드러나는 reader/repository 메서드를 기본으로 사용한다.
데이터가 반드시 있어야 하는 흐름이라면 조회 실패를 nullable 반환으로 도메인까지 밀어 넣지 않는다.
구현체 또는 도메인 서비스 경계에서 `CoreException` 등 프로젝트 예외 체계로 변환하고, 도메인 객체는 정상적으로 전달된 상태를 기준으로 규칙을 수행한다.

## 반드시. interfaces는 입출력 어댑터로 제한한다

REST API, GraphQL, 메시지 컨슈머는 시스템 진입점이지만 도메인 규칙의 주체가 아니다.
controller, resolver, consumer, dto는 인증 정보 추출, 요청 검증, DTO 매핑, 응답 직렬화까지만 담당한다.
비즈니스 분기나 영속성 호출을 직접 넣지 않고 `application`으로 위임한다.

## 반드시. DTO와 도메인 객체를 구분한다

외부 계약과 내부 모델은 변경 속도와 관심사가 다르다.
`interfaces/dto`는 외부 입출력 전용으로 두고, `application`의 `command`, `query`, `result`는 내부 유스케이스 입출력 전용으로 둔다.
`domain`에는 DTO를 두지 않고, 도메인 의미와 불변식을 가진 `model`, `type`, `event`를 둔다.
하나의 DTO를 모든 계층에서 재사용하지 않는다.

## 반드시. 저장과 조회의 관심사가 다르면 분리한다

쓰기 모델과 읽기 모델은 성능 요구와 데이터 형태가 자주 다르다.
처음부터 reader, writer, loader를 과하게 분리하지 않는다.
기본은 도메인 repository interface로 시작하고, 조회/쓰기 관심사가 실제로 달라질 때 reader, writer, loader 같은 역할별 interface로 나눈다.
단건 상태 변경, 복잡 조회, 캐시 로딩, 배치 적재를 하나의 클래스에 몰아넣지 않는다.

## 권장. 저장소 계약 이름은 도메인 성격에 맞춘다

저장소 계약 이름은 DB의 insert, update, select보다 도메인에서 기대하는 역할과 저장 방식이 드러나야 한다.
일반적인 aggregate 조회와 저장은 `RoomRepository`, `MissionRepository`처럼 `Repository`를 기본값으로 사용한다.
`save()`는 새 aggregate 저장과 기존 aggregate 변경 저장을 함께 표현할 수 있으며, create/update를 정책적으로 구분해야 할 때만 메서드를 나눈다.
조회 전용 책임이 분명하면 `RoomMemberReader`, 쓰기 전용 책임이 분명하면 `RoomMemberWriter`처럼 분리한다.
append-only 성격의 outbox, queue, event stream, activity log는 `Writer`보다 `Appender`를 우선 검토한다.
이미 만들어진 이벤트나 메시지를 외부로 내보내는 책임은 `Publisher`, 푸시/이메일 같은 전송 책임은 `Sender`, 값 생성은 `Generator`, 값 제공은 `Provider`, 조건에 따른 결정은 `Resolver`처럼 이름을 나눈다.

예시:

```text
RoomRepository        # 일반 aggregate 조회/저장
RoomMemberReader     # 멤버 조회, count, exists 등 조회 전용
RoomMemberWriter     # 멤버 저장/제거 등 쓰기 전용
OutboxAppender       # 이벤트를 append-only 저장소에 추가
DomainEventPublisher # 저장된 이벤트를 외부 브로커로 발행
PushSender           # 푸시 메시지 전송
InviteCodeGenerator  # 초대 코드 생성
MissionPromptProvider # 미션 질문 제공
TodayMissionResolver # 조건에 맞는 오늘 미션 결정
```

## 반드시. 트랜잭션과 일관성 경계는 application에서 먼저 판단한다

여러 도메인 객체와 저장소를 엮는 시나리오는 실패 시 복구 범위를 명확히 해야 한다.
어떤 유스케이스가 하나의 원자 단위인지 `application`에서 먼저 정하고, 도메인 서비스와 도메인 객체는 그 안에서 자신의 규칙과 상태 변경을 수행하게 한다.
트랜잭션 편의 때문에 계층 경계를 무너뜨리지 않는다.

## 반드시. 도메인 간 협력은 application에서 조율한다

한 도메인의 서비스가 다른 도메인의 서비스를 직접 호출하면 경계가 흐려지고 순환 의존이 생기기 쉽다.
도메인 간 협력이 필요하면 `application` 레이어가 각 도메인의 도메인 서비스를 호출해서 조율한다.
서로 다른 도메인의 서비스끼리 직접 호출하는 구조는 허용하지 않는다.

## 반드시. 서비스 레이어를 비대하게 만들지 않는다

서비스 하나에 검증, 조회, 상태 변경, 외부 호출, 이벤트 발행, 예외 변환이 계속 누적되면 유지보수가 급격히 어려워진다.
서비스가 비대해지기 시작하면 entity/aggregate, validator, policy, factory, domain service, application facade, reader/writer/repository interface, repository 구현체 등으로 책임을 다시 나눈다.
특히 애플리케이션 서비스가 모든 규칙의 쓰레기통이 되면 안 된다.

## 권장. 도메인 서비스는 정말 필요한 경우에만 둔다

모든 규칙을 서비스에 모으면 엔티티가 빈약해지고 절차형 코드가 된다.
한 엔티티의 책임이면 엔티티에 두고, 여러 엔티티나 정책을 조합해야 할 때만 도메인 서비스를 둔다.
단순 위임 서비스는 만들지 않는다.

## 권장. validator와 policy를 구체적으로 분리한다

검증과 의사결정 규칙이 섞이면 재사용성과 테스트 가독성이 떨어진다.
입력이나 상태의 유효성 판단은 `validator`, 비즈니스 허용 여부와 선택 규칙은 `policy`로 분리한다.
이름만 분리하고 내부에서 모든 일을 처리하는 거대한 클래스는 만들지 않는다.

## 권장. 공통화는 반복이 확인된 뒤에만 한다

성급한 공통화는 도메인 언어를 흐리고 결합도를 높인다.
우선 각 도메인 안에서 명확하게 유지하고, 동일한 문제와 변경 이유가 반복될 때만 공통 모듈로 승격한다.

## 권장. 프레임워크 기능보다 구조적 명확성을 우선한다

편한 데코레이터나 헬퍼가 계층 경계를 흐리게 만들 수 있다.
프레임워크의 자동 매핑, 전역 상태, 암묵적 주입 기능을 사용할 때도 코드 흐름과 의존 방향이 명확하게 드러나는 선택을 우선한다.
테스트가 어려워지거나 계층 경계가 숨겨지면 사용을 줄이거나 감싼다.

## 권장. 이벤트 드리븐 아키텍처를 적재적소에 활용한다

도메인 간 느슨한 결합, 후처리 분리, 비동기 확장, 장애 격리가 필요한 경우 이벤트 드리븐 아키텍처를 우선 검토한다.
단, 단순한 동기 호출이 더 명확한 곳까지 무리하게 이벤트로 바꾸지는 않는다.
도메인 객체나 도메인 서비스는 `RoomNameChanged`, `MissionCompleted` 같은 도메인 이벤트를 생성하거나 기록할 수 있지만, 메시지 브로커를 직접 호출하지 않는다.
application은 트랜잭션 안에서 aggregate 저장과 이벤트 저장 범위를 조율하고, infrastructure는 outbox 저장소, 브로커 발행, 재시도 같은 운영 세부사항을 담당한다.
이벤트를 쓰는 경우 발행 조건, 소비 보장, 중복 처리, 실패 재처리를 함께 설계한다.

## 권장. 트랜잭셔널 아웃박스 패턴을 고려한다

상태 변경과 이벤트 발행을 함께 보장해야 하는 경우 트랜잭셔널 아웃박스 패턴을 우선 검토한다.
데이터 저장은 성공했는데 이벤트 발행이 누락되거나, 이벤트는 발행됐는데 상태 반영이 실패하는 불일치를 방지해야 한다.
도메인 이벤트와 외부로 발행할 integration event를 구분하고, 브로커 전송 포맷은 인프라 계층에서 변환한다.
이벤트 기반 설계에서 신뢰성이 중요한 플로우라면 기본 선택지로 본다.

## 권장. 보상 트랜잭션을 설계에 포함한다

분산 트랜잭션이나 비동기 후속 처리에서는 단순 롤백이 불가능한 경우가 많다.
단계별 실패 가능성이 있는 플로우는 보상 트랜잭션 또는 취소 전략을 함께 설계한다.
특히 결제, 예약, 재고, 쿠폰, 포인트 같은 상태 변경은 되돌림 전략 없이 구현하지 않는다.

## 권장. 동시성 처리는 요구사항에 맞게 명시적으로 설계한다

예약, 결제, 재고, 선착순, 토큰 발급 같은 기능은 동시성 충돌이 기본 전제다.
락, 버전 관리, 원자적 갱신, 큐잉, 멱등성 키 등 필요한 전략을 숨기지 말고 의도적으로 선택한다.
동시성 이슈가 예상되는 기능은 테스트와 로그, 메트릭까지 함께 설계한다.
