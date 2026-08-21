package com.example.base_api.dashboard.service;

import com.example.base_api.cronograma.repository.CronogramaEventoRepository;
import com.example.base_api.dashboard.dto.DashboardResponse;
import com.example.base_api.escala.repository.EscalaRepository;
import com.example.base_api.ministerio.repository.MinisterioRepository;
import com.example.base_api.pessoa.repository.PessoaPausaRepository;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.servico.repository.ServicoRepository;
import com.example.base_api.usuario.repository.UsuarioRepository;
import com.example.base_api.voluntario.repository.PessoaServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public DashboardService(
            PessoaRepository pessoaRepository,
            MinisterioRepository ministerioRepository,
            ServicoRepository servicoRepository,
            PessoaServicoRepository pessoaServicoRepository,
            EscalaRepository escalaRepository,
            UsuarioRepository usuarioRepository,
            CronogramaEventoRepository cronogramaEventoRepository,
            PessoaPausaRepository pessoaPausaRepository
    ) {
        this.pessoaRepository = pessoaRepository;
        this.ministerioRepository = ministerioRepository;
        this.servicoRepository = servicoRepository;
        this.pessoaServicoRepository = pessoaServicoRepository;
        this.escalaRepository = escalaRepository;
        this.usuarioRepository = usuarioRepository;
        this.cronogramaEventoRepository = cronogramaEventoRepository;
        this.pessoaPausaRepository = pessoaPausaRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse obterResumo() {
        long totalPessoas = pessoaRepository.count();
        long totalPessoasAtivas = pessoaRepository.findByAtivoTrueOrderByNomeAsc().size();
        long totalVoluntariosAtivos = pessoaServicoRepository.findAll().stream()
                .filter(v -> Boolean.TRUE.equals(v.getAtivo()))
                .count();
        long totalPausasAtivas = pessoaPausaRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getAtiva()))
                .count();

        return new DashboardResponse(
                totalPessoas,
                totalPessoasAtivas,
                ministerioRepository.count(),
                servicoRepository.count(),
                totalVoluntariosAtivos,
                escalaRepository.count(),
                usuarioRepository.count(),
                usuarioRepository.findAtivosByPapel("LIDER").size(),
                cronogramaEventoRepository.count(),
                totalPausasAtivas
        );
    }
}
