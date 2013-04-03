package org.redpill.alfresco.repo.content.transform;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.filestore.FileContentReader;
import org.alfresco.repo.content.filestore.FileContentWriter;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

@ContextConfiguration(locations = { "classpath*:alfresco/subsystems/pdfaPilot/default/pdfapilot-context.xml",
"classpath:test-pdfa-pilot-convert-context.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class PdfaPilotContentTransformerWorkerIntegrationTest {

  @Autowired
  private ApplicationContext _applicationContext;

  private MimetypeService _mimetypeService;

  private PdfaPilotContentTransformerWorker _contentTransformerWorker;

  @Before
  public void setUp() {
    _contentTransformerWorker = (PdfaPilotContentTransformerWorker) _applicationContext.getBean("transformer.worker.PdfaPilot");

    _mimetypeService = mock(MimetypeService.class);

    _contentTransformerWorker.setMimetypeService(_mimetypeService);
  }

  @Test
  public void testIsAvailable() {
    assertTrue(_contentTransformerWorker.isAvailable());
  }

  @Test
  public void testGetVersionString() {
    String versionString = _contentTransformerWorker.getVersionString();

    assertTrue(StringUtils.hasText(versionString));
  }

  @Test
  public void testIsDocTransformable() {
    when(_mimetypeService.getExtension("application/msword")).thenReturn("doc");
    when(_mimetypeService.getExtension("application/pdf")).thenReturn("pdf");

    boolean isTransformable = _contentTransformerWorker.isTransformable("application/msword", "application/pdf", null);

    assertTrue(isTransformable);
  }

  @Test
  public void testIsPdfTransformable() {
    when(_mimetypeService.getExtension("application/pdf")).thenReturn("pdf");
    when(_mimetypeService.getExtension("application/pdf")).thenReturn("pdf");

    boolean isTransformable = _contentTransformerWorker.isTransformable("application/pdf", "application/pdf", null);

    assertTrue(isTransformable);
  }

  @Test
  public void testIsFooTransformable() {
    when(_mimetypeService.getExtension("application/foobar")).thenReturn("foo");
    when(_mimetypeService.getExtension("application/pdf")).thenReturn("pdf");

    boolean isTransformable = _contentTransformerWorker.isTransformable("application/foobar", "application/pdf", null);

    assertFalse(isTransformable);
  }

  @Test
  public void transformDoc() throws IOException {
    transformFile("/test.doc", MimetypeMap.MIMETYPE_WORD, 6);
  }

  @Test
  public void transformPdf() throws IOException {
    transformFile("/test.pdf", MimetypeMap.MIMETYPE_PDF, 5);
  }

  protected void transformFile(String filename, String sourceMimetype, int expectedPageCount) throws IOException {
    when(_mimetypeService.getExtension(sourceMimetype)).thenReturn(FilenameUtils.getExtension(filename));
    when(_mimetypeService.getExtension("application/pdf")).thenReturn("pdf");

    File sourceFile = File.createTempFile("test_", FilenameUtils.getName(filename));
    File targetFile = File.createTempFile("test_", "test.pdf");

    InputStream inputStream = this.getClass().getResourceAsStream(filename);

    OutputStream outputStream = new FileOutputStream(sourceFile);

    FileCopyUtils.copy(inputStream, outputStream);

    ContentReader reader = new FileContentReader(sourceFile);
    reader.setMimetype(sourceMimetype);

    FileContentWriter writer = new FileContentWriter(targetFile);
    writer.setMimetype(MimetypeMap.MIMETYPE_PDF);

    PdfaPilotTransformationOptions options = new PdfaPilotTransformationOptions();
    options.setOptimize(false);
    options.setLevel(PdfaPilotTransformationOptions.PDFA_LEVEL_1B);

    try {
      _contentTransformerWorker.transform(reader, writer, options);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    assertPageCount(expectedPageCount, writer.getFile());

    FileUtils.deleteQuietly(sourceFile);
    FileUtils.deleteQuietly(targetFile);
  }

  private void assertPageCount(int expectedPageCount, File file) throws IOException {
    PDDocument document = PDDocument.load(file);

    try {
      assertSame(expectedPageCount, document.getNumberOfPages());
    } finally {
      document.close();
    }
  }

  @Test
  public void testNotEnabled() {
    _contentTransformerWorker.setEnabled(false);

    assertFalse(_contentTransformerWorker.isTransformable(null, null, null));
  }

  @Test
  public void testNoTargetFormat() {
    when(_mimetypeService.getExtension("application/msword")).thenReturn("doc");
    when(_mimetypeService.getExtension("application/pdf")).thenReturn("");

    assertFalse(_contentTransformerWorker.isTransformable("application/msword", "application/pdf", null));
  }

  @Test
  public void testNullTargetFormat() throws IOException {
    when(_mimetypeService.getExtension("application/msword")).thenReturn(null);
    when(_mimetypeService.getExtension("application/pdf")).thenReturn("pdf");

    File sourceFile = new File("/tmp/test.doc");
    File targetFile = new File("/tmp/test.pdf");

    InputStream inputStream = this.getClass().getResourceAsStream("/test.doc");

    OutputStream outputStream = new FileOutputStream(sourceFile);

    FileCopyUtils.copy(inputStream, outputStream);

    ContentReader reader = new FileContentReader(sourceFile);
    reader.setMimetype(MimetypeMap.MIMETYPE_WORD);

    FileContentWriter writer = new FileContentWriter(targetFile);
    writer.setMimetype(MimetypeMap.MIMETYPE_PDF);

    TransformationOptions options = mock(TransformationOptions.class);

    try {
      _contentTransformerWorker.transform(reader, writer, options);
      fail();
    } catch (Exception e) {
      // this is correct...
    }

    FileUtils.deleteQuietly(sourceFile);
    FileUtils.deleteQuietly(targetFile);
  }

}
