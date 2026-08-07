package com.cth.sdm.application.service;

import com.cth.sdm.domain.model.User;
import java.io.InputStream;

public interface IngestionHandler {
    void ingest(InputStream stream, String originalFileName, String contentType, User owner);
}
