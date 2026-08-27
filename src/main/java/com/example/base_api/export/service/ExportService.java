package com.example.base_api.export.service;

import com.example.base_api.audit.service.AuditService;
import com.example.base_api.export.dto.ExportDocument;
import com.example.base_api.export.dto.ExportFile;
import com.example.base_api.export.dto.ExportSheet;
import com.example.base_api.export.dto.FormatoExportacao;
import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.disponibilidade.entity.DisponibilidadeSemanal;
import com.example.base_api.disponibilidade.entity.Indisponibilidade;
import com.example.base_api.disponibilidade.repository.DisponibilidadeSemanalRepository;
import com.example.base_api.disponibilidade.repository.IndisponibilidadeRepository;
import com.example.base_api.escala.entity.Escala;
import com.example.base_api.escala.entity.EscalaServico;
import com.example.base_api.escala.entity.EscalaVoluntario;
import com.example.base_api.escala.repository.EscalaRepository;
import com.example.base_api.escala.repository.EscalaServicoRepository;
import com.example.base_api.escala.repository.EscalaVoluntarioRepository;
import com.example.base_api.jornada.entity.JornadaMembro;
import com.example.base_api.jornada.repository.JornadaMembroRepository;
import com.example.base_api.ministerio.entity.Ministerio;
import com.example.base_api.ministerio.repository.MinisterioRepository;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.entity.PessoaHistorico;
import com.example.base_api.pessoa.repository.PessoaHistoricoRepository;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.servico.entity.Servico;
import com.example.base_api.servico.repository.ServicoRepository;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.repository.UsuarioRepository;
import com.example.base_api.voluntario.entity.PessoaServico;
import com.example.base_api.voluntario.repository.PessoaServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ExportService {

    private final ExportRenderer exportRenderer;
    private final PessoaRepository pessoaRepository;
    private final PessoaHistoricoRepository pessoaHistoricoRepository;
    private final JornadaMembroRepository jornadaMembroRepository;
    private final MinisterioRepository ministerioRepository;
    private final ServicoRepository servicoRepository;
    private final PessoaServicoRepository pessoaServicoRepository;
    private final EscalaRepository escalaRepository;
    private final EscalaServicoRepository escalaServicoRepository;
    private final EscalaVoluntarioRepository escalaVoluntarioRepository;
    private final DisponibilidadeSemanalRepository disponibilidadeSemanalRepository;
    private final IndisponibilidadeRepository indisponibilidadeRepository;
    private final UsuarioRepository usuarioRepository;
    private final AutorizacaoService autorizacaoService;
    private final AuditService auditService;

    public ExportService(
            ExportRenderer exportRenderer,
            PessoaRepository pessoaRepository,
            PessoaHistoricoRepository pessoaHistoricoRepository,
            JornadaMembroRepository jornadaMembroRepository,
            MinisterioRepository ministerioRepository,
            ServicoRepository servicoRepository,
            PessoaServicoRepository pessoaServicoRepository,
            EscalaRepository escalaRepository,
            EscalaServicoRepository escalaServicoRepository,
            EscalaVoluntarioRepository escalaVoluntarioRepository,
            DisponibilidadeSemanalRepository disponibilidadeSemanalRepository,
            IndisponibilidadeRepository indisponibilidadeRepository,
            UsuarioRepository usuarioRepository,
            AutorizacaoService autorizacaoService,
            AuditService auditService
    ) {
        this.exportRenderer = exportRenderer;
        this.pessoaRepository = pessoaRepository;
        this.pessoaHistoricoRepository = pessoaHistoricoRepository;
        this.jornadaMembroRepository = jornadaMembroRepository;
        this.ministerioRepository = ministerioRepository;
        this.servicoRepository = servicoRepository;
        this.pessoaServicoRepository = pessoaServicoRepository;
        this.escalaRepository = escalaRepository;
        this.escalaServicoRepository = escalaServicoRepository;
        this.escalaVoluntarioRepository = escalaVoluntarioRepository;
        this.disponibilidadeSemanalRepository = disponibilidadeSemanalRepository;
        this.indisponibilidadeRepository = indisponibilidadeRepository;
        this.usuarioRepository = usuarioRepository;
        this.autorizacaoService = autorizacaoService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public ExportFile exportar(String recurso, FormatoExportacao formato) {
        autorizacaoService.assertPastorOuAdmin(autorizacaoService.igrejaAtualId());
        Escopo escopo = carregarEscopo();
        ExportDocument document = switch (recurso.toLowerCase()) {
            case "pessoas" -> documentPessoas(escopo);
            case "historicos", "historico" -> documentHistoricos(escopo);
            case "jornadas" -> documentJornadas(escopo);
            case "ministerios" -> documentMinisterios(escopo);
            case "servicos" -> documentServicos(escopo);
            case "voluntarios" -> documentVoluntarios(escopo);
            case "escalas" -> documentEscalas(escopo);
            case "disponibilidades" -> documentDisponibilidades(escopo);
            case "usuarios" -> documentUsuarios(escopo);
            case "completo", "tudo" -> documentCompleto(escopo);
            default -> throw new BusinessException(
                    "Recurso inválido. Use: pessoas, historicos, jornadas, ministerios, servicos, voluntarios, escalas, disponibilidades, usuarios ou completo"
            );
        };
        ExportFile arquivo = exportRenderer.renderizar(document, formato);
        auditService.registrar("EXPORTAR", recurso.toLowerCase(), "formato=" + formato.name());
        return arquivo;
    }

    private Escopo carregarEscopo() {
        List<UUID> igrejaIds = autorizacaoService.idsParaListagem(null);
        if (igrejaIds.isEmpty()) {
            return Escopo.vazio();
        }
        List<Pessoa> pessoas = pessoaRepository.findByIgrejaIdInOrderByNomeAsc(igrejaIds);
        List<UUID> pessoaIds = pessoas.stream().map(Pessoa::getId).toList();
        List<Ministerio> ministerios = ministerioRepository.findByIgrejaIdInOrderByNomeAsc(igrejaIds);
        List<UUID> ministerioIds = ministerios.stream().map(Ministerio::getId).toList();
        List<Servico> servicos = ministerioIds.isEmpty()
                ? List.of()
                : servicoRepository.findByMinisterioIdIn(ministerioIds);
        List<Escala> escalas = escalaRepository.findByIgrejaIdInOrderByCompetenciaDescTituloAsc(igrejaIds);
        List<UUID> escalaIds = escalas.stream().map(Escala::getId).toList();
        List<Usuario> usuarios = usuarioRepository.findByIgrejaIdIn(igrejaIds);
        return new Escopo(pessoas, pessoaIds, ministerios, servicos, escalas, escalaIds, usuarios);
    }

    private ExportDocument documentPessoas(Escopo escopo) {
        ExportDocument doc = new ExportDocument("pessoas");
        doc.adicionarSheet(sheetPessoas(escopo));
        return doc;
    }

    private ExportDocument documentHistoricos(Escopo escopo) {
        ExportDocument doc = new ExportDocument("historicos");
        doc.adicionarSheet(sheetHistoricos(escopo));
        return doc;
    }

    private ExportDocument documentJornadas(Escopo escopo) {
        ExportDocument doc = new ExportDocument("jornadas");
        doc.adicionarSheet(sheetJornadas(escopo));
        return doc;
    }

    private ExportDocument documentMinisterios(Escopo escopo) {
        ExportDocument doc = new ExportDocument("ministerios");
        doc.adicionarSheet(sheetMinisterios(escopo));
        return doc;
    }

    private ExportDocument documentServicos(Escopo escopo) {
        ExportDocument doc = new ExportDocument("servicos");
        doc.adicionarSheet(sheetServicos(escopo));
        return doc;
    }

    private ExportDocument documentVoluntarios(Escopo escopo) {
        ExportDocument doc = new ExportDocument("voluntarios");
        doc.adicionarSheet(sheetVoluntarios(escopo));
        return doc;
    }

    private ExportDocument documentEscalas(Escopo escopo) {
        ExportDocument doc = new ExportDocument("escalas");
        doc.adicionarSheet(sheetEscalas(escopo));
        doc.adicionarSheet(sheetEscalaServicos(escopo));
        doc.adicionarSheet(sheetEscalaVoluntarios(escopo));
        return doc;
    }

    private ExportDocument documentDisponibilidades(Escopo escopo) {
        ExportDocument doc = new ExportDocument("disponibilidades");
        doc.adicionarSheet(sheetDisponibilidadeSemanal(escopo));
        doc.adicionarSheet(sheetIndisponibilidades(escopo));
        return doc;
    }

    private ExportDocument documentUsuarios(Escopo escopo) {
        ExportDocument doc = new ExportDocument("usuarios");
        doc.adicionarSheet(sheetUsuarios(escopo));
        return doc;
    }

    private ExportDocument documentCompleto(Escopo escopo) {
        ExportDocument doc = new ExportDocument("exportacao_completa");
        doc.adicionarSheet(sheetPessoas(escopo));
        doc.adicionarSheet(sheetHistoricos(escopo));
        doc.adicionarSheet(sheetJornadas(escopo));
        doc.adicionarSheet(sheetMinisterios(escopo));
        doc.adicionarSheet(sheetServicos(escopo));
        doc.adicionarSheet(sheetVoluntarios(escopo));
        doc.adicionarSheet(sheetEscalas(escopo));
        doc.adicionarSheet(sheetEscalaServicos(escopo));
        doc.adicionarSheet(sheetEscalaVoluntarios(escopo));
        doc.adicionarSheet(sheetDisponibilidadeSemanal(escopo));
        doc.adicionarSheet(sheetIndisponibilidades(escopo));
        doc.adicionarSheet(sheetUsuarios(escopo));
        return doc;
    }

    private ExportSheet sheetPessoas(Escopo escopo) {
        ExportSheet sheet = new ExportSheet("Pessoas", List.of(
                "ID", "Nome", "Nome Social", "Email", "Telefone",
                "Data Nascimento", "Data Cadastro", "Igreja ID", "Ativo", "Observações"
        ));
        for (Pessoa p : escopo.pessoas) {
            sheet.adicionarLinha(List.of(
                    str(p.getId()),
                    str(p.getNome()),
                    str(p.getNomeSocial()),
                    str(p.getEmail()),
                    str(p.getTelefone()),
                    str(p.getDataNascimento()),
                    str(p.getDataCadastro()),
                    str(p.getIgrejaId()),
                    str(p.getAtivo()),
                    str(p.getObservacoes())
            ));
        }
        return sheet;
    }

    private ExportSheet sheetHistoricos(Escopo escopo) {
        ExportSheet sheet = new ExportSheet("Historicos", List.of(
                "Pessoa", "Descrição", "Autor", "Cargo", "Data"
        ));
        for (PessoaHistorico h : listarHistoricos(escopo)) {
            sheet.adicionarLinha(List.of(
                    nomePessoa(escopo.pessoasPorId, h.getPessoaId()),
                    str(h.getDescricao()),
                    str(h.getAutorNome()),
                    str(h.getAutorCargo()),
                    str(h.getDataRegistro())
            ));
        }
        return sheet;
    }

    private ExportSheet sheetJornadas(Escopo escopo) {
        ExportSheet sheet = new ExportSheet("Jornadas", List.of(
                "Pessoa", "Aceitou Jesus", "Data Aceitou Jesus", "Batizado", "Data Batismo",
                "Trilha Base", "Data Trilha Base", "Membro", "Data Membro"
        ));
        for (JornadaMembro j : listarJornadas(escopo)) {
            sheet.adicionarLinha(List.of(
                    nomePessoa(escopo.pessoasPorId, j.getPessoaId()),
                    str(j.getAceitouJesus()),
                    str(j.getDataAceitouJesus()),
                    str(j.getBatizado()),
                    str(j.getDataBatismo()),
                    str(j.getConcluiuTrilhaBase()),
                    str(j.getDataConclusaoTrilhaBase()),
                    str(j.getMembro()),
                    str(j.getDataMembro())
            ));
        }
        return sheet;
    }

    private ExportSheet sheetMinisterios(Escopo escopo) {
        ExportSheet sheet = new ExportSheet("Ministerios", List.of("ID", "Nome", "Igreja ID", "Tipo", "Descrição", "Ativo"));
        for (Ministerio m : escopo.ministerios) {
            sheet.adicionarLinha(List.of(
                    str(m.getId()),
                    str(m.getNome()),
                    str(m.getIgrejaId()),
                    str(m.getTipo()),
                    str(m.getDescricao()),
                    str(m.getAtivo())
            ));
        }
        return sheet;
    }

    private ExportSheet sheetServicos(Escopo escopo) {
        ExportSheet sheet = new ExportSheet("Servicos", List.of(
                "ID", "Ministério", "Nome", "Descrição", "Ativo"
        ));
        for (Servico s : escopo.servicos) {
            sheet.adicionarLinha(List.of(
                    str(s.getId()),
                    nomeMinisterio(escopo.ministeriosPorId, s.getMinisterioId()),
                    str(s.getNome()),
                    str(s.getDescricao()),
                    str(s.getAtivo())
            ));
        }
        return sheet;
    }

    private ExportSheet sheetVoluntarios(Escopo escopo) {
        ExportSheet sheet = new ExportSheet("Voluntarios", List.of(
                "Pessoa", "Ministério", "Serviço", "Cargo", "Data Entrada", "Ativo", "Líder Responsável", "Observações"
        ));
        for (PessoaServico v : listarVoluntarios(escopo)) {
            Servico servico = escopo.servicosPorId.get(v.getServicoId());
            String ministerioNome = servico == null
                    ? ""
                    : nomeMinisterio(escopo.ministeriosPorId, servico.getMinisterioId());
            sheet.adicionarLinha(List.of(
                    nomePessoa(escopo.pessoasPorId, v.getPessoaId()),
                    ministerioNome,
                    servico == null ? "" : str(servico.getNome()),
                    str(v.getCargo()),
                    str(v.getDataEntrada()),
                    str(v.getAtivo()),
                    nomePessoa(escopo.pessoasPorId, v.getLiderResponsavelId()),
                    str(v.getObservacoes())
            ));
        }
        return sheet;
    }

    private ExportSheet sheetEscalas(Escopo escopo) {
        ExportSheet sheet = new ExportSheet("Escalas", List.of(
                "ID", "Título", "Competência", "Tipo", "Igreja ID", "Data", "Hora Início", "Hora Fim", "Local", "Ativo", "Descrição"
        ));
        for (Escala e : escopo.escalas) {
            sheet.adicionarLinha(List.of(
                    str(e.getId()),
                    str(e.getTitulo()),
                    str(e.getCompetencia()),
                    str(e.getTipo()),
                    str(e.getIgrejaId()),
                    str(e.getDataEvento()),
                    str(e.getHoraInicio()),
                    str(e.getHoraFim()),
                    str(e.getLocalEvento()),
                    str(e.getAtivo()),
                    str(e.getDescricao())
            ));
        }
        return sheet;
    }

    private ExportSheet sheetEscalaServicos(Escopo escopo) {
        ExportSheet sheet = new ExportSheet("Escala Servicos", List.of(
                "Escala", "Serviço", "Quantidade Necessária"
        ));
        for (EscalaServico item : listarEscalaServicos(escopo)) {
            Escala escala = escopo.escalasPorId.get(item.getEscalaId());
            Servico servico = escopo.servicosPorId.get(item.getServicoId());
            sheet.adicionarLinha(List.of(
                    escala == null ? str(item.getEscalaId()) : str(escala.getTitulo()),
                    servico == null ? str(item.getServicoId()) : str(servico.getNome()),
                    str(item.getQuantidadeNecessaria())
            ));
        }
        return sheet;
    }

    private ExportSheet sheetEscalaVoluntarios(Escopo escopo) {
        ExportSheet sheet = new ExportSheet("Escala Voluntarios", List.of(
                "Escala", "Pessoa", "Serviço", "Status", "Observações"
        ));
        for (EscalaVoluntario item : listarEscalaVoluntarios(escopo)) {
            Escala escala = escopo.escalasPorId.get(item.getEscalaId());
            Servico servico = escopo.servicosPorId.get(item.getServicoId());
            sheet.adicionarLinha(List.of(
                    escala == null ? str(item.getEscalaId()) : str(escala.getTitulo()),
                    nomePessoa(escopo.pessoasPorId, item.getPessoaId()),
                    servico == null ? str(item.getServicoId()) : str(servico.getNome()),
                    str(item.getStatus()),
                    str(item.getObservacoes())
            ));
        }
        return sheet;
    }

    private ExportSheet sheetDisponibilidadeSemanal(Escopo escopo) {
        ExportSheet sheet = new ExportSheet("Disponibilidade Semanal", List.of(
                "Pessoa", "Dia", "Hora Início", "Hora Fim", "Disponível", "Observações"
        ));
        for (DisponibilidadeSemanal d : listarDisponibilidades(escopo)) {
            sheet.adicionarLinha(List.of(
                    nomePessoa(escopo.pessoasPorId, d.getPessoaId()),
                    str(d.getDiaSemana()),
                    str(d.getHoraInicio()),
                    str(d.getHoraFim()),
                    str(d.getDisponivel()),
                    str(d.getObservacoes())
            ));
        }
        return sheet;
    }

    private ExportSheet sheetIndisponibilidades(Escopo escopo) {
        ExportSheet sheet = new ExportSheet("Indisponibilidades", List.of(
                "Pessoa", "Data Início", "Data Fim", "Hora Início", "Hora Fim", "Motivo", "Observações"
        ));
        for (Indisponibilidade i : listarIndisponibilidades(escopo)) {
            sheet.adicionarLinha(List.of(
                    nomePessoa(escopo.pessoasPorId, i.getPessoaId()),
                    str(i.getDataInicio()),
                    str(i.getDataFim()),
                    str(i.getHoraInicio()),
                    str(i.getHoraFim()),
                    str(i.getMotivo()),
                    str(i.getObservacoes())
            ));
        }
        return sheet;
    }

    private ExportSheet sheetUsuarios(Escopo escopo) {
        ExportSheet sheet = new ExportSheet("Usuarios", List.of(
                "ID", "Username", "Email", "Pessoa ID", "Igreja ID", "Ativo", "Papéis"
        ));
        for (Usuario u : escopo.usuarios) {
            String papeis = u.nomesPapeisDaIgrejaAtual().stream()
                    .sorted()
                    .collect(Collectors.joining(", "));
            sheet.adicionarLinha(List.of(
                    str(u.getId()),
                    str(u.getUsername()),
                    str(u.getEmail()),
                    str(u.getPessoaId()),
                    str(u.getIgrejaId()),
                    str(u.getAtivo()),
                    papeis
            ));
        }
        return sheet;
    }

    private List<PessoaHistorico> listarHistoricos(Escopo escopo) {
        if (escopo.pessoaIds.isEmpty()) {
            return List.of();
        }
        return pessoaHistoricoRepository.findByPessoaIdIn(escopo.pessoaIds);
    }

    private List<JornadaMembro> listarJornadas(Escopo escopo) {
        if (escopo.pessoaIds.isEmpty()) {
            return List.of();
        }
        return jornadaMembroRepository.findByPessoaIdIn(escopo.pessoaIds);
    }

    private List<PessoaServico> listarVoluntarios(Escopo escopo) {
        if (escopo.pessoaIds.isEmpty()) {
            return List.of();
        }
        return pessoaServicoRepository.findByPessoaIdIn(escopo.pessoaIds);
    }

    private List<EscalaServico> listarEscalaServicos(Escopo escopo) {
        if (escopo.escalaIds.isEmpty()) {
            return List.of();
        }
        return escalaServicoRepository.findByEscalaIdIn(escopo.escalaIds);
    }

    private List<EscalaVoluntario> listarEscalaVoluntarios(Escopo escopo) {
        if (escopo.escalaIds.isEmpty()) {
            return List.of();
        }
        return escalaVoluntarioRepository.findByEscalaIdIn(escopo.escalaIds);
    }

    private List<DisponibilidadeSemanal> listarDisponibilidades(Escopo escopo) {
        if (escopo.pessoaIds.isEmpty()) {
            return List.of();
        }
        return disponibilidadeSemanalRepository.findByPessoaIdIn(escopo.pessoaIds);
    }

    private List<Indisponibilidade> listarIndisponibilidades(Escopo escopo) {
        if (escopo.pessoaIds.isEmpty()) {
            return List.of();
        }
        return indisponibilidadeRepository.findByPessoaIdIn(escopo.pessoaIds);
    }

    private String nomePessoa(Map<UUID, Pessoa> pessoas, UUID id) {
        if (id == null) {
            return "";
        }
        Pessoa pessoa = pessoas.get(id);
        return pessoa == null ? id.toString() : pessoa.getNome();
    }

    private String nomeMinisterio(Map<UUID, Ministerio> ministerios, UUID id) {
        if (id == null) {
            return "";
        }
        Ministerio ministerio = ministerios.get(id);
        return ministerio == null ? id.toString() : ministerio.getNome();
    }

    private String str(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof LocalDate date) {
            return date.toString();
        }
        if (value instanceof LocalTime time) {
            return time.toString();
        }
        if (value instanceof Enum<?> e) {
            return e.name();
        }
        return String.valueOf(value);
    }

    private static final class Escopo {
        private final List<Pessoa> pessoas;
        private final List<UUID> pessoaIds;
        private final List<Ministerio> ministerios;
        private final Map<UUID, Pessoa> pessoasPorId;
        private final Map<UUID, Ministerio> ministeriosPorId;
        private final List<Servico> servicos;
        private final Map<UUID, Servico> servicosPorId;
        private final List<Escala> escalas;
        private final List<UUID> escalaIds;
        private final Map<UUID, Escala> escalasPorId;
        private final List<Usuario> usuarios;

        private Escopo(
                List<Pessoa> pessoas,
                List<UUID> pessoaIds,
                List<Ministerio> ministerios,
                List<Servico> servicos,
                List<Escala> escalas,
                List<UUID> escalaIds,
                List<Usuario> usuarios
        ) {
            this.pessoas = pessoas;
            this.pessoaIds = pessoaIds;
            this.ministerios = ministerios;
            this.pessoasPorId = pessoas.stream().collect(Collectors.toMap(Pessoa::getId, Function.identity()));
            this.ministeriosPorId = ministerios.stream().collect(Collectors.toMap(Ministerio::getId, Function.identity()));
            this.servicos = servicos;
            this.servicosPorId = servicos.stream().collect(Collectors.toMap(Servico::getId, Function.identity()));
            this.escalas = escalas;
            this.escalaIds = escalaIds;
            this.escalasPorId = escalas.stream().collect(Collectors.toMap(Escala::getId, Function.identity()));
            this.usuarios = usuarios;
        }

        private static Escopo vazio() {
            return new Escopo(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        }
    }
}
