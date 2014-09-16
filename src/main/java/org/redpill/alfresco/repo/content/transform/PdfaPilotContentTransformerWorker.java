package org.redpill.alfresco.repo.content.transform;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.content.transform.ContentTransformerHelper;
import org.alfresco.repo.content.transform.ContentTransformerWorker;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.alfresco.util.TempFileProvider;
import org.alfresco.util.exec.RuntimeExec;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.telnet.TelnetClient;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

public class PdfaPilotContentTransformerWorker extends ContentTransformerHelper implements ContentTransformerWorker, InitializingBean {

  private static final Logger LOG = Logger.getLogger(PdfaPilotContentTransformerWorker.class);

  /**
   * source variable name
   */
  private static final String VAR_SOURCE = "source";

  /**
   * target variable name
   */
  private static final String VAR_TARGET = "target";

  private static final String KEY_OPTIONS = "options";

  private String _endpointHost;

  private int _endpointPort;

  private String _pdfaPilotExe;

  private String _versionString;

  private boolean _available;

  private MimetypeService _mimetypeService;

  private DocumentFormatRegistry _documentFormatRegistry;

  private boolean _enabled;

  @Override
  public void setMimetypeService(MimetypeService mimetypeService) {
    _mimetypeService = mimetypeService;
  }

  public void setDocumentFormatRegistry(DocumentFormatRegistry documentFormatRegistry) {
    _documentFormatRegistry = documentFormatRegistry;
  }

  public void setEnabled(boolean enabled) {
    _enabled = enabled;

    if (!_enabled) {
      _available = false;
    }
  }

  /**
   * the system command executer
   */
  private RuntimeExec _executer;

  /**
   * the check command executer
   */
  private RuntimeExec _checkCommand;

  public void setExecuter(RuntimeExec executer) {
    _executer = executer;
  }

  public void setCheckCommand(RuntimeExec checkCommand) {
    _checkCommand = checkCommand;
  }

  public void setEndpointHost(String endpointHost) {
    _endpointHost = endpointHost;
  }

  public void setEndpointPort(int endpointPort) {
    _endpointPort = endpointPort;
  }

  public void setPdfaPilotExe(String pdfaPilotExe) {
    _pdfaPilotExe = pdfaPilotExe;
  }

  @Override
  public boolean isAvailable() {
    _available = pingServer();

    return _available;
  }

  @Override
  public String getVersionString() {
    return _versionString;
  }

  @Override
  public boolean isTransformable(String sourceMimetype, String targetMimetype, TransformationOptions options) {
    if (!isAvailable()) {
      return false;
    }

    String sourceExtension = _mimetypeService.getExtension(sourceMimetype);

    String targetExtension = _mimetypeService.getExtension(targetMimetype);

    // query the registry for the source format
    DocumentFormat sourceFormat = _documentFormatRegistry.getFormatByExtension(sourceExtension);

    if (sourceFormat == null) {
      // no document format
      return false;
    }

    // query the registry for the target format
    DocumentFormat targetFormat = _documentFormatRegistry.getFormatByExtension(targetExtension);

    if (targetFormat == null) {
      // no document format
      return false;
    }

    Map<String, ?> properties = targetFormat.getStoreProperties(sourceFormat.getInputFamily());

    return properties != null ? properties.size() > 0 : false;
  }

  @Override
  public void transform(final ContentReader reader, final ContentWriter writer, final TransformationOptions options) throws Exception {
    if (!isAvailable()) {
      throw new ContentIOException("Content conversion failed (unavailable): \n" + "   reader: " + reader + "\n" + "   writer: " + writer);
    }

    // get mimetypes
    String sourceMimetype = getMimetype(reader);

    String targetMimetype = getMimetype(writer);

    // get the extensions to use
    String sourceExtension = _mimetypeService.getExtension(sourceMimetype);

    String targetExtension = _mimetypeService.getExtension(targetMimetype);

    if (sourceExtension == null || targetExtension == null) {
      throw new AlfrescoRuntimeException("Unknown extensions for mimetypes: \n" + "   source mimetype: " + sourceMimetype + "\n" + "   source extension: " + sourceExtension + "\n"
          + "   target mimetype: " + targetMimetype + "\n" + "   target extension: " + targetExtension);
    }

    // create required temp files - PPCTW = PdfaPilotContentTransformerWorker -
    // good with short filename if pdfaPilot is on Windows machine with limited
    // hierarchy
    File sourceFile = TempFileProvider.createTempFile("PPCTW_source_", "." + sourceExtension);

    File targetFile = TempFileProvider.createTempFile("PPCTW_target_", "." + targetExtension);

    // pdfaPilot adds _PDFA to the final name, thereof this one here
    File finalTargetFile = new File(FilenameUtils.getFullPath(targetFile.getAbsolutePath()) + FilenameUtils.getBaseName(targetFile.getName()) + "_PDFA" + "." + targetExtension);

    // pull reader file into source temp file
    reader.getContent(sourceFile);

    // transformDoc the source temp file to the target temp file
    transformInternal(sourceFile, targetFile, finalTargetFile, options);

    // upload the output document
    if (finalTargetFile.exists() && finalTargetFile.length() > 0) {
      writer.putContent(finalTargetFile);
    } else if (targetFile.exists() && targetFile.length() > 0) {
      writer.putContent(targetFile);
    } else {
      boolean failSilently = isFailSilently(options);

      String message = "pdfaPilot transformation failed to write output file";

      if (failSilently) {
        LOG.warn(message);
      } else {
        throw new ContentIOException(message);
      }
    }

    // done
    if (LOG.isDebugEnabled()) {
      LOG.debug("Transformation completed: \n" + "   source: " + reader + "\n" + "   target: " + writer + "\n" + "   options: " + options);
    }
  }

