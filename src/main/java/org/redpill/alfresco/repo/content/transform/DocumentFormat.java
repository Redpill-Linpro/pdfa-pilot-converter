package org.redpill.alfresco.repo.content.transform;

import java.util.HashMap;
import java.util.Map;

public class DocumentFormat {

  private String _name;
  private String _extension;
  private String _mediaType;
  private DocumentFamily _inputFamily;
  private Map<String, ?> _loadProperties;
  private Map<DocumentFamily, Map<String, ?>> _storePropertiesByFamily;

  public DocumentFormat() {
    // default
  }

  public DocumentFormat(String name, String extension, String mediaType) {
    _name = name;
    _extension = extension;
    _mediaType = mediaType;
  }

  public String getName() {
    return _name;
  }

  public void setName(String name) {
    _name = name;
  }

  public String getExtension() {
    return _extension;
  }

  public void setExtension(String extension) {
    _extension = extension;
  }

  public String getMediaType() {
    return _mediaType;
  }

  public void setMediaType(String mediaType) {
    _mediaType = mediaType;
  }

  public DocumentFamily getInputFamily() {
    return _inputFamily;
  }

  public void setInputFamily(DocumentFamily documentFamily) {
    _inputFamily = documentFamily;
  }

  public Map<String, ?> getLoadProperties() {
    return _loadProperties;
  }

  public void setLoadProperties(Map<String, ?> loadProperties) {
    _loadProperties = loadProperties;
  }

  public Map<DocumentFamily, Map<String, ?>> getStorePropertiesByFamily() {
    return _storePropertiesByFamily;
  }

  public void setStorePropertiesByFamily(Map<DocumentFamily, Map<String, ?>> storePropertiesByFamily) {
    _storePropertiesByFamily = storePropertiesByFamily;
  }

  public void setStoreProperties(DocumentFamily family, Map<String, ?> storeProperties) {
    if (_storePropertiesByFamily == null) {
      _storePropertiesByFamily = new HashMap<DocumentFamily, Map<String, ?>>();
    }

    _storePropertiesByFamily.put(family, storeProperties);
  }

  public Map<String, ?> getStoreProperties(DocumentFamily family) {
    if (_storePropertiesByFamily == null) {
      return null;
    }

    return _storePropertiesByFamily.get(family);
  }

}
