# VFR test fixtures

- `kopidlno.xml.zip` — the real RÚIAN exchange format (VFR) file for the municipality Kopidlno (code 573060),
  downloaded on 2026-10-22 from <https://www.smartform.cz/download/kopidlno.xml.zip> (526 184 bytes). The archive
  holds one entry, `20210331_OB_573060_UZSZ.xml` (13.4 MB). RÚIAN data is published by ČÚZK (the Czech Office for
  Surveying, Mapping and Cadastre) as open data.
- `kopidlno-sample.xml` — a hand-trimmed excerpt of the zipped XML: the original root element (without the version
  history comment that follows it) and header, the complete `vf:Obce` and `vf:CastiObci` sections and the first item
  of `vf:KatastralniUzemi`, `vf:Ulice`, `vf:Parcely` and `vf:AdresniMista`. A synthetic `vf:SpravniObvody` section
  was added between `vf:Obce` and `vf:CastiObci` so that tests can check that unrelated sections are passed through.
