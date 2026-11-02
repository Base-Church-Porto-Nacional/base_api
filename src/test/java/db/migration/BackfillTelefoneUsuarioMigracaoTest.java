package db.migration;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BackfillTelefoneUsuarioMigracaoTest {

    private static V21__backfill_telefone_usuario.Candidato candidato(UUID id, String whatsapp, String telefone) {
        return new V21__backfill_telefone_usuario.Candidato(id, whatsapp, telefone);
    }

    @Test
    void usaWhatsappNormalizadoSenaoTelefone() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        Map<UUID, String> resultado = V21__backfill_telefone_usuario.telefonesParaPreencher(List.of(
                candidato(a, "(63) 99999-0000", "63 3333-4444"),
                candidato(b, null, "+55 (11) 98888-7777")), Set.of());

        assertThat(resultado).containsExactlyInAnyOrderEntriesOf(Map.of(
                a, "5563999990000",
                b, "5511988887777"));
    }

    @Test
    void whatsappInvalidoCaiNoTelefone() {
        UUID a = UUID.randomUUID();

        Map<UUID, String> resultado = V21__backfill_telefone_usuario.telefonesParaPreencher(List.of(
                candidato(a, "123", "(63) 3333-4444")), Set.of());

        assertThat(resultado).containsEntry(a, "556333334444");
    }

    @Test
    void pulaTelefoneJaUsadoPorOutraConta() {
        UUID a = UUID.randomUUID();

        Map<UUID, String> resultado = V21__backfill_telefone_usuario.telefonesParaPreencher(List.of(
                candidato(a, "(63) 99999-0000", null)), Set.of("5563999990000"));

        assertThat(resultado).isEmpty();
    }

    @Test
    void pulaTodosQuandoDoisCandidatosTemOMesmoTelefone() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();

        Map<UUID, String> resultado = V21__backfill_telefone_usuario.telefonesParaPreencher(List.of(
                candidato(a, "(63) 99999-0000", null),
                candidato(b, "5563999990000", null),
                candidato(c, "(63) 98888-1111", null)), Set.of());

        assertThat(resultado).containsOnlyKeys(c);
    }

    @Test
    void semTelefoneValidoNaoPreenche() {
        Map<UUID, String> resultado = V21__backfill_telefone_usuario.telefonesParaPreencher(List.of(
                candidato(UUID.randomUUID(), null, null),
                candidato(UUID.randomUUID(), "abc", "pessoa@email.com")), Set.of());

        assertThat(resultado).isEmpty();
    }
}
