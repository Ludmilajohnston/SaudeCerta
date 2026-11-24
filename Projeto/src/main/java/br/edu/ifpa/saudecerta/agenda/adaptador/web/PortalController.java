package br.edu.ifpa.saudecerta.agenda.adaptador.web;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ifpa.saudecerta.agenda.aplicacao.AgendaService;
import br.edu.ifpa.saudecerta.agenda.aplicacao.ConsultaDoPortal;
import br.edu.ifpa.saudecerta.seguranca.api.UsuarioAtual;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * ADAPTADOR DE ENTRADA do portal do paciente (E3-4). Acesso so para o perfil PACIENTE
 * (SecurityConfig: /api/portal/**). O paciente e identificado pelo token, nunca pela URL.
 */
@RestController
public class PortalController {

    public record Cancelamento(@NotBlank(message = "Informe o motivo do cancelamento") String motivo) {
    }

    private final AgendaService agenda;

    public PortalController(AgendaService agenda) {
        this.agenda = agenda;
    }

    @GetMapping("/api/portal/consultas")
    public List<ConsultaDoPortal> minhasConsultas(@AuthenticationPrincipal Jwt token) {
        return agenda.minhasConsultas(UsuarioAtual.de(token));
    }

    @PostMapping("/api/portal/consultas/{id}/cancelamento")
    public ConsultaDoPortal cancelar(@PathVariable Long id, @Valid @RequestBody Cancelamento dados,
            @AuthenticationPrincipal Jwt token) {
        return agenda.cancelarPeloPaciente(id, dados.motivo(), UsuarioAtual.de(token));
    }
}