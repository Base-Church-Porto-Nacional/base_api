package db.migration;

import com.example.base_api.security.Identificador;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

/**
 * Fase 2: contas anteriores ao login por telefone ficaram com usuario.telefone nulo. Preenche com o
 * WhatsApp da pessoa (senão o telefone), normalizado como no login ({@link Identificador#telefone}).
 * Só grava quando o número fica único: se já pertence a outra conta, ou se dois candidatos caem no
 * mesmo número (ex.: telefone da família), nenhum deles recebe — a secretaria corrige depois na pessoa.
 */
public class V21__backfill_telefone_usuario extends BaseJavaMigration {

    record Candidato(UUID usuarioId, String whatsapp, String telefone) {
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection conexao = context.getConnection();
        List<Candidato> candidatos = new ArrayList<>();
        Set<String> existentes = new HashSet<>();
        try (Statement st = conexao.createStatement()) {
            try (ResultSet rs = st.executeQuery("""
                    SELECT u.id, p.whatsapp, p.telefone
                    FROM usuario u
                    JOIN pessoa p ON p.id = u.pessoa_id
                    WHERE u.telefone IS NULL
                    """)) {
                while (rs.next()) {
                    candidatos.add(new Candidato(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3)));
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT telefone FROM usuario WHERE telefone IS NOT NULL")) {
                while (rs.next()) {
                    existentes.add(rs.getString(1));
                }
            }
        }

        Map<UUID, String> preencher = telefonesParaPreencher(candidatos, existentes);
        if (preencher.isEmpty()) {
            return;
        }
        try (PreparedStatement update = conexao.prepareStatement(
                "UPDATE usuario SET telefone = ? WHERE id = ? AND telefone IS NULL")) {
            for (Map.Entry<UUID, String> e : preencher.entrySet()) {
                update.setString(1, e.getValue());
                update.setObject(2, e.getKey());
                update.addBatch();
            }
            update.executeBatch();
        }
    }

    /** Telefone normalizado por usuário, sem números já usados nem repetidos entre os candidatos. */
    static Map<UUID, String> telefonesParaPreencher(Collection<Candidato> candidatos, Set<String> existentes) {
        Map<UUID, String> normalizados = new LinkedHashMap<>();
        Map<String, Integer> ocorrencias = new HashMap<>();
        for (Candidato c : candidatos) {
            Identificador.telefone(c.whatsapp())
                    .or(() -> Identificador.telefone(c.telefone()))
                    .ifPresent(numero -> {
                        normalizados.put(c.usuarioId(), numero);
                        ocorrencias.merge(numero, 1, Integer::sum);
                    });
        }
        Map<UUID, String> resultado = new LinkedHashMap<>();
        normalizados.forEach((usuarioId, numero) -> {
            if (!existentes.contains(numero) && ocorrencias.get(numero) == 1) {
                resultado.put(usuarioId, numero);
            }
        });
        return resultado;
    }
}
