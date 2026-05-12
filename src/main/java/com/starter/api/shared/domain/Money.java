package com.starter.api.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Value Object สำหรับจัดการเงินตราในระบบ
 * เป็น Immutable เสมอ และป้องกันการคำนวณข้ามสกุลเงิน
 * <p>
 * ค่าถูกปัดเศษให้ตรงกับทศนิยมมาตรฐานของสกุลเงินทุกครั้งที่สร้าง instance
 * การคำนวณ EIR / ตารางงวดที่ต้องการความละเอียดสูงในระหว่างทาง อาจใช้ {@link BigDecimal}
 * แล้วค่อยห่อเป็น {@code Money} ตอนบันทึกหรือแสดงผลตามนโยบายทีม
 */
public record Money(BigDecimal amount, Currency currency) implements Comparable<Money> {

    // มาตรฐานการปัดเศษของระบบบัญชีไทย (ตั้งแต่ 0.5 ปัดขึ้น)
    private static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_UP;
    private static final Currency DEFAULT_CURRENCY = Currency.getInstance("THB");

    /**
     * Compact Constructor: ทำหน้าที่ Validate ข้อมูลทุกครั้งที่มีการสร้าง Object
     */
    public Money {
        Objects.requireNonNull(amount, "จำนวนเงิน (Amount) ห้ามเป็น Null");
        Objects.requireNonNull(currency, "สกุลเงิน (Currency) ห้ามเป็น Null");

        // บังคับให้จำนวนเงินมีทศนิยมตรงตามมาตรฐานสกุลเงินนั้นๆ (เช่น THB = 2 ตำแหน่ง)
        amount = amount.setScale(currency.getDefaultFractionDigits(), DEFAULT_ROUNDING);
    }

    // --- Static Factory Methods (สร้าง Object ง่ายๆ) ---

    /**
     * รับสตริงในรูปแบบที่ {@link BigDecimal#BigDecimal(String)} รองรับ (เช่น {@code "1234.56"})
     * ไม่รองรับตัวคั่นหลักพันแบบมีเครื่องหมายจุลภาค
     */
    public static Money thb(String amount) {
        return new Money(new BigDecimal(amount), DEFAULT_CURRENCY);
    }

    public static Money thb(BigDecimal amount) {
        return new Money(amount, DEFAULT_CURRENCY);
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money zero() {
        return zero(DEFAULT_CURRENCY);
    }

    public static Money zero(Currency currency) {
        Objects.requireNonNull(currency, "สกุลเงิน (Currency) ห้ามเป็น Null");
        return new Money(BigDecimal.ZERO, currency);
    }

    // --- Core Arithmetic Operations (การคำนวณ) ---

    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.add(other.amount()), this.currency);
    }

    public Money subtract(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.subtract(other.amount()), this.currency);
    }

    /**
     * สำหรับการคูณ (เช่น คูณจำนวนงวด)
     */
    public Money multiply(int multiplier) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)), this.currency);
    }

    /**
     * สำหรับการคูณด้วยอัตราส่วน (เช่น คูณ VAT 7% -> multiply(0.07))
     * หมายเหตุ: รับเป็น BigDecimal ที่มีทศนิยมหลายตำแหน่งได้
     * และผลลัพธ์จะถูกปัดเศษให้ตรงกับทศนิยมมาตรฐานของสกุลเงินอัตโนมัติ
     */
    public Money multiply(BigDecimal multiplier) {
        Objects.requireNonNull(multiplier, "ตัวคูณ (Multiplier) ห้ามเป็น Null");
        BigDecimal result = this.amount.multiply(multiplier);
        return new Money(result, this.currency);
    }

    /**
     * หารด้วยตัวเลข (เช่น แบ่งเป็น N งวด) โดยปัดเศษตามทศนิยมมาตรฐานของสกุลเงินและ {@link #DEFAULT_ROUNDING}
     */
    public Money divide(BigDecimal divisor) {
        return divide(divisor, currency.getDefaultFractionDigits(), DEFAULT_ROUNDING);
    }

    /**
     * หารด้วยกำหนด scale และโหมดปัดเศษเอง (เช่น คำนวณดอกเบี้ยระหว่างทาง) ผลลัพธ์ยังถูก normalize
     * เป็นทศนิยมมาตรฐานของสกุลเงินเมื่อสร้าง {@code Money}
     */
    public Money divide(BigDecimal divisor, int scale, RoundingMode roundingMode) {
        Objects.requireNonNull(divisor, "ตัวหาร (Divisor) ห้ามเป็น Null");
        Objects.requireNonNull(roundingMode, "RoundingMode ห้ามเป็น Null");
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("ห้ามหารด้วยศูนย์");
        }
        BigDecimal result = this.amount.divide(divisor, scale, roundingMode);
        return new Money(result, this.currency);
    }

    public Money negate() {
        return new Money(this.amount.negate(), this.currency);
    }

    public Money abs() {
        if (isNegative()) {
            return negate();
        }
        return this;
    }

    public Money min(Money other) {
        assertSameCurrency(other);
        return this.compareTo(other) <= 0 ? this : other;
    }

    public Money max(Money other) {
        assertSameCurrency(other);
        return this.compareTo(other) >= 0 ? this : other;
    }

    // --- Comparison & Validation (การตรวจสอบเปรียบเทียบ) ---

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegative() {
        return this.amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isGreaterThan(Money other) {
        assertSameCurrency(other);
        return this.compareTo(other) > 0;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        assertSameCurrency(other);
        return this.compareTo(other) >= 0;
    }

    public boolean isLessThan(Money other) {
        assertSameCurrency(other);
        return this.compareTo(other) < 0;
    }

    public boolean isLessThanOrEqual(Money other) {
        assertSameCurrency(other);
        return this.compareTo(other) <= 0;
    }

    @Override
    public int compareTo(Money other) {
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount());
    }

    // --- Internal Helpers ---

    /**
     * ป้องกันข้อผิดพลาดร้ายแรง: ห้ามเอาเงินคนละสกุลมาบวก/ลบกันตรงๆ
     */
    private void assertSameCurrency(Money other) {
        Objects.requireNonNull(other, "Money ห้ามเป็น Null");
        if (!this.currency.equals(other.currency())) {
            throw new IllegalArgumentException(
                String.format("สกุลเงินไม่ตรงกัน: ไม่สามารถคำนวณ %s กับ %s ได้",
                    this.currency.getCurrencyCode(), other.currency().getCurrencyCode())
            );
        }
    }

    @Override
    public String toString() {
        return String.format("%s %s", currency.getCurrencyCode(), amount.toPlainString());
    }
}
