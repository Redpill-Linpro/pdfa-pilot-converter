package org.redpill.alfresco.repo.content.transform;

import java.util.Set;

public interface DocumentFormatRegistry {

  public DocumentFormat getFormatByExtension(String extension);

  public DocumentFormat getFormatByMediaType(String mediaType);

  public Set<DocumentFormat> getOutputFormats(DocumentFamily family);

}
