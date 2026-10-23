# ADR 0004: Allowlist zdrojů a SSRF

Stav: přijato

## Kontext

`POST /api/imports` přijímá `sourceUrl`, aby šla importovat libovolná obec z VDP. Služba tak na požádání stahuje
z adresy zadané klientem, což je učebnicový vektor SSRF: požadavek na interní síť, metadata cloudu, lokální porty.
Zdroje, které dávají smysl, jsou přitom jen dva: `smartform.cz` (soubor ze zadání) a `vdp.cuzk.gov.cz`.

## Zvažované možnosti

1. Libovolná URL bez kontroly. Nejjednodušší, SSRF bez omezení.
2. Pouze výchozí zdroj, `sourceUrl` neexistuje. Bezpečné, ale import jiné obce by nebyl možný.
3. Allowlist hostů + zákaz přesměrování + filtr cílových adres jako obrana do hloubky.
4. Úplná ochrana: vyřešit DNS, připnout IP adresu a spojit se jen s ní (s původním `Host`/SNI). Vyžaduje vlastní
   HTTP klient nebo request factory; `RestClient` to z krabice nenabízí.

## Rozhodnutí

Možnost 3, ve třech vrstvách:

- `SourceUrlPolicy` (`application`) přijme jen absolutní `https` URL bez user info, na portu 443 (nebo bez portu),
  jejíž host je přesně jeden z `ruian.import.allowed-hosts` nebo jeho subdoména (`www.smartform.cz` ano,
  `smartform.cz.evil.example` ne). Kontrola proběhne před jakýmkoli stažením a nezakládá záznam běhu.
- `spring.http.clients.redirects=dont-follow`: allowlist se vyhodnocuje jen na zadané URL, takže přesměrování z
  povoleného hosta jinam se nesmí následovat. `HttpVfrSource` hlásí 3xx jako nedostupný zdroj (502) a do zprávy dává
  host z `Location`.
- `InetAddressFilter.externalAddresses()` jako bean (`HttpClientConfig`): Spring Boot ho aplikuje na
  auto-konfigurované HTTP klienty a ty odmítnou loopback, privátní, link-local a další neveřejné adresy. Chrání před
  případem, kdy by povolený host ukazoval do vnitřní sítě.

## Důsledky

Plus:

- Import jiné obce funguje a přitom cíl stahování nelze zvolit mimo dva veřejné hosty.
- Každá vrstva má vlastní test (`SourceUrlPolicyTest`, `HttpVfrSourceTest` pro redirect, `ImportFlowIT` pro
  odmítnutí bez běhu).

Minus:

- Filtr adres se vyhodnocuje jednou na požadavek. Kdyby DNS povoleného hosta vrátilo mezi kontrolou a spojením jinou
  adresu (DNS rebinding), filtr to nezachytí. Přijato: allowlist obsahuje jen dva hosty provozované ČÚZK a
  Smartformem a plná ochrana (možnost 4) by stála vlastní klient.
- Nový zdroj dat znamená změnu konfigurace, ne jen jiné URL v požadavku.
- Zdroj dostupný pouze přes HTTP nebo na jiném portu importovat nelze.
