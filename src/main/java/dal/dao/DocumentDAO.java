package dal.dao;

import be.Customer;
import be.Document;
import be.ImageWrapper;
import be.User;
import dal.DBConnection;
import dal.factories.DocumentImageFactory;
import dal.interfaces.DAO;
import dal.interfaces.IDAO;
import utils.ThreadPool;
import utils.enums.ResultState;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class DocumentDAO extends DAO implements IDAO<Document> {
    private final DBConnection dbConnection;
    private final DocumentImageFactory imageFactory;
    private ConcurrentHashMap<UUID, Document> documents;

    public DocumentDAO() {
        dbConnection = DBConnection.getInstance();
        imageFactory = DocumentImageFactory.getInstance();
        documents = new ConcurrentHashMap<>();
        refreshCache();
    }

    /**
     * Adds a document to the database, retrieves the generated ID and links the document to the technicians.
     * Inserts the image filepaths into the database and links them to the document.
     * @param document document to add
     * @return ResultState.SUCCESSFUL if the document was added successfully, ResultState.FAILED otherwise
     */
    @Override
    public ResultState add(Document document) {
        Connection connection = null;
        try {
            connection = dbConnection.getConnection();

            // Insert the document into the database
            String sql = "INSERT INTO Document (JobTitle, JobDescription, Notes, CustomerId, DateOfCreation) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement ps = connection.prepareStatement(sql);
            fillPreparedStatement(ps, document);
            ps.executeUpdate();

            // Get the documentID from the database and set it as the document's ID
            sql = "SELECT DocumentID FROM Document WHERE JobTitle = ? AND JobDescription = ? AND Notes = ? AND CustomerID = ? AND DateOfCreation = ?";
            ps = connection.prepareStatement(sql);
            ps.setString(1, document.getJobTitle());
            ps.setString(2, document.getJobDescription());
            ps.setString(3, document.getOptionalNotes());
            ps.setString(4, document.getCustomer().getCustomerID().toString());
            ps.setDate(5, document.getDateOfCreation());

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                document.setDocumentID(UUID.fromString(rs.getString("DocumentID")));
            }

            // Link the document to the technicians
            sql = "INSERT INTO User_Document_Link (UserID, DocumentID) VALUES (?, ?)";
            ps = connection.prepareStatement(sql);
            for (User technician : document.getTechnicians()) {
                ps.setString(1, technician.getUserID().toString());
                ps.setString(2, document.getDocumentID().toString());
                ps.addBatch();
            }
            ps.executeBatch();

            //Save and link image filepaths to document
            saveImagesForDocument(connection, document);
            connection.commit(); // End transaction
            documents.put(document.getDocumentID(), document);
            return ResultState.SUCCESSFUL;
        } catch (Exception e) {
            e.printStackTrace();
            return ResultState.FAILED;
        } finally {
            dbConnection.releaseConnection(connection);
        }
    }

    /**
     * Updates a document in the database, updates the image filepaths and links them to the document.
     * @param document document to update
     * @return ResultState.SUCCESSFUL if the document was updated successfully, ResultState.FAILED otherwise
     */
    @Override
    public ResultState update(Document document) {
        String sql = "UPDATE Document SET JobTitle = ?, JobDescription = ?, Notes = ?, CustomerID = ?, DateOfCreation = ? " +
                "WHERE DocumentID = ?";
        Connection connection = null;
        try {
            // Check, if the customer is already in the database and update them, if not, add them
            connection = dbConnection.getConnection();

            // Update the document
            PreparedStatement ps = connection.prepareStatement(sql);
            fillPreparedStatement(ps, document);
            ps.setString(6, document.getDocumentID().toString());
            ps.executeUpdate();

            //Update document image links
            sql = "DELETE FROM Document_Image_Link WHERE DocumentID = ?;";
            ps = connection.prepareStatement(sql);
            ps.setString(1, document.getDocumentID().toString());
            ps.executeUpdate();

            //Save and link image filepaths to document
            saveImagesForDocument(connection, document);
            documents.put(document.getDocumentID(), document);

            return ResultState.SUCCESSFUL;
        } catch (Exception e) {
            e.printStackTrace();
            return ResultState.FAILED;
        } finally {
            dbConnection.releaseConnection(connection);
        }
    }

    /**
     * Deletes a document from the database along with the images linked to the document.
     * @param id id of the document to delete
     * @return ResultState.SUCCESSFUL if the document was deleted successfully, ResultState.FAILED otherwise
     */
    @Override
    public ResultState delete(UUID id) {
        String sql = "UPDATE Document SET Deleted = 1 WHERE DocumentID = ?;" +
                "DELETE FROM Document_Image_Link WHERE DocumentID = ?;" +
                "DELETE FROM Document_Drawing_Link WHERE DocumentID = ?;";
        Connection connection = null;
        try {
            connection = dbConnection.getConnection();
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, id.toString());
            ps.setString(2, id.toString());
            ps.setString(3, id.toString());
            ps.executeUpdate();
            documents.remove(id);
            return ResultState.SUCCESSFUL;
        } catch (SQLException e) {
            e.printStackTrace();
            return ResultState.FAILED;
        } finally {
            dbConnection.releaseConnection(connection);
        }
    }

    /**
     * Returns currently cached documents.
     * @return map of documents accessible by their IDs
     */
    @Override
    public Map<UUID, Document> getAll() {
        return documents;
    }

    /**
     * Retrieves a document by its ID from the cache, if possible, otherwise retrieves it from the database.
     * @param id id of the document to retrieve
     * @return document with the specified ID
     */
    @Override
    public Document getById(UUID id) {
        if (documents.containsKey(id)) {
            return documents.get(id);
        }

        String sql = "SELECT * FROM Document WHERE DocumentID = ? AND Deleted = 0";
        Connection connection = null;
        try {
            connection = dbConnection.getConnection();
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, id.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return createDocumentFromResultSet(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            dbConnection.releaseConnection(connection);
        }
        return null;
    }

    /**
     * Used to convert a ResultSet into a Document object.
     * @param rs ResultSet to convert
     * @return Document object
     * @throws SQLException if the ResultSet is invalid
     */
    private Document createDocumentFromResultSet(ResultSet rs) throws SQLException {
        UUID documentID = UUID.fromString(rs.getString("DocumentID"));
        if (documents.containsKey(documentID)) {
            return documents.get(documentID);
        }

        CustomerDAO customerDAO = new CustomerDAO();
        Customer customer = customerDAO.getById(UUID.fromString(rs.getString("CustomerID")));

        Document document = new Document (
                documentID,
                customer,
                rs.getString("JobDescription"),
                rs.getString("Notes"),
                rs.getString("JobTitle"),
                rs.getDate("DateOfCreation")
            );
        customer.addContract(document);
        document.setLoadingImages(true);
        CompletableFuture.runAsync(() -> assignImagesToDocument(document), ThreadPool.getInstance().getExecutorService());
        return document;
    }

    /**
     * Inserts all images linked to a document into the database and links them to the document.
     * @param document
     */
    public void assignImagesToDocument(Document document){
        String sql = "SELECT * FROM Document_Image_Link WHERE DocumentID = ? ORDER BY PictureIndex;";
        Connection connection = null;
        List<ImageWrapper> images = new ArrayList<>();
        try {
            connection = dbConnection.getConnection();
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, document.getDocumentID().toString());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String filepath = rs.getString("Filepath");
                String filename = rs.getString("FileName");
                String description = rs.getString("Description");
                images.add(new ImageWrapper(filepath, filename, imageFactory.create(filepath), description));
            }
            document.setDocumentImages(images);
            document.setLoadingImages(false);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dbConnection.releaseConnection(connection);
        }
    }

    /**
     * Called to refresh the cache of documents.
     */
    public void refreshCache() {
        documents.clear();
        String sql = "SELECT * FROM Document WHERE Deleted = 0";
        Connection connection = null;
        try {
            connection = dbConnection.getConnection();
            PreparedStatement ps = connection.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Document document = createDocumentFromResultSet(rs);
                documents.put(document.getDocumentID(), document);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            dbConnection.releaseConnection(connection);
        }
    }

    /**
     * Links a user to a document in the database or deletes the link.
     * @param user user to assign to the document
     * @param document document to assign the user to
     * @param isAssigning true if the user is being assigned to the document, false if the link is being deleted
     */
    public void assignUserToDocument(User user, Document document, boolean isAssigning){
        String sql = "INSERT INTO User_Document_Link (UserID, DocumentID) VALUES (?, ?);";
        if (!isAssigning) {
            sql = "DELETE FROM User_Document_Link WHERE UserID = ? AND DocumentID =?;";
        }

        Connection connection = null;
        try {
            connection = dbConnection.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, user.getUserID().toString());
            statement.setString(2, document.getDocumentID().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            dbConnection.releaseConnection(connection);
        }
    }

    /**
     * Links a document to a customer in the database or deletes the link.
     * @param documentIDs IDs of the documents to assign to the customer
     * @return map of documents accessible by their IDs
     */
    public HashMap<UUID, Document> getDocumentsByIDs(List<UUID> documentIDs) {
        HashMap<UUID, Document> documents = new HashMap<>();
        if (documentIDs.isEmpty()) {
            return documents;
        }

        // create comma-separated string of document IDs
        String sql = "SELECT * FROM Document WHERE DocumentID IN ("
                + String.join(",", Collections.nCopies(documentIDs.size(), "?"))
                + ")";
        Connection connection = null;
        try {
            connection = dbConnection.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            for (int i = 0; i < documentIDs.size(); i++) {
                statement.setString(i + 1, documentIDs.get(i).toString());
            }

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                Document document = createDocumentFromResultSet(rs);
                documents.put(document.getDocumentID(), document);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dbConnection.releaseConnection(connection);
        }
        return documents;
    }

    /**
     * Links images to a document in the database.
     * @param connection connection to the database
     * @param document document to link images to
     * @throws SQLException
     */
    private void saveImagesForDocument(Connection connection, Document document) throws SQLException {
        //Save and link image filepaths to document
        String sql = "INSERT INTO Document_Image_Link (DocumentID, Filepath, FileName, PictureIndex, Description) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        String documentID = document.getDocumentID().toString();
        for (int i = 0; i < document.getDocumentImages().size(); i++) {
            ImageWrapper image = document.getDocumentImages().get(i);
            ps.setString(1, documentID);
            ps.setString(2, image.getUrl());
            ps.setString(3, image.getName());
            ps.setInt(4, i);
            ps.setString(5, image.getDescription());
            ps.addBatch();
        }
        ps.executeBatch();
    }

    /**
     * Fills a prepared statement with the values of a document.
     * @param ps prepared statement to fill
     * @param document document to get values from
     * @throws SQLException
     */
    private void fillPreparedStatement(PreparedStatement ps, Document document) throws SQLException {
        ps.setString(1, document.getJobTitle());
        ps.setString(2, document.getJobDescription());
        ps.setString(3, document.getOptionalNotes());
        ps.setString(4, document.getCustomer().getCustomerID().toString());
        ps.setDate(5, document.getDateOfCreation());
    }

    /**
     * Links a drawing to a document in the database.
     * @param document document to link drawing to
     * @param canvasImage drawing to link to the document
     */
    public void addDrawingToDocument(Document document, String canvasImage) {
        String sql = "MERGE INTO Document_Drawing_Link AS target " +
                "USING (VALUES (?, ?)) AS source (DocumentID, Drawing) " +
                "ON (target.DocumentID = source.DocumentID) " +
                "WHEN MATCHED THEN " +
                "  UPDATE SET target.Drawing = source.Drawing " +
                "WHEN NOT MATCHED THEN " +
                "  INSERT (DocumentID, Drawing) VALUES (source.DocumentID, source.Drawing);";
        Connection connection = null;
        try {
            connection = dbConnection.getConnection();
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, document.getDocumentID().toString());
            ps.setString(2, canvasImage);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            dbConnection.releaseConnection(connection);
        }
    }

    /**
     * Gets the drawing linked to a document.
     * @param document document to get drawing from
     * @return URL to the blob of the drawing linked to the document
     */
    public String getDrawingOnDocument(Document document){
        String sql = "SELECT * FROM Document_Drawing_Link WHERE DocumentID = ?";
        Connection connection = null;
        try{
            connection = dbConnection.getConnection();
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, document.getDocumentID().toString());
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                String drawing = rs.getString("Drawing");
                return drawing;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            dbConnection.releaseConnection(connection);
        }
        return null;
    }
}

