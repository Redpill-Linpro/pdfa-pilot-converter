package org.redpill.alfresco.repo.content.transform;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.alfresco.util.exec.RuntimeExec;
import org.alfresco.util.exec.RuntimeExec.ExecutionResult;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import static org.junit.Assert.*;

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

    ContentReader reader = mock(ContentReader.class);
    ContentWriter writer = mock(ContentWriter.class);
    TransformationOptions options = mock(TransformationOptions.class);
    NodeRef sourceNodeRef = new NodeRef("workspace://SpacesStore/document");

    when(reader.getMimetype()).thenReturn("application/msword");
    when(writer.getMimetype()).thenReturn("application/pdf");
    when(mimetypeService.getExtension("application/msword")).thenReturn("doc");
    when(mimetypeService.getExtension("application/pdf")).thenReturn("pdf");
    when(executer.execute(anyMapOf(String.class, String.class), anyLong())).thenReturn(result);
    when(result.getExitValue()).thenReturn(0);
    when(options.getSourceNodeRef()).thenReturn(sourceNodeRef);
    when(nodeService.getProperty(sourceNodeRef, ContentModel.PROP_NAME)).thenReturn(sourceFilename);

    worker.transform(reader, writer, options);
  }

}
