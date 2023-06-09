package be;

import dal.factories.DocumentImageFactory;
import utils.enums.UserRole;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class User {
    private UUID userID;
    private String fullName, username, phoneNumber, profilePicturePath;
    private byte[][] password;
    private HashMap<UUID, Document> assignedDocuments;
    private UserRole userRole;
    private byte[] profilePicture;

    public User(){
        assignedDocuments = new HashMap<>();
    }

    public User(String fullName, String username, byte[][] password, String phoneNumber, UserRole userRole, String profilePicturePath) {
        this.fullName = fullName;
        this.username = username;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.userRole = userRole;
        this.assignedDocuments = new HashMap<>();

        this.profilePicturePath = Objects.requireNonNullElse(profilePicturePath, "/img/icons/userIcon.png");
        this.profilePicture = DocumentImageFactory.getInstance().create(profilePicturePath);
    }

    public User(UUID userID, String fullName, String username, byte[][] password, String phoneNumber, UserRole userRole, String profilePicturePath) {
        this(fullName, username, password, phoneNumber, userRole, profilePicturePath);
        this.userID = userID;
    }

    public UUID getUserID() {
        return userID;
    }

    public void setUserID(UUID userID) {
        this.userID = userID;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public byte[] getPassword() {
        return password[0];
    }

    public byte[] getSalt() {
        return password[1];
    }

    public void setPassword(byte[] password) {
        this.password[0] = password;
    }

    public void setSalt(byte[] salt) {
        this.password[1] = salt;
    }

    public HashMap<UUID, Document> getAssignedDocuments() {
        return assignedDocuments;
    }

    public void setAssignedDocuments(HashMap<UUID, Document> assignedDocuments) {
        this.assignedDocuments = assignedDocuments;
    }

    public UserRole getUserRole() {
        return userRole;
    }

    public void setUserRole(UserRole userRole) {
        this.userRole = userRole;
    }

    public byte[] getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(byte[] profilePicture) {
        this.profilePicture = profilePicture;
    }

    public void addDocument(Document document) {
        assignedDocuments.put(document.getDocumentID(), document);
    }

    public void addDocument(UUID documentID) {
        assignedDocuments.put(documentID, null);
    }

    public void removeDocument(Document document) {
        assignedDocuments.remove(document);
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getProfilePicturePath() {
        return profilePicturePath;
    }

    public void setProfilePicturePath(String profilePicturePath) {
        this.profilePicturePath = profilePicturePath;
    }

    public String getAssignation(Document document){
        if (assignedDocuments.containsValue(document))
            return "ASSIGNED: ";
        else return "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(userID, user.userID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userID);
    }
}
