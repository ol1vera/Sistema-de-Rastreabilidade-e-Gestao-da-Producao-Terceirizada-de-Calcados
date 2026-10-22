# US10 — Painel, filtros, indicadores e paginação

**Branch Git sugerida:** `feature/painel-filtros-relatorios`

Este pacote contém cópias dos arquivos existentes no projeto original para facilitar a identificação da funcionalidade. A pasta `arquivos/` mantém os caminhos relativos originais. Não substitua o repositório por estas pastas: no Git, cada feature deve ser trabalhada em uma branch separada.

## Arquivos incluídos

- `backend/src/main/java/br/edu/senai/rastreio/service/PainelService.java`
- `backend/src/main/java/br/edu/senai/rastreio/controller/PainelController.java`
- `backend/src/main/java/br/edu/senai/rastreio/dto/Pagina.java`
- `frontend/src/views/DashboardView.vue`
- `frontend/src/components/Filtros.vue`
- `frontend/src/components/Grafico.vue`
- `frontend/src/components/Pagination.vue`
- `frontend/src/api.js`
- `backend/src/test/java/br/edu/senai/rastreio/FluxoBase.java`

## Próximos passos

1. Atualize `develop` antes de criar a branch.
2. Faça as alterações relacionadas somente a esta funcionalidade.
3. Execute os testes relevantes e registre os resultados.
4. Faça commit e abra um Pull Request para `develop`.
5. Não inclua `.env`, senhas ou dados reais nos commits.
