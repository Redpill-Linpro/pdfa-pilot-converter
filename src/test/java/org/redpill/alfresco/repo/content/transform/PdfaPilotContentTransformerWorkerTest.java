package org.redpill.alfresco.repo.content.transform;

import static org.junit.Assert.assertNotEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.metadata.MetadataExtracter;
import org.alfresco.repo.content.metadata.MetadataExtracterRegistry;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.exec.RuntimeExec;
import org.alfresco.util.exec.RuntimeExec.ExecutionResult;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.redpill.alfresco.module.metadatawriter.factories.MetadataContentFactory;
import org.redpill.alfresco.module.metadatawriter.factories.UnsupportedMimetypeException;
import org.redpill.alfresco.module.metadatawriter.services.ContentFacade;

public class PdfaPilotContentTransformerWorkerTest {

  /**
   * This is a very complicated test which tests for if the long hyphen is
   * replaced in the filename
   * 
   * @throws Exception
   */
  @Test
  public void testTransform() throws Exception {
    final String sourceFilename = "this is a test file with long (–) hyphen åäö.doc";

    MimetypeService mimetypeService = mock(MimetypeService.class);
    RuntimeExec executer = mock(RuntimeExec.class);
    ExecutionResult result = mock(ExecutionResult.class);
    NodeService nodeService = mock(NodeService.class);
    MetadataContentFactory metadataContentFactory = mock(MetadataContentFactory.class);
    MetadataExtracterRegistry metadataExtracterRegistry = mock(MetadataExtracterRegistry.class);
    ContentFacade contentFacade = mock(ContentFacade.class);
    MetadataExtracter pdfExtracter = mock(MetadataExtracter.class);
    MetadataExtracter mswordExtracter = mock(MetadataExtracter.class);

    PdfaPilotContentTransformerWorker worker = new PdfaPilotContentTransformerWorker() {
      @Override
      protected void transformInternal(File sourceFile, File targetFile, File finalTargetFile, TransformationOptions options) throws Exception {
        assertNotEquals(sourceFilename, sourceFile.getName());

        FileUtils.writeStringToFile(targetFile, "FOOBAR", "UTF-8");
      }
    };

    worker.setDebug(true);
    worker.setMimetypeService(mimetypeService);
    worker.setExecuter(executer);
    worker.setNodeService(nodeService);
    worker.setMetadataContentFactory(metadataContentFactory);
    worker.setMetadataExtracterRegistry(metadataExtracterRegistry);

    ContentReader reader = mock(ContentReader.class);
    ContentWriter writer = mock(ContentWriter.class);
    TransformationOptions options = mock(TransformationOptions.class);
    NodeRef sourceNodeRef = new NodeRef("workspace://SpacesStore/document");
    String sourceMimetype = "application/msword";
    Map<QName, Serializable> mswordProperties = new HashMap<QName, Serializable>();
    mswordProperties.put(ContentModel.PROP_TITLE, "This is a title");
    Map<QName, Serializable> pdfProperties = new HashMap<QName, Serializable>();
    pdfProperties.put(ContentModel.PROP_TITLE, "This is a title<start>" + sourceNodeRef.toString() + "</end>");

    when(reader.getMimetype()).thenReturn(sourceMimetype);
    when(writer.getMimetype()).thenReturn("application/pdf");
    when(mimetypeService.getExtension("application/msword")).thenReturn("doc");
    when(mimetypeService.getExtension("application/pdf")).thenReturn("pdf");
    when(executer.execute(anyMapOf(String.class, String.class), anyLong())).thenReturn(result);
    when(result.getExitValue()).thenReturn(0);
    when(options.getSourceNodeRef()).thenReturn(sourceNodeRef);
    when(nodeService.getProperty(sourceNodeRef, ContentModel.PROP_NAME)).thenReturn(sourceFilename);
    when(metadataContentFactory.createContent(any(InputStream.class), any(OutputStream.class), eq(sourceMimetype))).thenThrow(UnsupportedMimetypeException.class);
    when(metadataContentFactory.createContent(any(InputStream.class), any(OutputStream.class), eq("application/pdf"))).thenThrow(UnsupportedMimetypeException.class);
    when(metadataExtracterRegistry.getExtracter("application/pdf")).thenReturn(pdfExtracter);
    when(metadataExtracterRegistry.getExtracter(sourceMimetype)).thenReturn(mswordExtracter);
    when(mswordExtracter.extract(any(ContentReader.class), anyMapOf(QName.class, Serializable.class))).thenReturn(mswordProperties);
    when(pdfExtracter.extract(any(ContentReader.class), anyMapOf(QName.class, Serializable.class))).thenReturn(pdfProperties);

    worker.transform(reader, writer, options);
  }

}
