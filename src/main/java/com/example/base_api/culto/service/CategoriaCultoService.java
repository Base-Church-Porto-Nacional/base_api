package com.example.base_api.culto.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.culto.dto.CategoriaCultoCreateRequest;
import com.example.base_api.culto.dto.CategoriaCultoResponse;
import com.example.base_api.culto.dto.CategoriaCultoUpdateRequest;
import com.example.base_api.culto.entity.CategoriaCulto;
import com.example.base_api.culto.repository.CategoriaCultoRepository;
import com.example.base_api.culto.repository.CultoRepository;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
public class CategoriaCultoService {

    private static final LocalTime HORA_INICIO_PADRAO = LocalTime.of(20, 0);
    private static final int DURACAO_PADRAO_MINUTOS = 90;

    private final CategoriaCultoRepository categoriaRepository;
    private final CultoRepository cultoRepository;
    private final AutorizacaoService autorizacaoService;
    private final PessoaRepository pessoaRepository;

    public CategoriaCultoService(
            CategoriaCultoRepository categoriaRepository,
            CultoRepository cultoRepository,
            AutorizacaoService autorizacaoService,
            PessoaRepository pessoaRepository
    ) {
        this.categoriaRepository = categoriaRepository;
        this.cultoRepository = cultoRepository;
        this.autorizacaoService = autorizacaoService;
        this.pessoaRepository = pessoaRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoriaCultoResponse> listar(UUID igrejaId, Boolean ativo) {
        List<UUID> ids = autorizacaoService.idsParaListagem(igrejaId);
        if (ids.isEmpty()) {
            return List.of();
        }
        List<CategoriaCulto> categorias = Boolean.TRUE.equals(ativo)
                ? categoriaRepository.findByIgrejaIdInAndAtivoOrderByTituloAsc(ids, true)
                : categoriaRepository.findByIgrejaIdInOrderByTituloAsc(ids);
        return categorias.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CategoriaCultoResponse buscarPorId(UUID id) {
        CategoriaCulto categoria = buscarEntidade(id);
        autorizacaoService.assertAcesso(categoria.getIgrejaId());
        return toResponse(categoria);
    }

    @Transactional
    public CategoriaCultoResponse criar(CategoriaCultoCreateRequest request) {
        UUID igrejaId = request.igrejaId() != null
                ? request.igrejaId()
                : autorizacaoService.requireUsuario().getIgrejaId();
        if (igrejaId == null) {
            throw new BusinessException("igrejaId é obrigatório");
        }
        autorizacaoService.assertPastorOuAdmin(igrejaId);

        String titulo = request.titulo().trim();
        validarTituloUnico(igrejaId, titulo, null);
        validarPregadorPadrao(request.pregadorPadraoId(), igrejaId);

        CategoriaCulto categoria = new CategoriaCulto();
        categoria.setIgrejaId(igrejaId);
        categoria.setTitulo(titulo);
        categoria.setDescricao(request.descricao());
        categoria.setDiaSemana(request.diaSemana());
        categoria.setHoraInicio(request.horaInicio() != null ? request.horaInicio() : HORA_INICIO_PADRAO);
        categoria.setDuracaoMinutos(request.duracaoMinutos() != null
                ? request.duracaoMinutos()
                : DURACAO_PADRAO_MINUTOS);
        categoria.setPregadorPadraoId(request.pregadorPadraoId());
        categoria.setAtivo(true);

        return toResponse(categoriaRepository.save(categoria));
    }

    @Transactional
    public CategoriaCultoResponse atualizar(UUID id, CategoriaCultoUpdateRequest request) {
        CategoriaCulto categoria = buscarEntidade(id);
        autorizacaoService.assertPastorOuAdmin(categoria.getIgrejaId());

        String titulo = request.titulo().trim();
        validarTituloUnico(categoria.getIgrejaId(), titulo, id);
        validarPregadorPadrao(request.pregadorPadraoId(), categoria.getIgrejaId());

        categoria.setTitulo(titulo);
        categoria.setDescricao(request.descricao());
        categoria.setDiaSemana(request.diaSemana());
        categoria.setHoraInicio(request.horaInicio() != null ? request.horaInicio() : HORA_INICIO_PADRAO);
        categoria.setDuracaoMinutos(request.duracaoMinutos() != null
                ? request.duracaoMinutos()
                : DURACAO_PADRAO_MINUTOS);
        categoria.setPregadorPadraoId(request.pregadorPadraoId());
        categoria.setAtivo(request.ativo());

        return toResponse(categoriaRepository.save(categoria));
    }

    @Transactional
    public void excluir(UUID id) {
        CategoriaCulto categoria = buscarEntidade(id);
        autorizacaoService.assertPastorOuAdmin(categoria.getIgrejaId());
        if (cultoRepository.countByCategoriaCultoIdAndAtivoTrue(categoria.getId()) > 0) {
            categoria.setAtivo(false);
            categoriaRepository.save(categoria);
            return;
        }
        categoriaRepository.delete(categoria);
    }

    @Transactional(readOnly = true)
    public CategoriaCulto buscarEntidade(UUID id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria de culto não encontrada"));
    }

    private void validarTituloUnico(UUID igrejaId, String titulo, UUID id) {
        boolean duplicado = id == null
                ? categoriaRepository.existsByIgrejaIdAndTituloIgnoreCase(igrejaId, titulo)
                : categoriaRepository.existsByIgrejaIdAndTituloIgnoreCaseAndIdNot(igrejaId, titulo, id);
        if (duplicado) {
            throw new BusinessException("Já existe uma categoria de culto com este título nesta igreja");
        }
    }

    private void validarPregadorPadrao(UUID pregadorId, UUID igrejaId) {
        if (pregadorId == null) {
            return;
        }
        Pessoa pessoa = pessoaRepository.findById(pregadorId)
                .orElseThrow(() -> new ResourceNotFoundException("Pessoa não encontrada"));
        autorizacaoService.assertPessoaDaIgreja(pessoa, igrejaId);
    }

    private CategoriaCultoResponse toResponse(CategoriaCulto categoria) {
        return new CategoriaCultoResponse(
                categoria.getId(),
                categoria.getIgrejaId(),
                categoria.getTitulo(),
                categoria.getDescricao(),
                categoria.getDiaSemana(),
                categoria.getHoraInicio(),
                categoria.getDuracaoMinutos(),
                categoria.getPregadorPadraoId(),
                nomePessoa(categoria.getPregadorPadraoId()),
                categoria.getAtivo(),
                categoria.getCreatedAt(),
                categoria.getUpdatedAt()
        );
    }

    private String nomePessoa(UUID pessoaId) {
        if (pessoaId == null) {
            return null;
        }
        return pessoaRepository.findById(pessoaId).map(Pessoa::getNome).orElse(null);
    }
}