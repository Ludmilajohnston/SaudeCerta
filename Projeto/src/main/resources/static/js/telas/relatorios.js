// E3-3: relatorio de consultas por periodo (somente administracao).

import { api } from '../api.js';
import { html, desenhar } from '../util.js';

const SITUACOES = {
  AGENDADA: 'Agendada',
  CONFIRMADA: 'Confirmada',
  EM_ATENDIMENTO: 'Em atendimento',
  REALIZADA: 'Realizada',
  CANCELADA: 'Cancelada',
};

// Periodo inicial: o mes atual inteiro (datas no formato AAAA-MM-DD).
const hoje = new Date();
let de = new Date(hoje.getFullYear(), hoje.getMonth(), 1).toLocaleDateString('sv-SE');
let ate = new Date(hoje.getFullYear(), hoje.getMonth() + 1, 0).toLocaleDateString('sv-SE');

export async function telaRelatorios(area) {
  let relatorio = null;
  let erro = null;
  try {
    relatorio = await api.relatorioDeConsultas(de, ate);
  } catch (e) {
    erro = e.detalhe ?? 'Não foi possível gerar o relatório.';
  }

  desenhar(area, html`
    <header class="cabecalho">
      <div>
        <h1>Relatórios</h1>
        <p class="subtitulo">Total de consultas no período, por situação, por profissional e por especialidade.</p>
      </div>
    </header>
    <form id="form-relatorio" class="busca">
      <label>De <input type="date" id="rel-de" value="${de}" required></label>
      <label>Até <input type="date" id="rel-ate" value="${ate}" required></label>
      <button type="submit">Gerar relatório</button>
    </form>
    ${erro ? html`<p class="vazio">${erro}</p>` : conteudo(relatorio)}`);

  area.querySelector('#form-relatorio').addEventListener('submit', (evento) => {
    evento.preventDefault();
    de = area.querySelector('#rel-de').value;
    ate = area.querySelector('#rel-ate').value;
    telaRelatorios(area);
  });
}

function conteudo(r) {
  return html`
    <p><b>Total de consultas no período:</b> ${r.total}</p>
    ${tabela('Por situação', 'Situação', r.porSituacao, SITUACOES)}
    ${tabela('Por profissional', 'Profissional', r.porProfissional)}
    ${tabela('Por especialidade', 'Especialidade', r.porEspecialidade)}`;
}

function tabela(titulo, coluna, totais, nomes = {}) {
  const linhas = Object.entries(totais ?? {});
  return html`
    <h2>${titulo}</h2>
    ${linhas.length === 0 ? html`<p class="vazio">Nenhuma consulta no período.</p>` : html`
      <table class="tabela">
        <thead><tr><th>${coluna}</th><th class="numero">Consultas</th></tr></thead>
        <tbody>
          ${linhas.map(([nome, total]) => html`
            <tr><td>${nomes[nome] ?? nome}</td><td class="numero">${total}</td></tr>`)}
        </tbody>
      </table>`}`;
}