# Graph Report - .  (2026-06-22)

## Corpus Check
- Corpus is ~2,037 words - fits in a single context window. You may not need a graph.

## Summary
- 23 nodes · 20 edges · 6 communities (2 shown, 4 thin omitted)
- Extraction: 95% EXTRACTED · 5% INFERRED · 0% AMBIGUOUS · INFERRED: 1 edges (avg confidence: 0.85)
- Token cost: 1,200 input · 800 output

## Community Hubs (Navigation)
- [[_COMMUNITY_NileDotCom Application Class|NileDotCom Application Class]]
- [[_COMMUNITY_E-Store GUI & MVC Architecture|E-Store GUI & MVC Architecture]]
- [[_COMMUNITY_Inventory FileReader Utilities|Inventory FileReader Utilities]]
- [[_COMMUNITY_FileReader Testing|FileReader Testing]]
- [[_COMMUNITY_Project Documentation & Overview|Project Documentation & Overview]]

## God Nodes (most connected - your core abstractions)
1. `NileDotCom` - 7 edges
2. `NileDotCom Constructor` - 5 edges
3. `FileReaderTest` - 3 edges
4. `loadInventory Method` - 2 edges
5. `NileDotCom main Method` - 2 edges
6. `setupListeners Method` - 1 edges
7. `createStyledButton Method` - 1 edges
8. `readInventoryFile Method` - 1 edges
9. `Launcher Script` - 1 edges
10. `MVC Architecture Design` - 1 edges

## Surprising Connections (you probably didn't know these)
- `MVC Architecture Design` --references--> `NileDotCom Constructor`  [EXTRACTED]
  README.md → NileDotCom.java
- `readInventoryFile Method` --semantically_similar_to--> `loadInventory Method`  [INFERRED] [semantically similar]
  FileReaderTest.java → NileDotCom.java
- `Launcher Script` --calls--> `NileDotCom main Method`  [EXTRACTED]
  run.sh → NileDotCom.java

## Communities (6 total, 4 thin omitted)

### Community 1 - "E-Store GUI & MVC Architecture"
Cohesion: 0.25
Nodes (8): readInventoryFile Method, createStyledButton Method, loadInventory Method, NileDotCom main Method, NileDotCom Constructor, setupListeners Method, MVC Architecture Design, Launcher Script

## Knowledge Gaps
- **7 isolated node(s):** `setupListeners Method`, `createStyledButton Method`, `readInventoryFile Method`, `FileReaderTest main Method`, `Launcher Script` (+2 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **4 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What connects `setupListeners Method`, `createStyledButton Method`, `readInventoryFile Method` to the rest of the system?**
  _7 weakly-connected nodes found - possible documentation gaps or missing edges._