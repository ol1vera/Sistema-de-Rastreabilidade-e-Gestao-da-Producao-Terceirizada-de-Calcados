/** Uma leitura contínua do mesmo código só é rearmada após ausência de 2,5 s.
 * A trava assíncrona também impede requisições simultâneas para códigos diferentes. */
export function criarTrava(agora = () => Date.now()) {
  let ocupado = false,
    ultimo = "",
    visto = 0;
  return {
    async executar(codigo, fn, manual = false) {
      const t = agora();
      const repetido = codigo === ultimo && t - visto < 2500;
      if (codigo === ultimo) visto = t;
      if (ocupado || (!manual && repetido)) return false;
      ocupado = true;
      ultimo = codigo;
      visto = t;
      try {
        await fn();
        return true;
      } finally {
        ocupado = false;
      }
    },
  };
}
