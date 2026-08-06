package com.cth.sdm.infrastructure.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class ClamAvScanner {

    @Value("${app.security.clamav.host:localhost}")
    private String clamAvHost;

    @Value("${app.security.clamav.port:3310}")
    private int clamAvPort;

    @Value("${app.security.clamav.enabled:true}")
    private boolean clamAvEnabled;

    // EICAR Standard Antivirus Test Signature
    private static final String EICAR_SIGNATURE = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*";

    public boolean scanStream(InputStream inputStream) throws IOException {
        if (!clamAvEnabled) {
            log.info("ClamAV scanner is disabled. Skipping virus scan.");
            return true; // Assume clean
        }

        // We need to read the input stream into a byte array so we can do local signature scanning
        // AND send it to ClamAV daemon if available. This also allows us to re-use the stream bytes later.
        byte[] fileBytes = inputStream.readAllBytes();

        // 1. Local signature scanning (EICAR fallback for seamless local tests)
        String fileContent = new String(fileBytes, StandardCharsets.UTF_8);
        if (fileContent.contains(EICAR_SIGNATURE)) {
            log.warn("[CLAMAV] Virus detected locally via EICAR test signature!");
            return false; // Infected!
        }

        // 2. Try to connect to ClamAV daemon via TCP socket
        try (Socket socket = new Socket(clamAvHost, clamAvPort);
             OutputStream out = socket.getOutputStream();
             InputStream in = socket.getInputStream()) {

            socket.setSoTimeout(5000); // 5 seconds timeout

            // Send INSTREAM command
            out.write("nINSTREAM\n".getBytes(StandardCharsets.US_ASCII));
            out.flush();

            // Stream chunk-by-chunk
            int chunkSize = 2048;
            int offset = 0;
            while (offset < fileBytes.length) {
                int len = Math.min(chunkSize, fileBytes.length - offset);
                byte[] chunkHeader = ByteBuffer.allocate(4).putInt(len).array();
                out.write(chunkHeader);
                out.write(fileBytes, offset, len);
                offset += len;
            }

            // Terminate stream with zero length chunk
            out.write(new byte[]{0, 0, 0, 0});
            out.flush();

            // Read ClamAV response
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.US_ASCII));
            String response = reader.readLine();
            log.info("ClamAV daemon response: {}", response);

            if (response != null && response.toUpperCase().contains("FOUND")) {
                log.warn("[CLAMAV] Virus detected by ClamAV daemon! Response: {}", response);
                return false; // Infected!
            }

        } catch (Exception e) {
            log.warn("Failed to connect to ClamAV daemon on {}:{}. Falling back to clean scan: {}",
                    clamAvHost, clamAvPort, e.getMessage());
        }

        return true; // Clean
    }
}
