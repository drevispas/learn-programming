package com.travel.shared.types;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 날짜 범위 - 불변 Value Object
 *
 * <h2>목적 (Purpose)</h2>
 * 체크인/체크아웃, 여행 기간 등 날짜 범위를 안전하게 표현
 *
 * <h2>핵심 개념 (Key Concept): Ch 2 Value Object</h2>
 * <pre>
 * [Key Point] 시작일과 종료일을 개별 필드로 관리하면:
 * - 시작일 > 종료일인 상태가 가능
 * - 관련 로직이 여러 곳에 분산
 *
 * [Why Record] DateRange로 묶으면:
 * - 생성 시점에 유효성 보장
 * - 기간 계산 등 관련 로직 집중
 * </pre>
 *
 * <h2>핵심 개념 (Key Concept): Ch 4 불가능한 상태 제거</h2>
 * <pre>
 * 컴파일러가 강제: startDate <= endDate
 * → 잘못된 날짜 범위는 생성 자체가 불가능
 * </pre>
 *
 * @param startDate 시작일 (포함)
 * @param endDate   종료일 (포함)
 */
public record DateRange(
        LocalDate startDate,
        LocalDate endDate
) {

    /**
     * Compact Constructor - 날짜 유효성 검증
     *
     * <p>[Key Point] startDate > endDate인 상태를 컴파일 타임에 방지할 수는 없지만,
     * 런타임에 생성 시점에서 100% 차단</p>
     */
    public DateRange {
        if (startDate == null) {
            throw new IllegalArgumentException("시작일은 null일 수 없습니다");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("종료일은 null일 수 없습니다");
        }
        // [Ch 4] 불가능한 상태 제거: 시작일이 종료일보다 뒤인 경우
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "시작일이 종료일보다 늦을 수 없습니다: " + startDate + " > " + endDate);
        }
    }

    // ============================================
    // 정적 팩토리 메서드
    // ============================================

    /**
     * 단일 날짜 범위 (시작일 = 종료일)
     *
     * @param date 날짜
     * @return 단일 날짜 범위
     */
    public static DateRange singleDay(LocalDate date) {
        return new DateRange(date, date);
    }

    /**
     * 오늘부터 N일간
     *
     * @param days 일수
     * @return 날짜 범위
     */
    public static DateRange fromTodayFor(int days) {
        LocalDate today = LocalDate.now();
        return new DateRange(today, today.plusDays(days - 1));
    }

    // ============================================
    // 계산 메서드
    // ============================================

    /**
     * 숙박 일수 계산
     *
     * <p>[Key Point] 체크인~체크아웃은 endDate - startDate
     * (예: 1/1 ~ 1/3 = 2박)</p>
     *
     * @return 숙박 일수
     */
    public long nights() {
        return ChronoUnit.DAYS.between(startDate, endDate);
    }

    /**
     * 전체 일수 계산 (시작일과 종료일 모두 포함)
     *
     * <p>예: 1/1 ~ 1/3 = 3일</p>
     *
     * @return 전체 일수
     */
    public long days() {
        return nights() + 1;
    }

    /**
     * 특정 날짜가 범위에 포함되는지 확인
     *
     * @param date 확인할 날짜
     * @return 포함 여부
     */
    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /**
     * 다른 범위와 겹치는지 확인
     *
     * @param other 다른 날짜 범위
     * @return 겹침 여부
     */
    public boolean overlaps(DateRange other) {
        return !this.endDate.isBefore(other.startDate) &&
               !this.startDate.isAfter(other.endDate);
    }

    @Override
    public String toString() {
        if (startDate.equals(endDate)) {
            return startDate.toString();
        }
        return startDate + " ~ " + endDate + " (" + nights() + "박)";
    }
}
