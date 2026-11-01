package com.example.base_api.escala.service;

import com.example.base_api.escala.entity.Escala;
import com.example.base_api.escala.entity.EscalaServico;
import com.example.base_api.escala.repository.EscalaServicoRepository;
import com.example.base_api.servico.entity.Servico;
import com.example.base_api.voluntario.entity.PessoaServico;
import com.example.base_api.voluntario.repository.PessoaServicoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Regras comuns para montar as necessidades de uma escala sugerida (mensal ou de evento). */
@Service
public class NecessidadeEscalaService {

    private final PessoaServicoRepository pessoaServicoRepository;
    private final EscalaServicoRepository escalaServicoRepository;

    public NecessidadeEscalaService(
            PessoaServicoRepository pessoaServicoRepository,
            EscalaServicoRepository escalaServicoRepository
    ) {
        this.pessoaServicoRepository = pessoaServicoRepository;
        this.escalaServicoRepository = escalaServicoRepository;
    }

    /**
     * Serviços que entram numa escala sugerida: ativos, não fictícios (ex.: Coordenador) e com ao menos
     * um voluntário ativo. Os voluntários de todos os candidatos são consultados de uma vez.
     */
    public List<UUID> servicosElegiveis(List<Servico> candidatos) {
        List<UUID> reais = candidatos.stream()
                .filter(s -> Boolean.TRUE.equals(s.getAtivo()))
                .filter(s -> !Boolean.TRUE.equals(s.getFiccao()))
                .map(Servico::getId)
                .toList();
        if (reais.isEmpty()) {
            return List.of();
        }
        Set<UUID> comVoluntario = pessoaServicoRepository.findByServicoIdInAndAtivoTrue(reais).stream()
                .map(PessoaServico::getServicoId)
                .collect(Collectors.toSet());
        return reais.stream().filter(comVoluntario::contains).toList();
    }

    /** Cria a necessidade de uma pessoa por serviço na escala. */
    public void criarNecessidades(Escala escala, List<UUID> servicoIds) {
        for (UUID servicoId : servicoIds) {
            EscalaServico necessidade = new EscalaServico();
            necessidade.setEscalaId(escala.getId());
            necessidade.setServicoId(servicoId);
            necessidade.setQuantidadeNecessaria(1);
            escalaServicoRepository.save(necessidade);
        }
    }
}
