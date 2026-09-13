package com.example.base_api.config;

import com.example.base_api.baseministerial.entity.BaseMinisterial;
import com.example.base_api.baseministerial.repository.BaseMinisterialRepository;
import com.example.base_api.cronograma.entity.CronogramaConteudo;
import com.example.base_api.cronograma.entity.CronogramaEvento;
import com.example.base_api.cronograma.entity.TipoEvento;
import com.example.base_api.cronograma.repository.CronogramaConteudoRepository;
import com.example.base_api.cronograma.repository.CronogramaEventoRepository;
import com.example.base_api.culto.entity.CategoriaCulto;
import com.example.base_api.culto.entity.Culto;
import com.example.base_api.culto.entity.CultoRelatorio;
import com.example.base_api.culto.entity.StatusCulto;
import com.example.base_api.culto.repository.CategoriaCultoRepository;
import com.example.base_api.culto.repository.CultoRelatorioRepository;
import com.example.base_api.culto.repository.CultoRepository;
import com.example.base_api.escala.entity.Escala;
import com.example.base_api.escala.entity.EscalaVoluntario;
import com.example.base_api.escala.entity.StatusEscalaVoluntario;
import com.example.base_api.escala.entity.TipoEscala;
import com.example.base_api.escala.repository.EscalaRepository;
import com.example.base_api.escala.repository.EscalaVoluntarioRepository;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.entity.IgrejaTipo;
import com.example.base_api.igreja.repository.IgrejaRepository;
import com.example.base_api.jornada.service.JornadaService;
import com.example.base_api.ministerio.entity.Ministerio;
import com.example.base_api.ministerio.entity.MinisterioTipo;
import com.example.base_api.ministerio.repository.MinisterioRepository;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.servico.entity.Servico;
import com.example.base_api.servico.repository.ServicoRepository;
import com.example.base_api.smallgroup.entity.DiaSemana;
import com.example.base_api.smallgroup.entity.SmallGroup;
import com.example.base_api.smallgroup.entity.SmallGroupEncontro;
import com.example.base_api.smallgroup.entity.SmallGroupParticipante;
import com.example.base_api.smallgroup.repository.SmallGroupEncontroRepository;
import com.example.base_api.smallgroup.repository.SmallGroupParticipanteRepository;
import com.example.base_api.smallgroup.repository.SmallGroupRepository;
import com.example.base_api.usuario.entity.Papel;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.entity.UsuarioPapel;
import com.example.base_api.usuario.repository.PapelRepository;
import com.example.base_api.usuario.repository.UsuarioPapelRepository;
import com.example.base_api.usuario.repository.UsuarioRepository;
import com.example.base_api.voluntario.entity.PessoaServico;
import com.example.base_api.voluntario.repository.PessoaServicoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Set;
import java.util.UUID;

