# Finance & loan/lease glossary (EN ↔ TH)

Shared vocabulary for backend, product, and ERP integration (e.g. NetSuite). Use **one English term in code** and **one Thai label in UI** per concept; extend the “Internal / ERP” column when you map fields.

**Spelling:** In finance, **principal** = เงินต้น (amount borrowed or outstanding). **Principle** = หลักการ (a rule)—do not use for money.

---

## 1. Core money & `Money` value object

| English (code / docs) | ไทย | Notes |
|------------------------|-----|--------|
| Money | จำนวนเงิน | Pair amount with currency; see `shared.domain.Money` |
| Amount | จำนวนเงิน | Prefer `Money` in domain; raw `BigDecimal` only where precision policy says so |
| Currency | สกุลเงิน | e.g. THB |
| Satang | สตางค์ | 0.01 THB |
| Rounding / round half up | การปัดเศษ (แบบครึ่งขึ้น) | Align with accounting policy (e.g. `HALF_UP`) |
| Immutable value object | วัตถุค่าที่เปลี่ยนแปลงไม่ได้ | Design term for `Money` |

---

## 2. Principal, balance, and loan lifecycle

| English | ไทย | Notes |
|---------|-----|--------|
| Principal | เงินต้น | Not “principle” |
| Original principal | เงินต้นเริ่มแรก | At disbursement |
| Outstanding principal | ยอดเงินต้นคงเหลือ | Remaining after repayments |
| Outstanding balance | ยอดคงค้าง | May include fees/interest per product definition |
| Loan / credit | สินเชื่อ / เงินกู้ | |
| Credit agreement | สัญญาสินเชื่อ | |
| Borrower | ผู้กู้ | |
| Lender / creditor | ผู้ให้กู้ / เจ้าหนี้ | Pick one per context (legal vs GL) |
| Disbursement | การโอนเงินกู้ / การเบิกจ่ายวงเงิน | Cash or booking to borrower |
| Maturity / maturity date | วันครบกำหนด / วันครบอายุสัญญา | |
| Tenor / term | ระยะเวลา / ระยะเวลาสัญญา | Length of contract |
| Early repayment | การชำระก่อนกำหนด | |
| Prepayment penalty | ค่าปรับชำระก่อนกำหนด | If product includes it |
| Rollover / renewal | การต่อสัญญา / การต่ออายุ | |
| Refinance | การรีไฟแนนซ์ | New facility replacing old |
| Collateral | หลักประกัน | e.g. vehicle |
| Delinquency | การค้างชำระ | Past due |
| Default | การผิดนัดชำระหนี้ | Define legally / operationally |

---

## 3. Interest rates (nominal, effective, fixed, floating)

| English | ไทย | Notes |
|---------|-----|--------|
| Interest | ดอกเบี้ย | |
| **Nominal interest rate** / **contract rate** / **stated rate** | **อัตราดอกเบี้ยตามสัญญา** | Rate written in agreement; may differ from EIR |
| **Effective interest rate (EIR)** | **อัตราดอกเบี้ยที่แท้จริง (EIR)** | IFRS amortized cost: rate that discounts estimated cash flows to carrying amount |
| **Annual percentage rate (APR)** | **อัตราดอกเบี้ยที่เทียบเป็นปี (APR)** | All-in borrower cost; definition must match regulator/marketing |
| **Fixed interest rate** | **อัตราดอกเบี้ยคงที่** | Same contractual rate over a defined period or full term |
| **Floating / variable interest rate** | **อัตราดอกเบี้ยผันแปร** | Tied to reference rate + margin |
| **Reference rate** / **benchmark rate** | **อัตราอ้างอิง** | e.g. MLR, MOR, policy rate—define source |
| **Margin** / **spread** | **มาร์จิ้น / ส่วนต่างอัตรา** | Added on top of reference rate |
| **Interest rate floor** | **ขั้นต่ำของอัตราดอกเบี้ย** | Rate cannot go below floor |
| **Interest rate cap** | **เพดานอัตราดอกเบี้ย** | Rate cannot exceed cap |
| **Teaser rate** / **promotional rate** | **อัตราดอกเบี้ยโปรโมชัน** | Low initial rate; document step-up |
| **Step-up / step-down rate** | **อัตราดอกเบี้ยแบบขั้นบันได** | Changes by schedule |
| **Zero-interest promotion (subsidized)** | **โปรดอกเบี้ย 0% (มีการอุดหนุน)** | True economics may still imply EIR > 0 |
| Accrued interest | ดอกเบี้ยค้างรับ / ดอกเบี้ยค้างจ่าย | Clarify asset vs liability side |
| Interest accrual | การตั้งดอกเบี้ยค้าง | Period-end recognition |
| Capitalization of interest | การทบดอกเบี้ยเป็นเงินต้น | If product adds unpaid interest to principal |

---

## 4. Payments, schedule, and day count

