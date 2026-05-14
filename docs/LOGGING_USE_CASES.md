# Logging & tracing — ตัวอย่าง use case (starter-api)

เอกสารนี้อธิบายพฤติกรรมจริงของระบบ log บน **stdout แบบ JSON** (เหมาะกับ container), **correlation id**, **Micrometer/Brave trace**, **global exception logging**, และ **การเก็บ body เฉพาะเมื่อจำเป็น**

อ้างอิงโค้ดหลัก:

- [`LoggingProperties`](../src/main/java/com/starter/api/app/logging/LoggingProperties.java) — `app.logging.*`
- [`CorrelationIdFilter`](../src/main/java/com/starter/api/app/logging/CorrelationIdFilter.java)
- [`HttpRequestLogFilter`](../src/main/java/com/starter/api/app/logging/HttpRequestLogFilter.java)
- [`GlobalExceptionHandler`](../src/main/java/com/starter/api/app/exception/GlobalExceptionHandler.java)
- [`logback-spring.xml`](../src/main/resources/logback-spring.xml)

---

## 1) การตั้งค่า (สรุป)

| Property | ค่าเริ่มต้น | ความหมาย |
|----------|-------------|-----------|
| `app.logging.enabled` | `true` | เปิด/ปิด access log และ logic ของ `HttpRequestLogFilter` |
| `app.logging.log-body-on-error` | `true` | เมื่อ HTTP status ≥ 500 และ content-type เป็น JSON จะ log request/response body (หลัง mask) |
| `app.logging.log-body-debug` | `false` | เปิดแล้ว log body **ทุก status** (ใช้เฉพาะ dev/staging; ระวังข้อมูลอ่อนไหว) |
| `app.logging.max-body-bytes` | `8192` | จำกัดความยาว body ที่นำไป log |
| `app.logging.sensitive-field-names` | รายการในคลาส | ชื่อ field (ไม่สนตัวพิมพ์เล็กใหญ่) ที่จะถูกแทนที่ด้วย `***` ใน JSON |

**Tracing (แพลตฟอร์ม):**

- `management.tracing.sampling.probability` — อัตรา sampling ของ trace (production มักต่ำกว่า 1.0)
- Log บรรทัด JSON จะมี field จาก MDC เช่น `traceId`, `spanId` (จาก Brave) และ `requestId` (จาก correlation filter)

---

## 2) Correlation สำหรับ support / ข้ามทีม

**สถานการณ์:** ลูกค้าแจ้งปัญหา ต้องการให้ backend ค้น log บรรทัดเดียวกับคำขอนั้นได้

**ทำอย่างไร**

1. Client หรือ API Gateway ส่ง header **`X-Request-Id`** (หรือ `X-Correlation-Id`) มาทุกคำขอ
2. ถ้าไม่ส่ง ระบบสร้าง UUID ให้ และส่งกลับใน response header **`X-Request-Id`** เสมอ
3. ทุกบรรทัด log ของคำขอนั้นจะมี **`requestId`** ใน JSON (MDC)

**ตัวอย่าง request**

```http
GET /api/v1/health HTTP/1.1
Host: api.example.com
X-Request-Id: ticket-2026-0514-001
```

**สิ่งที่ควรเห็นใน log (ย่อ field)**

- `message` ของ access log: `http_request method=GET path=/api/v1/health status=200 duration_ms=...`
- `requestId`: `ticket-2026-0514-001`
- response ไป client มี header `X-Request-Id: ticket-2026-0514-001`

---

## 3) Distributed trace (ข้าม service / Datadog / APM)

**สถานการณ์:** มีหลาย service ต้องการ trace เดียวกัน

**พฤติกรรมปัจจุบัน**

- Spring Boot + **Micrometer Brave** สร้าง/รับ **W3C `traceparent`** ผ่าน `ServerHttpObservationFilter`
- บรรทัด log จะมี **`traceId`** และ **`spanId`** ใน JSON เมื่อมี active span

**Use case**

1. Service A เรียก starter-api พร้อม header มาตรฐาน `traceparent`
2. Log ของ starter-api ใช้ `traceId` เดียวกับ A (ค้นใน Datadog/Tempo/Jaeger ได้)
3. **`requestId`** ยังใช้แยก “เลข ticket ฝั่งธุรกิจ” ได้คู่กับ trace ทางเทคนิค

เมื่อมี **`RestClient` / `RestClient.Builder`** จาก Spring context การส่งต่อ observation/tracing ไปยัง downstream ทำได้ผ่าน auto-configuration ของ Spring Boot (ไม่ต้องฝัง vendor SDK สำหรับ log)

---

## 4) Request ปกติสำเร็จ (ไม่ log body)

**เงื่อนไข:** `app.logging.log-body-debug=false`, status 2xx/3xx/4xx ที่ไม่ใช่ 500+

**ผลลัพธ์**

- มีบรรทัด **INFO** `http_request method=... path=... status=... duration_ms=...`
- **ไม่**มี `http_request_body` / `http_response_body` (ประหยัด memory และลดความเสี่ยงข้อมูลรั่ว)

---

## 5) Business error (4xx) — log เตือน ไม่ stack หนัก

**สถานการณ์:** โยน `ApiBusinessException` เช่น not found

