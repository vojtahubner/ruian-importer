# ruian-importer

[![CI](https://github.com/vojtahubner/ruian-importer/actions/workflows/ci.yml/badge.svg)](https://github.com/vojtahubner/ruian-importer/actions/workflows/ci.yml)

Služba, která stáhne výměnný formát RÚIAN (VFR XML) jedné obce, streamově z něj vyčte obec a její části, uloží je do
PostgreSQL a vystaví přes REST API. Řešení testovací úlohy Trixi „Zpracuj XML a ulož“ ([docs/zadani.md](docs/zadani.md));
body zadání mapuje tabulka Splnění zadání níže.

## Požadavky a spuštění

| Cesta | Co je potřeba | Příkazy |
| --- | --- | --- |
| Jen Docker | Docker Engine s Compose v2 (Docker Desktop stačí) | `docker compose --profile app up --build` (viz Rychlý start); na hostiteli není potřeba JDK ani Maven |
| Vývojářská | JDK 25 a Docker | `./mvnw spring-boot:run` (službu `db` z `compose.yaml` spustí Spring Boot sám), `./mvnw test` (bez Dockeru), `./mvnw verify` (i integrační testy, JaCoCo report v `target/site/jacoco/index.html`) |

Build spouštějte přes Maven Wrapper `./mvnw` (Maven si stáhne sám); systémový `mvn` může běžet na jiném JDK a není
s projektem testovaný.

## Rychlý start

```bash
docker compose --profile app up --build
```

První build trvá 3–5 minut (stáhne se image s JDK a závislosti Mavenu; další buildy jdou z cache). Po startu proběhne
import automaticky (`ruian.import.on-startup=true`): stáhne se `kopidlno.xml.zip` z `www.smartform.cz`, zparsuje
a obec s částmi se uloží. Bez internetu aplikace nastartuje také: běh je v `GET /api/imports` zaznamenaný jako
`FAILED`, data API jsou prázdná a `curl -X POST localhost:8080/api/imports` import zopakuje. Obě služby poslouchají jen
na `localhost`; když na portu 5432 už běží jiný PostgreSQL, pomůže `DB_PORT=55432 docker compose --profile app up --build`.

Ověření z druhého terminálu:

```bash
curl -s localhost:8080/api/municipalities
```

```json
[{"code":573060,"name":"Kopidlno"}]
```

```bash
curl -s localhost:8080/api/municipalities/573060/parts
```

```json
[{"code":31801,"name":"Drahoraz","municipalityCode":573060},
 {"code":31828,"name":"Pševes","municipalityCode":573060},
 {"code":69299,"name":"Kopidlno","municipalityCode":573060},
 {"code":69302,"name":"Ledkov","municipalityCode":573060},
 {"code":97373,"name":"Mlýnec","municipalityCode":573060}]
```

Záznam o běhu vrátí `GET /api/imports` (`status` `SUCCEEDED`, `municipalityCode` 573060, `partCount` 5) a stejných
pět řádků ukáže i databáze (JSON výše je zalomený jen pro čitelnost):

```bash
docker compose exec db psql -U ruian -d ruian -c "select code, name, municipality_code from municipality_part order by name"
```

## Splnění zadání

| Bod zadání | Kde v projektu |
| --- | --- |
| Stažení ZIP z URL | `HttpVfrSource` streamuje tělo odpovědi přes `RestClient`; výchozí adresa je v `ruian.import.default-source-url`, po startu import spouští `StartupImportRunner` |
| Parsování standardním nástrojem | StAX z JDK (`javax.xml.stream`) ve `StaxVfrParser`: čte jen `vf:Obec` a `vf:CastObce` a po konci sekce `vf:CastiObci` přestane |
| Tabulky obec a část obce | Flyway migrace `V1__municipality.sql`: `municipality` = obec (kód, název), `municipality_part` = část obce (kód, název, kód obce) |
| Naplnění tabulek | `MunicipalityPersistenceAdapter.replace`: upsert obce a jejích částí v jedné transakci, části chybějící ve zdroji smaže |
| Spring a/nebo Docker | obojí: Spring Boot 4.1, `Dockerfile` a `compose.yaml` |
| Nad rámec zadání | historie běhů importu (tabulka `import_run`), REST API nad uloženými daty, import libovolné obce zadáním URL v `POST /api/imports` |

## API

| Metoda | Cesta | Popis | Odpovědi |
| --- | --- | --- | --- |
| `POST` | `/api/imports` | Spustí synchronní import; tělo `{"sourceUrl": "..."}` je nepovinné, bez něj se použije výchozí zdroj | `201` + `Location: /api/imports/{id}` a záznam běhu; `400` nečitelné tělo nebo neplatná či příliš dlouhá URI; `422` zdroj mimo allowlist nebo neplatné VFR; `502` zdroj nedostupný; `500` neočekávaná chyba |
| `GET` | `/api/imports` | Poslední běhy, nejnovější první; parametr `limit` 1–100, výchozí 20 | `200`; `400` limit mimo rozsah |
| `GET` | `/api/imports/{id}` | Detail běhu | `200`; `404` |
| `GET` | `/api/municipalities` | Uložené obce seřazené podle kódu | `200` |
| `GET` | `/api/municipalities/{code}` | Obec podle kódu RÚIAN | `200`; `404` |
| `GET` | `/api/municipalities/{code}/parts` | Části obce seřazené podle kódu | `200`; `404` neznámá obec |

Swagger UI: <http://localhost:8080/swagger-ui.html> (OpenAPI `/v3/api-docs`); Actuator: `/actuator/health` (i stav
databáze), `/actuator/info`, `/actuator/metrics`; Postman kolekce:
[docs/postman/ruian-importer.postman_collection.json](docs/postman/ruian-importer.postman_collection.json)
(proměnná `baseUrl`).

Chyby mají formát RFC 9457 (`application/problem+json`); `type` je `urn:problem-type:` + `source-not-allowed` (422),
`invalid-vfr` (422), `source-unavailable` (502), `import-failed` (500), `not-found` (404) nebo `internal-error` (500).
Když import selže až po kontrole zdroje, je běh uložený jako `FAILED` a problém na něj ukazuje (`instance`
a `importRunId`); zdroj mimo allowlist žádný běh nezakládá.

Import jiné obce z VDP (Veřejný dálkový přístup k RÚIAN):

```bash
curl -s -X POST localhost:8080/api/imports -H 'Content-Type: application/json' -d '{"sourceUrl":"https://vdp.cuzk.gov.cz/vymenny_format/soucasna/20260930_OB_573060_UZSZ.xml.zip"}'
```

VDP drží jen několik posledních měsíčních snímků (`YYYYMMDD` v názvu je poslední den předchozího měsíce); pro prošlý
soubor VDP vrací HTML stránku, kterou importer odmítne jako neplatné VFR (`422`).

## Kde začít číst

1. [`StaxVfrParser`](src/main/java/cz/vojtechhubner/ruian/adapter/out/xml/StaxVfrParser.java) – jádro úlohy: rozpoznání ZIP a StAX kurzor s early exitem.
2. [`ImportService`](src/main/java/cz/vojtechhubner/ruian/application/ImportService.java) – use case importu: kontrola zdroje, stažení, parsování, uložení a záznam běhu včetně selhání.
3. [`MunicipalityPersistenceAdapter`](src/main/java/cz/vojtechhubner/ruian/adapter/out/persistence/MunicipalityPersistenceAdapter.java) – jediný zapisující kód nad obcemi, advisory lock proti souběžným importům.
4. [`V1__municipality.sql`](src/main/resources/db/migration/V1__municipality.sql) – obě tabulky ze zadání.
5. [`ImportFlowIT`](src/test/java/cz/vojtechhubner/ruian/ImportFlowIT.java) – celý tok od `POST /api/imports` po řádky v PostgreSQL nad skutečným souborem Kopidlna.

## Architektura

Kód je rozdělený hexagonálně (Ports & Adapters): `domain` je čistá Java (model, výjimky a porty), `application`
obsahuje use casy a politiku zdrojů, `adapter.in` jsou vstupy (REST, import při startu), `adapter.out` výstupy (HTTP,
StAX, JPA) a `config` jen konfigurační třídy Springu. Závislosti míří dovnitř k doméně a hlídá je `ArchitectureTest`
(ArchUnit). Diagram komponent, sekvenční diagram importu, datový model, transakční hranice a chybové stavy popisuje
[docs/architecture.md](docs/architecture.md).

| Pattern | Kde | Jaký problém tu řeší |
| --- | --- | --- |
| Ports & Adapters | `domain.port.*`, `application`, `adapter.*`; hlídá `ArchitectureTest` | doména a use casy nezávisí na Springu, JPA ani HTTP; `ImportService` se testuje s falešným zdrojem a mocky úložišť |
| Adapter | `HttpVfrSource` (`RestClient` → `VfrSource`), `StaxVfrParser` (StAX → `VfrParser`), `*PersistenceAdapter` (JPA → `MunicipalityStore`, `ImportRunStore`) | překlad API knihovny na port domény včetně výjimek: HTTP status, `XMLStreamException` nebo `IOException` se ven dostanou jen jako doménové výjimky |
| Repository za portem | `MunicipalityStore` a `ImportRunStore` nad Spring Data repozitáři, převod v `PersistenceMapper` | entity JPA nikdy neopustí adapter; aplikace dostává nezměnitelné recordy |
| Application service s oddělenými porty pro zápis a čtení | `ImportService` (`ImportMunicipalityUseCase`, `ImportRunQuery`), `MunicipalityQueryService` (`MunicipalityQuery`) | controller závisí jen na portu, který potřebuje; v `@WebMvcTest` se mockují jen potřebné porty, ne celá služba |
| Execute-around | port `VfrSource.read(URI, Reader<T>)`, implementace `HttpVfrSource` | stream HTTP odpovědi žije jen uvnitř callbacku: zavření a překlad I/O chyb jsou na jednom místě, nic se nebufferuje |
| Policy + value objects | `SourceUrlPolicy`, `ImportProperties`, recordy `Municipality`, `MunicipalityPart`, `MunicipalityDataset`, `ImportRun` | pravidla pro zdrojovou URL na jednom místě s vlastním testem; neplatný stav (část cizí obce, `FAILED` běh bez zprávy) nejde ani sestavit |

Konvence napříč kódem: konstruktorová injekce do `final` polí, statické factory metody, DTO oddělená od domény
s ručním mapperem, immutabilita (recordy, `List.copyOf`) a fail-fast validace v kompaktních konstruktorech.

## Technologie

- Java 25 a Spring Boot 4.1.2 (Spring Framework 7, Jackson 3): `RestClient`, `ProblemDetail`, `@ConfigurationProperties` na recordu.
- PostgreSQL 18 a Flyway: schéma vzniká migracemi `V1__municipality.sql` a `V2__import_run.sql`, Hibernate ho při startu jen validuje (`ddl-auto=validate`).
- JDK StAX (`javax.xml.stream`) bez další XML knihovny, streamování s konstantní pamětí.
- Testcontainers 2 (`@ServiceConnection`) pro integrační testy, ArchUnit, JaCoCo.
- Bez Lomboku a MapStructu: recordy a ruční mappery. JSpecify `@NullMarked` na každém balíčku, `@Nullable` jen tam, kde je `null` legitimní hodnota.
- springdoc-openapi: Swagger UI a OpenAPI dokument generované z controllerů.
- Docker: multi-stage `Dockerfile` (Temurin 25 JDK pro build, JRE Alpine pro běh), vrstvený jar (`-Djarmode=tools extract --layers`), neprivilegovaný uživatel, `HEALTHCHECK` na `/actuator/health`.
- GitHub Actions (job `verify` s `./mvnw verify`, job `docker` s buildem image) a Dependabot (Maven, GitHub Actions a Docker, týdně).

## Testování

`./mvnw test` spustí 148 unit testů (`*Test`, Surefire, bez Dockeru), `./mvnw verify` k nim přidá 22 integračních
(`*IT`, Failsafe, Testcontainers s PostgreSQL 18):

- `ImportFlowIT` (7 testů, `@SpringBootTest`): celý tok přes HTTP nad skutečným souborem Kopidlna – import a čtení přes API, idempotence druhého importu, nedostupný zdroj jako `FAILED` běh s 502, zdroj mimo allowlist bez běhu, health s databází, OpenAPI.
- `PersistenceAdaptersIT` (11 testů, `@DataJpaTest`): `replace` nad skutečným PostgreSQL – založení, změna jen změněných řádků, smazání chybějících částí, přesun části mezi obcemi, nezávislost obcí; ukládání a řazení běhů.
- `JpaMappingIT` (4 testy, `@DataJpaTest`): Flyway tabulky a validace Hibernate, audit časy z aplikačních hodin, kaskádové mazání částí, round trip `import_run`.

Unit testy pokrývají každou vrstvu:

- doména: invarianty recordů a výjimek (kladné kódy, názvy do 48 znaků, část cizí obce, duplicitní část, konzistence `SUCCEEDED`/`FAILED` běhu);
- aplikace: `ImportService` s Mockito (allowlist se kontroluje před stažením, `FAILED` běh při selhání, skrytí detailů neočekávané chyby, selhání zápisu historie – u `FAILED` běhu jako suppressed), `SourceUrlPolicy` parametrizovaně (povolené subdomény a port 443; odmítnutí `http`, user info, jiného portu, IP adresy a cizího hosta), `ImportProperties` přes `ApplicationContextRunner`;
- vstupy: controllery v `@WebMvcTest` (status kódy, `Location`, ProblemDetail s `instance` a `importRunId`, 500 bez textu příčiny), `StartupImportRunner` (selhání nepoloží aplikaci);
- výstupy: `HttpVfrSource` v `@RestClientTest` (404/500/3xx jako nedostupný zdroj, blokovaná adresa, přerušený stream, timeout), `StaxVfrParser` (plain i zazipované XML, cizí položky archivu, žádná nebo více obcí, část cizí obce, duplicitní část, XXE, zastavení po sekci `vf:CastiObci`, limit velikosti, poškozené XML; `StaxVfrParserRealFileTest` nad skutečným `kopidlno.xml.zip`), `BoundedInputStream`, `PersistenceMapper`;
- `ArchitectureTest` (ArchUnit): `domain` jen `java.*` a JSpecify; `application` bez `adapter` a `config`; adaptery nezávislé na sobě; `config` bez adapterů; JPA a Hibernate jen v `adapter.out.persistence`; `@Transactional` na veřejných metodách `*PersistenceAdapter`, v `application` nikde; bez cyklů; `@RestController` jen v `adapter.in.web`.

Pokrytí měří JaCoCo nad oběma fázemi dohromady (`config`, `dto` a hlavní třída jsou vyloučené) a `./mvnw verify` selže
pod 70 % řádků; poslední běh CI naměřil zhruba 98 % řádků a 92 % větví (artefakt `reports`).

Parser čte jen začátek souboru: v rozbaleném XML Kopidlna (13 411 844 B) končí sekce `vf:CastiObci` na bajtu 10 440,
takže z 526 kB archivu se přečtou asi 2 kB; `StaxVfrParserRealFileTest` hlídá limit 200 kB.

## Konfigurace

| Vlastnost | Výchozí hodnota | Význam |
| --- | --- | --- |
| `ruian.import.default-source-url` | `https://www.smartform.cz/download/kopidlno.xml.zip` | zdroj pro import při startu a pro `POST /api/imports` bez `sourceUrl` |
| `ruian.import.allowed-hosts` | `smartform.cz`, `vdp.cuzk.gov.cz` | hosty (a jejich subdomény), ze kterých se smí stahovat; jen `https` na portu 443; prázdný seznam zastaví start |
| `ruian.import.on-startup` | `true` | import výchozího zdroje po startu (`StartupImportRunner`) |
| `ruian.import.max-uncompressed-bytes` | `64MB` | nejvíc rozbaleného XML z jednoho zdroje (`BoundedInputStream`) |
| `spring.http.clients.connect-timeout` | `5s` | timeout navázání spojení |
| `spring.http.clients.read-timeout` | `60s` | timeout čtení odpovědi; import je synchronní, takže omezuje i dobu požadavku |
| `spring.http.clients.redirects` | `dont-follow` | přesměrování se nenásledují, 3xx je nedostupný zdroj (allowlist platí jen pro zadanou URL) |

Profily: `docker` (v image přes `SPRING_PROFILES_ACTIVE`) vypíná Docker Compose support a zapíná strukturované
logování ECS; `test` (integrační testy) vypíná Docker Compose support a import při startu.

## Rozhodnutí

- [ADR 0001 – Hexagonální architektura](docs/adr/0001-hexagonalni-architektura.md)
- [ADR 0002 – StAX streaming a early exit](docs/adr/0002-stax-streaming-a-early-exit.md)
- [ADR 0003 – Synchronní import, historie běhů a synchronizace částí](docs/adr/0003-synchronni-import-historie-behu-a-synchronizace-casti.md)
- [ADR 0004 – Allowlist zdrojů a SSRF](docs/adr/0004-allowlist-zdroju-a-ssrf.md)

## Limity a co dál

- Import je synchronní a historie nezná stav `RUNNING`: `POST /api/imports` vrátí 201 až po dokončení. Pro pomalé zdroje by se běh zakládal předem jako `RUNNING` a import spouštěl asynchronně (202 Accepted).
- Bez stránkování: `GET /api/imports` má jen `limit` (nejvýš 100), seznamy obcí a částí stránkování nemají; pro jednu obec to stačí, pro celostátní data ne.
- Bez metrik a událostí: počet a trvání importů se nepublikují, Actuator vystavuje jen standardní metriky.
- Bez autentizace na `POST /api/imports`: přidala by se přes Spring Security (HTTP Basic pro interní nasazení, nebo JWT jako resource server), `POST` jen pro roli importéra, `GET` veřejné.
- Jedna obec na import: soubor s více obcemi (celostátní `ST_*` z VDP) parser odmítne jako neplatné VFR (422).
- DNS rebinding není řešen: ochranou proti SSRF je allowlist hostů, `dont-follow` a `InetAddressFilter`, který se vyhodnocuje jednou na požadavek; kdo ovládá DNS povoleného hosta, může jeho adresu změnit mezi kontrolou a spojením.
- Datum dat z hlavičky VFR (`vf:Hlavicka`) se neukládá; `import_run` zná čas běhu, ne stáří snímku.
- Chybí test nad statutárním městem (Brno), jehož VFR obsahuje městské obvody a části (`vf:Momc`), které parser ignoruje.
- Rychlejší start kontejneru: CDS archiv a AOT zpracování kontextu by start zkrátily z několika sekund na zlomek sekundy.