| English | ไทย | Notes |
|---------|-----|--------|
| Installment | งวด / ค่างวด | |
| Payment | การชำระเงิน | |
| Payment due date | วันครบกำหนดชำระ | |
| Payment frequency | ความถี่การชำระ | Monthly, quarterly, etc. |
| Amortization schedule | ตารางการชำระหนี้ / ตารางคำนวณเงินต้น–ดอก | Principal vs interest split per period |
| Principal component / principal portion | ส่วนเงินต้น | Of one installment |
| Interest component / interest portion | ส่วนดอกเบี้ย | Of one installment |
| Down payment | เงินดาวน์ | Vehicle / asset context |
| Balloon payment | งวดปิดท้ายแบบก้อนใหญ่ | Large final payment |
| Grace period | ระยะผ่อนผัน | No penalty or no payment—define precisely |
| **Day-count convention** | **วิธีนับจำนวนวันต่อดอกเบี้ย** | e.g. ACT/365, 30/360—must be explicit in specs |
| Business day adjustment | การเลื่อนวันตามวันทำการ | If due date falls on holiday |

---

## 5. Lease (IFRS 16–aware wording)

| English | ไทย | Notes |
|---------|-----|--------|
| Lease | การเช่า | |
| Finance lease | สัญญาเช่าเงิน | Lessee recognizes ROU asset + liability (IFRS 16) |
| Operating lease (lessor perspective) | สัญญาเช่าดำเนินงาน | Classification lessor-side; align with accounting |
| Lease agreement | สัญญาเช่า | |
| Lessor | ผู้ให้เช่า | |
| Lessee | ผู้เช่า | |
| Lease payments | ค่างวดเช่า | |
| Lease term | ระยะเวลาเช่า | |
| Residual value | มูลค่าคงเหลือ (ปลายสัญญา) | |
| Right-of-use (ROU) asset | สิทธิการใช้สินทรัพย์ | IFRS 16 lessee |
| Lease liability | หนี้สินจากสัญญาเช่า | IFRS 16 lessee |

---

## 6. Fees, penalties, and tax

| English | ไทย | Notes |
|---------|-----|--------|
| Fee | ค่าธรรมเนียม | Arrangement, admin, insurance, etc. |
| Late fee / penalty interest | ค่าปรับ / ดอกเบี้ยผิดนัด | Product-specific |
| VAT | ภาษีมูลค่าเพิ่ม | |
| Tax base | ฐานภาษี | |
| Withholding tax | ภาษีหัก ณ ที่จ่าย | |
| Tax localization | การปรับให้สอดคล้องภาษีในประเทศ | Layer before ERP |

---

## 7. Accounting & ERP (NetSuite-oriented)

| English | ไทย | Notes |
|---------|-----|--------|
| General ledger (GL) | บัญชีแยกประเภททั่วไป | |
| Journal / journal entry | สมุดรายวัน / รายการบันทึกบัญชี | |
| Posting | การบันทึกลงบัญชี | |
| Accounts receivable (AR) | ลูกหนี้ | |
| Accounts payable (AP) | เจ้าหนี้ | |
| Invoice | ใบแจ้งหนี้ | |
| Vendor bill | ใบรับวางบิล | AP |
| Carrying amount / amortized cost | มูลค่าตามบัญชี / ต้นทุนทยอยตัด | IFRS wording for loans |

---

## 8. IFRS 9 & credit risk

| English | ไทย | Notes |
|---------|-----|--------|
| Expected credit loss (ECL) | การด้อยค่าของสินทรัพย์ทางการเงินตามที่คาดว่าจะเกิด (ECL) | Often keep acronym “ECL” |
| Probability of default (PD) | ความน่าจะเป็นที่จะผิดนัด (PD) | |
| Loss given default (LGD) | อัตราสูญเสียเมื่อผิดนัด (LGD) | |
| Exposure at default (EAD) | ยอดเปิดรับความเสี่ยงเมื่อผิดนัด (EAD) | |
| Stage 1 / 2 / 3 | ระยะที่ 1 / 2 / 3 | Per ECL model policy |
| Impairment allowance | เงินสำรองด้อยค่า | |
| Write-off | การตัดหนี้สูญ | |

---

## 9. Suggested Java / API naming (examples)

| Domain concept | Suggested field / method (English) | ไทย (UI) |
|----------------|-----------------------------------|----------|
| Outstanding principal | `principalOutstanding` | ยอดเงินต้นคงเหลือ |
| Contract nominal rate | `contractInterestRate` / `nominalAnnualRate` | อัตราดอกเบี้ยตามสัญญา |
| Effective rate | `effectiveInterestRate` | อัตราดอกเบี้ยที่แท้จริง (EIR) |
| Fixed vs floating flag | `rateType` = `FIXED` / `FLOATING` | ประเภทอัตรา: คงที่ / ผันแปร |
| One installment total | `installmentAmount` | ค่างวด |
| Interest part | `interestPortion` | ส่วนดอกเบี้ย |
| Principal part | `principalPortion` | ส่วนเงินต้น |

---

## 10. Maintaining this file

- When a Thai label could mean two things (e.g. **เจ้าหนี้** vs **ผู้ให้กู้**), add one line under **Notes**.
- When integrating NetSuite, add a column in your internal wiki: **NetSuite record / field**.
- Review quarterly with risk, finance, and product.
