package com.example.base_api.igreja.service;

import com.example.base_api.igreja.dto.IgrejaFilialCreateRequest;
import com.example.base_api.igreja.dto.IgrejaResponse;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.entity.IgrejaTipo;
import com.example.base_api.igreja.repository.IgrejaRepository;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.repository.PapelRepository;
import com.example.base_api.usuario.repository.UsuarioPapelRepository;
import com.example.base_api.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IgrejaServiceTest {

    @Mock
    private IgrejaRepository igrejaRepository;
    @Mock
    private PessoaRepository pessoaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private UsuarioPapelRepository usuarioPapelRepository;
    @Mock
    private PapelRepository papelRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private IgrejaContextoService igrejaContextoService;
    @Mock
    private AutorizacaoService autorizacaoService;

    @InjectMocks
    private IgrejaService igrejaService;

    @Test
    void deveListarIgrejasVisiveisDaMatriz() {
        Igreja matriz = igreja(IgrejaTipo.MATRIZ, null);
        Igreja filial = igreja(IgrejaTipo.FILIAL, matriz.getId());
        when(igrejaContextoService.igrejasVisiveis()).thenReturn(List.of(matriz, filial));

        List<IgrejaResponse> visiveis = igrejaService.listarVisiveis();

        assertThat(visiveis).hasSize(2);
        assertThat(visiveis.get(0).tipo()).isEqualTo(IgrejaTipo.MATRIZ);
        assertThat(visiveis.get(1).tipo()).isEqualTo(IgrejaTipo.FILIAL);
    }

    @Test
    void deveMontarResponseComPastor() {
        UUID pastorId = UUID.randomUUID();
        Igreja igreja = igreja(IgrejaTipo.MATRIZ, null);
        igreja.setPastorPessoaId(pastorId);
        when(pessoaRepository.nomesPorId(any())).thenReturn(Map.of(pastorId, "Ana Costa"));

        IgrejaResponse response = igrejaService.toResponse(igreja);

        assertThat(response.pastorPessoaId()).isEqualTo(pastorId);
        assertThat(response.pastorNome()).isEqualTo("Ana Costa");
    }

    @Test
    void respostaDaIgrejaNaoTemAdmin() {
        assertThat(java.util.Arrays.stream(IgrejaResponse.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName))
                .containsExactly("id", "nome", "tipo", "igrejaMatrizId", "pastorPessoaId", "pastorNome",
                        "ativo", "createdAt", "updatedAt");
    }

    @Test
    void membroNaoCriaFilial() {
        UUID matrizId = UUID.randomUUID();
        doThrow(new AccessDeniedException("Somente o pastor da matriz pode criar filial"))
                .when(autorizacaoService).assertPodeCriarFilial();

        IgrejaFilialCreateRequest request = new IgrejaFilialCreateRequest(
                "Nova Filial",
                UUID.randomUUID(),
                "Admin Filial",
                "admin.filial@base.church",
                "admin123"
        );

        assertThatThrownBy(() -> igrejaService.criarFilial(matrizId, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    private Igreja igreja(IgrejaTipo tipo, UUID matrizId) {
        Igreja igreja = new Igreja();
        igreja.setId(UUID.randomUUID());
        igreja.setNome(tipo == IgrejaTipo.MATRIZ ? "Base Church" : "Filial");
        igreja.setTipo(tipo);
        igreja.setIgrejaMatrizId(matrizId);
        igreja.setAtivo(true);
        igreja.setCreatedAt(Instant.now());
        igreja.setUpdatedAt(Instant.now());
        return igreja;
    }

    @Test
    void listarVisiveisBuscaPastoresEmLote() {
        Pessoa pastora = new Pessoa();
        pastora.setId(UUID.randomUUID());
        pastora.setNome("Pra. Ana");
        Igreja sede = igrejaDeTeste("Sede");
        sede.setPastorPessoaId(pastora.getId());
        Igreja filial = igrejaDeTeste("Filial");

        when(igrejaContextoService.igrejasVisiveis()).thenReturn(List.of(sede, filial));
        when(pessoaRepository.nomesPorId(any())).thenReturn(Map.of(pastora.getId(), "Pra. Ana"));

        List<IgrejaResponse> igrejas = igrejaService.listarVisiveis();

        assertThat(igrejas).extracting(IgrejaResponse::pastorNome).containsExactly("Pra. Ana", null);
        verify(pessoaRepository, never()).findById(any());
    }

    private Igreja igrejaDeTeste(String nome) {
        Igreja igreja = new Igreja();
        igreja.setId(UUID.randomUUID());
        igreja.setNome(nome);
        igreja.setTipo(IgrejaTipo.FILIAL);
        igreja.setAtivo(true);
        return igreja;
    }

    // --- troca de pastor da igreja ---

    private Pessoa pessoaDa(UUID igrejaId) {
        Pessoa p = new Pessoa();
        p.setId(UUID.randomUUID());
        p.setNome("Pr. Novo");
        p.setIgrejaId(igrejaId);
        return p;
    }

    @Test
    void secretariaNaoTrocaPastorDaIgreja() {
        Igreja igreja = igreja(IgrejaTipo.MATRIZ, null);
        igreja.setPastorPessoaId(UUID.randomUUID());
        Pessoa novo = pessoaDa(igreja.getId());
        when(igrejaRepository.findById(igreja.getId())).thenReturn(Optional.of(igreja));
        doThrow(new AccessDeniedException("Sem permissão para esta ação"))
                .when(autorizacaoService).assertTem(igreja.getId(), com.example.base_api.security.Capacidade.FILIAL_CRIAR);

        assertThatThrownBy(() -> igrejaService.atualizar(igreja.getId(),
                new com.example.base_api.igreja.dto.IgrejaUpdateRequest("Base", novo.getId(), true)))
                .isInstanceOf(AccessDeniedException.class);
        verify(igrejaRepository, never()).save(any());
    }

    @Test
    void secretariaAtualizaNomeMantendoPastor() {
        Igreja igreja = igreja(IgrejaTipo.MATRIZ, null);
        Pessoa pastor = pessoaDa(igreja.getId());
        igreja.setPastorPessoaId(pastor.getId());
        when(igrejaRepository.findById(igreja.getId())).thenReturn(Optional.of(igreja));
        when(igrejaRepository.save(any(Igreja.class))).thenAnswer(i -> i.getArgument(0));

        igrejaService.atualizar(igreja.getId(),
                new com.example.base_api.igreja.dto.IgrejaUpdateRequest("Novo nome", pastor.getId(), true));

        assertThat(igreja.getNome()).isEqualTo("Novo nome");
        verify(autorizacaoService, never()).assertTem(any(), any());
    }

    @Test
    void pastorDeOutraIgrejaNaoPodeSerDefinido() {
        Igreja igreja = igreja(IgrejaTipo.MATRIZ, null);
        igreja.setPastorPessoaId(UUID.randomUUID());
        Pessoa alheio = pessoaDa(UUID.randomUUID());
        when(igrejaRepository.findById(igreja.getId())).thenReturn(Optional.of(igreja));
        when(pessoaRepository.findById(alheio.getId())).thenReturn(Optional.of(alheio));

        assertThatThrownBy(() -> igrejaService.atualizar(igreja.getId(),
                new com.example.base_api.igreja.dto.IgrejaUpdateRequest("Base", alheio.getId(), true)))
                .isInstanceOf(com.example.base_api.common.exception.BusinessException.class)
                .hasMessage("A pessoa não pertence a esta igreja");
        verify(igrejaRepository, never()).save(any());
    }

    @Test
    void pastorTrocaPastorPorPessoaDaIgreja() {
        Igreja igreja = igreja(IgrejaTipo.MATRIZ, null);
        igreja.setPastorPessoaId(UUID.randomUUID());
        Pessoa novo = pessoaDa(igreja.getId());
        when(igrejaRepository.findById(igreja.getId())).thenReturn(Optional.of(igreja));
        when(pessoaRepository.findById(novo.getId())).thenReturn(Optional.of(novo));
        when(igrejaRepository.save(any(Igreja.class))).thenAnswer(i -> i.getArgument(0));

        igrejaService.atualizar(igreja.getId(),
                new com.example.base_api.igreja.dto.IgrejaUpdateRequest("Base", novo.getId(), true));

        assertThat(igreja.getPastorPessoaId()).isEqualTo(novo.getId());
        verify(autorizacaoService).assertTem(igreja.getId(), com.example.base_api.security.Capacidade.FILIAL_CRIAR);
    }
}