  protected void transformInternal(File sourceFile, File targetFile, File finalTargetFile, TransformationOptions options) throws Exception {
    Map<String, String> properties = new HashMap<String, String>(5);

    String commandOptions = "";

    boolean failSilently = isFailSilently(options);

    // set properties
    if (options instanceof PdfaPilotTransformationOptions) {
      PdfaPilotTransformationOptions pdfaPilotOptions = (PdfaPilotTransformationOptions) options;

      if (StringUtils.isNotBlank(pdfaPilotOptions.getLevel())) {
        commandOptions = "--level=" + pdfaPilotOptions.getLevel();
      }

      if (pdfaPilotOptions.isOptimize()) {
        commandOptions += " --optimizepdf";
      }
    } else {
      commandOptions += " --optimizepdf";
    }

    properties.put(KEY_OPTIONS, commandOptions);
    properties.put(VAR_SOURCE, sourceFile.getAbsolutePath());
    properties.put(VAR_TARGET, targetFile.getAbsolutePath());

    // execute the statement
    long timeoutMs = options.getTimeoutMs();

    RuntimeExec.ExecutionResult result = _executer.execute(properties, timeoutMs);

    // everything from pdfaPilot that's equal to or above 100 is an error
    if (result.getExitValue() >= 100) {
      targetFile.delete();
      finalTargetFile.delete();

      String message = "Failed to perform pdfaPilot transformation: \n" + result;

      if (failSilently) {
        LOG.warn(message);
      } else {
        throw new ContentIOException(message);
      }
    }

    // success
    if (LOG.isDebugEnabled()) {
      LOG.debug("pdfaPilot executed successfully: \n" + _executer);
    }
  }

  private boolean isFailSilently(TransformationOptions options) {
    boolean failSilently = false;

    if (options instanceof PdfaPilotTransformationOptions) {
      PdfaPilotTransformationOptions pdfaPilotOptions = (PdfaPilotTransformationOptions) options;

      failSilently = pdfaPilotOptions.isFailSilently();
    }

    return failSilently;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    // available defaults to false, changes after afterPropertiesSet is done
    _available = false;

    // if the subsystem is not enabled, just exit
    if (!_enabled) {
      return;
    }

    if (_executer == null) {
      throw new AlfrescoRuntimeException("System runtime executer not set");
    }

    Assert.hasText(_endpointHost);
    Assert.isTrue(_endpointPort > 0);
    Assert.hasText(_pdfaPilotExe);
    Assert.notNull(_mimetypeService, "MimetypeService must be set");
    Assert.notNull(_documentFormatRegistry, "DocumentFormatRegistry must be set");

    try {
      // On some platforms / versions, the -version command seems to return an
      // error code whilst still
      // returning output, so let's not worry about the exit code!
      RuntimeExec.ExecutionResult result = _checkCommand.execute();

      _versionString = result.getStdOut().trim();

      if (!pingServer()) {
        throw new RuntimeException("Could not find any pdfaPilot servers on " + _endpointHost + ":" + _endpointPort);
      }
    } catch (Throwable e) {
      LOG.error(getClass().getSimpleName() + " not available: " + (e.getMessage() != null ? e.getMessage() : ""));

      // debug so that we can trace the issue if required
      LOG.debug(e);
    }

    _available = true;
  }

  public boolean pingServer() {
    boolean result;

    TelnetClient telnetClient = new TelnetClient();

    try {
      telnetClient.setDefaultTimeout(1000);

      telnetClient.setConnectTimeout(1000);

      telnetClient.connect(_endpointHost, _endpointPort);

      result = true;
    } catch (final Exception ex) {
      result = false;
    } finally {
      try {
        telnetClient.disconnect();
      } catch (IOException e) {
        LOG.error(e.getMessage(), e);
      }
    }

    return result;
  }

}
