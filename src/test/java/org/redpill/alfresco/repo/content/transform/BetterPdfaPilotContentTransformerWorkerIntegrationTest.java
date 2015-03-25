package org.redpill.alfresco.repo.content.transform;

import static org.junit.Assert.*;

import java.io.File;

import javax.annotation.Resource;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.filestore.FileContentWriter;
import org.alfresco.repo.content.transform.ContentTransformerWorker;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.util.TempFileProvider;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.redpill.alfresco.test.AbstractRepoIntegrationTest;

public class BetterPdfaPilotContentTransformerWorkerIntegrationTest extends AbstractRepoIntegrationTest {

  private static final Logger LOG = Logger.getLogger(BetterPdfaPilotContentTransformerWorkerIntegrationTest.class);

  private static final String DEFAULT_USERNAME = "testuser_" + System.currentTimeMillis();
  private static SiteInfo _site;
  private static NodeRef _defaultUser;

  @Resource(name = "transformer.worker.pdfaPilot")
  private ContentTransformerWorker _contentTransformerWorker;

  @Override
  public void beforeClassSetup() {
    LOG.debug("beforeClassSetup");

    super.beforeClassSetup();

    _defaultUser = createUser(DEFAULT_USERNAME);

    LOG.debug("Created user " + DEFAULT_USERNAME + ": " + _defaultUser);

    _authenticationComponent.setCurrentUser(DEFAULT_USERNAME);

    _site = createSite();

    LOG.debug("Created site " + _site.getShortName());
  }

  @Override
  public void afterClassSetup() {
    LOG.debug("afterClassSetup");

    super.afterClassSetup();

    deleteSite(_site);

    _authenticationComponent.setCurrentUser(_authenticationComponent.getSystemUserName());

    deleteUser(DEFAULT_USERNAME);

    _authenticationComponent.clearCurrentSecurityContext();
  }

  @Test
  public void testOdt() throws Exception {
    testDocument("test.odt");
  }

  @Test
  public void testDoc() throws Exception {
    testDocument("test.doc");
  }

  @Test
  public void testDocx() throws Exception {
    testDocument("test.docx");
  }

  @Test
  public void testPdf() throws Exception {
    testDocument("test.pdf");
  }

  @Test
  public void testXlsm() throws Exception {
    testDocument("test.xlsm");
  }

  @Test
  public void testXls() throws Exception {
    testDocument("test.xls");
  }

  @Test
  public void testXlsx() throws Exception {
    testDocument("test.xlsx");
  }

  public void testDocument(String filename) throws Exception {
    System.out.println("Converting " + filename + "...");
    
    NodeRef document = uploadDocument(_site, filename).getNodeRef();
    ContentReader contentReader = _contentService.getReader(document, ContentModel.PROP_CONTENT);
    assertTrue(contentReader.exists());

    File file = TempFileProvider.createTempFile("temp_", ".pdf");
    ContentWriter contentWriter = new FileContentWriter(file);
    contentWriter.setMimetype("application/pdf");

    PdfaPilotTransformationOptions options = new PdfaPilotTransformationOptions();
    options.setLevel("2b");
    options.setOptimize(false);
    options.setSourceNodeRef(document);

    _contentTransformerWorker.transform(contentReader, contentWriter, options);
    
    System.out.println(file.getAbsolutePath());
  }

}
