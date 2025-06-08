# 🎫 선착순 쿠폰 발급 시스템

대용량 트래픽에서 안전하고 효율적인 선착순 쿠폰 발급을 위한 Spring Boot 기반 시스템입니다.

## 📋 목차

- [프로젝트 개요](#-프로젝트-개요)
- [주요 기능](#-주요-기능)
- [기술 스택](#-기술-스택)
- [아키텍처](#-아키텍처)
- [핵심 문제 해결](#-핵심-문제-해결)

## 🎯 프로젝트 개요

선착순 이벤트 서비스 개발 시 발생하는 대표적인 문제들을 해결한 고성능 쿠폰 발급 시스템입니다.

### 해결하고자 한 문제

- **동시성 문제**: 정해진 수량보다 많은 쿠폰이 발급되는 Race Condition
- **성능 문제**: 대량 트래픽으로 인한 서버 다운 및 응답 지연
- **중복 발급**: 한 사용자가 여러 개의 쿠폰을 발급받는 문제
- **안정성**: 시스템 장애 시 쿠폰 발급 누락 방지

## ✨ 주요 기능

- ⚡ **고성능 동시성 제어**: Redis INCR을 통한 원자적 연산
- 🔒 **중복 발급 방지**: Redis Set을 활용한 사용자별 발급 제한
- 📊 **비동기 처리**: Kafka를 통한 부하 분산
- 🔄 **장애 복구**: 실패한 쿠폰 발급 재처리 시스템
- 🎯 **정확한 수량 제어**: 선착순 100명 정확히 제한

## 🛠 기술 스택

### Backend
- **Java 17**
- **Spring Boot 3.x**
- **Spring Data JPA**
- **Spring Kafka**

### Database & Cache
- **MySQL**: 쿠폰 데이터 영구 저장
- **Redis**: 동시성 제어 및 중복 방지

### Message Queue
- **Apache Kafka**: 비동기 쿠폰 발급 처리

### Testing
- **JUnit 5**

## 🏗 아키텍처

```
┌─────────────────┐    ┌──────────────┐    ┌─────────────────┐
│   Client        │────│   API        │────│   Redis         │
│   (Multiple     │    │   Server     │    │   (INCR/SET)    │
│   Requests)     │    │              │    │                 │
└─────────────────┘    └──────────────┘    └─────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │   Kafka          │
                    │   Producer       │
                    └──────────────────┘
                              │
                              ▼
                    ┌──────────────────┐    ┌─────────────────┐
                    │   Kafka          │────│   MySQL         │
                    │   Consumer       │    │   (Coupon DB)   │
                    └──────────────────┘    └─────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │   Failed Event   │
                    │   Handler        │
                    └──────────────────┘
```

## 🔧 핵심 문제 해결

### 1. 동시성 문제 해결 (Race Condition)

**문제**: 여러 스레드가 동시에 쿠폰 개수를 확인할 때 정해진 수량을 초과하여 발급

**해결책**: Redis INCR 활용
```java
public void apply(Long userId) {
    long count = couponCountRepository.increment(); // 원자적 연산
    
    if (count > 100) {
        return;
    }
    
    couponCreateProducer.create(userId);
}
```

### 2. 중복 발급 방지

**문제**: 한 사용자가 여러 개의 쿠폰을 발급받을 수 있음

**해결책**: Redis Set 자료구조 활용
```java
public void apply(Long userId) {
    Long apply = appliedUserRepository.add(userId); // Redis SADD
    if(apply != 1) {
        return; // 이미 발급받은 사용자
    }
    // 쿠폰 발급 로직...
}
```

### 3. 성능 최적화

**문제**: 대량 트래픽으로 인한 서버 과부하

**해결책**: Kafka를 통한 비동기 처리
- Producer: 빠른 응답으로 사용자 경험 개선
- Consumer: 안정적인 쿠폰 발급 처리

### 4. 장애 복구 시스템

**문제**: Consumer에서 에러 발생 시 쿠폰 발급 누락

**해결책**: Failed Event 저장 및 배치 재처리
```java
@KafkaListener(topics = "coupon_create", groupId = "group_1")
public void listener(Long userId) {
    try {
        couponRepository.save(new Coupon(userId));
    } catch (Exception e) {
        logger.error("failed to save coupon: "+ userId);
        failedEventRepository.save(new FailedEvent(userId));
    }
}
```

### API 엔드포인트

#### 쿠폰 발급 신청
```http
POST /api/coupons/apply
Content-Type: application/json

{
    "userId": 1
}
```

#### 응답 예시
```json
{
    "status": "success",
    "message": "쿠폰 발급 신청이 완료되었습니다."
}
```

### Redis 명령어로 상태 확인

```bash
# 발급된 쿠폰 수 확인
redis-cli
> GET coupon_count

# 발급받은 사용자 목록 확인
> SMEMBERS applied_user
```

주요 테스트 케이스:
- `한번만응모()`: 기본 기능 테스트
- `여러명응모_정합성_문제_해결_with_Redis()`: 동시성 해결 검증
- `한명당_한개의_쿠폰발급()`: 중복 발급 방지 검증
