# แนวทางสถาปัตยกรรม (Architecture Guideline)

> ฉบับภาษาอังกฤษ: [ARCHITECTURE_GUIDELINE.md](./ARCHITECTURE_GUIDELINE.md)

กฎที่นักพัฒนาทุกคนปฏิบัติตาม เมื่อไม่แน่ใจให้เปิดเอกสารนี้ก่อน

เอกสารนี้สอดคล้องกับโค้ดเบส **ปัจจุบัน** ของ `starter-api` และ baseline **เฟส 1** ของ modular monolith

---

## สิ่งที่มีอยู่ในปัจจุบัน

| พื้นที่ | ตำแหน่ง / หมายเหตุ |
| --- | --- |
| จุดเข้าแอป | `com.starter.api.StarterApplication` |
| การประกอบแอป | `app/` (`config`, `exception`, `security`, `logging` — filter และคุณสมบัติการล็อก) |
| shared kernel | `shared/api/ApiResponse.java`, `shared/error/ErrorCodes.java`, `shared/exception/ApiBusinessException.java` |
| Health endpoint | `modules/health/api/HealthController.java` เปิด `GET /api/v1/health` |
| โมดูล auth | `modules/auth/api`, `modules/auth/domain`, `modules/auth/infra` (เฉพาะโครงสร้าง) |
| Security | `app/security/SecurityConfig` เป็น placeholder; ยังไม่มี auth chain จริง |

---

## โครงสร้างโมดูล (เฟส 1)

```
app/       Application composition and cross-cutting web configuration
shared/    Shared kernel used by multiple modules
modules/
  ├── health/  Lightweight liveness API
  └── auth/    Authentication module skeleton (api/domain/infra)
```

เฟสถัดไป (บันทึกไว้เท่านั้น ยังไม่ได้ implement):

```
facility/  floorplan/  interest/  payment/
```

### ทิศทางการพึ่งพา (Dependency Direction)

```
app            -> modules.* and shared (compose modules)
modules.auth   -> shared
modules.health -> shared
shared         -> (must not depend on modules.* / business modules)
```

กฎ:

- การเรียกข้ามโมดูลต้องใช้สัญญาสาธารณะของโมดูล (โดยทั่วไปแพ็กเกจ `api`)
- ห้าม import แพ็กเกจ `infra` ของโมดูลอื่นโดยตรง
- adapter กับระบบภายนอกอยู่ใน `infra` ของโมดูลเจ้าของ (เช่น `modules/auth/infra`) ไม่ใช่ root integration กลาง
- รักษา `shared` ให้ปราศจาก business logic เฉพาะโดเมน

---

## Response Envelope

ทุก endpoint คืนค่า `ApiResponse<T>` (ดู `shared/api/ApiResponse.java`)

รูปแบบกำหนดใน `shared/api/ApiResponse.java` (Jackson `JsonInclude.Include.NON_NULL` — ฟิลด์ null จะไม่ถูกส่งใน JSON)

```json
// Success (ApiResponse.success(data) - message defaults to "OK")
{ "success": true, "data": { ... }, "message": "OK", "timestamp": "..." }

// Error (ApiResponse.error(code, message))
{ "success": false, "errorCode": "BILL_NOT_FOUND", "message": "...", "timestamp": "..." }
```

ใช้ `ApiResponse.success(data, message)` เมื่อต้องการข้อความสำเร็จที่กำหนดเองสำหรับ client ใช้ `ApiResponse.error(code, message, data)` เมื่อต้องการแนบ payload ตอนล้มเหลว

**ห้าม** คืนค่า object ดิบ, `String` ลอยๆ หรือ body ของ `ResponseEntity` แบบ ad-hoc สำหรับ business API โดยไม่ใช้ `ApiResponse`

---

## รหัสข้อผิดพลาด (Error Codes)

เพิ่มรหัส business error ทั้งหมดใน `shared/error/ErrorCodes.java` เป็นค่าคงที่ `public static final String`

การตั้งชื่อ: `FEATURE_REASON` ในรูปแบบ `UPPER_SNAKE_CASE`

```java
// Good
BILL_NOT_FOUND
AUTH_INVALID_CREDENTIALS
PAYMENT_INSUFFICIENT_BALANCE

// Bad - too generic, no feature prefix
NOT_FOUND
ERROR
FAILED
```

