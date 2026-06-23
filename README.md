# Nile dot com - E-Store

[![CI](https://github.com/eduardo-canelas/E-Store-Application/actions/workflows/ci.yml/badge.svg)](https://github.com/eduardo-canelas/E-Store-Application/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://adoptium.net/)
[![Build](https://img.shields.io/badge/build-Gradle-02303A.svg)](https://gradle.org/)
[![Tests](https://img.shields.io/badge/tests-JUnit_5-25A162.svg)](https://junit.org/junit5/)

A Java Swing desktop storefront built on a clean, tested architecture: an
extracted domain layer, a repository-backed embedded SQLite store, natural
language catalogue search, and a custom-painted, theming UI. Originally a
single-file CNT 4714 assignment, rebuilt into a production-shaped application.

The interface is a mill3.studio-inspired brutalist-editorial system: warm paper
canvas, hard ink borders, serif display type, mono micro-labels, a single
acid-lime accent, and full-pill buttons that invert on hover - with a one-click
dark mode.

## Highlights

- **Layered architecture** - business rules live in `com.nilecom.domain`, isolated
  from Swing and from storage, so they are unit-tested directly.
- **Repository pattern over SQLite** - `InventoryRepository` / `OrderRepository`
  interfaces backed by embedded SQLite (xerial sqlite-jdbc); the CSV catalogue is
  imported as a first-run seed.
- **Natural language search** - type "cheap usb cable under $10"; the query parser
  extracts price bounds, a sort hint, and keyword terms, then filters the catalogue.
  Deterministic, dependency-free, fully tested.
- **Catalogue browser** - search or browse all products, click a result to open a
  detail view with a live, quantity-driven price and bulk-discount preview.
- **Inline receipt** - checkout renders an itemized, perforated paper receipt in
  place (no modal), with PNG export and printing.
- **Order history** - every checkout is persisted and replayable from an in-app
  history view.
- **Light / dark theming** - semantic design tokens flip the entire UI on toggle.
- **Tested + CI** - JUnit 5 across domain, search, and persistence; JaCoCo
  coverage gate (>=80% on the tested layers); GitHub Actions on every push.

## Quick start

Requires JDK 17+. The Gradle wrapper is committed, so no local Gradle install is
needed.

```bash
# build, run tests, verify coverage
./gradlew build

# launch the app
./gradlew run

# or build a self-contained runnable jar and run it
./gradlew fatJar
java -jar build/libs/nile-dot-com-2.0.0-all.jar
```

Convenience launchers: `./run.sh` (macOS/Linux) and `run.bat` (Windows).

## Using it

1. **Find a product** - type a natural-language query ("cheap cable under $10") and
   Search, or Browse all.
2. **Open a result** - click any product to see details; set a quantity and watch
   the line total and bulk discount update live.
3. **Add to cart** - up to 5 line items; bulk discounts apply automatically
   (5+ -> 10%, 10+ -> 15%, 15+ -> 20%).
4. **Checkout** - generates an inline receipt (6% sales tax), saves the order, and
   appends to `transactions.csv`. Export the receipt as PNG or print it.
5. **History** - reopen any past order from the History view.

## Architecture

```
com.nilecom
├── App                       composition root (boots SQLite, seeds, launches UI)
├── domain                    pure business logic (no Swing, no SQL)
│   ├── Product, CartItem, Cart, Order
│   └── Pricing               discount tiers + tax
├── search
│   └── NaturalLanguageSearch query parsing + filtering
├── persistence               repository pattern
│   ├── InventoryRepository / OrderRepository      (interfaces)
│   ├── SqliteDatabase, SqliteInventoryRepository, SqliteOrderRepository
│   ├── CsvInventoryImporter  (seed import)
│   └── CsvTransactionLog     (append-only audit trail)
└── ui
    ├── NileDotCom            custom-painted Swing storefront
    └── Theme                 light/dark semantic design tokens
```

Business logic depends only on interfaces, so storage is swappable and the rules
are testable in isolation - the layering the original single-file version only
claimed to have.

## Testing

```bash
./gradlew test                 # run the suite
open build/reports/tests/test/index.html
./gradlew jacocoTestReport     # coverage
open build/reports/jacoco/test/html/index.html
```

Covered: pricing tiers and boundaries, cart capacity and money math, the search
query parser, CSV import edge cases, and SQLite repository round-trips
(seed/find/save/reload).

## Tech

Java 17, Swing (custom 2D painting, zero UI dependencies), SQLite (xerial
sqlite-jdbc), Gradle, JUnit 5, JaCoCo, GitHub Actions.

## Author

Eduardo Canelas - [github.com/eduardo-canelas](https://github.com/eduardo-canelas)
