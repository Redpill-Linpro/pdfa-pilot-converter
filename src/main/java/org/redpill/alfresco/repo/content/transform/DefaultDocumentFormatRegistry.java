package org.redpill.alfresco.repo.content.transform;

import org.alfresco.repo.content.MimetypeMap;

import java.util.Collections;

public class DefaultDocumentFormatRegistry extends SimpleDocumentFormatRegistry {

  public DefaultDocumentFormatRegistry() {
    DocumentFormat pdf = new DocumentFormat("Portable Document Format", "pdf", MimetypeMap.MIMETYPE_PDF);
    pdf.setStoreProperties(DocumentFamily.TEXT, Collections.singletonMap("FilterName", "word_pdf_Export"));
    pdf.setStoreProperties(DocumentFamily.SPREADSHEET, Collections.singletonMap("FilterName", "excel_pdf_Export"));
    pdf.setStoreProperties(DocumentFamily.PRESENTATION, Collections.singletonMap("FilterName", "powerpoint_pdf_Export"));
    // pdf.setStoreProperties(DocumentFamily.DRAWING, Collections.singletonMap("FilterName", "draw_pdf_Export"));
    addFormat(pdf);

    DocumentFormat doc = new DocumentFormat();
    doc.setName("Microsoft Word");
    doc.setExtension("doc");
    doc.setMediaType(MimetypeMap.MIMETYPE_WORD);
    doc.setInputFamily(DocumentFamily.TEXT);
    addFormat(doc);

    DocumentFormat docx = new DocumentFormat("Microsoft Word 2007 XML", "docx", MimetypeMap.MIMETYPE_OPENXML_WORDPROCESSING);
    docx.setInputFamily(DocumentFamily.TEXT);
    addFormat(docx);

    DocumentFormat xls = new DocumentFormat("Microsoft Excel", "xls", MimetypeMap.MIMETYPE_EXCEL);
    xls.setInputFamily(DocumentFamily.SPREADSHEET);
    addFormat(xls);

    DocumentFormat xlsx = new DocumentFormat("Microsoft Excel 2007 XML", "xlsx", MimetypeMap.MIMETYPE_OPENXML_SPREADSHEET);
    xlsx.setInputFamily(DocumentFamily.SPREADSHEET);
    addFormat(xlsx);

    DocumentFormat ods = new DocumentFormat("OpenDocument Spreadsheet", "ods", MimetypeMap.MIMETYPE_OPENDOCUMENT_SPREADSHEET);
    ods.setInputFamily(DocumentFamily.SPREADSHEET);
    addFormat(ods);

    DocumentFormat ppt = new DocumentFormat("Microsoft PowerPoint", "ppt", MimetypeMap.MIMETYPE_PPT);
    ppt.setInputFamily(DocumentFamily.PRESENTATION);
    addFormat(ppt);

    DocumentFormat pptx = new DocumentFormat("Microsoft PowerPoint 2007 XML", "pptx", MimetypeMap.MIMETYPE_OPENXML_PRESENTATION);
    pptx.setInputFamily(DocumentFamily.PRESENTATION);
    addFormat(pptx);
  }

}
