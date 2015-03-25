package org.redpill.alfresco.repo.content.transform;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.filestore.FileContentReader;
import org.alfresco.repo.content.metadata.MetadataExtracter;
import org.alfresco.repo.content.metadata.MetadataExtracterRegistry;
import org.alfresco.repo.content.transform.ContentTransformerHelper;
import org.alfresco.repo.content.transform.ContentTransformerWorker;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.MD5;
import org.alfresco.util.TempFileProvider;
import org.alfresco.util.exec.RuntimeExec;
import org.alfresco.util.exec.RuntimeExec.ExecutionResult;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.telnet.TelnetClient;
import org.apache.log4j.Logger;
import org.redpill.alfresco.module.metadatawriter.factories.MetadataContentFactory;
import org.redpill.alfresco.module.metadatawriter.factories.UnsupportedMimetypeException;
import org.redpill.alfresco.module.metadatawriter.services.ContentFacade;
import org.redpill.alfresco.module.metadatawriter.services.ContentFacade.ContentException;
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

  private NodeService _nodeService;

  private MetadataContentFactory _metadataContentFactory;

  private MetadataExtracterRegistry _metadataExtracterRegistry;

  private boolean _enabled;

  private boolean _debug = false;

  @Override
  public void setMimetypeService(MimetypeService mimetypeService) {
    _mimetypeService = mimetypeService;
  }

  public void setDocumentFormatRegistry(DocumentFormatRegistry documentFormatRegistry) {
    _documentFormatRegistry = documentFormatRegistry;
  }

  public void setNodeService(NodeService nodeService) {
    _nodeService = nodeService;
  }

  public void setEnabled(boolean enabled) {
    _enabled = enabled;

    if (!_enabled) {
      _available = false;
    }
  }

  public void setDebug(boolean debug) {
    _debug = debug;
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
    if (_debug) {
      return true;
    }

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

    // get mime types
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
    File sourceFile = getTempFromFile(options.getSourceNodeRef(), sourceExtension);

    if (LOG.isDebugEnabled()) {
      LOG.debug("Setting source file to " + sourceFile);
    }

    File targetFile = TempFileProvider.createTempFile("PPCTW_target_", "." + targetExtension);

    // pdfaPilot adds _PDFA to the final name, thereof this one here
    File finalTargetFile = new File(FilenameUtils.getFullPath(targetFile.getAbsolutePath()) + FilenameUtils.getBaseName(targetFile.getName()) + "_PDFA" + "." + targetExtension);

    // pull reader file into source temp file
    reader.getContent(sourceFile);

    // write a unique hash of the nodeRef to the document to be converted
    // String title = changeMetadataTitle(sourceFile, options.getSourceNodeRef(), sourceMimetype);

    // transformDoc the source temp file to the target temp file
    transformInternal(sourceFile, targetFile, finalTargetFile, options);

    // if (title != null) {
    //  verifyMetadata(options.getSourceNodeRef(), targetFile.length() > 0 && targetFile.exists() ? targetFile : finalTargetFile, title);
    // }

    // upload the output document
    if (finalTargetFile.exists() && finalTargetFile.length() > 0) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Writing content from " + finalTargetFile.getAbsolutePath());
      }

      writer.putContent(finalTargetFile);

      targetFile.delete();
      finalTargetFile.delete();
    } else if (targetFile.exists() && targetFile.length() > 0) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Writing content from " + targetFile.getAbsolutePath());
      }

      writer.putContent(targetFile);

      targetFile.delete();
      finalTargetFile.delete();
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

  /**
   * Extracts the metadata title from the file. If no title found it returns an
   * empty string (not null).
   * 
   * @param file
   * @param mimetype
   * @return the title or an empty string
   */
  private String extractMetadataTitle(File file, String mimetype) {
    MetadataExtracter extracter = _metadataExtracterRegistry.getExtracter(mimetype);

    Map<QName, Serializable> properties = new HashMap<QName, Serializable>();

    FileContentReader contentReader = new FileContentReader(file);
    contentReader.setMimetype(mimetype);

    properties = extracter.extract(contentReader, properties);

    String title = DefaultTypeConverter.INSTANCE.convert(String.class, properties.get(ContentModel.PROP_TITLE));

    if (LOG.isDebugEnabled()) {
      LOG.debug("Extracted title '" + title + "' from file '" + file.getAbsolutePath() + "'");
    }

    return StringUtils.isNotBlank(title) ? title : "";
  }

  private void verifyMetadata(NodeRef node, File file, String title) throws UnsupportedMimetypeException {
    String extractedHash = extractMetadataTitle(file, "application/pdf");

    String hash = MD5.Digest(node.toString().getBytes());

    if (!hash.equals(extractedHash)) {
      throw new RuntimeException("The converted file for nodeRef '" + node + "' is not the same as the source file!");
    }

    // write back the original title
    writeMetadataTitle(file, node, "application/pdf", title);
  }

  /**
   * Extracts the title from the document and returns it if found, otherwise
   * returns null. Writes a hash of the nodeRef as a new title.
   * 
   * @param file
   * @param node
   * @param mimetype
   * @return the title or null if no title found or the hash couldn't be written.
   */
  private String changeMetadataTitle(File file, NodeRef node, String mimetype) {
    String title = null;

    try {
      title = extractMetadataTitle(file, mimetype);

      String hash = MD5.Digest(node.toString().getBytes());

      writeMetadataTitle(file, node, mimetype, hash);
    } catch (UnsupportedMimetypeException ex) {
      if (LOG.isTraceEnabled()) {
        LOG.trace(ex.getMessage(), ex);
      }

      LOG.error(ex.getMessage());
      
      title = null;
    }

    return title;
  }

  /**
   * Writes a metadata title to a document that supports it. Throws an exception
   * if title can't be written.
   * 
   * @param file
   * @param node
   * @param mimetype
   * @param title
   * @throws UnsupportedMimetypeException
   */
  private void writeMetadataTitle(File file, NodeRef node, String mimetype, String title) throws UnsupportedMimetypeException {
    InputStream inputStream = null;
    OutputStream outputStream = null;

    File tempFile = getTempFromFile(node, FilenameUtils.getExtension(file.getName()));

    try {
      inputStream = new FileInputStream(file);
      outputStream = new FileOutputStream(tempFile);

      ContentFacade contentFacade = _metadataContentFactory.createContent(inputStream, outputStream, mimetype);

      // write the nodeRef to the Title field and include the old title
      contentFacade.writeMetadata("Title", title);

      contentFacade.save();

      FileUtils.copyFile(tempFile, file);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    } catch (ContentException ex) {
      throw new RuntimeException(ex);
    } finally {
      IOUtils.closeQuietly(inputStream);
      IOUtils.closeQuietly(outputStream);
      tempFile.delete();
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

    if (LOG.isDebugEnabled()) {
      LOG.debug(StringUtils.join(_executer.getCommand(properties), " "));
    }

    ExecutionResult result = _executer.execute(properties, timeoutMs);

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

  private File getTempFromFile(NodeRef nodeRef, String extension) {
    if (nodeRef == null || !_nodeService.exists(nodeRef)) {
      return TempFileProvider.createTempFile("PPCTW_", "." + extension);
    }

    try {
      final File systemTempDir = TempFileProvider.getTempDir();

      final File tempDir = new File(systemTempDir + "/" + System.nanoTime());

      FileUtils.forceMkdir(tempDir);

      String filename = (String) _nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
      
      filename = filename.trim();

      String basename = FilenameUtils.getBaseName(filename);

      // TODO: Investigate if this is really needed
      // callas currently has a bug that makes it crash if the whole filepath is
      // longer than 260 characters
      basename = StringUtils.substring(basename, 0, 100);

      // 0x2013 is the long hyphen, not allowed here...
      char c = 0x2013;
      if (StringUtils.contains(basename, c)) {
        if (LOG.isTraceEnabled()) {
          LOG.trace("Long hyphen replaced with short one");
        }

        basename = StringUtils.replaceChars(basename, c, '-');
      }

      filename = basename + "." + extension;

      if (LOG.isTraceEnabled()) {
        LOG.trace("Filename before normalization");

        for (char character : filename.toCharArray()) {
          LOG.trace(character + " : " + (int) character);
        }
      }

      filename = Normalizer.normalize(filename, Form.NFKC);

      if (LOG.isTraceEnabled()) {
        LOG.trace("Filename after normalization");

        for (char character : filename.toCharArray()) {
          LOG.trace(character + " : " + (int) character);
        }
      }

      return new File(tempDir, filename);
    } catch (final Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public void setMetadataContentFactory(MetadataContentFactory metadataContentFactory) {
    _metadataContentFactory = metadataContentFactory;
  }

  public void setMetadataExtracterRegistry(MetadataExtracterRegistry metadataExtracterRegistry) {
    _metadataExtracterRegistry = metadataExtracterRegistry;
  }

}
