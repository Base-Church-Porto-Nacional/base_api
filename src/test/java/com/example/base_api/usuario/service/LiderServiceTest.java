package com.example.base_api.usuario.service;

import com.example.base_api.baseministerial.repository.BaseMinisterialRepository;
import com.example.base_api.ministerio.repository.MinisterioRepository;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.smallgroup.repository.SmallGroupRepository;
import com.example.base_api.usuario.dto.LiderResponse;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiderServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PessoaRepository pessoaRepository;
    @Mock
    private MinisterioRepository ministerioRepository;
    @Mock
    private BaseMinisterialRepository baseMinisterialRepository;
    @Mock
    private SmallGroupRepository smallGroupRepository;
    @Mock
    private AutorizacaoService autorizacaoService;

    @InjectMocks
    private LiderService liderService;

    @Test
    void liderDeGrupoSemPapelApareceNaLista() {
        UUID igrejaId = UUID.randomUUID();
        List<UUID> ids = List.of(igrejaId);
        Pessoa comConta = pessoa("Lia", "11999990000");
        Pessoa semConta = pessoa("Gil", null);
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setUsername("lia");
        usuario.setPessoaId(comConta.getId());

        when(autorizacaoService.idsParaListagem(igrejaId)).thenReturn(ids);
        when(ministerioRepository.idsLideres(ids)).thenReturn(List.of(comConta.getId()));
        when(smallGroupRepository.idsLideres(ids)).thenReturn(List.of(semConta.getId()));
        when(pessoaRepository.findByIdInAndAtivoTrue(any())).thenReturn(List.of(comConta, semConta));
        when(usuarioRepository.findByPessoaIdIn(any())).thenReturn(List.of(usuario));

        List<LiderResponse> lideres = liderService.listar(igrejaId);

        assertThat(lideres).extracting(LiderResponse::pessoaNome).containsExactlyInAnyOrder("Lia", "Gil");
        LiderResponse gil = lideres.stream().filter(l -> "Gil".equals(l.pessoaNome())).findFirst().orElseThrow();
        assertThat(gil.usuarioId()).isNull();
        assertThat(gil.username()).isNull();
        LiderResponse lia = lideres.stream().filter(l -> "Lia".equals(l.pessoaNome())).findFirst().orElseThrow();
        assertThat(lia.username()).isEqualTo("lia");
        assertThat(lia.telefone()).isEqualTo("11999990000");
        verify(pessoaRepository, never()).findById(any());
    }

    @Test
    void pessoaLiderEmDoisMinisteriosContaUmaVez() {
        UUID igrejaId = UUID.randomUUID();
        List<UUID> ids = List.of(igrejaId);
        UUID pessoaId = UUID.randomUUID();
        Pessoa pessoa = new Pessoa();
        pessoa.setId(pessoaId);

        when(ministerioRepository.idsLideres(ids)).thenReturn(List.of(pessoaId, pessoaId));
        when(ministerioRepository.idsCoordenadores(ids)).thenReturn(Arrays.asList(pessoaId, null));
        when(baseMinisterialRepository.idsLideres(ids)).thenReturn(List.of(pessoaId));
        when(pessoaRepository.findByIdInAndAtivoTrue(any())).thenReturn(List.of(pessoa));

        Set<UUID> lideres = liderService.pessoaIdsLideres(ids);

        assertThat(lideres).containsExactly(pessoaId);
    }

    private Pessoa pessoa(String nome, String telefone) {
        Pessoa p = new Pessoa();
        p.setId(UUID.randomUUID());
        p.setNome(nome);
        p.setTelefone(telefone);
        return p;
    }
}
