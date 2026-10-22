# Testovací úloha – Zpracuj XML a ulož

Vytvořte javovskou aplikaci, která stáhne data v XML z internetu, zpracuje je a uloží do SQL databáze.

## Zadání

- Zazipované XML na URL <https://www.smartform.cz/download/kopidlno.xml.zip> obsahuje všechny adresy v obci Kopidlno.
- Cílem je napsat program, který stáhne soubor z daného URL, zparsuje data z XML a (některá) uloží do DB.
- Aplikace má dvě tabulky – **obec** a **část obce**.
  - U obce stačí do DB vložit kód a název, u části obce kód, název a kód obce, ke které část obce patří.
- Program by měl tyto dvě tabulky naplnit. (V XML by měla být jedna obec – element `vf:Obec` – a několik málo částí obce – `vf:CastObce`.)
- Program nemusí databázové schéma vytvářet, to stačí udělat ručně.
- Na parsování použijte nějaký standardní nástroj (knihovnu, např. DOM, SAX, StAX, …).
  - Není nutné při parsování získat všechna data, která jsou v XML, stačí z něj získat jen ta, která budeme dávat do DB.
- Jako databázi lze použít libovolnou SQL databázi.
- Program by měl být napsán v Javě (samozřejmě můžete použít jakýkoliv framework, který znáte a usnadní vám práci).
  - Potěší nás, pokud použijete Spring a/nebo Docker.

Chuti a fantazii se meze nekladou a na úloze nám můžete ukázat, co umíte. Těšíme se na vaše inovativní řešení.

## Odevzdání

1. Úlohu vypracujte ve vlastním veřejném repozitáři.
2. Pošlete nám URL svého repozitáře.
