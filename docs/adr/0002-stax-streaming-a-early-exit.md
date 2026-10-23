# ADR 0002: StAX streaming a early exit

Stav: přijato

## Kontext

VFR soubor obce je ZIP s jedním XML; Kopidlno má po rozbalení 13 411 844 bajtů, větší obce řádově víc. Potřebujeme
z něj jen element `vf:Obec` (kód, název) a elementy `vf:CastObce` (kód, název, kód obce), které jsou v sekcích
`vf:Obce` a `vf:CastiObci` hned za hlavičkou. Zbytek souboru (ulice, parcely, adresní místa) nás nezajímá. Zadání
požaduje standardní parser a zdroj URL může zadat uživatel, takže vstupu nelze věřit.

## Zvažované možnosti

1. DOM: celý dokument v paměti. Nejjednodušší kód, ale paměť úměrná velikosti souboru a čte se vše.
2. SAX: push model s callbacky, konstantní paměť. Předčasné ukončení jen výjimkou, stav parsování se drží
   v handleru mimo tok programu.
3. JAXB: generování tříd ze schémat VFR, pak opět celý dokument v paměti, navíc velká závislost.
4. StAX (JDK, `javax.xml.stream`): pull model, konstantní paměť, čtení se prostě přeruší, kód parsování je obyčejná
   smyčka.

## Rozhodnutí

Možnost 4. `StaxVfrParser` používá kurzorové API `XMLStreamReader` z JDK, bez další knihovny. Čte `vf:Obec` a
`vf:CastObce` podle namespace a lokálního jména, vše ostatní přeskakuje a na koncovém elementu `vf:CastiObci` (nebo
`vf:Data`) skončí, aniž by zbytek souboru četl. ZIP rozpozná podle prvních čtyř bajtů a bere první položku `.xml`,
takže totéž funguje pro plain XML i archiv. Vstup se obaluje do `BoundedInputStream`, který počítá rozbalené bajty a
nad `ruian.import.max-uncompressed-bytes` (64 MB) parsování ukončí; DTD a externí entity jsou vypnuté. Vztah
část → obec se po přečtení kontroluje (část cizí obce je chyba).

## Důsledky

Plus:

- Paměť nezávisí na velikosti souboru a nic se neukládá na disk.
- Z 13 411 844 bajtů XML se přečte 10 440 (konec `vf:CastiObci`), z 526 kB archivu zhruba 2 kB komprimovaných dat;
  `StaxVfrParserRealFileTest` to hlídá.
- ZIP bomba ani XXE neprojdou, chyby přenosu (`IOException`) se odlišují od chyb formátu (`VfrParseException`).

Minus:

- Ruční stavový automat: čtení potomků elementu a přeskakování cizích podstromů je vlastní kód (`readMunicipality`,
  `readPart`, `skipElement`), který by DOM nebo JAXB daly zadarmo.
- Zbytek dokumentu se nevaliduje: soubor poškozený až za sekcí `vf:CastiObci` projde (test
  `stopsReadingAfterTheMunicipalityParts` to dokonce využívá).
- Spoléhá na pořadí sekcí ve VFR (obce před částmi obcí, obojí na začátku), které schéma VFR zaručuje.
