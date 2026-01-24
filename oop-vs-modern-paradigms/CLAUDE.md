# CLAUDE.md

This file provides guidance to Claude Code when working with files in this directory.

## Project Overview

Comparative study documents contrasting Traditional OOP with modern paradigms (DDD+FP from "Domain Modeling Made Functional" and DOP from "Data-Oriented Programming in Java"). All code examples use Java 25.

## Document Conventions

### Language
- Body text: Korean
- Code comments: Korean or English (context-dependent)
- Headings: Korean + English subtitle
- Diagrams: English only (Korean double-width chars break alignment)

### Code Examples
- All examples target **Java 25** with `--enable-preview`
- Before (OOP): Traditional mutable class-based Java
- After (Modern): Records, Sealed Interfaces, Pattern Matching, static pure functions
- Use `// [X]` prefix for anti-pattern code, `// [O]` for recommended code

### Block Formatting
- 코드 블록: `**[코드 XX.N]** Title` 레이블, `// package:` 주석, `N| ` 라인넘버, 2칸 들여쓰기
- ASCII 다이어그램: `**[그림 XX.N]** Title` 레이블
- 마크다운 표: `**[표 XX.N]** Title` 레이블
- XX = 파일 번호 (00~13, A, B, C), N = 파일 내 타입별 순번
- 레이블은 블록 바로 위에 배치 (레이블과 블록 사이 빈 줄 없음, 레이블 위에는 빈 줄)
- 패키지 결정: Order→order, Payment→payment, Customer→member, Product→product, Coupon→coupon, Auth→auth, Money/VO→shared, Shipping→shipping, Tax→tax, Rule→rule, Generic→com.example.pattern, Infra→infra

### ASCII Diagrams
- Use `/ascii-diagrams` skill for all diagrams
- English text only inside diagrams
- Verify alignment with fixed-width column calculation

### Template Structure (per concept)
Each concept item follows this structure:
1. Title with numbering and English subtitle
2. Core concept (keywords, insight, explanation, diagram)
3. What it is NOT (misconceptions)
4. Before: Traditional OOP (code + problems)
5. After: Modern Approach (code + improvements)
6. Additional details for understanding
7. Common mistakes
8. Key takeaways

### Conflict Resolution (DMMF vs DOP)
When the two sources disagree, use split After sections:
- "After: DMMF approach (DDD + FP)"
- "After: DOP approach"
- "Perspective difference analysis" (commonalities, each stance, practical recommendation)

## Source References
- DMMF: `../domain-modeling-made-functional-2018-scott-wlaschin/lectures/`
- DOP: `../dop-in-java-2026-chris-kiehl/lectures/`

## File Naming
- `00-overview.md` through `13-comprehensive-integration.md`
- `appendix-a-*`, `appendix-b-*`, `appendix-c-*`
