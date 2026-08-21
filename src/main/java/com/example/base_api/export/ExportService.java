package com.example.base_api.export;

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
import com.example.base_api.servico.entity.Servico;
import com.example.base_api.servico.repository.ServicoRepository;
import com.example.base_api.usuario.entity.Papel;
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
            UsuarioRepository usuarioRepository
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
    }

    @Transactional(readOnly = true)
    public ExportFile exportar(String recurso, FormatoExportacao formato) {
        ExportDocument document = switch (recurso.toLowerCase()) {
            case "pessoas" -> documentPessoas();
            case "historicos", "historico" -> documentHistoricos();
            case "jornadas" -> documentJornadas();
            case "ministerios" -> documentMinisterios();
            case "servicos" -> documentServicos();
            case "voluntarios" -> documentVoluntarios();
            case "escalas" -> documentEscalas();
            case "disponibilidades" -> documentDisponibilidades();
            case "usuarios" -> documentUsuarios();
            case "completo", "tudo" -> documentCompleto();
            default -> throw new BusinessException(
                    "Recurso inválido. Use: pessoas, historicos, jornadas, ministerios, servicos, voluntarios, escalas, disponibilidades, usuarios ou completo"
            );
        };
        return exportRenderer.renderizar(document, formato);
    }

    private ExportDocument documentPessoas() {
        ExportDocument doc = new ExportDocument("pessoas");
        doc.adicionarSheet(sheetPessoas());
        return doc;
    }

    private ExportDocument documentHistoricos() {
        ExportDocument doc = new ExportDocument("historicos");
        doc.adicionarSheet(sheetHistoricos());
        return doc;
    }

    private ExportDocument documentJornadas() {
        ExportDocument doc = new ExportDocument("jornadas");
        doc.adicionarSheet(sheetJornadas());
        return doc;
    }

    private ExportDocument documentMinisterios() {
        ExportDocument doc = new ExportDocument("ministerios");
        doc.adicionarSheet(sheetMinisterios());
        return doc;
    }

    private ExportDocument documentServicos() {
        ExportDocument doc = new ExportDocument("servicos");
        doc.adicionarSheet(sheetServicos());
        return doc;
    }

    private ExportDocument documentVoluntarios() {
        ExportDocument doc = new ExportDocument("voluntarios");
        doc.adicionarSheet(sheetVoluntarios());
        return doc;
    }

    private ExportDocument documentEscalas() {
        ExportDocument doc = new ExportDocument("escalas");
        doc.adicionarSheet(sheetEscalas());
        doc.adicionarSheet(sheetEscalaServicos());
        doc.adicionarSheet(sheetEscalaVoluntarios());
        return doc;
    }

    private ExportDocument documentDisponibilidades() {
        ExportDocument doc = new ExportDocument("disponibilidades");
        doc.adicionarSheet(sheetDisponibilidadeSemanal());
        doc.adicionarSheet(sheetIndisponibilidades());
        return doc;
    }

    private ExportDocument documentUsuarios() {
        ExportDocument doc = new ExportDocument("usuarios");
        doc.adicionarSheet(sheetUsuarios());
        return doc;
    }

    private ExportDocument documentCompleto() {
        ExportDocument doc = new ExportDocument("exportacao_completa");
        doc.adicionarSheet(sheetPessoas());
        doc.adicionarSheet(sheetHistoricos());
        doc.adicionarSheet(sheetJornadas());
        doc.adicionarSheet(sheetMinisterios());
        doc.adicionarSheet(sheetServicos());
        doc.adicionarSheet(sheetVoluntarios());
        doc.adicionarSheet(sheetEscalas());
        doc.adicionarSheet(sheetEscalaServicos());
        doc.adicionarSheet(sheetEscalaVoluntarios());
        doc.adicionarSheet(sheetDisponibilidadeSemanal());
        doc.adicionarSheet(sheetIndisponibilidades());
        doc.adicionarSheet(sheetUsuarios());
        return doc;
    }

    private ExportSheet sheetPessoas() {
        ExportSheet sheet = new ExportSheet("Pessoas", List.of(
                "ID", "Nome", "Nome Social", "Email", "Telefone",
                "Data Nascimento", "Data Cadastro", "Ativo", "Observações"
        ));
        for (Pessoa p : pessoaRepository.findAll()) {
            sheet.adicionarLinha(List.of(
                    str(p.getId()),
                    str(p.getNome()),
                    str(p.getNomeSocial()),
                    str(p.getEmail()),
                    str(p.getTelefone()),
                    str(p.getDataNascimento()),
                    str(p.getDataCadastro()),
                    str(p.getAtivo()),
                    str(p.getObservacoes())
            ));
        }
        return sheet;
    }

    private ExportSheet sheetHistoricos() {
        Map<UUID, Pessoa> pessoas = indexPessoas();
        ExportSheet sheet = new ExportSheet("Historicos", List.of(
                "Pessoa", "Descrição", "Autor", "Cargo", "Data"
        ));
        for (PessoaHistorico h : pessoaHistoricoRepository.findAll()) {
            sheet.adicionarLinha(List.of(
                    nomePessoa(pessoas, h.getPessoaId()),
                    str(h.getDescricao()),
                    str(h.getAutorNome()),
                    str(h.getAutorCargo()),
                    str(h.getDataRegistro())
            ));
        }
        return sheet;
    }

    private ExportSheet sheetJornadas() {
        Map<UUID, Pessoa> pessoas = indexPessoas();
        ExportSheet sheet = new ExportSheet("Jornadas", List.of(
                "Pessoa", "Aceitou Jesus", "Data Aceitou Jesus", "Batizado", "Data Batismo",
                "Trilha Base", "Data Trilha Base", "Membro", "Data Membro"
        ));
        for (JornadaMembro j : jornadaMembroRepository.findAll()) {
            sheet.adicionarLinha(List.of(
                    nomePessoa(pessoas, j.getPessoaId()),
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

    private ExportSheet sheetMinisterios() {
        ExportSheet sheet = new ExportSheet("Ministerios", List.of("ID", "Nome", "Descrição", "Ativo"));
        for (Ministerio m : ministerioRepository.findAll()) {
            sheet.adicionarLinha(List.of(
                    str(m.getId()),
                    str(m.getNome()),
                    str(m.getDescricao()),
                    str(m.getAtivo())
            ));
        }
        return sheet;
    }

    private ExportSheet sheetServicos() {
        Map<UUID, Ministerio> ministerios = indexMinisterios();
        ExportSheet sheet = new ExportSheet("Servicos", List.of(
                "ID", "Ministério", "Nome", "Descrição", "Ativo"
        ));
        for (Servico s : servicoRepository.findAll()) {
            sheet.adicionarLinha(List.of(
                    str(s.getId()),
                    nomeMinisterio(ministerios, s.getMinisterioId()),
                    str(s.getNome()),
                    str(s.getDescricao()),
                    str(s.getAtivo())
            ));
        }
        return sheet;
    }

    private ExportSheet sheetVoluntarios() {
        Map<UUID, Pessoa> pessoas = indexPessoas();
        Map<UUID, Servico> servicos = indexServicos();
        Map<UUID, Ministerio> ministerios = indexMinisterios();

        ExportSheet sheet = new ExportSheet("Voluntarios", List.of(
                "Pessoa", "Ministério", "Serviço", "Cargo", "Data Entrada", "Ativo", "Líder Responsável", "Observações"
        ));
        for (PessoaServico v : pessoaServicoRepository.findAll()) {
            Servico servico = servicos.get(v.getServicoId());
            String ministerioNome = servico == null
                    ? ""
                    : nomeMinisterio(ministerios, servico.getMinisterioId());
            sheet.adicionarLinha(List.of(
                    nomePessoa(pessoas, v.getPessoaId()),
                    ministerioNome,
                    servico == null ? "" : str(servico.getNome()),
                    str(v.getCargo()),
                    str(v.getDataEntrada()),
                    str(v.getAtivo()),
                    nomePessoa(pessoas, v.getLiderResponsavelId()),
                    str(v.getObservacoes())
            ));
        }
        return sheet;
    }

    private ExportSheet sheetEscalas() {
        ExportSheet sheet = new ExportSheet("Escalas", List.of(
                "ID", "Título", "Data", "Hora Início", "Hora Fim", "Local", "Ativo", "Descrição"
        ));
        for (Escala e : escalaRepository.findAll()) {
            sheet.adicionarLinha(List.of(
                    str(e.getId()),
                    str(e.getTitulo()),
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

    private ExportSheet sheetEscalaServicos() {
        Map<UUID, Escala> escalas = indexEscalas();
        Map<UUID, Servico> servicos = indexServicos();
        ExportSheet sheet = new ExportSheet("Escala Servicos", List.of(
                "Escala", "Serviço", "Quantidade Necessária"
        ));
        for (EscalaServico item : escalaServicoRepository.findAll()) {
            Escala escala = escalas.get(item.getEscalaId());
            Servico servico = servicos.get(item.getServicoId());
            sheet.adicionarLinha(List.of(
                    escala == null ? str(item.getEscalaId()) : str(escala.getTitulo()),
                    servico == null ? str(item.getServicoId()) : str(servico.getNome()),
                    str(item.getQuantidadeNecessaria())
            ));
        }
        return sheet;
    }

    private ExportSheet sheetEscalaVoluntarios() {
        Map<UUID, Escala> escalas = indexEscalas();
        Map<UUID, Pessoa> pessoas = indexPessoas();
        Map<UUID, Servico> servicos = indexServicos();
        ExportSheet sheet = new ExportSheet("Escala Voluntarios", List.of(
                "Escala", "Pessoa", "Serviço", "Status", "Observações"
        ));
        for (EscalaVoluntario item : escalaVoluntarioRepository.findAll()) {
            Escala escala = escalas.get(item.getEscalaId());
            Servico servico = servicos.get(item.getServicoId());
            sheet.adicionarLinha(List.of(
                    escala == null ? str(item.getEscalaId()) : str(escala.getTitulo()),
                    nomePessoa(pessoas, item.getPessoaId()),
                    servico == null ? str(item.getServicoId()) : str(servico.getNome()),
                    str(item.getStatus()),
                    str(item.getObservacoes())
            ));
        }
        return sheet;
    }

    private ExportSheet sheetDisponibilidadeSemanal() {
        Map<UUID, Pessoa> pessoas = indexPessoas();
        ExportSheet sheet = new ExportSheet("Disponibilidade Semanal", List.of(
                "Pessoa", "Dia", "Hora Início", "Hora Fim", "Disponível", "Observações"
        ));
        for (DisponibilidadeSemanal d : disponibilidadeSemanalRepository.findAll()) {
            sheet.adicionarLinha(List.of(
                    nomePessoa(pessoas, d.getPessoaId()),
                    str(d.getDiaSemana()),
                    str(d.getHoraInicio()),
                    str(d.getHoraFim()),
                    str(d.getDisponivel()),
                    str(d.getObservacoes())
            ));
        }
        return sheet;
    }

    private ExportSheet sheetIndisponibilidades() {
        Map<UUID, Pessoa> pessoas = indexPessoas();
        ExportSheet sheet = new ExportSheet("Indisponibilidades", List.of(
                "Pessoa", "Data Início", "Data Fim", "Hora Início", "Hora Fim", "Motivo", "Observações"
        ));
        for (Indisponibilidade i : indisponibilidadeRepository.findAll()) {
            sheet.adicionarLinha(List.of(
                    nomePessoa(pessoas, i.getPessoaId()),
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

    private ExportSheet sheetUsuarios() {
        ExportSheet sheet = new ExportSheet("Usuarios", List.of(
                "ID", "Username", "Email", "Pessoa ID", "Ativo", "Papéis"
        ));
        for (Usuario u : usuarioRepository.findAll()) {
            String papeis = u.getPapeis().stream()
                    .map(Papel::getNome)
                    .sorted()
                    .collect(Collectors.joining(", "));
            sheet.adicionarLinha(List.of(
                    str(u.getId()),
                    str(u.getUsername()),
                    str(u.getEmail()),
                    str(u.getPessoaId()),
                    str(u.getAtivo()),
                    papeis
            ));
        }
        return sheet;
    }

    private Map<UUID, Pessoa> indexPessoas() {
        return pessoaRepository.findAll().stream()
                .collect(Collectors.toMap(Pessoa::getId, Function.identity()));
    }

    private Map<UUID, Ministerio> indexMinisterios() {
        return ministerioRepository.findAll().stream()
                .collect(Collectors.toMap(Ministerio::getId, Function.identity()));
    }

    private Map<UUID, Servico> indexServicos() {
        return servicoRepository.findAll().stream()
                .collect(Collectors.toMap(Servico::getId, Function.identity()));
    }

    private Map<UUID, Escala> indexEscalas() {
        return escalaRepository.findAll().stream()
                .collect(Collectors.toMap(Escala::getId, Function.identity()));
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
}
