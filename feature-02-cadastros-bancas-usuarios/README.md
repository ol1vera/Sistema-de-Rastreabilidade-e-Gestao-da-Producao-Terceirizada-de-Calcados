# US02 — Cadastro de equipe, usuários e bancas

**Branch Git sugerida:** `feature/cadastros-bancas-usuarios`

Este pacote contém cópias dos arquivos existentes no projeto original para facilitar a identificação da funcionalidade. A pasta `arquivos/` mantém os caminhos relativos originais. Não substitua o repositório por estas pastas: no Git, cada feature deve ser trabalhada em uma branch separada.

## Arquivos incluídos

- `backend/src/main/java/br/edu/senai/rastreio/controller/CadastroController.java`
- `backend/src/main/java/br/edu/senai/rastreio/service/CadastroService.java`
- `backend/src/main/java/br/edu/senai/rastreio/entity/Usuario.java`
- `backend/src/main/java/br/edu/senai/rastreio/entity/Banca.java`
- `backend/src/main/java/br/edu/senai/rastreio/repository/UsuarioRepository.java`
- `backend/src/main/java/br/edu/senai/rastreio/repository/BancaRepository.java`
- `frontend/src/views/CadastroView.vue`
- `backend/src/main/java/br/edu/senai/rastreio/dto/Entradas.java`

## Próximos passos

1. Atualize `develop` antes de criar a branch.
2. Faça as alterações relacionadas somente a esta funcionalidade.
3. Execute os testes relevantes e registre os resultados.
4. Faça commit e abra um Pull Request para `develop`.
5. Não inclua `.env`, senhas ou dados reais nos commits.
