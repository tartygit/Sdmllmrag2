package com.cth.sdm.application.service;

import com.cth.sdm.domain.model.DocumentVersion;
import java.io.InputStream;

public interface PreviewEngine {
    InputStream generatePreview(DocumentVersion version);
}
