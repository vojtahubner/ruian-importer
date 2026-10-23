# ADR 0003: Synchronní import, historie běhů a synchronizace částí

Stav: přijato

## Kontext

Zadání chce naplnit tabulky obec a část obce. Chceme navíc, aby import šel spouštět opakovaně (při startu i přes API)
a dopadl vždy stejně, aby po sobě nechával záznam a aby nevadilo, když ho dva klienti nebo dvě instance spustí
současně. Zároveň nechceme, aby chyba uprostřed importu nechala v databázi půlku dat.

## Zvažované možnosti

Přístup k databázi:

1. JPA (Hibernate, Spring Data) + Flyway. Entity, auditing časů, repozitáře zadarmo; schéma verzované migracemi,
   Hibernate ho jen validuje.
2. `JdbcClient` + ruční SQL + Flyway. Méně vrstev, plná kontrola nad SQL, ale ruční mapování řádků a časů.
3. JPA s `ddl-auto=update` bez migrací. Nejméně práce, ale schéma mimo kontrolu a zadání výslovně počítá s ručně
   připraveným schématem.

Naplnění tabulek: smazat vše a vložit znovu; `INSERT ... ON CONFLICT`; nebo load-then-diff (načíst existující řádky,
změnit jen rozdíly, smazat chybějící).

Souběh: nic; zámek v aplikaci (`synchronized`); `SELECT ... FOR UPDATE` na řádku obce (při prvním importu řádek
neexistuje); PostgreSQL advisory lock.

Běh importu: synchronně, odpověď 201 po dokončení, záznam o běhu až s výsledkem; nebo asynchronně, 202 Accepted,
záznam ve stavu `RUNNING` a dotazování na průběh.

## Rozhodnutí

- JPA + Flyway (možnost 1): `V1__municipality.sql`, `V2__import_run.sql`, `ddl-auto=validate`. Jediný netriviální
  kus SQL (advisory lock) je nativní příkaz přes `Session.doWork`.
- Load-then-diff v `MunicipalityPersistenceAdapter.replace()`: obec se přejmenuje nebo založí; části se hledají podle
  vlastního, celostátně jedinečného kódu, takže část přesunutá z jiné obce se přeřadí místo zdvojení; nezměněné řádky
  se nesahají (audit časy se posunou jen při změně); části obce, které ve zdroji nejsou, se smažou. Vše v jedné
  transakci.
- Transakční advisory lock `pg_advisory_xact_lock(1, kód obce)` na začátku `replace()`.
- Synchronní import s jedním záznamem `import_run` až na konci, ve stavu `SUCCEEDED` nebo `FAILED`; stav `RUNNING`
  neexistuje. Záznam se ukládá v samostatné transakci, takže přežije rollback dat.

## Důsledky

Plus:

- Import je idempotentní (`secondImportIsIdempotent` v `ImportFlowIT`), API nikdy neukáže napůl zapsanou obec,
  neúspěšný běh je dohledatelný s důvodem.
- Souběžné importy téže obce se serializují i napříč instancemi, importy různých obcí se neblokují, zámek se uvolní
  sám při commitu nebo rollbacku.
- Schéma je v repozitáři a drift mezi entitami a tabulkami zastaví start.

Minus:

- Dlouhé stažení drží HTTP požadavek otevřený (omezeno `spring.http.clients.read-timeout` 60 s); klient nevidí
  průběh a při pádu aplikace uprostřed importu žádný záznam nevznikne.
- Zámek chrání jen stejnou obec; globální limit souběžných importů neexistuje.
- JPA stojí mapování entit na recordy a pozornost u `persist` vs. `save` (merge by přepsal audit čas); `JdbcClient`
  by byl kratší.
