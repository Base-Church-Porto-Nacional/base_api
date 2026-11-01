package com.example.base_api.export.service;

import com.example.base_api.audit.service.AuditService;
import com.example.base_api.export.dto.ExportDocument;
import com.example.base_api.export.dto.ExportFile;
import com.example.base_api.export.dto.ExportSheet;
import com.example.base_api.export.dto.FormatoExportacao;
import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.culto.entity.CategoriaCulto;
import com.example.base_api.culto.entity.Culto;
import com.example.base_api.culto.entity.CultoRelatorio;
import com.example.base_api.culto.repository.CategoriaCultoRepository;
import com.example.base_api.culto.repository.CultoRelatorioRepository;
import com.example.base_api.culto.repository.CultoRepository;
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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
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
    private final CultoRepository cultoRepository;
    private final CultoRelatorioRepository cultoRelatorioRepository;
    private final CategoriaCultoRepository categoriaCultoRepository;
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
            CultoRepository cultoRepository,
            CultoRelatorioRepository cultoRelatorioRepository,
            CategoriaCultoRepository categoriaCultoRepository,
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
        this.cultoRepository = cultoRepository;
        this.cultoRelatorioRepository = cultoRelatorioRepository;
        this.categoriaCultoRepository = categoriaCultoRepository;
        this.autorizacaoService = autorizacaoService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public ExportFile exportar(String recurso, FormatoExportacao formato) {
        autorizacaoService.assertPastorOuAdmin(autorizacaoService.igrejaAtualId());
        Escopo escopo = carregarEscopo();
        ExportDocument document = switch (recurso.toLowerCase()) {
            case "pessoas" -> documento("pessoas", sheetPessoas(escopo));
            case "historicos", "historico" -> documento("historicos", sheetHistoricos(escopo));
            case "jornadas" -> documento("jornadas", sheetJornadas(escopo));
            case "ministerios" -> documento("ministerios", sheetMinisterios(escopo));
            case "servicos" -> documento("servicos", sheetServicos(escopo));
            case "voluntarios" -> documento("voluntarios", sheetVoluntarios(escopo));
            case "escalas" -> documento("escalas",
                    sheetEscalas(escopo), sheetEscalaServicos(escopo), sheetEscalaVoluntarios(escopo));
            case "disponibilidades" -> documento("disponibilidades",
                    sheetDisponibilidadeSemanal(escopo), sheetIndisponibilidades(escopo));
            case "usuarios" -> documento("usuarios", sheetUsuarios(escopo));
            case "cultos" -> documento("cultos", sheetCultos(escopo));
            case "completo", "tudo" -> documento("exportacao_completa",
                    sheetPessoas(escopo),
                    sheetHistoricos(escopo),
                    sheetJornadas(escopo),
                    sheetMinisterios(escopo),
                    sheetServicos(escopo),
                    sheetVoluntarios(escopo),
                    sheetEscalas(escopo),
                    sheetEscalaServicos(escopo),
                    sheetEscalaVoluntarios(escopo),
                    sheetDisponibilidadeSemanal(escopo),
                    sheetIndisponibilidades(escopo),
                    sheetUsuarios(escopo),
                    sheetCultos(escopo));
            default -> throw new BusinessException(
                    "Recurso inválido. Use: pessoas, historicos, jornadas, ministerios, servicos, voluntarios, escalas, disponibilidades, usuarios, cultos ou completo"
            );
        };
        ExportFile arquivo = exportRenderer.renderizar(document, formato);
        auditService.registrar("EXPORTAR", recurso.toLowerCase(), "formato=" + formato.name());
        return arquivo;
    }

    private Escopo carregarEscopo() {
        return new Escopo(autorizacaoService.idsParaListagem(null));
    }

    private static ExportDocument documento(String nome, ExportSheet... sheets) {
        ExportDocument doc = new ExportDocument(nome);
        for (ExportSheet sheet : sheets) {
            doc.adicionarSheet(sheet);
        }
        return doc;
    }

    private ExportSheet sheetPessoas(Escopo escopo) {
        ExportSheet sheet = new ExportSheet("Pessoas", List.of(
                "ID", "Nome", "Nome Social", "Email", "Telefone", "WhatsApp",
                "Data Nascimento", "Data Cadastro", "Igreja ID", "Ativo", "Observações"
        ));
        for (Pessoa p : escopo.pessoas()) {
            sheet.adicionarLinha(List.of(
                    str(p.getId()),
                    str(p.getNome()),
                    str(p.getNomeSocial()),
                    str(p.getEmail()),
                    str(p.getTelefone()),
                    str(p.getWhatsapp()),
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
                    nomePessoa(escopo.pessoasPorId(), h.getPessoaId()),
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
                    nomePessoa(escopo.pessoasPorId(), j.getPessoaId()),
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
        for (Ministerio m : escopo.ministerios()) {
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
        for (Servico s : escopo.servicos()) {
            sheet.adicionarLinha(List.of(
                    str(s.getId()),
                    nomeMinisterio(escopo.ministeriosPorId(), s.getMinisterioId()),
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
            Servico servico = escopo.servicosPorId().get(v.getServicoId());
            String ministerioNome = servico == null
                    ? ""
                    : nomeMinisterio(escopo.ministeriosPorId(), servico.getMinisterioId());
            sheet.adicionarLinha(List.of(
                    nomePessoa(escopo.pessoasPorId(), v.getPessoaId()),
                    ministerioNome,
                    servico == null ? "" : str(servico.getNome()),
                    str(v.getCargo()),
                    str(v.getDataEntrada()),
                    str(v.getAtivo()),
                    nomePessoa(escopo.pessoasPorId(), v.getLiderResponsavelId()),
                    str(v.getObservacoes())
            ));
        }
        return sheet;
    }

    private ExportSheet sheetEscalas(Escopo escopo) {
        ExportSheet sheet = new ExportSheet("Escalas", List.of(
                "ID", "Título", "Competência", "Tipo", "Igreja ID", "Data", "Hora Início", "Hora Fim", "Local", "Ativo", "Descrição"
        ));
        for (Escala e : escopo.escalas()) {
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
            Escala escala = escopo.escalasPorId().get(item.getEscalaId());
            Servico servico = escopo.servicosPorId().get(item.getServicoId());
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
            Escala escala = escopo.escalasPorId().get(item.getEscalaId());
            Servico servico = escopo.servicosPorId().get(item.getServicoId());
            sheet.adicionarLinha(List.of(
                    escala == null ? str(item.getEscalaId()) : str(escala.getTitulo()),
                    nomePessoa(escopo.pessoasPorId(), item.getPessoaId()),
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
                    nomePessoa(escopo.pessoasPorId(), d.getPessoaId()),
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
                    nomePessoa(escopo.pessoasPorId(), i.getPessoaId()),
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
        for (Usuario u : escopo.usuarios()) {
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

    private ExportSheet sheetCultos(Escopo escopo) {
        ExportSheet sheet = new ExportSheet("Cultos", List.of(
                "Data", "Hora Início", "Competência", "Categoria", "Título", "Status", "Pregador", "Igreja ID",
                "Adultos", "Crianças", "Total Presentes", "Conversões", "Reconciliações", "Alianças",
                "Carros", "Motos", "Bicicletas", "Total Veículos", "Observações"
        ));
        List<Culto> cultos = escopo.igrejaIds().isEmpty()
                ? List.of()
                : cultoRepository.filtrar(escopo.igrejaIds(), null, null, null, null, null, null);
        if (cultos.isEmpty()) {
            return sheet;
        }
        Map<UUID, CultoRelatorio> relatorios = cultoRelatorioRepository
                .findByCultoIdIn(cultos.stream().map(Culto::getId).toList()).stream()
                .collect(Collectors.toMap(CultoRelatorio::getCultoId, Function.identity()));
        Map<UUID, CategoriaCulto> categorias = categoriaCultoRepository
                .findAllById(cultos.stream().map(Culto::getCategoriaCultoId).distinct().toList()).stream()
                .collect(Collectors.toMap(CategoriaCulto::getId, Function.identity()));
        for (Culto c : cultos) {
            CategoriaCulto categoria = categorias.get(c.getCategoriaCultoId());
            CultoRelatorio r = relatorios.get(c.getId());
            sheet.adicionarLinha(List.of(
                    str(c.getData()),
                    str(c.getHoraInicio()),
                    str(c.getCompetencia()),
                    categoria == null ? "" : str(categoria.getTitulo()),
                    str(c.getTitulo()),
                    str(c.getStatus()),
                    nomePessoa(escopo.pessoasPorId(), c.getPregadorId()),
                    str(c.getIgrejaId()),
                    r == null ? "" : str(r.getAdultos()),
                    r == null ? "" : str(r.getCriancas()),
                    r == null ? "" : str(r.getAdultos() + r.getCriancas()),
                    r == null ? "" : str(r.getConversoes()),
                    r == null ? "" : str(r.getReconciliacoes()),
                    r == null ? "" : str(r.getAliancas()),
                    r == null ? "" : str(r.getCarros()),
                    r == null ? "" : str(r.getMotos()),
                    r == null ? "" : str(r.getBicicletas()),
                    r == null ? "" : str(r.getCarros() + r.getMotos() + r.getBicicletas()),
                    r == null ? "" : str(r.getObservacoes())
            ));
        }
        return sheet;
    }

    private List<PessoaHistorico> listarHistoricos(Escopo escopo) {
        return seHouver(escopo.pessoaIds(), pessoaHistoricoRepository::findByPessoaIdIn);
    }

    private List<JornadaMembro> listarJornadas(Escopo escopo) {
        return seHouver(escopo.pessoaIds(), jornadaMembroRepository::findByPessoaIdIn);
    }

    private List<PessoaServico> listarVoluntarios(Escopo escopo) {
        return seHouver(escopo.pessoaIds(), pessoaServicoRepository::findByPessoaIdIn);
    }

    private List<EscalaServico> listarEscalaServicos(Escopo escopo) {
        return seHouver(escopo.escalaIds(), escalaServicoRepository::findByEscalaIdIn);
    }

    private List<EscalaVoluntario> listarEscalaVoluntarios(Escopo escopo) {
        return seHouver(escopo.escalaIds(), escalaVoluntarioRepository::findByEscalaIdIn);
    }

    private List<DisponibilidadeSemanal> listarDisponibilidades(Escopo escopo) {
        return seHouver(escopo.pessoaIds(), disponibilidadeSemanalRepository::findByPessoaIdIn);
    }

    private List<Indisponibilidade> listarIndisponibilidades(Escopo escopo) {
        return seHouver(escopo.pessoaIds(), indisponibilidadeRepository::findByPessoaIdIn);
    }

    /** Executa a consulta só quando há ids; com lista vazia devolve vazio sem ir ao banco. */
    private static <T> List<T> seHouver(List<UUID> ids, Function<List<UUID>, List<T>> consulta) {
        return ids.isEmpty() ? List.of() : consulta.apply(ids);
    }

    private String nomePessoa(Map<UUID, Pessoa> pessoas, UUID id) {
        return nomeOuId(pessoas, id, Pessoa::getNome);
    }

    private String nomeMinisterio(Map<UUID, Ministerio> ministerios, UUID id) {
        return nomeOuId(ministerios, id, Ministerio::getNome);
    }

    /** Nome da entidade com o id; vazio se o id é nulo e o próprio id se a entidade não está no mapa. */
    private static <T> String nomeOuId(Map<UUID, T> entidades, UUID id, Function<T, String> nome) {
        if (id == null) {
            return "";
        }
        T entidade = entidades.get(id);
        return entidade == null ? id.toString() : nome.apply(entidade);
    }

    private String str(Object value) {
        return Objects.toString(value, "");
    }

    /**
     * Dados da rede visível carregados sob demanda: cada parte é consultada uma única vez e só se
     * alguma planilha do recurso exportado precisar dela.
     */
    private final class Escopo {
        private final List<UUID> igrejaIds;
        private final Supplier<List<Pessoa>> pessoas = lazy(() -> igrejaIds().isEmpty()
                ? List.of()
                : pessoaRepository.findByIgrejaIdInOrderByNomeAsc(igrejaIds()));
        private final Supplier<Map<UUID, Pessoa>> pessoasPorId = lazy(() -> porId(pessoas(), Pessoa::getId));
        private final Supplier<List<Ministerio>> ministerios = lazy(() -> igrejaIds().isEmpty()
                ? List.of()
                : ministerioRepository.findByIgrejaIdInOrderByNomeAsc(igrejaIds()));
        private final Supplier<Map<UUID, Ministerio>> ministeriosPorId =
                lazy(() -> porId(ministerios(), Ministerio::getId));
        private final Supplier<List<Servico>> servicos = lazy(() -> seHouver(
                ministerios().stream().map(Ministerio::getId).toList(), servicoRepository::findByMinisterioIdIn));
        private final Supplier<Map<UUID, Servico>> servicosPorId = lazy(() -> porId(servicos(), Servico::getId));
        private final Supplier<List<Escala>> escalas = lazy(() -> igrejaIds().isEmpty()
                ? List.of()
                : escalaRepository.findByIgrejaIdInOrderByCompetenciaDescTituloAsc(igrejaIds()));
        private final Supplier<Map<UUID, Escala>> escalasPorId = lazy(() -> porId(escalas(), Escala::getId));
        private final Supplier<List<Usuario>> usuarios = lazy(() -> igrejaIds().isEmpty()
                ? List.of()
                : usuarioRepository.findByIgrejaIdIn(igrejaIds()));

        private Escopo(List<UUID> igrejaIds) {
            this.igrejaIds = igrejaIds;
        }

        List<UUID> igrejaIds() {
            return igrejaIds;
        }

        List<Pessoa> pessoas() {
            return pessoas.get();
        }

        List<UUID> pessoaIds() {
            return pessoas().stream().map(Pessoa::getId).toList();
        }

        Map<UUID, Pessoa> pessoasPorId() {
            return pessoasPorId.get();
        }

        List<Ministerio> ministerios() {
            return ministerios.get();
        }

        Map<UUID, Ministerio> ministeriosPorId() {
            return ministeriosPorId.get();
        }

        List<Servico> servicos() {
            return servicos.get();
        }

        Map<UUID, Servico> servicosPorId() {
            return servicosPorId.get();
        }

        List<Escala> escalas() {
            return escalas.get();
        }

        List<UUID> escalaIds() {
            return escalas().stream().map(Escala::getId).toList();
        }

        Map<UUID, Escala> escalasPorId() {
            return escalasPorId.get();
        }

        List<Usuario> usuarios() {
            return usuarios.get();
        }
    }

    private static <T> Map<UUID, T> porId(List<T> itens, Function<T, UUID> id) {
        return itens.stream().collect(Collectors.toMap(id, Function.identity()));
    }

    /** Supplier que calcula o valor na primeira chamada e o reaproveita nas seguintes. */
    private static <T> Supplier<T> lazy(Supplier<T> fonte) {
        return new Supplier<>() {
            private T valor;
            private boolean carregado;

            @Override
            public T get() {
                if (!carregado) {
                    valor = fonte.get();
                    carregado = true;
                }
                return valor;
            }
        };
    }
}
