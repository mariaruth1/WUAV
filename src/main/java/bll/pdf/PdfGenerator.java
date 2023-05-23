package bll.pdf;

import be.Address;
import be.Document;
import javafx.beans.property.DoubleProperty;
import utils.enums.DocumentPropertyType;
import com.itextpdf.io.font.FontConstants;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.border.Border;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.property.*;
import gui.nodes.DocumentPropertyCheckboxWrapper;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class PdfGenerator {
    private static PdfFont FONT;
    private static final int FONT_SIZE = 12;
    private PdfPageEventHandler pageNumberHandler;

    //Create formatting elements
    private Paragraph lineBreak = new Paragraph();
    private Paragraph lineBreak3 = new Paragraph("\n" + "\n" + "\n");
    private AreaBreak pageBreak = new AreaBreak(AreaBreakType.NEXT_AREA);
    private float margin = 75;

    public Path generatePdf(Document document, List<DocumentPropertyCheckboxWrapper> checkBoxes) {
        PdfDocumentWrapper temporary = getNumberOfPages(document, checkBoxes);
        PdfDocumentWrapper pdf = generateItextDocument(document, checkBoxes, temporary);
        pdf.getPdfDocument().close();
        return pdf.getPath();
    }

    private PdfDocumentWrapper generateItextDocument(Document document, List<DocumentPropertyCheckboxWrapper> checkBoxes, PdfDocumentWrapper wrapper) {
        try {
            FONT = PdfFontFactory.createFont(FontConstants.HELVETICA);

            // Open a new PDF document
            Path path = getPath(document);
            PdfWriter writer = new PdfWriter(path.toString());
            PdfDocument pdfDoc = new PdfDocument(writer);
            com.itextpdf.layout.Document doc = new com.itextpdf.layout.Document(pdfDoc);

            if (wrapper != null) {
                pageNumberHandler = new PdfPageEventHandler(pdfDoc, wrapper.getNumberOfPages());
                pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE, pageNumberHandler);
            }

            doc.setMargins(margin, margin, margin, margin);
            doc.add(getHeaderTable(document));
            doc.add(lineBreak);
            doc.add(getLogoTable());
            doc.add(lineBreak3);

            // Determine which properties to add to the document
            for (DocumentPropertyCheckboxWrapper checkboxWrapper : checkBoxes) {
                if (checkboxWrapper.getProperty() == DocumentPropertyType.DATE_OF_CREATION) {
                    doc.add(new Paragraph(String.valueOf(document.getDateOfCreation())));
                    doc.add(lineBreak);
                }
                if (checkboxWrapper.getProperty() == DocumentPropertyType.JOB_TITLE) {
                    doc.add(new Paragraph(document.getJobTitle()).setBold().setFontSize(14));
                }
                if (checkboxWrapper.getProperty() == DocumentPropertyType.JOB_DESCRIPTION) {
                    doc.add(new Paragraph(document.getJobDescription()));
                }
                if (checkboxWrapper.getProperty() == DocumentPropertyType.NOTES) {
                    doc.add(getOptionalNotes(document));
                }
                if (checkboxWrapper.getProperty() == DocumentPropertyType.TECHNICIANS) {
                    doc.add(new Paragraph(document.getTechnicianNames()));
                }
            }
            //Add images
            List<DocumentPropertyCheckboxWrapper> imageCheckboxes = checkBoxes.stream().filter(checkbox -> checkbox.getProperty() == DocumentPropertyType.IMAGE).toList();
            if (!imageCheckboxes.isEmpty()) {
                doc.add(pageBreak);
                for (DocumentPropertyCheckboxWrapper image : imageCheckboxes) {
                    doc.add(createImageTable(image));
                }
            }
            return new PdfDocumentWrapper(doc, path, document, checkBoxes);
        } catch (IOException e) {
            throw new RuntimeException(e);

        }
    }

    public PdfDocumentWrapper getNumberOfPages(Document document, List<DocumentPropertyCheckboxWrapper> checkBoxes) {
        PdfDocumentWrapper wrapper = generateItextDocument(document, checkBoxes, null);
        wrapper.getPdfDocument().close();
        wrapper.delete();
        return wrapper;
    }

    private Table createImageTable(DocumentPropertyCheckboxWrapper image) {
        Table imageTable = new Table(1);

        ImageData imageData;
        try {
            imageData = ImageDataFactory.create(image.getImage().getUrl());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        Image documentImage = new Image(imageData);
        documentImage.setHorizontalAlignment(HorizontalAlignment.CENTER);
        documentImage.setAutoScale(true);
        imageTable.addCell(documentImage);

        Cell footerCell = new Cell();
        String description = image.getImage().getDescription() == null ? "" : image.getImage().getDescription();
        footerCell.add(description).setTextAlignment(TextAlignment.CENTER);
        footerCell.setBorder(Border.NO_BORDER);
        imageTable.addFooterCell(footerCell);

        imageTable.setHorizontalAlignment(HorizontalAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE);
        removeBorder(imageTable);
        imageTable.setMarginTop(20);
        return imageTable;
    }

    private Paragraph getOptionalNotes(Document document) {
        String optionalNotes;
        if (!document.getOptionalNotes().isEmpty()) {
            optionalNotes = "Additional notes: " + document.getOptionalNotes();
        } else optionalNotes = "";
        return new Paragraph(optionalNotes);
    }

    private Table getHeaderTable(Document document) {
        //Left side header, customer info
        Paragraph customerParagraph = new Paragraph(getCustomerInfo(document));
        //Right side header, WUAV info
        Paragraph WUAVheader = new Paragraph(getWUAVinfo());
        WUAVheader.setTextAlignment(TextAlignment.RIGHT);
        //Create pdf header
        Table headerTable = new Table(2);

        Cell customerCell = new Cell();
        customerCell.add(customerParagraph);

        Cell WUAVCell = new Cell();
        WUAVCell.add(WUAVheader);

        headerTable.addCell(customerCell);
        headerTable.addCell(WUAVCell);
        removeBorder(headerTable);

        return headerTable;
    }

    private Table getLogoTable(){
        //Add WUAV logo
        ImageData data;
        try {
            data = ImageDataFactory.create("https://easvprojects.blob.core.windows.net/wuav/9e112cc6-1487-426a-9bdc-2a4fd7b91861/7e7d9e00-507b-47ee-989b-8686859b41aa-wuav.png");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        Image logoImage = new Image(data);
        logoImage.setHeight(50).setHorizontalAlignment(HorizontalAlignment.RIGHT);
        Table logoTable = new Table(1);
        logoTable.addCell(logoImage).setPageNumber(1);
        removeBorder(logoTable);

        return logoTable;
    }

    private String getWUAVinfo() {
        return           "WUAV"
                + "\n" + "Murervej 7a"
                + "\n" + "6710 Esbjerg V"
                + "\n" + "+45 7511 9191"
                + "\n" + "info@wuav.dk";
    }

    private String getCustomerInfo(Document document) {
        Address customerAddress = document.getCustomer().getCustomerAddress();
        return document.getCustomer().getCustomerName()
                + "\n" + customerAddress.getStreetName()+ " " + customerAddress.getStreetNumber()
                + "\n" + customerAddress.getPostcode() + " " + customerAddress.getTown()
                + "\n" + document.getCustomer().getCustomerPhoneNumber()
                + "\n" + document.getCustomer().getCustomerEmail();
    }

    private void removeBorder(Table table) {
        for (IElement iElement : table.getChildren()) {
            ((Cell)iElement).setBorder(Border.NO_BORDER);
        }
    }

    private Path getPath(Document document) {
        // Check if the file already exists
        String home = System.getProperty("user.home");
        Path path = Path.of(home + "/Downloads/" + document.getDocumentID() + ".pdf");
        if (new File(home + "/Downloads/" + document.getDocumentID() + ".pdf").exists()) {
            int i = 1;
            while (new File(home + "/Downloads/" + document.getDocumentID() + " (" + i + ")" + ".pdf").exists()) {
                i++;
            }
            path = Path.of(home + "/Downloads/" + document.getDocumentID() + " (" + i + ")" + ".pdf");
        }
        return path;
    }
}
