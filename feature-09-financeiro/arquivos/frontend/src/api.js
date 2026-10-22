import axios from "axios";
// Uma única origem (/api) evita CORS aberto. O proxy do Nginx encaminha à API Java.
export const api = axios.create({ baseURL: "/api", timeout: 15000 });
export function parametros(obj) {
  return Object.fromEntries(
    Object.entries(obj).filter(
      ([, v]) => v !== "" && v !== null && v !== undefined,
    ),
  );
}
/** Catálogos usados em selects são carregados por páginas, sem truncar em 100 itens. */
export async function catalogo(nome) {
  let page = 0,
    resultado = [];
  while (true) {
    const { data } = await api.get("/" + nome, { params: { page, size: 100 } });
    resultado.push(...data.content);
    if (++page >= data.totalPages) break;
  }
  return resultado;
}