**ผลลัพธ์**

- HTTP status ตามที่กำหนดใน exception
- Response envelope `ApiResponse` พร้อม `errorCode` ที่อ่านได้จาก client
- Log ระดับ **WARN** จาก `GlobalExceptionHandler`: ข้อความ `business_error code=... httpStatus=... message=...`
- **ไม่**มี stack trace เต็มสำหรับเคส business ที่คาดไว้

---

## 6) Validation error (400)

**สถานการณ์:** `@Valid` ล้มเหลว → `MethodArgumentNotValidException`

**ผลลัพธ์**

- HTTP 400, `errorCode` = `VALIDATION_ERROR`
- Log **WARN** `validation_error` พร้อมสรุป field errors (ถูก truncate ไม่เกิน ~2000 ตัวอักษรใน log message)

---

## 7) JSON ผิดรูปแบบ (400)

**สถานการณ์:** body ไม่ใช่ JSON ที่อ่านได้ แต่ส่ง `Content-Type: application/json`

**ผลลัพธ์**

- HTTP 400, `errorCode` = `BAD_REQUEST`, message ทั่วไปต่อ client
- Log **WARN** `http_message_not_readable` พร้อมสาเหตุย่อ (truncate)

---

## 8) Server error (5xx) — client ได้ข้อความทั่วไป, log มีรายละเอียด + body (ถ้าเปิด)

**สถานการณ์:** bug / ข้อผิดพลาดไม่คาดคิด → `Exception` ถูก catch ที่ `GlobalExceptionHandler`

**ผลลัพธ์ต่อ client**

- HTTP 500
- Body: `errorCode` = `INTERNAL_ERROR`, `message` = `An unexpected error occurred` (คงที่ — ไม่รั่วรายละเอียดภายใน)

**ผลลัพธ์ใน log**

- **ERROR** `unhandled_exception` พร้อม stack trace
- ถ้า `app.logging.log-body-on-error=true` และ request/response เป็น **application/json**:
  - **WARN** `http_request_body {...}` (ค่าอ่อนไหวถูก mask)
  - **WARN** `http_response_body {...}`

**ตัวอย่างการ mask**

Request JSON:

```json
{"username":"alice","password":"super-secret"}
```

ใน log ควรเห็น `password` เป็น `***` ไม่ควรเห็น `super-secret` (รายการ key เริ่มต้นอยู่ใน `LoggingProperties`)

---

## 9) โหมด debug — log body ทุก status

**สถานการณ์:** ไล่ปัญหาใน staging ต้องเห็น body แม้ response 200

**ตั้งค่า**

```properties
app.logging.log-body-debug=true
```

**ผลลัพธ์**

- หลังจบคำขอ จะ log `http_request_body` และ `http_response_body` (JSON) **ทุกครั้ง** ที่มี body และ content-type เป็น JSON
- **ห้าม**เปิดใน production ยกเว้นมีเหตุจำเป็นและระยะเวลาสั้นมาก

---

## 10) Path ที่ไม่เก็บ body / ไม่ access-log จาก filter นี้

| Path prefix | พฤติกรรม |
|-------------|----------|
| `/actuator` | `HttpRequestLogFilter` **ไม่รัน** (ลด noise); correlation ยังทำงานกับคำขออื่นตามปกติ |
| `/swagger-ui`, `/v3/api-docs` | ไม่ใช้ content-caching สำหรับ body (ลด memory / noise) แต่ยังมี access log บรรทัดเดียว |

---

## 11) ปิดระบบ access log ชั่วคราว

```properties
app.logging.enabled=false
```

ใช้เมื่อต้องการลด log volume หรือ debug ปัญหา filter เอง (ยังมี log จาก Spring/แอปอื่นได้ตาม level)

---

## 12) การค้นใน Elasticsearch / Datadog (แนวทาง)

แอปพิมพ์ **JSON บน stdout** — ฝั่งแพลตฟอร์มใช้ agent เก็บต่อ

**คีย์ที่ควรใช้ค้น**

- `requestId` — ตรงกับ `X-Request-Id` ที่ client แจ้ง
- `traceId` — ตรงกับ distributed trace
- `message` เช่น `http_request`, `business_error`, `unhandled_exception`
- `service` — ชื่อ service (จาก `spring.application.name` + custom field ใน logback)

---

## 13) Checklist ก่อนขึ้น production

- ปิด `app.logging.log-body-debug` ใน production
- พิจารณา `management.tracing.sampling.probability` ให้เหมาะกับ traffic และค่าใช้จ่าย APM
- ทบทวน `app.logging.sensitive-field-names` ให้ครอบคลุมฟิลด์ของโดเมนจริง (PII, token ของพาร์ทเนอร์ ฯลฯ)
- ยืนยันว่า gateway ส่ง `traceparent` และ/หรือ `X-Request-Id` ตามมาตรฐานทีม

---

## 14) การทดสอบอัตโนมัติ

ดู [`LoggingIntegrationTest`](../src/test/java/com/starter/api/app/logging/LoggingIntegrationTest.java) และ [`TestLoggingController`](../src/test/java/com/starter/api/testsupport/TestLoggingController.java) เป็นตัวอย่างการ assert header, envelope 500, และการไม่รั่วรหัสผ่านใน stdout
