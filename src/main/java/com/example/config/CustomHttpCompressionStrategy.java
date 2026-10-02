package com.example.config;

import io.micronaut.context.annotation.Primary;
import io.micronaut.http.server.netty.HttpCompressionStrategy;
import io.netty.handler.codec.http.HttpResponse;
import jakarta.inject.Singleton;

@Primary
@Singleton
public class CustomHttpCompressionStrategy implements HttpCompressionStrategy {

    /**
     * Retorna {@code false} para desabilitar completamente a compressão.
     * Isso impede a inicialização do Zstd e evita o UnsatisfiedLinkError
     * na imagem nativa.
     */
    @Override
    public boolean shouldCompress(HttpResponse response) {
        return false;
    }

    /**
     * Retorna {@code false} para que a estratégia seja considerada desabilitada.
     * Quando a estratégia está desabilitada, o Micronaut não tenta aplicar
     * nenhum tipo de compressão.
     */
    @Override
    public boolean isEnabled() {
        return false;
    }

    /**
     * Retorna o valor padrão de 32 MB (33554432 bytes).
     * Este valor não é utilizado, pois a compressão está desabilitada,
     * mas precisa ser retornado para satisfazer o contrato da interface.
     */
    @Override
    public int getMaxZstdEncodeSize() {
        return 33554432; // 32 MB, valor padrão do Micronaut
    }
}