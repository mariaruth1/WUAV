package be;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Document {
    private UUID documentID;
    private Customer customer;
    private Date dateOfCreation;
    private List<User> technicians;
    private List<ImageWrapper> documentImages;
    private String jobDescription, optionalNotes, jobTitle, drawingUrl;
    private BooleanProperty isLoadingImages;

    public Document (){
        this.technicians = new ArrayList<>();
        this.documentImages = new ArrayList<>();
        this.isLoadingImages = new SimpleBooleanProperty(false);
    }

    public Document(Customer customer, String jobDescription, String optionalNotes, String jobTitle, Date dateOfCreation) {
        this();
        this.customer = customer;
        this.jobDescription = jobDescription;
        this.optionalNotes = optionalNotes;
        this.jobTitle = jobTitle;
        this.dateOfCreation = dateOfCreation;
    }

    public Document(UUID documentID, Customer customer, String jobDescription, String optionalNotes, String jobTitle, Date dateOfCreation) {
       this(customer, jobDescription, optionalNotes, jobTitle, dateOfCreation);
       this.documentID = documentID;
    }

    public UUID getDocumentID() {
        return documentID;
    }

    public void setDocumentID(UUID documentID) {
        this.documentID = documentID;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Date getDateOfCreation() {
        return dateOfCreation;
    }

    public void setDateOfCreation(Date dateOfCreation) {
        this.dateOfCreation = dateOfCreation;
    }

    public List<User> getTechnicians() {
        return technicians;
    }

    public void setTechnicians(List<User> technicians) {
        this.technicians = technicians;
    }

    public List<ImageWrapper> getDocumentImages() {
        return documentImages;
    }

    public void setDocumentImages(List<ImageWrapper> documentImages) {
        this.documentImages = documentImages;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public String getOptionalNotes() {
        return optionalNotes;
    }

    public void setOptionalNotes(String optionalNotes) {
        this.optionalNotes = optionalNotes;
    }

    public String getTechnicianNames() {
        StringBuilder technicianString = new StringBuilder();
        int technicianCount = technicians.size();
        if (!technicians.isEmpty()) {
            technicianString.append("Technician(s): ");
            for (int i = 0; i < technicianCount; i++) {
                if (i != technicianCount - 1) {
                    technicianString.append(technicians.get(i).getFullName()).append(", ");
                } else technicianString.append(technicians.get(i).getFullName());
            }
        }
        return technicianString.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Document document = (Document) o;
        return documentID.equals(document.documentID);
    }

    @Override
    public int hashCode() {
        return documentID.hashCode();
    }

    public BooleanProperty isLoadingImagesProperty() {
        return isLoadingImages;
    }

    public boolean isLoadingImages() {
        return isLoadingImages.get();
    }

    public void setLoadingImages(boolean isLoadingImages) {
        this.isLoadingImages.set(isLoadingImages);
    }
    public void setDrawing(String url){
        this.drawingUrl = url;
    }
    public String getDrawing(){
        return this.drawingUrl;
    }
}
