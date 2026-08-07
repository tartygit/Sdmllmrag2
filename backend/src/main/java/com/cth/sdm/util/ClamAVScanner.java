package com.cth.sdm.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@Component
public class ClamAVScanner {

    private final String host;
    private final int port;
    private final boolean enabled;

    public ClamAVScanner(
            @Value("${app.clamav.host:localhost}") String host,
            @Value("${app.clamav.port:3310}") int port,
            @Value("${app.clamav.enabled:false}") boolean enabled) {
        this.host = host;
        this.port = port;
        this.enabled = enabled;
    }

    public boolean isSafe(String filePath) {
        if (!enabled) {
            return true;
        }
        try {
            File file = new File(filePath);
            try (InputStream is = new FileInputStream(file)) {
                fi.solita.clamav.ClamAVClient client = new fi.solita.clamav.ClamAVClient(host, port);
                byte[] response = client.scan(is);
                return fi.solita.clamav.ClamAVClient.isCleanReply(response);
            }
        } catch (Exception e) {
            return false;
        }
    }
}
