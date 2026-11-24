import { api } from '../api.js';
import {
  html, desenhar, dataCurta, dataEHora, hora, idade, etiqueta, avisar, avisarErro, lerFormulario,
} from '../util.js';


function cabecalhoDoPaciente(paciente) {
  return html`
    <div class="paciente-cabecalho">
      <div>
        <h1>${paciente.nome}</h1>
        <p>${idade(paciente.dataNascimento)}, nascimento em ${dataCurta(paciente.dataNascimento)}</p>
      </div>
      ${paciente.alertas ? html`<p class="alerta-bloco"><strong>Atenção</strong> ${paciente.alertas}</p>` : ''}
    </div>`;
}
function listaPrescricoes(prescricoes = []) {
  if (!prescricoes.length) {
    return html`<p class="vazio">Nenhuma prescrição registrada.</p>`;
  }

  return html`
    ${prescricoes.map((p, i) => html`
      <div>
        <h3>Prescrição ${i + 1}</h3>
        <ul>
          ${p.itens.map(item => html`
            <li>
              <strong>${item.medicamento}</strong>
              <p>Dose: ${item.dose}</p>
              <p>Posologia: ${item.posologia}</p>
            </li>
          `)}
        </ul>
      </div>
    `)}
  `;
}
function historico(atendimentos, ignorarId) {
  const lista = atendimentos.filter((a) => a.id !== ignorarId);
  if (lista.length === 0) {
    return html`<p class="vazio">Sem atendimentos anteriores.</p>`;
  }
  return html`
    <ol class="linha-do-tempo">
      ${lista.map((a) => html`
        <li>
          <header>
            <time>${dataEHora(a.inicio)}</time>
            <span>${a.profissional}, ${a.especialidade}</span>
            ${a.status === 'ABERTO' ? etiqueta(a.status) : ''}
          </header>
          ${a.queixaPrincipal ? html`<h3>${a.queixaPrincipal}</h3>` : ''}
          ${a.evolucao ? html`<p><b>Evolução.</b> ${a.evolucao}</p>` : ''}
          ${a.conduta ? html`<p><b>Conduta.</b> ${a.conduta}</p>` : ''}
${a.prescricoes?.length ? html`
  <h3>Prescrições</h3>
  ${listaPrescricoes(a.prescricoes)}
` : ''}
        </li>`)}
    </ol>`;
}

const avisoDeAuditoria = html`
  <p class="nota-auditoria">Este acesso ficou registrado na trilha de auditoria, com seu usuário, a data e a hora.</p>`;

/** Leitura do prontuario: #/prontuario/{pacienteId} */
export async function telaProntuario(area, { parametro }) {
  let prontuario;
  try {
    prontuario = await api.prontuario(parametro);
  } catch (erro) {
    desenhar(area, html`<p class="vazio">${erro.detalhe}</p>`);
    return;
  }
  desenhar(area, html`
    <a class="voltar" href="#/pacientes">Pacientes</a>
    ${cabecalhoDoPaciente(prontuario.paciente)}
    ${avisoDeAuditoria}
    <h2>Histórico de atendimentos</h2>
    ${historico(prontuario.atendimentos)}`);
}

