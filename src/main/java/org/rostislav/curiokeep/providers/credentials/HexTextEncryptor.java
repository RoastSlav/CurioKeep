package org.rostislav.curiokeep.providers.credentials;

import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/** Stores the output of a {@link BytesEncryptor} as a hex string so it fits a text column. */
final class HexTextEncryptor implements TextEncryptor {

    private final BytesEncryptor delegate;

    HexTextEncryptor(BytesEncryptor delegate) {
        this.delegate = delegate;
    }

    @Override
    public String encrypt(String text) {
        return HexFormat.of().formatHex(delegate.encrypt(text.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public String decrypt(String encryptedText) {
        return new String(delegate.decrypt(HexFormat.of().parseHex(encryptedText)), StandardCharsets.UTF_8);
    }
}
