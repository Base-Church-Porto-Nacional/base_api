package com.example.base_api.jornada.service;

import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.jornada.dto.JornadaResponse;
import com.example.base_api.jornada.dto.JornadaUpdateRequest;
import com.example.base_api.jornada.dto.PessoaEtapaRequest;
import com.example.base_api.jornada.dto.PessoaEtapaResponse;
import com.example.base_api.jornada.entity.JornadaEtapaCatalogo;
import com.example.base_api.jornada.entity.JornadaMembro;
import com.example.base_api.jornada.entity.PessoaJornadaEtapa;
import com.example.base_api.jornada.repository.JornadaEtapaCatalogoRepository;
import com.example.base_api.jornada.repository.JornadaMembroRepository;
import com.example.base_api.jornada.repository.PessoaJornadaEtapaRepository;
import com.example.base_api.pessoa.repository.PessoaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class JornadaService {

    private final JornadaMembroRepository jornadaMembroRepository;
    private final JornadaEtapaCatalogoRepository etapaCatalogoRepository;
    private final PessoaJornadaEtapaRepository pessoaJornadaEtapaRepository;
    private final PessoaRepository pessoaRepository;

    public JornadaService(
            JornadaMembroRepository jornadaMembroRepository,
            JornadaEtapaCatalogoRepository etapaCatalogoRepository,
            PessoaJornadaEtapaRepository pessoaJornadaEtapaRepository,
            PessoaRepository pessoaRepository
    ) {
        this.jornadaMembroRepository = jornadaMembroRepository;
        this.etapaCatalogoRepository = etapaCatalogoRepository;
        this.pessoaJornadaEtapaRepository = pessoaJornadaEtapaRepository;
        this.pessoaRepository = pessoaRepository;
    }

    @Transactional
    public void criarJornadaInicial(UUID pessoaId) {
        if (jornadaMembroRepository.findByPessoaId(pessoaId).isPresent()) {
            return;
        }
        JornadaMembro jornada = new JornadaMembro();
        jornada.setPessoaId(pessoaId);
        jornadaMembroRepository.save(jornada);
    }

    @Transactional(readOnly = true)
    public JornadaResponse buscarPorPessoa(UUID pessoaId) {
        garantirPessoaExiste(pessoaId);
        return toResponse(buscarJornada(pessoaId));
    }

    @Transactional
    public JornadaResponse atualizar(UUID pessoaId, JornadaUpdateRequest request) {
        garantirPessoaExiste(pessoaId);
        JornadaMembro jornada = buscarJornada(pessoaId);

        jornada.setAceitouJesus(request.aceitouJesus());
        jornada.setDataAceitouJesus(request.dataAceitouJesus());
        jornada.setBatizado(request.batizado());
        jornada.setDataBatismo(request.dataBatismo());
        jornada.setConcluiuTrilhaBase(request.concluiuTrilhaBase());
        jornada.setDataConclusaoTrilhaBase(request.dataConclusaoTrilhaBase());
        jornada.setMembro(request.membro());
        jornada.setDataMembro(request.dataMembro());

        return toResponse(jornadaMembroRepository.save(jornada));
    }

    @Transactional(readOnly = true)
    public List<PessoaEtapaResponse> listarEtapasDaPessoa(UUID pessoaId) {
        garantirPessoaExiste(pessoaId);
        return pessoaJornadaEtapaRepository.findByPessoaId(pessoaId).stream()
                .map(this::toEtapaResponse)
                .toList();
    }

    @Transactional
    public PessoaEtapaResponse registrarEtapa(UUID pessoaId, PessoaEtapaRequest request) {
        garantirPessoaExiste(pessoaId);
        JornadaEtapaCatalogo etapa = etapaCatalogoRepository.findById(request.etapaId())
                .orElseThrow(() -> new ResourceNotFoundException("Etapa da jornada não encontrada"));

        PessoaJornadaEtapa registro = pessoaJornadaEtapaRepository
                .findByPessoaIdAndEtapaId(pessoaId, etapa.getId())
                .orElseGet(() -> {
                    PessoaJornadaEtapa novo = new PessoaJornadaEtapa();
                    novo.setPessoaId(pessoaId);
                    novo.setEtapaId(etapa.getId());
                    return novo;
                });

        registro.setConcluida(request.concluida());
        registro.setDataConclusao(request.dataConclusao());
        registro.setObservacoes(request.observacoes());

        return toEtapaResponse(pessoaJornadaEtapaRepository.save(registro));
    }

    @Transactional
    public void excluirEtapa(UUID pessoaId, UUID registroId) {
        garantirPessoaExiste(pessoaId);
        PessoaJornadaEtapa registro = pessoaJornadaEtapaRepository.findById(registroId)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de etapa não encontrado"));

        if (!registro.getPessoaId().equals(pessoaId)) {
            throw new ResourceNotFoundException("Registro de etapa não encontrado para esta pessoa");
        }

        pessoaJornadaEtapaRepository.delete(registro);
    }

    private JornadaMembro buscarJornada(UUID pessoaId) {
        return jornadaMembroRepository.findByPessoaId(pessoaId)
                .orElseThrow(() -> new ResourceNotFoundException("Jornada não encontrada para a pessoa"));
    }

    private void garantirPessoaExiste(UUID pessoaId) {
        if (!pessoaRepository.existsById(pessoaId)) {
            throw new ResourceNotFoundException("Pessoa não encontrada");
        }
    }

    private JornadaResponse toResponse(JornadaMembro jornada) {
        return new JornadaResponse(
                jornada.getId(),
                jornada.getPessoaId(),
                jornada.getAceitouJesus(),
                jornada.getDataAceitouJesus(),
                jornada.getBatizado(),
                jornada.getDataBatismo(),
                jornada.getConcluiuTrilhaBase(),
                jornada.getDataConclusaoTrilhaBase(),
                jornada.getMembro(),
                jornada.getDataMembro(),
                jornada.getCreatedAt(),
                jornada.getUpdatedAt()
        );
    }

    private PessoaEtapaResponse toEtapaResponse(PessoaJornadaEtapa registro) {
        JornadaEtapaCatalogo etapa = etapaCatalogoRepository.findById(registro.getEtapaId())
                .orElseThrow(() -> new ResourceNotFoundException("Etapa da jornada não encontrada"));
        return new PessoaEtapaResponse(
                registro.getId(),
                registro.getPessoaId(),
                registro.getEtapaId(),
                etapa.getCodigo(),
                etapa.getNome(),
                registro.getConcluida(),
                registro.getDataConclusao(),
                registro.getObservacoes(),
                registro.getCreatedAt(),
                registro.getUpdatedAt()
        );
    }
}
