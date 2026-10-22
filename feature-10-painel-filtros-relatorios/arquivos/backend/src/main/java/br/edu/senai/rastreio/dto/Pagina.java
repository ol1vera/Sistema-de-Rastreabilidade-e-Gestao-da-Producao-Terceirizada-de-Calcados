package br.edu.senai.rastreio.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/** Contrato estável de paginação, independente da serialização interna do Spring. */
public record Pagina<T>(List<T> content, int number, int size, long totalElements, int totalPages) {
  public static <T> Pagina<T> de(Page<T> page) {
    return new Pagina<>(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }
}
