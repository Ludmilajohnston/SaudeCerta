// E3-4: portal do paciente. Mostra so as consultas de quem esta logado (o servidor filtra pelo token).

import { api } from '../api.js';
import { html, desenhar, dataEHora } from '../util.js';

const SITUACOES = {
  AGENDADA: 'Agendada',
  CONFIRMADA: 'Confirmada',
  EM_ATENDIMENTO: 'Em atendimento',
  REALIZADA: 'Realizada',
  CANCELADA: 'Cancelada',
};

export async function telaPortal(area) {
  let consultas;
  try {
    consultas = await api.minhasConsultas();
  } catch (erro) {
    desenhar(area, html`<p class="vazio">${erro.detalhe}</p>`);
    return;
  }

  const agora = new Date();
  const proximas = consultas.filter((c) => new Date(c.inicio) >= agora);
  const anteriores = consultas.filter((c) => new Date(c.inicio) < agora).reverse();

  desenhar(area, html`
    <header class="cabecalho">
      <div>
        <h1>Minhas consultas</h1>
        <p class="subtitulo">Suas consultas na clínica. Para remarcar, fale com a recepção.</p>
      </div>
    </header>
    ${tabela('Próximas consultas', proximas, 'Você não tem consultas marcadas.')}
    ${tabela('Consultas anteriores', anteriores, 'Nenhuma consulta anterior.')}`);

  area.querySelectorAll('[data-cancelar]').forEach((botao) => {
    botao.addEventListener('click', async () => {
      const motivo = window.prompt('Informe o motivo do cancelamento:');
      if (motivo === null) return;
      try {
        await api.cancelarMinhaConsulta(botao.dataset.cancelar, motivo);
        telaPortal(area);
      } catch (erro) {
        window.alert(erro.detalhe);
      }
    });
  });
}

function tabela(titulo, lista, mensagemVazia) {
  return html`
    <h2>${titulo}</h2>
    ${lista.length === 0 ? html`<p class="vazio">${mensagemVazia}</p>` : html`
      <table class="tabela">
        <thead><tr><th>Quando</th><th>Profissional</th><th>Especialidade</th><th>Situação</th><th></th></tr></thead>
        <tbody>
          ${lista.map((c) => html`
            <tr>
              <td class="numero">${dataEHora(c.inicio)}</td>
              <td>${c.profissional}</td>
              <td>${c.especialidade ?? ''}</td>
              <td>${SITUACOES[c.status] ?? c.status}${c.motivoCancelamento ? html`<br><small>Motivo: ${c.motivoCancelamento}</small>` : ''}</td>
              <td>${c.podeCancelar ? html`<button type="button" data-cancelar="${c.id}">Cancelar</button>` : ''}</td>
            </tr>`)}
        </tbody>
      </table>`}`;
}