เมื่อเพิ่มรหัสใหม่: append ใน `ErrorCodes.java` พร้อม prefix ของ feature **ห้ามลบหรือเปลี่ยนชื่อ** รหัสที่ client หรือ log พึ่งพาอยู่แล้ว

---

## การล็อก การ trace และ correlation

API ล็อกเป็น **JSON โครงสร้างไปที่ stdout** (เหมาะกับ container; agent ของแพลตฟอร์มส่งต่อไป Elasticsearch, Datadog ฯลฯ) อย่าพึ่งไฟล์ `.log` ในเครื่องใน production และอย่า embed SDK ขนส่งล็อกของ vendor ในแอป

| ประเด็น | ตำแหน่ง / หมายเหตุ | วิธี trace หรือ correlate |
| --- | --- | --- |
| Support / ticket id | `app/logging/CorrelationIdFilter` — อ่าน `X-Request-Id` / `X-Correlation-Id` ขาเข้า เก็บ **`requestId`** ใน MDC และสะท้อน id ใน response | กรองบรรทัดล็อกด้วย **`requestId`** (หรือค่า header ที่ส่งกลับให้ client) |
| Distributed trace (APM) | Micrometer / Brave — W3C **`traceparent`** ขาเข้า, MDC **`traceId`** / **`spanId`** ต่อคำขอ | กรองด้วย **`traceId`** ข้ามบริการเมื่อ caller ส่งต่อ tracing header |
| บรรทัด access + body (ถ้ามี) | `app/logging/HttpRequestLogFilter`, `LoggingProperties` (`app.logging.*`) | บรรทัด INFO `http_request`; body เป็น JSON เมื่อ **5xx** หรือเมื่อ flag **debug** เปิด (พร้อม redaction) |
| Error กลาง + ระดับล็อก | `app/exception/GlobalExceptionHandler` | คีย์ล็อกเช่น `business_error`, `validation_error`, `unhandled_exception` |
| HTTP ขาออก | `app/config/RestClientRequestIdPropagationConfig` — **`RestClientCustomizer`** คัดลอก **`requestId`** จาก MDC ไปยัง **`X-Request-Id`** ขาออกสำหรับ `RestClient` ที่สร้างจาก `RestClient.Builder` ที่ auto-config | บริการ downstream ที่ใช้ pattern เดียวกันจะเห็น support id **เดียวกัน** |

อ้างอิงการตั้งค่าและตัวอย่าง end-to-end (รวม interceptor **`X-Request-Id`** ของ **starter-app**): **`docs/LOGGING_USE_CASES.md`** filter เว็บข้ามตัดหรือการ wire HTTP client ใหม่ควรอยู่ใน **`app/`** (ไม่ใช่ใน `modules/*`) เว้นแต่ทีมจะยกระดับเป็น shared library อย่างชัดเจน

---

## กฎ Controller

- ไม่มี business logic — ส่งต่อ service แล้วคืน `ApiResponse.success(...)`
- ใช้ `@Valid` กับทุก `@RequestBody` ที่รับอินพุต
- เพิ่ม `@Operation` และ `@ApiResponses` ทุก endpoint (Springdoc)
- endpoint แบบ list ควรรองรับ pagination ที่เหมาะสม (`page` / `size`)

---

## กฎ Service

- business logic อยู่ใน domain service ของโมดูล
- `@Transactional` บน path ที่เขียน; `@Transactional(readOnly = true)` บนการอ่านที่เหมาะสม
- throw `ApiBusinessException` สำหรับความล้มเหลวทางธุรกิจที่คาดได้
- ห้ามคืน JPA entity โดยตรงจากเมธอด service ที่หันหน้า API

---

## กฎ Repository / Entity

- ใช้ Spring Data derived query หรือ `@Query` กับ JPQL เป็นหลัก
- หลีกเลี่ยง native SQL เว้นแต่ยอมรับ trade-off ด้าน portability และบันทึกไว้
- ใช้ `BigDecimal` สำหรับเงิน
- ใช้ `@Enumerated(EnumType.STRING)` กับ enum
- โดยค่าเริ่มต้นเลือก lazy fetch บน association

---

## การจัดการสคีมาฐานข้อมูล (Flyway)

