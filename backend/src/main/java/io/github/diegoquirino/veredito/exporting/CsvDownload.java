package io.github.diegoquirino.veredito.exporting;

/** A generated CSV file: the name for {@code Content-Disposition} and the UTF-8 bytes (no BOM). */
public record CsvDownload(String filename, byte[] content) {}