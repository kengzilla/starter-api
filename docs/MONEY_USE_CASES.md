# `Money` use cases & sample flows

เอกสารนี้โฟกัส **การใช้ `com.starter.api.shared.domain.Money` เท่านั้น** — รวมตัวอย่าง waterfall (**ข้อ 9**), กรณีศึกษาตารางผ่อนและทศนิยม (**ข้อ 8**), และการเขียนแบบลำดับคำสั่ง (`min` / `subtract`) โดยไม่พึ่งคลาส helper แยก

---

## สารบัญ

1. [สินเชื่อ: เงินต้น + ค่าธรรมเนียม + VAT + ชำระคืนเงินต้น](#1-สินเชื่อ-เงินต้น--ค่าธรรมเนียม--vat--ชำระคืนเงินต้น)
2. [ลีส / แบ่งยอดเป็นจำนวนงวด (`divide`)](#2-ลีส--แบ่งยอดเป็นจำนวนงวด-divide)
3. [ดอกเบี้ยงวดด้วย `multiply` และหมายเหตุ EIR](#3-ดอกเบี้ยงวดด้วย-multiply-และหมายเหตุ-eir)
4. [จำกัดยอดชำระไม่เกินยอดคงเหลือ (`min`)](#4-จำกัดยอดชำระไม่เกินยอดคงเหลือ-min)
5. [รวมหลายบรรทัด (`zero` + `add`)](#5-รวมหลายบรรทัด-zero--add)
6. [ภาษี: ฐาน + VAT → รวม](#6-ภาษี-ฐาน--vat--รวม)
7. [ข้อควรระวัง](#7-ข้อควรระวัง)
8. [กรณีศึกษา: ผ่อน 24 งวด · ทศนิยม · EIR](#8-กรณีศึกษา-ผ่อน-24-งวด--ทศนิยม--eir)
9. [ลูกค้าชำระเงิน — waterfall ด้วย `Money` อย่างเดียว](#9-ลูกค้าชำระเงิน--waterfall-ด้วย-money-อย่างเดียว)
10. [Overpayment / ยอดเกิน](#10-overpayment--ยอดเกิน)
11. [สรุปยอดส่ง GL หรือ interface](#11-สรุปยอดส่ง-gl-หรือ-interface) (11.1–11.4 ตัวอย่างครบ)
12. [Policy ที่ทีมต้องกำหนดเอง](#12-policy-ที่ทีมต้องกำหนดเอง)
13. [Related code](#related-code)

---

## 1. สินเชื่อ: เงินต้น + ค่าธรรมเนียม + VAT + ชำระคืนเงินต้น

**Use case:** มีเงินต้นวงสินเชื่อ คิดค่าธรรมเนียมจัดการร้อยละหนึ่ง บวก VAT 7% บนค่าธรรมเนียม แล้วมีการชำระเข้ามาหักเงินต้น

```java
import com.starter.api.shared.domain.Money;
import java.math.BigDecimal;

Money principal = Money.thb("500000.00");
BigDecimal feeRate = new BigDecimal("0.01");
Money arrangementFee = principal.multiply(feeRate); // 5,000.00 THB
Money vatOnFee = arrangementFee.multiply(new BigDecimal("0.07")); // 350.00 THB
Money totalFeeCharge = arrangementFee.add(vatOnFee);

Money payment = Money.thb("15000.00");
Money remainingPrincipal = principal.subtract(payment);

if (remainingPrincipal.isNegative()) {
    throw new IllegalStateException("Overpayment vs principal");
}
```

`Money` จะ normalize ทศนิยมตามสกุลเงิน (THB = 2 ตำแหน่ง) ทุกครั้งที่สร้างค่าใหม่

---

## 2. ลีส / แบ่งยอดเป็นจำนวนงวด (`divide`)

**Use case:** ยอด financed แบ่งเท่าๆ ตามจำนวนงวด (ตัวอย่างง่าย — งวดจริงมักมี day-count และกฎปัดเศษแยก)

```java
import com.starter.api.shared.domain.Money;
import java.math.BigDecimal;

Money financedAmount = Money.thb("960000");
int periods = 48;
Money perPeriod = financedAmount.divide(BigDecimal.valueOf(periods));
```

---

## 3. ดอกเบี้ยงวดด้วย `multiply` และหมายเหตุ EIR

**Use case:** คำนวณส่วนดอกเบี้ยจากยอดเงินต้นคงเหลือคูณอัตรารายเดือน (ตัวเลขอัตราเป็นตัวอย่าง)

```java
import com.starter.api.shared.domain.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;

Money principalOutstanding = Money.thb("100000.00");
BigDecimal monthlyRate = new BigDecimal("0.0041666667"); // ตัวอย่างเท่านั้น
Money interestPortion = principalOutstanding.multiply(monthlyRate);
```

**หมายเหตุ EIR / ตารางงวด:** การวนคำนวณ EIR หรือ schedule หลายรอบ บางทีมเก็บ **ค่ากลางเป็น `BigDecimal` ความละเอียดสูง** แล้วค่อยห่อเป็น `Money` ตอน “โพสต์” หรือแสดงผล — ดูที่ Javadoc ของ `Money` ในโค้ดหลัก

```java
BigDecimal raw = principalOutstanding
    .amount()
    .multiply(monthlyRate)
    .setScale(6, RoundingMode.HALF_UP);
Money interestRounded = Money.thb(raw);
```

กรณีศึกษาแบบมีตัวเลขครบ (249,000 THB · 19.86% ต่อปี · 24 งวด) รวม PMT, ตารางงวด, และเมื่อไหร่ควรใช้ EIR — ดู **[ข้อ 8](#8-กรณีศึกษา-ผ่อน-24-งวด--ทศนิยม--eir)**

---

## 4. จำกัดยอดชำระไม่เกินยอดคงเหลือ (`min`)

**Use case:** ลูกค้าเสนอชำระ X แต่ต้องไม่เกินยอดคงค้าง

```java
import com.starter.api.shared.domain.Money;

Money outstanding = Money.thb("250000.00");
Money proposedPayment = Money.thb("300000.00");
Money appliedPayment = proposedPayment.min(outstanding);
// appliedPayment = 250,000 THB
```

หรือใช้ `if (proposedPayment.isGreaterThan(outstanding))` แล้วกำหนดค่าเองแบบ imperative ก็ได้

---

## 5. รวมหลายบรรทัด (`zero` + `add`)

**Use case:** รวมยอดบรรทัดใบเสร็จหรือรายการ interface

```java
import com.starter.api.shared.domain.Money;

Money total = Money.zero();
total = total.add(Money.thb("1000.00"));
total = total.add(Money.thb("250.50"));
// total → THB 1250.50
```

ถ้าในอนาคตมีหลายสกุลเงิน ใช้ `Money.zero(Currency.getInstance("USD"))` แล้วบวกเฉพาะ `Money` สกุลเดียวกัน

---

## 6. ภาษี: ฐาน + VAT → รวม

**Use case:** ฐานภาษี × 7% แล้วบวกเป็นยอดรวมก่อนส่งภาษี / localization

```java
import com.starter.api.shared.domain.Money;
import java.math.BigDecimal;

Money net = Money.thb("10000.00");
Money vat = net.multiply(new BigDecimal("0.07"));
Money gross = net.add(vat);
```

---

## 7. ข้อควรระวัง

- **ห้ามบวกคนละสกุล** — `add` / `min` / `compareTo` จะโยน `IllegalArgumentException` เมื่อสกุลไม่ตรงกัน
- **`thb(String)`** ต้องเป็นรูปแบบที่ `BigDecimal(String)` รองรับ เช่น `"1234.56"` ไม่ใช่ `"1,234.56"`
- **`null`** — ส่ง `null` เข้า `Money` หรือใช้กับเมธอดที่คาดหวัง `Money` จะได้ `NullPointerException` หรือข้อความจาก `requireNonNull` ตามจุดที่เรียก

---

## 8. กรณีศึกษา: ผ่อน 24 งวด · ทศนิยม · EIR

ตัวอย่างนี้ผูกกับคำถามว่า **`Money` กับความละเอียดทศนิยม** ใช้ร่วมกับตารางผ่อนอย่างไร และ **EIR** จำเป็นหรือไม่

### 8.1 สมมติฐาน (ตัวเลขจากโจทย์)

| รายการ | ค่า |
|--------|-----|
| ราคาสินทรัพย์ (รวม VAT) ใช้เป็นยอดจัดไฟแนนซ์ | **249,000.00 THB** |
| อัตราดอกเบี้ย nominal ต่อปี | **19.86%** (ต้องยืนยันในสัญญาว่าเป็น nominal แบบไหน — ที่นี่ใช้แบบ **หาร 12 เป็นอัตรารายเดือน**) |
| จำนวนงวด | **24** เดือน (2 ปี) |

> **หมายเหตุ VAT:** ถ้าสัญญาแยกฐานภาษี / ยอดกู้ไม่เท่าราคารวม VAT ต้องใช้เงินต้นตามสัญญา ไม่ใช่ 249,000 โดยอัตโนมัติ

### 8.2 อัตราดอกรายเดือน (nominal)

แบบจำลองทั่วไป: \( r = 0{,}1986 \div 12 = 0{,}01655 \) ต่อเดือน

เก็บใน `BigDecimal` ด้วยความละเอียดพอ (เช่น 8–10 หลาย) ก่อนนำไปคูณกับยอดคงค้าง — **อย่าใช้ `double`**

### 8.3 ค่างวดคงที่ (PMT) แบบดอกลดต้นลด (annuity)

สูตรค่างวดคงที่เมื่อดอกคิดจากยอดคงค้างและอัตราคงที่รายเดือน:

\[
\text{PMT} = P \times \frac{r(1+r)^n}{(1+r)^n - 1}
\]

เมื่อ \(P = 249{,}000\), \(r = 0{,}1986/12\), \(n = 24\)  
คำนวณด้วย `BigDecimal` แล้วปัด `HALF_UP` เป็นสตางค์ (เหมือน `Money`) ได้ประมาณ:

**ค่างวด ≈ 12,656.03 THB / เดือน**

(ตัวเลขนี้เป็น **อ้างอิงทางคณิตศาสตร์** จากสมมติฐานเดียวกับตารางด้านล่าง — โปรดักต์จริงต้องตรวจกฎปัดและงวดสุดท้าย)

### 8.4 แยกเงินต้น / ดอกในแต่ละงวด และตารางผ่อน

ในแต่ละงวด \(t\) (ทั่วไป):

1. **ดอกเบี้ยงวด** ≈ ยอดคงค้างก่อนชำระ × \(r\) — คำนวณเป็น `BigDecimal` ด้วย scale กลางทาง **6–8 หลาย** แล้วปัดเป็น **2 หลาย** (สตางค์) ตามนโยบาย  
2. **เงินต้นงวด** = ค่างวดที่ปัดแล้ว − ดอกที่ปัดแล้ว  
3. **ยอดคงค้างใหม่** = ยอดเดิม − เงินต้นงวด (เก็บเป็น `BigDecimal` ระหว่างลูป ลด drift)

**ตัวอย่างงวดแรกๆ** (ใช้ PMT = 12,656.03 และปัดดอกเป็นสตางค์แบบ HALF_UP — เพื่อสาธิตเท่านั้น):

| งวด | ค่างวด (THB) | ดอก (THB) | เงินต้น (THB) | คงเหลือเงินต้นโดยประมาณ (THB) |
|-----|----------------|------------|----------------|----------------------------------|
| 1 | 12,656.03 | 4,120.95 | 8,535.08 | 240,464.92 |
| 2 | 12,656.03 | 3,979.69 | 8,676.34 | 231,788.58 |
| 3 | 12,656.03 | 3,836.10 | 8,819.93 | 222,968.65 |
| … | … | … | … | … |
| 24 | (มักปรับงวดสุดท้ายให้คงเหลือ = 0) | … | … | 0 |

ยอดคงเหลือคอลัมท้ายอาจคลาดเคลื่อน **0.01** ต่อจุดถ้านโยบายปัดดอก/เงินต้นไม่ตรงกับที่ระบบใช้ — **งวดสุดท้าย** มักปรับ “ปิดยอด” (balloon adjustment) ให้ตรงสัญญา

### 8.5 ทำไมต้องแยก `BigDecimal` กับ `Money` ในตารางนี้

- **`Money`** บังคับ **scale = ทศนิยมมาตรฐานสกุลเงิน (THB = 2)** ทุกครั้งที่สร้าง — เหมาะกับ **ค่างวดที่แสดง / ใบเสร็จ / บรรทัดบัญชี**  
- **ระหว่างวน 24 งวด** ถ้าใช้ `Money` ทุกขั้น การคูณ `balance × r` จะถูกปัดเป็น 2 หลายเร็ว อาจสะสมคลาดเคลื่อนเทียบกับ **เก็บ `BigDecimal` scale สูงแล้วค่อยปัดตอนส่งออก**  
- แนวทางที่สอดคล้อง Javadoc ของ `Money`: **ลูปด้วย `BigDecimal`** → แปลงแต่ละงวดเป็น `Money` ตอน persist หรือ API response

### 8.6 ต้องมี **EIR (อัตราดอกเบี้ยที่แท้จริง)** หรือไม่

| สถานการณ์ | คำแนะนำ |
|-----------|---------|
| แค่ต้องการ **ค่างวด + ตาราง** จาก nominal 19.86% แบบหาร 12 | **ไม่บังคับ** ต้องคำนวณ EIR แบบ IFRS ก็ได้ตารางแบบข้างบน |
| รายงาน **ต้นทุนทางบัญชีแบบ amortized cost** (IFRS 9) หรือมี **ค่าธรรมเนียมแรกเข้า** / cash flow ไม่สม่ำเสมอ | **ควรคำนวณ EIR** เป็นดอกที่ทำให้ PV(เงินงวด + อื่นๆ) = เงินที่ได้รับสุทธิ ณ วันเริ่มสัญญา |
| ต้องการ **อัตราต่อปีที่เทียบเท่า** กับ “19.86% ต่อปี แบบคิดดอกรายเดือน” | แยกจาก EIR บัญชี: อัตราที่แท้จริงจาก **ทบต้นรายเดือน** มัก **สูงกว่า** 19.86% ในตัวอย่างนี้ประมาณ **21.77% ต่อปี** \((1 + 0{,}1986/12)^{12} - 1\) — ใช้อธิบายผู้กู้ / เอกสารภายใน ไม่ใช่แทนค่าทางกฎหมายโดยอัตโนมัติ |

### 8.7 โค้ดตัวอย่าง (PMT + วนครบทุกงวด + ปิดยอดงวดสุดท้าย)

แนวทาง: งวด `1` ถึง `periods - 1` ใช้ค่างวดคงที่ `pmt` (ปัดสตางค์แล้ว) แยกดอก / เงินต้นด้วย `BigDecimal` เก็บ `balance`  
งวดสุดท้าย: ดอกคิดจากยอดคงค้างที่เหลือ, เงินต้น = ยอดคงค้าง (ปิดเต็มจำนวนเป็นสตางค์), **ค่างวดรวมอาจไม่เท่า `pmt`** (เพี้ยนเซ็นต์จากการปัดกลางทาง — ตัวอย่างตัวเลขนี้งวดที่ 24 ราว **12,656.07** เทียบกับ **12,656.03**)

```java
import com.starter.api.shared.domain.Money;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

// --- พารามิเตอร์สมมติ (ตรงกับข้อ 8.1) ---
BigDecimal balance = new BigDecimal("249000.00");
BigDecimal annualNominalRate = new BigDecimal("0.1986");
int periods = 24;
int calcScale = 12; // ความละเอียดระหว่างคำนวณอัตรารายเดือน
RoundingMode rm = RoundingMode.HALF_UP;

BigDecimal monthlyRate = annualNominalRate.divide(BigDecimal.valueOf(12), calcScale, rm);

// (1+r)^n และ PMT = P * r * (1+r)^n / ((1+r)^n - 1)
BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
BigDecimal onePlusRPowN = onePlusR.pow(periods, new MathContext(calcScale));
BigDecimal pmt = balance
    .multiply(monthlyRate)
    .multiply(onePlusRPowN)
    .divide(onePlusRPowN.subtract(BigDecimal.ONE), 2, rm);

Money installmentMoney = Money.thb(pmt); // ค่างวดมาตรฐานที่แสดงลูกค้า (~ 12,656.03)

// --- งวด 1 .. (periods - 1): ค่างวดคงที่ ---
for (int period = 1; period < periods; period++) {
    BigDecimal interest = balance.multiply(monthlyRate).setScale(2, rm);
    BigDecimal principalPart = pmt.subtract(interest);
    balance = balance.subtract(principalPart);

    Money paymentMoney = Money.thb(pmt);
    Money interestMoney = Money.thb(interest);
    Money principalMoney = Money.thb(principalPart);
    Money balanceMoney = Money.thb(balance.setScale(2, rm));

    // TODO: บันทึกลง schedule / entity ตาม period, paymentMoney, interestMoney, principalMoney, balanceMoney
}

// --- งวดสุดท้าย: ปิดยอดเงินต้นที่เหลือ (ค่างวดอาจต่างจาก pmt เล็กน้อย) ---
BigDecimal lastInterest = balance.multiply(monthlyRate).setScale(2, rm);
BigDecimal lastPrincipal = balance.setScale(2, rm); // กินยอดคงค้างทั้งก้อนเป็นสตางค์
BigDecimal lastPayment = lastPrincipal.add(lastInterest);
balance = balance.subtract(lastPrincipal);

Money lastPaymentMoney = Money.thb(lastPayment);
Money lastInterestMoney = Money.thb(lastInterest);
Money lastPrincipalMoney = Money.thb(lastPrincipal);
// balance ควรเป็น BigDecimal.ZERO (หรือใกล้มาก) — ถ้ามี dust ให้กำหนดนโยบายปิดบัญชีในโมดูลจริง

// installmentMoney = 12,656.03 ; lastPaymentMoney มักราว 12,656.07 ในสมมติฐานนี้
```

---

## 9. ลูกค้าชำระเงิน — waterfall ด้วย `Money` อย่างเดียว

**Use case:** ได้รับเงินจากลูกค้าแล้วแบ่งชำระตามลำดับ **ค่าปรับล่าช้า → ดอกเบี้ยค้างรับ → เงินต้น** โดยไม่สร้างฟังก์ชัน helper — ใช้ตัวแปร `remaining` เก็บยอดที่ยังไม่ได้จัดสรร

หลักการ: ในแต่ละขั้นใช้ `remaining.min(outstandingX)` เพื่อไม่ให้เกินยอดค้างของขั้นนั้น แล้ว `remaining = remaining.subtract(...)` เพื่อไปขั้นถัดไป

```java
import com.starter.api.shared.domain.Money;

Money paymentFromCustomer = Money.thb("50000");              // เงินที่ลูกค้าโอนเข้ามาจริง (งวดนี้)

Money outstandingLateFees = Money.thb("1500");               // ยอดค่าปรับ / ค่าธรรมเนียมล่าช้าที่ยังค้างอยู่
Money outstandingInterest = Money.thb("3200.50");            // ยอดดอกเบี้ยค้างรับ
Money outstandingPrincipal = Money.thb("200000");          // ยอดเงินต้นคงเหลือ (ก่อนหักงวดนี้)

Money remaining = paymentFromCustomer;                     // ยอดที่ยังไม่ได้จัดสรรไปขั้นใด — เริ่มจากยอดชำระทั้งก้อน

Money toLateFees = remaining.min(outstandingLateFees);     // หักค่าปรับก่อน: ไม่เกินทั้งยอดค้างและยอดที่เหลือ
remaining = remaining.subtract(toLateFees);                  // หักยอดที่ไปค่าปรับออกจาก remaining

Money toInterest = remaining.min(outstandingInterest);     // หักดอกเบี้ย: เท่าที่ค้างหรือเท่าที่ remaining พอ
remaining = remaining.subtract(toInterest);                  // หักส่วนดอกออกจาก remaining

Money toPrincipal = remaining.min(outstandingPrincipal);     // หักเงินต้น: ไม่เกินยอดค้างและไม่เกิน remaining
Money unallocatedRemainder = remaining.subtract(toPrincipal); // ส่วนที่เหลือหลังหักครบ 3 ชั้น (เช่น ชำระเกิน → suspense)

// ผลลัพธ์ตัวอย่างนี้: toLateFees = 1,500 · toInterest = 3,200.50 · toPrincipal = 45,299.50 · unallocatedRemainder = 0.00
```

หลังจัดสรร: `50_000 - 1_500 - 3_200.50 = 45_299.50` ไปเงินต้น ไม่เกิน `200_000` ดังนั้น `unallocatedRemainder` เป็น `THB 0.00`

เมื่อนำไปใช้จริง ทีมอาจห่อผลลัพธ์เป็น record ของโมดูล payment (เช่น `ReceiptAllocation`) หรือเขียนเมธอด private ใน service — แต่ **ตรรกะ `min` + `subtract` ชุดเดียวกับตัวอย่างนี้**

### Partial payment (ชำระไม่พอดอกเบี้ยทั้งหมด)

ใช้ลำดับเดียวกัน แค่เปลี่ยนตัวเลข:

```java
import com.starter.api.shared.domain.Money;

Money payment = Money.thb("2000");                         // ลูกค้าชำระน้อยกว่ายอดดอกที่ค้างทั้งหมด
Money remaining = payment;                                 // เริ่มนับจากยอดชำระ

Money outstandingLateFees = Money.thb("500");              // ค่าปรับค้าง
Money outstandingInterest = Money.thb("8000");             // ดอกค้างมากกว่าเงินที่เหลือหลังหักค่าปรับ
Money outstandingPrincipal = Money.thb("100000");          // เงินต้นค้าง (งวดนี้ยังไม่ถึงขั้นนี้)

Money toLateFees = remaining.min(outstandingLateFees);     // หักค่าปรับเต็ม 500 (remaining เหลือ 1,500)
remaining = remaining.subtract(toLateFees);

Money toInterest = remaining.min(outstandingInterest);     // หักดอกได้แค่ 1,500 ที่เหลือ ไม่ถึง 8,000
remaining = remaining.subtract(toInterest);

Money toPrincipal = remaining.min(outstandingPrincipal);     // remaining = 0 → หักเงินต้นได้ 0
Money unallocatedRemainder = remaining.subtract(toPrincipal); // เหลือ 0 (ไม่มีเงินเกินหลังหักครบตามลำดับ)

// ผลลัพธ์: toLateFees = 500 · toInterest = 1,500 · toPrincipal = 0 · unallocatedRemainder = 0
```

---

## 10. Overpayment — ชำระเกินยอดค้างทั้งหมด

เมื่อ `payment` มากกว่าผลรวมค่าปรับ + ดอก + เงินต้นที่ค้างอยู่ ณ วันนั้น ตัวแปร `unallocatedRemainder` หลังขั้นเงินต้นจะเป็นบวก → นำไปเป็น suspense / คืนลูกค้า / ตัดเป็นรายการอื่นตาม policy

```java
Money payment = Money.thb("100000");
Money remaining = payment;

Money toLateFees = remaining.min(Money.thb("0"));
remaining = remaining.subtract(toLateFees);

Money toInterest = remaining.min(Money.thb("500"));
remaining = remaining.subtract(toInterest);

Money toPrincipal = remaining.min(Money.thb("10000"));
Money unallocatedRemainder = remaining.subtract(toPrincipal);

// toPrincipal = 10,000 ; unallocatedRemainder = 89,500
```

---

## 11. สรุปยอดส่ง GL หรือ interface

หลังจัดสรรเงินชำระ (เช่น จาก waterfall ข้อ 9) มักต้องสรุปยอดที่จะส่งไป **บัญชีแยกประเภท / NetSuite / ระบบอื่น** ว่ารวมเท่าใด และแต่ละบรรทัดเป็นเท่าใด

### 11.1 สรุปยอดตาม component (ค่าปรับ / ดอกเบี้ย / เงินต้น)

สมมติใช้ผลจัดสรรเดียวกับตัวอย่างข้อ 9 (ชำระ 50,000 บาท): ค่าปรับ 1,500 · ดอก 3,200.50 · เงินต้น 45,299.50

```java
import com.starter.api.shared.domain.Money;

Money feesPosted = Money.thb("1500.00");
Money interestPosted = Money.thb("3200.50");
Money principalPosted = Money.thb("45299.50");

Money totalApplied = feesPosted.add(interestPosted).add(principalPosted);
// totalApplied → THB 50000.00 (ควรเท่ากับยอดที่ลูกค้าชำระจริง)
```

### 11.2 ใบเสร็จหลายบรรทัด (รายการค่าธรรมเนียม + VAT + อื่นๆ)

```java
import com.starter.api.shared.domain.Money;

Money arrangementFee = Money.thb("5000.00");
Money vatOnFee = Money.thb("350.00");
Money insuranceFee = Money.thb("1200.00");

Money receiptTotal = arrangementFee.add(vatOnFee).add(insuranceFee);
// receiptTotal → THB 6550.00
```

### 11.3 รวมหลายงวดหรือหลายรายการก่อนส่ง batch

```java
import com.starter.api.shared.domain.Money;

Money installment1 = Money.thb("8500.00");
Money installment2 = Money.thb("8500.00");
Money installment3 = Money.thb("8500.25");

Money batchTotalPrincipal = installment1.add(installment2).add(installment3);
// batchTotalPrincipal → THB 25500.25
```

### 11.4 ตรวจว่ายอดรวม component ตรงกับเงินที่รับเข้า

```java
import com.starter.api.shared.domain.Money;

Money customerPayment = Money.thb("50000.00");
Money feesPosted = Money.thb("1500.00");
Money interestPosted = Money.thb("3200.50");
Money principalPosted = Money.thb("45299.50");

Money sumComponents = feesPosted.add(interestPosted).add(principalPosted);
boolean matchesReceipt = sumComponents.compareTo(customerPayment) == 0;
// matchesReceipt == true → พร้อมโพสต์หรือส่ง interface ได้
```

---

## 12. Policy ที่ทีมต้องกำหนดเอง

- **ลำดับ waterfall** — ถ้าสัญญากำหนดดอกก่อนค่าปรับ ให้สลับลำดับบล็อก `min` / `subtract` ให้สอดคล้อง
- **ชั้นเพิ่ม** (ค่าทวง, ค่าทนาย, ประกัน ฯลฯ) — เพิ่มขั้นระหว่าง `remaining` กับขั้นสุดท้าย
- **สกุลเงิน** — ทุก `Money` ในสูตรต้องสกุลเดียวกัน มิฉะนั้นได้ `IllegalArgumentException`

---

## Related code

| Type | Package / path |
|------|----------------|
| `Money` | `com.starter.api.shared.domain.Money` |