Flyway เป็นแหล่งอ้างอิงหลักของสคีมาใน local และสภาพแวดล้อมที่ใช้ร่วมกัน

กฎ:

- ทุกการเปลี่ยนสคีมาเป็น **ไฟล์ใหม่** ใต้ `src/main/resources/db/migration/`
- การตั้งชื่อ: `V{n}__{description}.sql`
- **ห้าม** แก้ migration ที่ apply แล้ว
- อย่าพึ่ง `ddl-auto=update` ในสภาพแวดล้อมที่ใช้ร่วมหรือ production

---

## Security (สถานะปัจจุบันและขั้นถัดไป)

**ปัจจุบัน:**

- `app/security/SecurityConfig` มีเฉพาะ placeholder
- **ไม่มี** Spring Security filter chain, **ไม่มี** JWT/OAuth2 และ **ไม่มี** auth interceptor ที่ wire แล้ว
- CORS ปัจจุบันอนุญาต `http://localhost:*` สำหรับการพัฒนา

**ขั้นถัดไปสำหรับโมดูล auth:**

1. implement สัญญาใน `modules/auth/api`
2. เพิ่ม business rule และ logic token/policy ใน `modules/auth/domain`
3. เพิ่ม persistence/adapter ภายนอกใน `modules/auth/infra`
4. wire security จริงใน `app/security` และกำหนด public/protected routes ให้ชัดเจน

---

## สัญญา health endpoint

- Endpoint: `GET /api/v1/health`
- วัตถุประสงค์: สัญญาณ liveness เบาๆ สำหรับ server/runtime
- payload ที่คาดหวัง: สถานะบริการ (`UP`), ชื่อบริการ, timestamp

---

## Checklist: เพิ่มโมดูลใหม่

เมื่อเพิ่มโมดูลใต้ `modules/<name>/` ใช้ checklist ขั้นต่ำนี้:

- [ ] สร้างโครงสร้างแพ็กเกจ: `modules/<name>/api`, `modules/<name>/domain`, `modules/<name>/infra`
- [ ] วาง controller/สัญญาสาธารณะของโมดูลใน `api`
- [ ] วาง business rule และ orchestration ใน `domain` (ไม่ใช่ใน controller)
- [ ] วาง database/adapter ภายนอกใน `infra` (เช่น REST client, storage, messaging)
- [ ] การเรียกข้ามโมดูลใช้สัญญา `api` เท่านั้น; ห้าม import `infra` ของโมดูลอื่น
- [ ] ถ้า logic เป็น generic และหลายโมดูลใช้ร่วม ย้ายไป `shared` (และให้ปราศจาก business เฉพาะโดเมน)
- [ ] เพิ่มเทสต์เฉพาะโมดูล (path controller/service และกรณี error ที่มีความหมาย)
- [ ] อัปเดต `README.md` และแนวทางนี้เมื่อขอบเขตโมดูลหรือสัญญาเปลี่ยน

---

## Definition of Done

สำหรับ REST feature ใหม่แต่ละรายการ ยืนยันว่า:

- [ ] Controller ส่งต่อ service — ไม่มี business rule ใน controller
- [ ] Response ใช้ `ApiResponse<T>`
- [ ] ความล้มเหลวทางธุรกิจใช้ `ApiBusinessException` พร้อมค่าคงที่จาก `ErrorCodes.java`
- [ ] รหัส error ใหม่มี prefix ของ feature และเพิ่มใน `ErrorCodes.java`
- [ ] `@Valid` บน request body ที่ต้อง validate
- [ ] `@Transactional` เหมาะสมบนเมธอด service
- [ ] ไม่คืน JPA entity เป็น body สาธารณะของ API โดยตรง
- [ ] endpoint มีเอกสารด้วย `@Operation` และ `@ApiResponses`
- [ ] เทสต์ครอบคลุม success และแต่ละ path error ที่มีความหมาย
- [ ] การเปลี่ยนสคีมาส่งเป็น Flyway migration ใหม่
- [ ] ปฏิบัติตาม baseline การล็อก/correlation (ไม่ล็อกความลับ; ดู `docs/LOGGING_USE_CASES.md` และหัวข้อ **การล็อก การ trace และ correlation** ด้านบน)
