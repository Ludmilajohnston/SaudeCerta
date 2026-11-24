package br.edu.ifpa.saudecerta.prontuario.dominio;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.text.Normalizer;
import java.util.regex.Pattern;

import br.edu.ifpa.saudecerta.compartilhado.erro.RegraDeNegocioException;

public class Prescricao {

    public record Item(String medicamento, String dose, String posologia) {
    }

    private final Long atendimentoId;
    private final List<Item> itens = new ArrayList<>();

    public Prescricao(Long atendimentoId) {
        this.atendimentoId = atendimentoId;
    }

    public void adicionar(Item item, String alertasDoPaciente) {

        if (item == null
                || vazio(item.medicamento())
                || vazio(item.dose())
                || vazio(item.posologia())) {

            throw new RegraDeNegocioException(
                    "Informe o medicamento, a dose e a posologia.");
        }

        String medicamento = normalizar(item.medicamento());

        boolean repetido = itens.stream()
                .anyMatch(i -> normalizar(i.medicamento())
                        .equals(medicamento));

        if (repetido) {
            throw new RegraDeNegocioException(
                    "Este medicamento já está na prescrição.");
        }

        if (!vazio(alertasDoPaciente)) {

            String alertas = normalizar(alertasDoPaciente);

            String nomeMedicamento = medicamento
                    .replaceFirst("\\s+\\d.*$", "")
                    .trim();

            boolean citado = Pattern.compile(
                    "\\b" + Pattern.quote(nomeMedicamento) + "\\b")
                    .matcher(alertas)
                    .find();

            if (citado) {
                throw new RegraDeNegocioException(
                        "Medicamento recusado: possível alergia ou alerta do paciente.");
            }
        }

        itens.add(item);
    }

    public void validarParaEmissao() {

        if (itens.isEmpty()) {
            throw new RegraDeNegocioException(
                    "A prescrição deve conter pelo menos um medicamento.");
        }
    }

    private static boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ");
    }

    public Long getAtendimentoId() {
        return atendimentoId;
    }

    public List<Item> getItens() {
        return List.copyOf(itens);
    }
}