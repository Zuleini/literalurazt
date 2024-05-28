package com.aluracursos.literalurazt.dto;

import com.aluracursos.literalurazt.model.Autor;

public record LibroDTO(
        Long Id,
        String titulo,
        Autor autor,
        String idioma,
        Double descargas) {
}