@Configuration
@Profile("dev")
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    static final UUID MATRIZ_ID = UUID.fromString("c1000000-0000-4000-8000-000000000001");

    @Bean
    CommandLineRunner seedDemoData(
            IgrejaRepository igrejaRepository,
            PessoaRepository pessoaRepository,
            UsuarioRepository usuarioRepository,
            UsuarioPapelRepository usuarioPapelRepository,
            PapelRepository papelRepository,
            MinisterioRepository ministerioRepository,
            BaseMinisterialRepository baseMinisterialRepository,
            ServicoRepository servicoRepository,
            PessoaServicoRepository pessoaServicoRepository,
            CronogramaEventoRepository cronogramaEventoRepository,
            CronogramaConteudoRepository cronogramaConteudoRepository,
            EscalaRepository escalaRepository,
            EscalaVoluntarioRepository escalaVoluntarioRepository,
            CategoriaCultoRepository categoriaCultoRepository,
            CultoRepository cultoRepository,
            CultoRelatorioRepository cultoRelatorioRepository,
            SmallGroupRepository smallGroupRepository,
            SmallGroupEncontroRepository smallGroupEncontroRepository,
            SmallGroupParticipanteRepository smallGroupParticipanteRepository,
            JornadaService jornadaService,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            Igreja matriz = igrejaRepository.findById(MATRIZ_ID).orElseThrow(
                    () -> new IllegalStateException("Igreja matriz não encontrada. Verifique a migration V5.")
            );

            Papel adminRole = papel(papelRepository, "ADMIN");
            Papel pastorRole = papel(papelRepository, "PASTOR");
            Papel liderRole = papel(papelRepository, "LIDER");
            Papel voluntarioRole = papel(papelRepository, "VOLUNTARIO");
            Papel membroRole = papel(papelRepository, "MEMBRO");

            Pessoa ana = pessoa(pessoaRepository, jornadaService, "Ana Costa", "ana.costa@base.church", "(11) 98888-1001", MATRIZ_ID, LocalDate.of(2022, 3, 12));
            Pessoa joao = pessoa(pessoaRepository, jornadaService, "João da Silva", "joao.silva@email.com", "(11) 97777-2002", MATRIZ_ID, LocalDate.of(2023, 1, 20));
            Pessoa maria = pessoa(pessoaRepository, jornadaService, "Maria Oliveira", "maria.oliveira@email.com", "(11) 96666-3003", MATRIZ_ID, LocalDate.of(2023, 5, 8));
            Pessoa bruno = pessoa(pessoaRepository, jornadaService, "Bruno Santos", "bruno.santos@email.com", "(11) 95555-4004", MATRIZ_ID, LocalDate.of(2024, 2, 14));
            Pessoa fernanda = pessoa(pessoaRepository, jornadaService, "Fernanda Dias", "fernanda.dias@email.com", "(11) 94444-5005", MATRIZ_ID, LocalDate.of(2024, 6, 1));
            Pessoa carla = pessoa(pessoaRepository, jornadaService, "Carla Mendes", "carla.mendes@email.com", "(11) 93333-6006", MATRIZ_ID, LocalDate.of(2026, 8, 10));
            Pessoa carlos = pessoa(pessoaRepository, jornadaService, "Carlos Lima", "carlos.lima@email.com", "(11) 92222-7007", MATRIZ_ID, LocalDate.of(2021, 11, 3));
            Pessoa pedro = pessoa(pessoaRepository, jornadaService, "Pedro Alves", "pedro.alves@base.church", "(11) 91111-8008", MATRIZ_ID, LocalDate.of(2020, 8, 1));
            Pessoa luciana = pessoa(pessoaRepository, jornadaService, "Luciana Rocha", "luciana.rocha@base.church", "(11) 90000-9009", MATRIZ_ID, LocalDate.of(2025, 1, 15));
            Pessoa rafael = pessoa(pessoaRepository, jornadaService, "Rafael Souza", "rafael.souza@email.com", "(11) 98877-1010", MATRIZ_ID, LocalDate.of(2025, 4, 10));

            Igreja filial = igrejaRepository.findAll().stream()
                    .filter(i -> i.getTipo() == IgrejaTipo.FILIAL)
                    .findFirst()
                    .orElseGet(() -> {
                        Igreja nova = new Igreja();
                        nova.setNome("Base Church — Zona Sul");
                        nova.setTipo(IgrejaTipo.FILIAL);
                        nova.setIgrejaMatrizId(MATRIZ_ID);
                        nova.setPastorPessoaId(pedro.getId());
                        nova.setAtivo(true);
                        return igrejaRepository.save(nova);
                    });

            pedro.setIgrejaId(filial.getId());
            luciana.setIgrejaId(filial.getId());
            rafael.setIgrejaId(filial.getId());
            pessoaRepository.save(pedro);
            pessoaRepository.save(luciana);
            pessoaRepository.save(rafael);

            String senha = passwordEncoder.encode("admin123");

            Usuario admin = usuario(
                    usuarioRepository, usuarioPapelRepository, "admin", "ana.costa@base.church", senha,
                    ana.getId(), MATRIZ_ID, Set.of(adminRole, pastorRole)
            );
            Usuario uFilial = usuario(
                    usuarioRepository, usuarioPapelRepository, "filial", "luciana.rocha@base.church", senha,
                    luciana.getId(), filial.getId(), Set.of(adminRole)
            );
            usuario(usuarioRepository, usuarioPapelRepository, "joao", "joao.silva@email.com", senha, joao.getId(), MATRIZ_ID, Set.of(liderRole, voluntarioRole, membroRole));
            usuario(usuarioRepository, usuarioPapelRepository, "maria", "maria.oliveira@email.com", senha, maria.getId(), MATRIZ_ID, Set.of(voluntarioRole, membroRole));
            usuario(usuarioRepository, usuarioPapelRepository, "midia", "carlos.lima@email.com", senha, carlos.getId(), MATRIZ_ID, Set.of(liderRole, voluntarioRole, membroRole));
            usuario(usuarioRepository, usuarioPapelRepository, "membro", "fernanda.dias@email.com", senha, fernanda.getId(), MATRIZ_ID, Set.of(voluntarioRole, membroRole));
            usuario(usuarioRepository, usuarioPapelRepository, "pedro", "pedro.alves@base.church", senha, pedro.getId(), filial.getId(), Set.of(pastorRole));

            matriz.setNome("Base Church");
            matriz.setPastorPessoaId(ana.getId());
            matriz.setAdminUsuarioId(admin.getId());
            igrejaRepository.save(matriz);

            filial.setPastorPessoaId(pedro.getId());
            filial.setAdminUsuarioId(uFilial.getId());
            igrejaRepository.save(filial);

            Ministerio louvor = ministerio(ministerioRepository, servicoRepository, pessoaServicoRepository,
                    "Louvor", MATRIZ_ID, MinisterioTipo.MUSICA, joao.getId(), maria.getId(), "Ministério de música e louvor.",
                    "Ensaio às quintas, 20h.", "Abertura · Ofertório · Encerramento", "Vocal", "Violão");
            Ministerio midiaMin = ministerio(ministerioRepository, servicoRepository, pessoaServicoRepository,
                    "Mídia", MATRIZ_ID, MinisterioTipo.MIDIA, carlos.getId(), bruno.getId(), "Projeção, som e transmissões.",
                    null, null, "Projeção", "Som");
            Ministerio infantil = ministerio(ministerioRepository, servicoRepository, pessoaServicoRepository,
                    "Infantil", MATRIZ_ID, MinisterioTipo.GERAL, maria.getId(), fernanda.getId(), null,
                    null, null, "Professor");
            Ministerio recepcao = ministerio(ministerioRepository, servicoRepository, pessoaServicoRepository,
                    "Recepção", MATRIZ_ID, MinisterioTipo.GERAL, ana.getId(), ana.getId(), null,
                    null, null, "Recepção");
            Ministerio louvorZonaSul = ministerio(ministerioRepository, servicoRepository, pessoaServicoRepository,
                    "Louvor Zona Sul", filial.getId(), MinisterioTipo.MUSICA, rafael.getId(), rafael.getId(), null,
                    null, null, "Vocal");

            BaseMinisterial adoracao = baseMinisterial(baseMinisterialRepository, "Adoração",
                    "Louvor, mídia e ambientação dos cultos.", MATRIZ_ID, joao.getId(), maria.getId());
            BaseMinisterial servico = baseMinisterial(baseMinisterialRepository, "Serviço",
                    "Recepção, estacionamento e suporte à igreja.", MATRIZ_ID, ana.getId(), fernanda.getId());
            baseMinisterial(baseMinisterialRepository, "Missão",
                    "Evangelismo e ação missionária.", MATRIZ_ID, carlos.getId(), bruno.getId());
            baseMinisterial(baseMinisterialRepository, "Conectados",
                    "Acolhimento, redes e integração de novos membros.", MATRIZ_ID, null, null);
            BaseMinisterial discipulado = baseMinisterial(baseMinisterialRepository, "Discipulado",
                    "Ensino, células e formação de líderes.", MATRIZ_ID, pedro.getId(), rafael.getId());

            vincularBase(ministerioRepository, louvor, adoracao.getId());
            vincularBase(ministerioRepository, midiaMin, adoracao.getId());
            vincularBase(ministerioRepository, infantil, discipulado.getId());
            vincularBase(ministerioRepository, recepcao, servico.getId());
            vincularBase(ministerioRepository, louvorZonaSul, adoracao.getId());

            Ministerio pequenosGrupos = ministerio(ministerioRepository, servicoRepository, pessoaServicoRepository,
                    "Pequenos grupos", MATRIZ_ID, MinisterioTipo.GERAL, bruno.getId(), fernanda.getId(),
                    "Pequenos grupos reunidos em casas pela semana.", null, null);
            vincularBase(ministerioRepository, pequenosGrupos, discipulado.getId());

            SmallGroup casaJoao = smallGroup(smallGroupRepository, "Casa do João", MATRIZ_ID,
                    DiaSemana.QUARTA, "19:30", joao.getId(), pequenosGrupos.getId(), 4);
            SmallGroup mulheresFe = smallGroup(smallGroupRepository, "Mulheres de Fé", MATRIZ_ID,
                    DiaSemana.TERCA, "20:00", fernanda.getId(), pequenosGrupos.getId(), 5);
            SmallGroup discipulos = smallGroup(smallGroupRepository, "Discípulos", MATRIZ_ID,
                    DiaSemana.SEXTA, "19:00", bruno.getId(), pequenosGrupos.getId(), 3);

            if (smallGroupEncontroRepository.findFirstBySmallGroupIdAndData(
                    casaJoao.getId(), LocalDate.of(2026, 8, 5)).isEmpty()) {
                SmallGroupEncontro e1 = encontro(smallGroupEncontroRepository, casaJoao.getId(),
                        LocalDate.of(2026, 8, 5), joao.getId(), "Estudo de João 15.");
                participante(smallGroupParticipanteRepository, e1.getId(), joao.getId(), joao.getNome());
                participante(smallGroupParticipanteRepository, e1.getId(), carla.getId(), carla.getNome());
                participante(smallGroupParticipanteRepository, e1.getId(), maria.getId(), maria.getNome());

                SmallGroupEncontro e2 = encontro(smallGroupEncontroRepository, mulheresFe.getId(),
                        LocalDate.of(2026, 8, 4), fernanda.getId(), "Encontro de oração.");
                participante(smallGroupParticipanteRepository, e2.getId(), fernanda.getId(), fernanda.getNome());
                participante(smallGroupParticipanteRepository, e2.getId(), ana.getId(), ana.getNome());
                participante(smallGroupParticipanteRepository, e2.getId(), carla.getId(), carla.getNome());

                SmallGroupEncontro e3 = encontro(smallGroupEncontroRepository, discipulos.getId(),
                        LocalDate.of(2026, 8, 7), bruno.getId(), "Revisão do curso de discipulado.");
                participante(smallGroupParticipanteRepository, e3.getId(), bruno.getId(), bruno.getNome());
                participante(smallGroupParticipanteRepository, e3.getId(), carlos.getId(), carlos.getNome());
                participante(smallGroupParticipanteRepository, e3.getId(), joao.getId(), joao.getNome());
            }

            if (cronogramaEventoRepository.count() == 0) {
                CronogramaEvento c1 = culto(cronogramaEventoRepository, "Culto de domingo", LocalDate.of(2026, 8, 2), MATRIZ_ID, ana.getId(), "Templo principal");
                CronogramaEvento c2 = culto(cronogramaEventoRepository, "Culto de domingo", LocalDate.of(2026, 8, 9), MATRIZ_ID, ana.getId(), "Templo principal");
                CronogramaEvento c3 = culto(cronogramaEventoRepository, "Culto de domingo", LocalDate.of(2026, 8, 16), MATRIZ_ID, carlos.getId(), "Templo principal");
                CronogramaEvento c4 = culto(cronogramaEventoRepository, "Culto de domingo", LocalDate.of(2026, 8, 23), MATRIZ_ID, ana.getId(), "Templo principal");
                culto(cronogramaEventoRepository, "Encontro de jovens", LocalDate.of(2026, 8, 15), MATRIZ_ID, ana.getId(), "Salão");
                culto(cronogramaEventoRepository, "Culto de domingo", LocalDate.of(2026, 8, 2), filial.getId(), pedro.getId(), "Zona Sul");

                CronogramaConteudo conteudo2 = new CronogramaConteudo();
                conteudo2.setEventoId(c2.getId());
                conteudo2.setTextoPregacao("João 15 — Permanecei em mim.\n\n1. A videira verdadeira\n2. O fruto que permanece\n3. Amar uns aos outros");
                conteudo2.setLouvor("1. Bondade de Deus\n2. Teu Santo Nome\n3. Oceanos");
                conteudo2.setPregadorPessoaId(ana.getId());
                cronogramaConteudoRepository.save(conteudo2);

                CronogramaConteudo conteudo3 = new CronogramaConteudo();
                conteudo3.setEventoId(c3.getId());
                conteudo3.setTextoPregacao("Salmo 23 — O Senhor é o meu pastor.");
                conteudo3.setLouvor("1. Ao único\n2. Porque Ele vive");
                conteudo3.setPregadorPessoaId(ana.getId());
                cronogramaConteudoRepository.save(conteudo3);

                Servico vocal = servicoRepository.findByMinisterioIdAndNomeIgnoreCase(louvor.getId(), "Vocal").orElse(null);
                Servico violao = servicoRepository.findByMinisterioIdAndNomeIgnoreCase(louvor.getId(), "Violão").orElse(null);
                Servico projecao = servicoRepository.findByMinisterioIdAndNomeIgnoreCase(midiaMin.getId(), "Projeção").orElse(null);

                Escala sup = escala(escalaRepository, "Supervisores — agosto", "2026-08", TipoEscala.SUPERVISOR, MATRIZ_ID, null, ana.getId(), ana.getId());
                item(escalaVoluntarioRepository, sup, LocalDate.of(2026, 8, 2), c1.getId(), ana.getId(), null, StatusEscalaVoluntario.CONFIRMADO);
                item(escalaVoluntarioRepository, sup, LocalDate.of(2026, 8, 9), c2.getId(), ana.getId(), null, StatusEscalaVoluntario.CONFIRMADO);
                item(escalaVoluntarioRepository, sup, LocalDate.of(2026, 8, 16), c3.getId(), carlos.getId(), null, StatusEscalaVoluntario.PENDENTE);
                item(escalaVoluntarioRepository, sup, LocalDate.of(2026, 8, 23), c4.getId(), ana.getId(), null, StatusEscalaVoluntario.PENDENTE);

                Escala preg = escala(escalaRepository, "Pregadores — agosto", "2026-08", TipoEscala.PREGADOR, MATRIZ_ID, null, ana.getId(), ana.getId());
                item(escalaVoluntarioRepository, preg, LocalDate.of(2026, 8, 2), c1.getId(), ana.getId(), null, StatusEscalaVoluntario.CONFIRMADO);
                item(escalaVoluntarioRepository, preg, LocalDate.of(2026, 8, 9), c2.getId(), ana.getId(), null, StatusEscalaVoluntario.CONFIRMADO);
                item(escalaVoluntarioRepository, preg, LocalDate.of(2026, 8, 16), c3.getId(), ana.getId(), null, StatusEscalaVoluntario.PENDENTE);
                item(escalaVoluntarioRepository, preg, LocalDate.of(2026, 8, 23), c4.getId(), pedro.getId(), null, StatusEscalaVoluntario.PENDENTE);

                Escala cultoEscala = escala(escalaRepository, "Escala de culto — agosto", "2026-08", TipoEscala.CULTO, MATRIZ_ID, null, ana.getId(), ana.getId());
                item(escalaVoluntarioRepository, cultoEscala, LocalDate.of(2026, 8, 9), c2.getId(), joao.getId(), vocal == null ? null : vocal.getId(), StatusEscalaVoluntario.CONFIRMADO);
                item(escalaVoluntarioRepository, cultoEscala, LocalDate.of(2026, 8, 9), c2.getId(), carlos.getId(), projecao == null ? null : projecao.getId(), StatusEscalaVoluntario.PENDENTE);

                Escala louvorEscala = escala(escalaRepository, "Louvor — agosto", "2026-08", TipoEscala.MINISTERIO, MATRIZ_ID, louvor.getId(), ana.getId(), ana.getId());
                item(escalaVoluntarioRepository, louvorEscala, LocalDate.of(2026, 8, 9), c2.getId(), joao.getId(), vocal == null ? null : vocal.getId(), StatusEscalaVoluntario.CONFIRMADO);
                item(escalaVoluntarioRepository, louvorEscala, LocalDate.of(2026, 8, 9), c2.getId(), maria.getId(), violao == null ? null : violao.getId(), StatusEscalaVoluntario.PENDENTE);
                item(escalaVoluntarioRepository, louvorEscala, LocalDate.of(2026, 8, 16), c3.getId(), joao.getId(), vocal == null ? null : vocal.getId(), StatusEscalaVoluntario.PENDENTE);
            }

            if (categoriaCultoRepository.count() == 0) {
                CategoriaCulto domingo = categoriaCulto(categoriaCultoRepository, "Domingo inesquecível",
                        com.example.base_api.culto.entity.DiaSemana.DOMINGO, LocalTime.of(20, 0), 90, MATRIZ_ID, ana.getId());
                CategoriaCulto quarta = categoriaCulto(categoriaCultoRepository, "Quarta de oração",
                        com.example.base_api.culto.entity.DiaSemana.QUARTA, LocalTime.of(20, 0), 60, MATRIZ_ID, ana.getId());
                gerarCultosDoMes(cultoRepository, domingo, YearMonth.of(2026, 8));
                gerarCultosDoMes(cultoRepository, quarta, YearMonth.of(2026, 8));

                Culto cultasso = cultoRepository
                        .findByCategoriaCultoIdAndData(domingo.getId(), LocalDate.of(2026, 8, 16))
                        .orElseThrow(() -> new IllegalStateException("Culto seed não gerado"));
                cultasso.setTitulo("Cultasso");
                cultasso.setTema("Destravados: Conforto");
                cultasso.setLouvor("1. Bondade de Deus\n2. Teu Santo Nome\n3. Ao único");
                cultasso.setTextoPregacao("Isaías 61 — Conforto aos destravados.");
                cultasso.setStatus(StatusCulto.REALIZADO);
                cultoRepository.save(cultasso);

                CultoRelatorio relatorio = new CultoRelatorio();
                relatorio.setCultoId(cultasso.getId());
                relatorio.setAdultos(19);
                relatorio.setCriancas(2);
                relatorio.setConversoes(2);
                relatorio.setReconciliacoes(1);
                relatorio.setAliancas(3);
                relatorio.setCarros(7);
                relatorio.setMotos(4);
                relatorio.setBicicletas(0);
                relatorio.setObservacoes("Culto de celebração com batismos.");
                cultoRelatorioRepository.save(relatorio);
            }

            log.info("Dados demo prontos. Logins: admin, filial, joao, maria, midia, membro, pedro — senha admin123");
        };
    }

    private Papel papel(PapelRepository repo, String nome) {
        return repo.findByNomeAndIgrejaIdIsNull(nome)
                .orElseThrow(() -> new IllegalStateException("Papel " + nome + " não encontrado"));
    }

    private Pessoa pessoa(
            PessoaRepository repo,
            JornadaService jornadaService,
            String nome,
            String email,
            String telefone,
            UUID igrejaId,
            LocalDate cadastro
    ) {
        return repo.findByEmailIgnoreCase(email).orElseGet(() -> {
            Pessoa p = new Pessoa();
            p.setNome(nome);
            p.setEmail(email);
            p.setTelefone(telefone);
            p.setIgrejaId(igrejaId);
            p.setDataCadastro(cadastro);
            p.setAtivo(true);
            Pessoa salva = repo.save(p);
            jornadaService.criarJornadaInicial(salva.getId());
            return salva;
        });
    }

    private Usuario usuario(
            UsuarioRepository repo,
            UsuarioPapelRepository papelRepo,
            String username,
            String email,
            String senhaHash,
            UUID pessoaId,
            UUID igrejaId,
            Set<Papel> papeis
    ) {
        Usuario salvo = repo.findByUsername(username).map(existente -> {
            existente.setPessoaId(pessoaId);
            existente.setIgrejaId(igrejaId);
            existente.setEmail(email);
            existente.setAtivo(true);
            return repo.save(existente);
        }).orElseGet(() -> {
            Usuario u = new Usuario();
            u.setUsername(username);
            u.setEmail(email);
            u.setSenhaHash(senhaHash);
            u.setPessoaId(pessoaId);
            u.setIgrejaId(igrejaId);
            u.setAtivo(true);
            return repo.save(u);
        });
        if (papelRepo.findByUsuarioId(salvo.getId()).isEmpty()) {
            for (Papel papel : papeis) {
                UsuarioPapel vinculo = new UsuarioPapel();
                vinculo.setUsuarioId(salvo.getId());
                vinculo.setPapelId(papel.getId());
                vinculo.setIgrejaId(igrejaId);
                vinculo.setPapel(papel);
                papelRepo.save(vinculo);
            }
        }
        return salvo;
    }

    private Ministerio ministerio(
            MinisterioRepository ministerioRepository,
            ServicoRepository servicoRepository,
            PessoaServicoRepository pessoaServicoRepository,
            String nome,
            UUID igrejaId,
            MinisterioTipo tipo,
            UUID liderId,
            UUID coordenadorId,
            String descricao,
            String anotacoes,
            String ordem,
            String... servicosReais
    ) {
        return ministerioRepository.findByIgrejaIdAndNomeIgnoreCase(igrejaId, nome).orElseGet(() -> {
            Ministerio m = new Ministerio();
            m.setNome(nome);
            m.setIgrejaId(igrejaId);
            m.setTipo(tipo);
            m.setLiderPessoaId(liderId);
            m.setCoordenadorPessoaId(coordenadorId);
            m.setDescricao(descricao);
            m.setAnotacoes(anotacoes);
            m.setOrdem(ordem);
            m.setAtivo(true);
            Ministerio salvo = ministerioRepository.save(m);

            Servico coordenador = new Servico();
            coordenador.setMinisterioId(salvo.getId());
            coordenador.setNome(Ministerio.SERVICO_COORDENADOR);
            coordenador.setFiccao(true);
            coordenador.setAtivo(true);
            servicoRepository.save(coordenador);

            if (coordenadorId != null) {
                PessoaServico vinculo = new PessoaServico();
                vinculo.setPessoaId(coordenadorId);
                vinculo.setServicoId(coordenador.getId());
                vinculo.setAtivo(true);
                pessoaServicoRepository.save(vinculo);
            }

            for (String servicoNome : servicosReais) {
                Servico s = new Servico();
                s.setMinisterioId(salvo.getId());
                s.setNome(servicoNome);
                s.setFiccao(false);
                s.setAtivo(true);
                servicoRepository.save(s);
            }
            return salvo;
        });
    }

    private BaseMinisterial baseMinisterial(
            BaseMinisterialRepository repo,
            String nome,
            String descricao,
            UUID igrejaId,
            UUID liderId,
            UUID coordenadorId
    ) {
        return repo.findByIgrejaIdAndNomeIgnoreCase(igrejaId, nome).orElseGet(() -> {
            BaseMinisterial b = new BaseMinisterial();
            b.setNome(nome);
            b.setDescricao(descricao);
            b.setIgrejaId(igrejaId);
            b.setLiderPessoaId(liderId);
            b.setCoordenadorPessoaId(coordenadorId);
            b.setAtivo(true);
            return repo.save(b);
        });
    }

    private void vincularBase(MinisterioRepository repo, Ministerio ministerio, UUID baseMinisterialId) {
        if (ministerio.getBaseMinisterialId() != null) {
            return;
        }
        ministerio.setBaseMinisterialId(baseMinisterialId);
        repo.save(ministerio);
    }

    private SmallGroup smallGroup(
            SmallGroupRepository repo,
            String nome,
            UUID igrejaId,
            DiaSemana diaSemana,
            String horario,
            UUID liderId,
            UUID ministerioId,
            int nota
    ) {
        return repo.findByIgrejaIdAndNomeIgnoreCase(igrejaId, nome).orElseGet(() -> {
            SmallGroup g = new SmallGroup();
            g.setNome(nome);
            g.setIgrejaId(igrejaId);
            g.setDiaSemana(diaSemana);
            g.setHorario(horario);
            g.setLiderPessoaId(liderId);
            g.setMinisterioId(ministerioId);
            g.setAtivo(true);
            g.setNota(nota);
            return repo.save(g);
        });
    }

    private SmallGroupEncontro encontro(
            SmallGroupEncontroRepository repo,
            UUID grupoId,
            LocalDate data,
            UUID anfitriaoId,
            String anotacoes
    ) {
        SmallGroupEncontro e = new SmallGroupEncontro();
        e.setSmallGroupId(grupoId);
        e.setData(data);
        e.setAnfitriaoPessoaId(anfitriaoId);
        e.setAnotacoes(anotacoes);
        return repo.save(e);
    }

    private void participante(
            SmallGroupParticipanteRepository repo,
            UUID encontroId,
            UUID pessoaId,
            String pessoaNome
    ) {
        SmallGroupParticipante p = new SmallGroupParticipante();
        p.setEncontroId(encontroId);
        p.setPessoaId(pessoaId);
        p.setPessoaNome(pessoaNome);
        repo.save(p);
    }

    private CronogramaEvento culto(
            CronogramaEventoRepository repo,
            String titulo,
            LocalDate data,
            UUID igrejaId,
            UUID supervisorId,
            String localEvento
    ) {
        CronogramaEvento e = new CronogramaEvento();
        e.setTitulo(titulo);
        e.setTipo(titulo.contains("jovens") ? TipoEvento.EVENTO : TipoEvento.CULTO);
        e.setDataEvento(data);
        e.setHoraInicio(LocalTime.of(18, 0));
        e.setHoraFim(LocalTime.of(20, 0));
        e.setLocalEvento(localEvento);
        e.setIgrejaId(igrejaId);
        e.setCompetencia(YearMonth.from(data).toString());
        e.setSupervisorPessoaId(supervisorId);
        e.setAtivo(true);
        return repo.save(e);
    }

    private Escala escala(
            EscalaRepository repo,
            String titulo,
            String competencia,
            TipoEscala tipo,
            UUID igrejaId,
            UUID ministerioId,
            UUID supervisorId,
            UUID pregadorId
    ) {
        Escala e = new Escala();
        e.setTitulo(titulo);
        e.setCompetencia(competencia);
        e.setTipo(tipo);
        e.setIgrejaId(igrejaId);
        e.setMinisterioId(ministerioId);
        e.setSupervisorPessoaId(supervisorId);
        e.setPregadorPessoaId(pregadorId);
        e.setDataEvento(LocalDate.parse(competencia + "-01"));
        e.setHoraInicio(LocalTime.MIDNIGHT);
        e.setAtivo(true);
        return repo.save(e);
    }

    private void item(
            EscalaVoluntarioRepository repo,
            Escala escala,
            LocalDate data,
            UUID eventoId,
            UUID pessoaId,
            UUID servicoId,
            StatusEscalaVoluntario status
    ) {
        EscalaVoluntario ev = new EscalaVoluntario();
        ev.setEscalaId(escala.getId());
        ev.setDataItem(data);
        ev.setEventoId(eventoId);
        ev.setPessoaId(pessoaId);
        ev.setServicoId(servicoId);
        ev.setHorario(LocalTime.of(18, 0));
        ev.setStatus(status);
        repo.save(ev);
    }

    private CategoriaCulto categoriaCulto(
            CategoriaCultoRepository repo,
            String titulo,
            com.example.base_api.culto.entity.DiaSemana diaSemana,
            LocalTime horaInicio,
            int duracaoMinutos,
            UUID igrejaId,
            UUID pregadorPadraoId
    ) {
        return repo.findByIgrejaIdAndTituloIgnoreCase(igrejaId, titulo).orElseGet(() -> {
            CategoriaCulto c = new CategoriaCulto();
            c.setIgrejaId(igrejaId);
            c.setTitulo(titulo);
            c.setDiaSemana(diaSemana);
            c.setHoraInicio(horaInicio);
            c.setDuracaoMinutos(duracaoMinutos);
            c.setPregadorPadraoId(pregadorPadraoId);
            c.setAtivo(true);
            return repo.save(c);
        });
    }

    private void gerarCultosDoMes(CultoRepository repo, CategoriaCulto categoria, YearMonth mes) {
        var dia = switch (categoria.getDiaSemana()) {
            case DOMINGO -> java.time.DayOfWeek.SUNDAY;
            case SEGUNDA -> java.time.DayOfWeek.MONDAY;
            case TERCA -> java.time.DayOfWeek.TUESDAY;
            case QUARTA -> java.time.DayOfWeek.WEDNESDAY;
            case QUINTA -> java.time.DayOfWeek.THURSDAY;
            case SEXTA -> java.time.DayOfWeek.FRIDAY;
            case SABADO -> java.time.DayOfWeek.SATURDAY;
        };
        mes.atDay(1).datesUntil(mes.atEndOfMonth().plusDays(1))
                .filter(data -> data.getDayOfWeek() == dia)
                .forEach(data -> {
                    if (repo.existsByCategoriaCultoIdAndData(categoria.getId(), data)) {
                        return;
                    }
                    Culto culto = new Culto();
                    culto.setIgrejaId(categoria.getIgrejaId());
                    culto.setCategoriaCultoId(categoria.getId());
                    culto.setTitulo(categoria.getTitulo());
                    culto.setData(data);
                    culto.setHoraInicio(categoria.getHoraInicio());
                    culto.setHoraFim(categoria.getHoraInicio().plusMinutes(categoria.getDuracaoMinutos()));
                    culto.setPregadorId(categoria.getPregadorPadraoId());
                    culto.setStatus(StatusCulto.AGENDADO);
                    culto.setCompetencia(mes.toString());
                    culto.setAtivo(true);
                    repo.save(culto);
                });
    }
}
