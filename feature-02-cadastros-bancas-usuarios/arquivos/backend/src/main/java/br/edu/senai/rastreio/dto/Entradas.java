package br.edu.senai.rastreio.dto;

import br.edu.senai.rastreio.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** DTOs impedem que o cliente envie status, preço contratado ou totais calculados. */
public class Entradas {
  public record Login(@NotBlank @Email String email, @NotBlank @Size(max = 72) String senha) {}

  public record BancaInput(
      @NotBlank @Size(max = 120) String nome,
      @NotBlank @Size(max = 20) String documento,
      @NotBlank @Size(max = 120) String responsavel,
      @NotBlank @Size(max = 20) String telefone,
      boolean ativo) {}

  public record ServicoInput(
      @NotBlank @Size(max = 120) String nome,
      @Size(max = 500) String descricao,
      @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal valorPar,
      boolean ativo) {}

  public record UsuarioInput(
      @NotBlank @Size(max = 120) String nome,
      @NotBlank @Email @Size(max = 180) String email,
      @Size(max = 72) String senha,
      @NotNull Perfil perfil,
      Long bancaId,
      boolean ativo) {}

  public record LoteInput(
      @NotBlank @Size(max = 100) String programacao,
      @NotBlank @Size(max = 120) String modelo,
      @NotBlank @Size(max = 80) String cor,
      @NotNull Long bancaId,
      @NotNull Long servicoId,
      @NotNull @FutureOrPresent LocalDate prazo,
      @NotEmpty @Size(max = 100)
          Map<@NotBlank @Size(max = 20) String, @NotNull @Min(0) @Max(1000000) Integer> grade) {}

  public record Cancelamento(@NotBlank @Size(max = 500) String motivo) {}

  public record PerdaInput(
      @NotNull TipoPerda tipo,
      @Min(1) @Max(100000000) int quantidade,
      @NotBlank @Size(max = 500) String observacao) {}

  public record Leitura(@NotBlank @Size(max = 50) String codigo) {}
}
