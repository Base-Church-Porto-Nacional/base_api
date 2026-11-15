package com.example.base_api.security;

import com.example.base_api.baseministerial.repository.BaseMinisterialRepository;
import com.example.base_api.ministerio.repository.MinisterioRepository;
import com.example.base_api.smallgroup.repository.SmallGroupRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * As consultas que derivam liderança (escopo e rótulo LIDER) filtram ministério, base e grupo ativos no
 * banco. O JPQL em si é validado no boot (ddl validate + parsing das @Query); aqui garantimos o filtro.
 */
class ConsultasLiderancaAtivaTest {

    @Test
    void consultasDeMinisterioFiltramAtivos() {
        assertFiltraAtivo(MinisterioRepository.class, "findAtivosLideradosOuCoordenados", "m.ativo = true");
        assertFiltraAtivo(MinisterioRepository.class, "idsLideresDeAtivos", "m.ativo = true");
        assertFiltraAtivo(MinisterioRepository.class, "idsCoordenadoresDeAtivos", "m.ativo = true");
    }

    @Test
    void consultasDeBaseFiltramAtivas() {
        assertFiltraAtivo(BaseMinisterialRepository.class, "findAtivasLideradasOuCoordenadas", "b.ativo = true");
        assertFiltraAtivo(BaseMinisterialRepository.class, "idsLideresDeAtivas", "b.ativo = true");
        assertFiltraAtivo(BaseMinisterialRepository.class, "idsCoordenadoresDeAtivas", "b.ativo = true");
    }

    @Test
    void consultaDeGrupoFiltraAtivos() {
        assertFiltraAtivo(SmallGroupRepository.class, "idsLideresDeAtivos", "g.ativo = true");
    }

    private static void assertFiltraAtivo(Class<?> repositorio, String metodo, String filtro) {
        Method m = Arrays.stream(repositorio.getMethods())
                .filter(x -> x.getName().equals(metodo))
                .findFirst()
                .orElseThrow(() -> new AssertionError(repositorio.getSimpleName() + "." + metodo + " não existe"));
        Query query = m.getAnnotation(Query.class);
        assertThat(query).as(metodo + " deve ter @Query").isNotNull();
        assertThat(query.value().replaceAll("\\s+", " ")).as(metodo).containsIgnoringCase(filtro);
    }
}
