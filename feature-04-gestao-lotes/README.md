# US04 — Criar, editar, cancelar e listar lotes

**Branch Git sugerida:** `feature/gestao-lotes`

Este pacote contém cópias dos arquivos existentes no projeto original para facilitar a identificação da funcionalidade. A pasta `arquivos/` mantém os caminhos relativos originais. Não substitua o repositório por estas pastas: no Git, cada feature deve ser trabalhada em uma branch separada.

## Arquivos incluídos

- `backend/src/main/java/br/edu/senai/rastreio/entity/Lote.java`
- `backend/src/main/java/br/edu/senai/rastreio/controller/LoteController.java`
- `backend/src/main/java/br/edu/senai/rastreio/service/LoteService.java`
- `backend/src/main/java/br/edu/senai/rastreio/repository/LoteRepository.java`
- `backend/src/main/java/br/edu/senai/rastreio/dto/Entradas.java`
- `frontend/src/views/LotesView.vue`
- `frontend/src/views/LoteFormView.vue`
- `frontend/src/views/LoteDetailView.vue`
- `frontend/src/components/Filtros.vue`
- `backend/src/test/java/br/edu/senai/rastreio/FluxoBase.java`

## Próximos passos

1. Atualize `develop` antes de criar a branch.
2. Faça as alterações relacionadas somente a esta funcionalidade.
3. Execute os testes relevantes e registre os resultados.
4. Faça commit e abra um Pull Request para `develop`.
5. Não inclua `.env`, senhas ou dados reais nos commits.