/** Atendimento em andamento: #/atendimento/{consultaId} */
export async function telaAtendimento(area, { parametro }) {
  let atendimento;
  let prontuario;
  try {
    atendimento = await api.iniciarAtendimento(Number(parametro));
    prontuario = await api.prontuario(atendimento.pacienteId);
  } catch (erro) {
    desenhar(area, html`
      <a class="voltar" href="#/painel">Painel do dia</a>
      <p class="vazio">${erro.detalhe}</p>`);
    return;
  }

  const aberto = atendimento.status === 'ABERTO';

  desenhar(area, html`
    <a class="voltar" href="#/painel">Painel do dia</a>
    ${cabecalhoDoPaciente(prontuario.paciente)}
    ${avisoDeAuditoria}
    <div class="duas-colunas colunas-atendimento">
      <div>
      <section class="quadro">
        <h2>Atendimento de ${dataCurta(atendimento.inicio)}, iniciado às ${hora(atendimento.inicio)}</h2>
        <form id="form-atendimento" novalidate>
          <label>Queixa principal
            <input name="queixaPrincipal" value="${atendimento.queixaPrincipal ?? ''}" maxlength="300" ${aberto ? '' : 'disabled'}>
          </label>
          <label>Evolução clínica
            <textarea name="evolucao" rows="7" maxlength="4000" ${aberto ? '' : 'disabled'}>${atendimento.evolucao ?? ''}</textarea>
          </label>
          <label>Conduta
            <textarea name="conduta" rows="4" maxlength="2000" ${aberto ? '' : 'disabled'}>${atendimento.conduta ?? ''}</textarea>
          </label>
          ${aberto ? html`
            <div class="dialogo-acoes">
              <button type="submit" class="botao botao-secundario">Salvar rascunho</button>
              <button type="button" class="botao" id="finalizar">Finalizar atendimento</button>
            </div>
            <p class="dica">Depois de finalizado, o registro não pode mais ser alterado.</p>`
          : html`<p class="dica">Atendimento finalizado em ${dataEHora(atendimento.fim)}. O registro não pode mais ser alterado.</p>`}
        </form>
      </section>
      <section class="quadro">
        <h2>Prescrição</h2>
      <div id="lista-prescricoes">
  ${listaPrescricoes(atendimento.prescricoes)}
</div>

${aberto ? html`
  <form id="form-prescricao">
    <label>Medicamento
      <input name="medicamento" required maxlength="255">
    </label>

    <label>Dose
      <input name="dose" required maxlength="255">
    </label>

    <label>Posologia
      <textarea name="posologia" required maxlength="1000"></textarea>
    </label>

    <button type="button" class="botao botao-secundario" id="adicionar-item">
      Adicionar medicamento
    </button>

    <div id="itens-prescricao"></div>

    <button type="button" class="botao" id="emitir-prescricao">
      Emitir prescrição
    </button>
  </form>
` : ''}
      </section>
      </div>
      <section class="quadro">
        <h2>Histórico</h2>
        ${historico(prontuario.atendimentos, atendimento.id)}
      </section>
    </div>`);

  if (!aberto) return;
  const formPrescricao = area.querySelector('#form-prescricao');
const itens = [];

formPrescricao.addEventListener('submit', evento => {
  evento.preventDefault();
});

function atualizarItens() {
  desenhar(area.querySelector('#itens-prescricao'), html`
    <ul>
      ${itens.map((item, i) => html`
        <li>
          ${item.medicamento} — ${item.dose} — ${item.posologia}
          <button type="button" data-remover="${i}">
            Remover
          </button>
        </li>
      `)}
    </ul>
  `);

  area.querySelectorAll('[data-remover]').forEach(botao => {
    botao.addEventListener('click', () => {
      itens.splice(Number(botao.dataset.remover), 1);
      atualizarItens();
    });
  });
}

area.querySelector('#adicionar-item').addEventListener('click', () => {

  if (!formPrescricao.reportValidity()) return;

  const dados = lerFormulario(formPrescricao);

  if (!dados.medicamento.trim()
      || !dados.dose.trim()
      || !dados.posologia.trim()) {
    avisar('Preencha todos os campos.');
    return;
  }

  itens.push({
    medicamento: dados.medicamento.trim(),
    dose: dados.dose.trim(),
    posologia: dados.posologia.trim()
  });

  atualizarItens();
  formPrescricao.reset();
});

const emitir = area.querySelector('#emitir-prescricao');

emitir.addEventListener('click', async () => {

  if (!itens.length) {
    avisar('Adicione pelo menos um medicamento.');
    return;
  }

  emitir.disabled = true;

  try {
    await api.prescrever(atendimento.id, itens);

    itens.length = 0;
    atualizarItens();
    formPrescricao.reset();

    avisar('Prescrição emitida com sucesso.');

    const prescricoes = await api.prescricoes(atendimento.id);

    desenhar(
      area.querySelector('#lista-prescricoes'),
      listaPrescricoes(prescricoes)
    );

  } catch (erro) {
    avisarErro(erro);
  } finally {
    emitir.disabled = false;
  }
});
  const formulario = area.querySelector('#form-atendimento');

  async function salvar() {
    return api.registrarAtendimento(atendimento.id, lerFormulario(formulario));
  }

  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    try {
      await salvar();
      avisar('Rascunho salvo.');
    } catch (erro) {
      avisarErro(erro);
    }
  });

  area.querySelector('#finalizar').addEventListener('click', async () => {
    try {
      await salvar();
      await api.finalizarAtendimento(atendimento.id);
      avisar('Atendimento finalizado.');
      location.hash = '#/painel';
    } catch (erro) {
      avisarErro(erro);
    }
  });
}
