package com.example.base_api.usuario.service;

import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.usuario.dto.LiderResponse;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
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
    private AutorizacaoService autorizacaoService;

    @InjectMocks
    private LiderService liderService;

    @Test
    void listarBuscaAsPessoasDosLideresNumaUnicaConsulta() {
        UUID igrejaId = UUID.randomUUID();
        Pessoa pessoa = new Pessoa();
        pessoa.setId(UUID.randomUUID());
        pessoa.setNome("Lia");
        pessoa.setTelefone("11999990000");
        Usuario comPessoa = lider("lia", pessoa.getId());
        Usuario semPessoa = lider("sem", null);

        when(autorizacaoService.idsParaListagem(igrejaId)).thenReturn(List.of(igrejaId));
        when(usuarioRepository.findAtivosByPapelAndIgrejaIdIn("LIDER", List.of(igrejaId)))
                .thenReturn(List.of(comPessoa, semPessoa));
        when(pessoaRepository.findAllById(any())).thenReturn(List.of(pessoa));

        List<LiderResponse> lideres = liderService.listar(igrejaId);

        assertThat(lideres).extracting(LiderResponse::pessoaNome).containsExactly("Lia", null);
        assertThat(lideres).extracting(LiderResponse::telefone).containsExactly("11999990000", null);
        verify(pessoaRepository, never()).findById(any());
    }

    private Usuario lider(String username, UUID pessoaId) {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setUsername(username);
        usuario.setPessoaId(pessoaId);
        return usuario;
    }
}
