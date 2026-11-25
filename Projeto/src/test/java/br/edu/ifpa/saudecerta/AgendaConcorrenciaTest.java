package br.edu.ifpa.saudecerta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class AgendaConcorrenciaTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private String entrar() throws Exception {

        String corpo = mvc.perform(
                post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of(
                        "login", "recepcao",
                        "senha", "saude123"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return "Bearer " + json.readTree(corpo)
                .get("token").asText();
    }

    private int agendar(
            String token,
            int pacienteId,
            int profissionalId,
            String inicio) throws Exception {

        String pedido = json.writeValueAsString(Map.of(
                "pacienteId", pacienteId,
                "profissionalId", profissionalId,
                "inicio", inicio));

        return mvc.perform(post("/api/consultas")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(pedido))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    @Test
    @DisplayName("E3-5: duas solicitacoes simultaneas nao podem ocupar o mesmo horario")
    void impedeAgendamentoSimultaneo() throws Exception {

        String token = entrar();

        LocalDate dia = LocalDate.now().plusDays(15);

        if (dia.getDayOfWeek() == DayOfWeek.SUNDAY) {
            dia = dia.plusDays(1);
        }

        String inicio = dia + "T09:00:00";

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CyclicBarrier barreira = new CyclicBarrier(2);

        try {

            Future<Integer> primeira = executor.submit(() -> {
                barreira.await(10, TimeUnit.SECONDS);
                return agendar(token, 3, 4, inicio);
            });

            Future<Integer> segunda = executor.submit(() -> {
                barreira.await(10, TimeUnit.SECONDS);
                return agendar(token, 6, 4, inicio);
            });

            int resultado1 =
                    primeira.get(30, TimeUnit.SECONDS);

            int resultado2 =
                    segunda.get(30, TimeUnit.SECONDS);

            System.out.println(
                    "Primeira requisicao: HTTP " + resultado1);

            System.out.println(
                    "Segunda requisicao: HTTP " + resultado2);

            assertThat(new int[]{resultado1, resultado2})
                    .containsExactlyInAnyOrder(201, 409);

        } finally {
            executor.shutdownNow();
        }
    }
    @Test
@DisplayName("E3-5: paciente nao pode ter duas consultas simultaneas")
void impedeConflitoSimultaneoDoPaciente() throws Exception {

    String token = entrar();

    LocalDate dia = LocalDate.now().plusDays(20);

    if (dia.getDayOfWeek() == DayOfWeek.SUNDAY) {
        dia = dia.plusDays(1);
    }

    String inicio = dia + "T10:00:00";

    ExecutorService executor = Executors.newFixedThreadPool(2);
    CyclicBarrier barreira = new CyclicBarrier(2);

    try {
        Future<Integer> primeira = executor.submit(() -> {
            barreira.await(10, TimeUnit.SECONDS);
            return agendar(token, 3, 4, inicio);
        });

        Future<Integer> segunda = executor.submit(() -> {
            barreira.await(10, TimeUnit.SECONDS);
            return agendar(token, 3, 2, inicio);
        });

        assertThat(new int[]{
                primeira.get(30, TimeUnit.SECONDS),
                segunda.get(30, TimeUnit.SECONDS)
        }).containsExactlyInAnyOrder(201, 409);

    } finally {
        executor.shutdownNow();
    }
}

private long criarConsulta(
        String token, int pacienteId,
        int profissionalId, String inicio) throws Exception {

    String corpo = mvc.perform(post("/api/consultas")
            .header("Authorization", token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of(
                    "pacienteId", pacienteId,
                    "profissionalId", profissionalId,
                    "inicio", inicio))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

    return json.readTree(corpo).get("id").asLong();
}

private int remarcar(
        String token, long id, String inicio) throws Exception {

    return mvc.perform(
            org.springframework.test.web.servlet.request
                    .MockMvcRequestBuilders.put(
                            "/api/consultas/{id}/horario", id)
            .header("Authorization", token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(
                    Map.of("inicio", inicio))))
            .andReturn().getResponse().getStatus();
}

@Test
@DisplayName("E3-5: duas remarcacoes simultaneas nao podem ocupar o mesmo horario")
void impedeRemarcacaoSimultanea() throws Exception {

    String token = entrar();

    LocalDate dia = LocalDate.now().plusDays(25);

    if (dia.getDayOfWeek() == DayOfWeek.SUNDAY) {
        dia = dia.plusDays(1);
    }

    long id1 = criarConsulta(token, 3, 4,
            dia + "T11:00:00");

    long id2 = criarConsulta(token, 6, 4,
            dia + "T11:30:00");

    String novoHorario = dia + "T12:00:00";

    ExecutorService executor = Executors.newFixedThreadPool(2);
    CyclicBarrier barreira = new CyclicBarrier(2);

    try {
        Future<Integer> primeira = executor.submit(() -> {
            barreira.await(10, TimeUnit.SECONDS);
            return remarcar(token, id1, novoHorario);
        });

        Future<Integer> segunda = executor.submit(() -> {
            barreira.await(10, TimeUnit.SECONDS);
            return remarcar(token, id2, novoHorario);
        });

        assertThat(new int[]{
                primeira.get(30, TimeUnit.SECONDS),
                segunda.get(30, TimeUnit.SECONDS)
        }).containsExactlyInAnyOrder(200, 409);

    } finally {
        executor.shutdownNow();
    }
}
}