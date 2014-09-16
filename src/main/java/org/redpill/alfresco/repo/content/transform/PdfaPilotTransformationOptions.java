package org.redpill.alfresco.repo.content.transform;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.alfresco.service.namespace.QName;

public class PdfaPilotTransformationOptions extends TransformationOptions {

  public static final String PDFA_LEVEL_3B = "3b";
  public static final String PDFA_LEVEL_3U = "3u";
  public static final String PDFA_LEVEL_3A = "3a";
  public static final String PDFA_LEVEL_2B = "2b";
  public static final String PDFA_LEVEL_2U = "2u";
  public static final String PDFA_LEVEL_2A = "2a";
  public static final String PDFA_LEVEL_1B = "1b";
  public static final String PDFA_LEVEL_1A = "1a";
  public static final String PDFA_LEVEL_NONE = "";

  private String _level = PDFA_LEVEL_NONE;

  private boolean _optimize = true;

  private boolean _failSilently = false;

  /**
   * Default constructor
   */
  public PdfaPilotTransformationOptions() {
  }

  /**
   * Constructor
   *
   * @param sourceNodeRef
   *          the source node reference
   * @param sourceContentProperty
   *          the source content property
   * @param targetNodeRef
   *          the target node reference
   * @param targetContentProperty
   *          the target content property
   */
  public PdfaPilotTransformationOptions(NodeRef sourceNodeRef, QName sourceContentProperty, NodeRef targetNodeRef, QName targetContentProperty) {
    super(sourceNodeRef, sourceContentProperty, targetNodeRef, targetContentProperty);
  }

  /**
   * Constructor
   *
   * @param sourceNodeRef
   *          the source node reference
   * @param targetNodeRef
   *          the target node reference
   */
  public PdfaPilotTransformationOptions(NodeRef sourceNodeRef, NodeRef targetNodeRef) {
    super(sourceNodeRef, null, targetNodeRef, null);
  }

  public void setLevel(String level) {
    _level = level;
  }

  public String getLevel() {
    return _level;
  }

  public void setOptimize(boolean optimize) {
    _optimize = optimize;
  }

  public boolean isOptimize() {
    return _optimize;
  }

  public void setFailSilently(boolean failSilently) {
    _failSilently = failSilently;
  }

  public boolean isFailSilently() {
    return _failSilently;
  }

}
