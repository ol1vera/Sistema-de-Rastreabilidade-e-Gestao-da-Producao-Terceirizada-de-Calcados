<script setup>
import { ref, computed, onMounted } from "vue";
import { useRoute } from "vue-router";
import { api, catalogo } from "../api";
import { useTask } from "../composables/useTask";
import { moeda, perfis } from "../utils";
import Feedback from "../components/Feedback.vue";
import Pagination from "../components/Pagination.vue";
const tipo = useRoute().params.tipo,
  permitidos = ["bancas", "servicos", "usuarios"],
  titulo = {
    bancas: "Bancas terceirizadas",
    servicos: "Serviços terceirizados",
    usuarios: "Usuários",
  }[tipo],
  lista = ref(null),
  page = ref(0),
  busca = ref(""),
  bancas = ref([]),
  form = ref(null),
  id = ref(null);
const { busy, erro, sucesso, campos, run } = useTask();
const valido = computed(() => permitidos.includes(tipo));
async function atualizar() {
  lista.value = (
    await api.get("/" + tipo, {
      params: { page: page.value, size: 20, busca: busca.value },
    })
  ).data;
}
function carregar(p = 0) {
  page.value = p;
  return run(atualizar);
}
onMounted(() =>
  run(async () => {
    if (!valido.value) {
      erro.value = "Cadastro inexistente.";
      return;
    }
    await atualizar();
    if (tipo === "usuarios") bancas.value = await catalogo("bancas");
  }),
);
function editar(item = null) {
  id.value = item?.id || null;
  form.value =
    tipo === "bancas"
      ? {
          nome: "",
          documento: "",
          responsavel: "",
          telefone: "",
          ativo: true,
          ...item,
        }
      : tipo === "servicos"
        ? { nome: "", descricao: "", valorPar: "", ativo: true, ...item }
        : {
            nome: item?.nome || "",
            email: item?.email || "",
            senha: "",
            perfil: item?.perfil || "GESTOR",
            bancaId: item?.banca?.id || "",
            ativo: item?.ativo ?? true,
          };
}
function salvar() {
  run(async () => {
    const body = { ...form.value };
    if (tipo === "usuarios")
      body.bancaId =
        body.perfil === "OPERADOR_BANCA" ? Number(body.bancaId) : null;
    if (tipo === "servicos") body.valorPar = Number(body.valorPar);
    if (id.value) await api.put("/" + tipo + "/" + id.value, body);
    else await api.post("/" + tipo, body);
    form.value = null;
    await atualizar();
    sucesso.value = "Cadastro salvo com sucesso.";
  });
}
</script>
<template>
  <div class="page-head">
    <div>
      <span class="eyebrow">ADMINISTRAÇÃO</span>
      <h1>{{ titulo || "Cadastro não encontrado" }}</h1>
      <p>Cadastros vinculados ao histórico são preservados.</p>
    </div>
    <button v-if="valido" @click="editar()">+ Novo cadastro</button>
  </div>
  <Feedback :busy="busy" :erro="erro" :sucesso="sucesso" :campos="campos" />
  <section class="panel" v-if="form">
    <h3>{{ id ? "Editar cadastro" : "Novo cadastro" }}</h3>
    <form @submit.prevent="salvar">
      <div class="form-grid">
        <label>Nome<input v-model="form.nome" required maxlength="120" /></label
        ><template v-if="tipo === 'bancas'"
          ><label
            >CPF ou CNPJ<input
              v-model="form.documento"
              required
              maxlength="20" /></label
          ><label
            >Responsável<input
              v-model="form.responsavel"
              required
              maxlength="120" /></label
          ><label
            >Telefone com DDD<input
              v-model="form.telefone"
              required
              maxlength="20"
              type="tel" /></label></template
        ><template v-if="tipo === 'servicos'"
          ><label
            >Valor por par (R$)<input
              v-model.number="form.valorPar"
              type="number"
              required
              min="0.01"
              max="9999999999.99"
              step="0.01" /></label
          ><label class="span2"
            >Descrição<textarea
              v-model="form.descricao"
              maxlength="500"
            ></textarea>
          </label>
          <p class="muted span2">
            Alterações de preço valem apenas para novos lotes.
          </p></template
        ><template v-if="tipo === 'usuarios'"
          ><label
            >E-mail<input
              v-model="form.email"
              type="email"
              required
              maxlength="180" /></label
          ><label
            >{{ id ? "Nova senha (deixe vazio para manter)" : "Senha inicial"
            }}<input
              v-model="form.senha"
              type="password"
              :required="!id"
              minlength="10"
              maxlength="72"
              autocomplete="new-password" /></label
          ><label
            >Perfil<select v-model="form.perfil">
              <option v-for="(n, p) in perfis" :key="p" :value="p">
                {{ n }}
              </option>
            </select></label
          ><label v-if="form.perfil === 'OPERADOR_BANCA'"
            >Banca<select v-model="form.bancaId" required>
              <option value="" disabled>Selecione</option>
              <option
                v-for="b in bancas.filter((x) => x.ativo)"
                :value="b.id"
                :key="b.id"
              >
                {{ b.nome }}
              </option>
            </select></label
          ></template
        ><label class="check"
          ><input v-model="form.ativo" type="checkbox" /> Cadastro ativo</label
        >
      </div>
      <div class="actions">
        <button :disabled="busy">Salvar cadastro</button
        ><button type="button" class="secondary" @click="form = null">
          Fechar
        </button>
      </div>
    </form>
  </section>
  <section v-if="valido" class="panel">
    <form class="filters" @submit.prevent="carregar()">
      <label
        >Buscar por nome<input
          v-model="busca"
          placeholder="Nome do cadastro" /></label
      ><button class="secondary">Buscar</button>
    </form>
    <template v-if="lista"
      ><p class="empty" v-if="!lista.content.length">
        Nenhum cadastro encontrado.
      </p>
      <div class="table-wrap" v-else>
        <table>
          <thead>
            <tr>
              <th>Nome</th>
              <th>
                {{
                  tipo === "bancas"
                    ? "Documento"
                    : tipo === "servicos"
                      ? "Valor / par"
                      : "E-mail / perfil"
                }}
              </th>
              <th>Situação</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in lista.content" :key="r.id">
              <td>
                {{ r.nome
                }}<small v-if="tipo === 'bancas'"
                  >{{ r.responsavel }} · {{ r.telefone }}</small
                >
              </td>
              <td>
                {{
                  tipo === "bancas"
                    ? r.documento
                    : tipo === "servicos"
                      ? moeda(r.valorPar)
                      : r.email
                }}<small v-if="tipo === 'usuarios'"
                  >{{ perfis[r.perfil] }}
                  {{ r.banca ? "· " + r.banca.nome : "" }}</small
                >
              </td>
              <td>
                <span :class="['badge', r.ativo ? 'CONCLUIDO' : 'CANCELADO']">{{
                  r.ativo ? "Ativo" : "Inativo"
                }}</span>
              </td>
              <td>
                <button class="secondary" @click="editar(r)">Editar</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <Pagination
        :page="page"
        :pages="lista.totalPages"
        :total="lista.totalElements"
        @change="carregar"
    /></template>
  </section>
</template>
