package br.edu.ifpa.saudecerta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Testes de integracao da E3-4: o paciente so ve e so cancela as proprias consultas. */
@SpringBootTest
@AutoConfigureMockMvc
class PortalDoPacienteTest {

    private static final String SENHA = "saude123";
    private static final long JOANA = 1;
    private static final long OUTRO_PACIENTE = 3;
    private static final long PROFISSIONAL = 4;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private String entrar(String login) throws Exception {
        String corpo = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("login", login, "senha", SENHA))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(corpo).get("token").asText();
    }

    /** Um dia util mais distante, para nao colidir com os horarios do FluxoDaClinicaTest. */
    private static LocalDate diaUtilDistante() {
        LocalDate dia = LocalDate.now().plusDays(9);
        return dia.getDayOfWeek() == DayOfWeek.SUNDAY ? dia.plusDays(1) : dia;
    }

    private long criarConsulta(String token, long pacienteId, String inicio) throws Exception {
        String pedido = json.writeValueAsString(Map.of(
                "pacienteId", pacienteId, "profissionalId", PROFISSIONAL, "inicio", inicio));
        String corpo = mvc.perform(post("/api/consultas").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(corpo).get("id").asLong();
    }

    private String motivo(String texto) throws Exception {
        return json.writeValueAsString(Map.of("motivo", texto));
    }

    @Test
    @DisplayName("E3-4: trocar o id na URL nao mostra nem cancela consulta de outro paciente")
    void naoAcessaConsultaDeOutroPaciente() throws Exception {
        String recepcao = entrar("recepcao");
        String joana = entrar("joana");
        LocalDate dia = diaUtilDistante();

        long deOutro = criarConsulta(recepcao, OUTRO_PACIENTE, dia + "T16:00:00");

        // A lista da Joana nao contem a consulta do outro paciente
        String lista = mvc.perform(get("/api/portal/consultas").header("Authorization", joana))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode c : json.readTree(lista)) {
            assertThat(c.path("id").asLong()).isNotEqualTo(deOutro);
        }

        // Tentar cancelar a consulta do outro paciente pelo id: 404
        mvc.perform(post("/api/portal/consultas/{id}/cancelamento", deOutro).header("Authorization", joana)
                .contentType(MediaType.APPLICATION_JSON).content(motivo("Tentativa indevida")))
                .andExpect(status().isNotFound());

        // E a consulta do outro paciente continua agendada
        String agenda = mvc.perform(get("/api/agenda").param("data", dia.toString())
                .param("profissionalId", String.valueOf(PROFISSIONAL)).header("Authorization", recepcao))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        boolean continuaAgendada = false;
        for (JsonNode c : json.readTree(agenda)) {
            if (c.path("id").asLong() == deOutro) {
                continuaAgendada = "AGENDADA".equals(c.path("status").asText());
            }
        }
        assertThat(continuaAgendada).as("consulta do outro paciente intacta").isTrue();
    }

    @Test
    @DisplayName("E3-4: paciente ve e cancela a propria consulta futura, sem dados clinicos")
    void pacienteVeECancelaAPropriaConsulta() throws Exception {
        String recepcao = entrar("recepcao");
        String joana = entrar("joana");
        LocalDate dia = diaUtilDistante();

        long minha = criarConsulta(recepcao, JOANA, dia + "T16:30:00");

        String lista = mvc.perform(get("/api/portal/consultas").header("Authorization", joana))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        boolean encontrada = false;
        for (JsonNode c : json.readTree(lista)) {
            // Nenhum dado clinico ou interno no portal
            assertThat(c.has("alertasDoPaciente")).isFalse();
            assertThat(c.has("evolucao")).isFalse();
            assertThat(c.has("conduta")).isFalse();
            assertThat(c.has("observacao")).isFalse();
            if (c.path("id").asLong() == minha) {
                encontrada = true;
                assertThat(c.path("podeCancelar").asBoolean()).isTrue();
            }
        }
        assertThat(encontrada).as("a Joana ve a propria consulta").isTrue();

        // Sem motivo: recusado
        mvc.perform(post("/api/portal/consultas/{id}/cancelamento", minha).header("Authorization", joana)
                .contentType(MediaType.APPLICATION_JSON).content(motivo("")))
                .andExpect(status().is4xxClientError());

        // Com motivo: cancelada
        mvc.perform(post("/api/portal/consultas/{id}/cancelamento", minha).header("Authorization", joana)
                .contentType(MediaType.APPLICATION_JSON).content(motivo("Vou viajar")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"))
                .andExpect(jsonPath("$.podeCancelar").value(false));
    }

    @Test
    @DisplayName("E3-4: outros perfis nao acessam o portal (403)")
    void outrosPerfisNaoAcessamOPortal() throws Exception {
        mvc.perform(get("/api/portal/consultas").header("Authorization", entrar("recepcao")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/portal/consultas").header("Authorization", entrar("ana")))
                .andExpect(status().isForbidden());
    }
}