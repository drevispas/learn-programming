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
