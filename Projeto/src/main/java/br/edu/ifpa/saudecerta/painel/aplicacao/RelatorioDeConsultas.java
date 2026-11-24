package br.edu.ifpa.saudecerta.painel.aplicacao;

import java.time.LocalDate;
import java.util.Map;

/** Resposta do relatorio E3-3: totais de consultas no periodo. */
public record RelatorioDeConsultas(
        LocalDate de,
        LocalDate ate,
        long total,
        Map<String, Long> porSituacao,
        Map<String, Long> porProfissional,
        Map<String, Long> porEspecialidade) {
}