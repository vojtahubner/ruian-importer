# ADR 0001: Hexagonální architektura

Stav: přijato

## Kontext

Zadání je malé: stáhnout soubor, zparsovat dvě sekce XML a naplnit dvě tabulky. Projekt má ale zároveň ukázat, jak
by se kód psal v aplikaci, která poroste: další zdroje dat, další výstupy, změna úložiště. Chceme, aby se jádro
(stažení, parsování, uložení, záznam běhu) dalo otestovat bez databáze a sítě a aby bylo zřejmé, co na čem závisí.

## Zvažované možnosti

1. Klasické vrstvy controller → service → repository, JPA entity protékají všemi vrstvami. Nejméně kódu, ale service
   závisí na Springu i JPA a testuje se jen s databází nebo s mocky repozitářů.
2. Transaction script: jeden balíček, jedna třída, která stáhne, zparsuje a uloží. Pro zadání by stačil, nic
   neukazuje.
3. Hexagonální architektura (Ports & Adapters): doména bez frameworku, use casy v `application`, technologie
   v adapterech za porty, pravidla vynucená ArchUnitem.

## Rozhodnutí

Možnost 3. Balíčky `domain` (model, výjimky, `port.in`, `port.out`), `application` (`ImportService`,
`MunicipalityQueryService`, `SourceUrlPolicy`, `ImportProperties`), `adapter.in` (web, startup), `adapter.out`
(http, xml, persistence) a `config`. `domain` smí importovat jen `java.*` a JSpecify; `application` nezná adaptery;
adaptery nezávisí na sobě; JPA zůstává v persistence adapteru. Pravidla jsou zapsaná v `ArchitectureTest`, takže
porušení neprojde buildem.

Vstupní porty jsou rozdělené na zápis (`ImportMunicipalityUseCase`) a čtení (`ImportRunQuery`, `MunicipalityQuery`),
aby controllery závisely jen na tom, co volají. Doména používá recordy s validací v kompaktních konstruktorech; entity
JPA a DTO API jsou samostatné typy s ručními mappery.

## Důsledky

Plus:

- `ImportServiceTest` běží bez Springu s falešným `VfrSource` a mocky úložišť; chování při selhání se testuje za
  milisekundy.
- Každý adapter má vlastní test s odpovídajícím nástrojem (`MockRestServiceServer`, fixture XML, Testcontainers) a
  dá se vyměnit, aniž by se sáhlo do use casu, například `JdbcClient` místo JPA.
- Závislosti jsou explicitní a hlídané, ne konvence v hlavě autora.

Minus:

- Více souborů: porty, mappery, DTO a `package-info.java` na každý balíček. Pro úlohu této velikosti je to režie,
  přijatá vědomě jako ukázka.
- Mapování entita ↔ record stojí pár řádků a pozornost (viz `persist` vs. `save` v
  `MunicipalityPersistenceAdapter`).
