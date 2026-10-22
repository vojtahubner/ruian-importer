package cz.vojtechhubner.ruian.adapter.out.xml;

/**
 * XML namespaces of the VFR elements the parser reads.
 */
final class VfrNamespaces {

    /** Exchange format envelope: {@code vf:VymennyFormat}, {@code vf:Data}, {@code vf:Obec}, {@code vf:CastObce}. */
    static final String VF = "urn:cz:isvs:ruian:schemas:VymennyFormatTypy:v1";

    /** Municipality fields: {@code obi:Kod}, {@code obi:Nazev}. */
    static final String OBI = "urn:cz:isvs:ruian:schemas:ObecIntTypy:v1";

    /** Municipality part fields: {@code coi:Kod}, {@code coi:Nazev}, {@code coi:Obec}. */
    static final String COI = "urn:cz:isvs:ruian:schemas:CastObceIntTypy:v1";

    private VfrNamespaces() {
    }
}
