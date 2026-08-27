package com.example.base_api.dashboard.service;

import com.example.base_api.cronograma.repository.CronogramaEventoRepository;
import com.example.base_api.dashboard.dto.DashboardResponse;
import com.example.base_api.escala.repository.EscalaRepository;
import com.example.base_api.ministerio.entity.Ministerio;
import com.example.base_api.ministerio.repository.MinisterioRepository;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaPausaRepository;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.servico.repository.ServicoRepository;
import com.example.base_api.usuario.repository.UsuarioRepository;
import com.example.base_api.voluntario.repository.PessoaServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DashboardService {

    private final PessoaRepository pessoaRepository;
    private final MinisterioRepository ministerioRepository;
    private final ServicoRepository servicoRepository;
    private final PessoaServicoRepository pessoaServicoRepository;
    private final EscalaRepository escalaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CronogramaEventoRepository cronogramaEventoRepository;
    private final PessoaPausaRepository pessoaPausaRepository;
    private final AutorizacaoService autorizacaoService;

    public DashboardService(
            PessoaRepository pessoaRepository,
            MinisterioRepository ministerioRepository,
            ServicoRepository servicoRepository,
            PessoaServicoRepository pessoaServicoRepository,
            EscalaRepository escalaRepository,
            UsuarioRepository usuarioRepository,
            CronogramaEventoRepository cronogramaEventoRepository,
            PessoaPausaRepository pessoaPausaRepository,
            AutorizacaoService autorizacaoService
    ) {
        this.pessoaRepository = pessoaRepository;
        this.ministerioRepository = ministerioRepository;
        this.servicoRepository = servicoRepository;
        this.pessoaServicoRepository = pessoaServicoRepository;
        this.escalaRepository = escalaRepository;
        this.usuarioRepository = usuarioRepository;
        this.cronogramaEventoRepository = cronogramaEventoRepository;
        this.pessoaPausaRepository = pessoaPausaRepository;
        this.autorizacaoService = autorizacaoService;
    }

    @Transactional(readOnly = true)
    public DashboardResponse obterResumo() {
        autorizacaoService.assertPastorOuAdmin(autorizacaoService.igrejaAtualId());
        List<UUID> ids = autorizacaoService.idsParaListagem(null);
        if (ids.isEmpty()) {
            return new DashboardResponse(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        List<UUID> pessoaIds = pessoaRepository.findByIgrejaIdInOrderByNomeAsc(ids).stream()
                .map(Pessoa::getId)
                .toList();
        List<UUID> ministerioIds = ministerioRepository.findByIgrejaIdInOrderByNomeAsc(ids).stream()
                .map(Ministerio::getId)
                .toList();

        long totalVoluntariosAtivos = pessoaIds.isEmpty()
                ? 0
                : pessoaServicoRepository.countByPessoaIdInAndAtivoTrue(pessoaIds);
        long totalPausasAtivas = pessoaIds.isEmpty()
                ? 0
                : pessoaPausaRepository.countByPessoaIdInAndAtivaTrue(pessoaIds);
        long totalServicos = ministerioIds.isEmpty()
                ? 0
                : servicoRepository.countByMinisterioIdIn(ministerioIds);

        return new DashboardResponse(
                pessoaRepository.countByIgrejaIdIn(ids),
                pessoaRepository.countByIgrejaIdInAndAtivoTrue(ids),
                ministerioRepository.countByIgrejaIdIn(ids),
                totalServicos,
                totalVoluntariosAtivos,
                escalaRepository.countByIgrejaIdIn(ids),
                usuarioRepository.countByIgrejaIdIn(ids),
                usuarioRepository.findAtivosByPapelAndIgrejaIdIn("LIDER", ids).size(),
                cronogramaEventoRepository.countByIgrejaIdIn(ids),
                totalPausasAtivas
        );
    }
}
