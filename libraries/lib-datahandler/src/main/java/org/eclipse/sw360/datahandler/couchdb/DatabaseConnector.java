/*
 * Copyright Siemens AG, 2014-2015. Part of the SW360 Portal Project.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.couchdb;

import com.google.common.collect.ImmutableSet;
import org.eclipse.sw360.datahandler.thrift.ThriftUtils;
import org.apache.log4j.Logger;
import org.ektorp.*;
import org.ektorp.http.HttpClient;
import org.ektorp.impl.StdCouchDbConnector;
import org.ektorp.util.Documents;

import java.net.MalformedURLException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Database Connector to a CouchDB database
 *
 * @author cedric.bodet@tngtech.com
 */
public class DatabaseConnector extends StdCouchDbConnector {

    private static final Logger log = Logger.getLogger(DatabaseConnector.class);

    private final String dbName;
    private final DatabaseInstance instance;

    private String adminRole = "_admin";

    /**
     * Create a connection to the database
     *
     * @param httpClient    HttpClient with authentication of CouchDB server
     * @param dbName name of the database on the CouchDB server
     */
    public DatabaseConnector(HttpClient httpClient, String dbName) throws MalformedURLException {
        this(httpClient, dbName, new MapperFactory());
    }

    /**
     * Create a connection to the database
     *
     * @param httpClient    Supplier<HttpClient> with authentication of CouchDB server
     * @param dbName name of the database on the CouchDB server
     */
    public DatabaseConnector(Supplier<HttpClient> httpClient, String dbName) throws MalformedURLException {
        this(httpClient.get(), dbName, new MapperFactory());
    }

    /**
     * Create a connection to the database
     *
     * @param httpClient    HttpClient with authentication of CouchDB server
     * @param dbName        name of the database on the CouchDB server
     * @param mapperFactory Specific mapper factory to use for serialization
     */
    public DatabaseConnector(HttpClient httpClient, String dbName, MapperFactory mapperFactory) throws MalformedURLException {
        this(dbName, new DatabaseInstance(httpClient), mapperFactory);
    }

    private DatabaseConnector(String dbName, DatabaseInstance instance, MapperFactory mapperFactory) throws MalformedURLException {
        super(dbName, instance, mapperFactory);
        this.instance = instance;
        this.dbName = dbName;
        // Create the database if it does not exists yet
        instance.createDatabase(dbName);
        restrictAccessToAdmins();
    }

    public Optional<Status> restrictAccessToAdmins() {
        boolean hasChanged = false;

        Function<SecurityGroup,SecurityGroup> addAdminRole = securityGroup -> {
            List<String> newGroupRoles = securityGroup.getRoles();
            newGroupRoles.add(adminRole);
            return new SecurityGroup(securityGroup.getNames(), newGroupRoles);
        };

        Security security = Optional.ofNullable(getSecurity())
                .orElse(new Security());

        SecurityGroup adminGroup = Optional.ofNullable(security.getAdmins())
                .orElse(new SecurityGroup());
        SecurityGroup memberGroup = Optional.ofNullable(security.getMembers())
                .orElse(new SecurityGroup());

        if(!adminGroup.getRoles().contains(adminRole)){
            adminGroup = addAdminRole.apply(adminGroup);
            hasChanged = true;
        }

        if(!memberGroup.getRoles().contains(adminRole)){
            memberGroup = addAdminRole.apply(memberGroup);
            hasChanged = true;
        }

        if(hasChanged){
            return Optional.of(updateSecurity(new Security(adminGroup,memberGroup)));
        }
        return Optional.empty();
    }

    /**
     * Creates the Object as a document in the database. If the id is not set it will be generated by the database.
     */
    public <T> boolean add(T document) {
        try {
            super.create(document);
            return true;
        } catch (UpdateConflictException e) {
            log.warn("Update conflict exception while adding object!", e);
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("Illegal argument exception while adding document", e);
            return false;
        }
    }

    /**
     * Get an object of class type from the database and deserialize it.
     */
    public <T> T get(Class<T> type, String id) {
        try {
            return super.get(type, id);
        } catch (DocumentNotFoundException e) {
            log.info("Document not found for ID: " + id);
            return null;
        } catch (DbAccessException e) {
            log.error("Document ID " + id + " could not be successfully converted to " + type.getName(), e);
            return null;
        }
    }

    /**
     * Get a list of documents from their IDs. All documents should be of the same type.
     */
    public <T> List<T> get(Class<T> type, Collection<String> ids) {
        if (ids == null) return Collections.emptyList();

        // Copy to set in order to avoid duplicates
        Set<String> idSet = ImmutableSet.copyOf(ids);

        ViewQuery q = new ViewQuery()
                .allDocs()
                .includeDocs(true)
                .keys(idSet);

        return queryView(q, type);
    }

    @Override
    public void update(Object document) {
        if (document != null) {
            try {
                final Class documentClass = document.getClass();
                if (ThriftUtils.isMapped(documentClass)) {
                    DocumentWrapper wrapper = getDocumentWrapper(document, documentClass);
                    if (wrapper != null) {
                        super.update(wrapper);
                    }
                } else {
                    super.update(document);
                }
            } catch (UpdateConflictException | IllegalArgumentException e) {
                log.error("Document cannot be updated " + document, e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private DocumentWrapper getDocumentWrapper(Object document, Class documentClass) {
        final Class<? extends DocumentWrapper> wrapperClass = ThriftUtils.getWrapperClass(documentClass);
        final String documentId = Documents.getId(document);
        DocumentWrapper wrapper = get(wrapperClass, documentId);

        if (wrapper == null || !wrapper.getClass().equals(wrapperClass)) {
            log.error("document " + documentId + " cannot be wrapped");
            return null;
        }

        if (!wrapper.getId().equals(documentId)) {
            log.error("round trip from database is not identity for id " + documentId);
            return null;
        }
        if (!wrapper.getRevision().equals(Documents.getRevision(document))) {
            log.error("concurrent access to document " + documentId);
            return null;
        }

        wrapper.updateNonMetadata(document);
        return wrapper;
    }

    /**
     * Returns true if the database contains a document with the given ID.
     */
    public boolean contains(String id) {
        return (id != null) && super.contains(id);
    }

    /**
     * Delete the document with the given id. Returns true if the delete was successful.
     */
    public boolean deleteById(String id) {
        if (super.contains(id)) {
            String rev = super.getCurrentRevision(id);
            super.delete(id, rev);
            return true;
        }
        return false;
    }

    public String getDbName() {
        return dbName;
    }

    public DatabaseInstance getInstance() {
        return instance;
    }

    /**
     * Deletes all objects in the supplied collection.
     *
     * @param deletionCandidates , the objects that will be deleted
     * @return The list will only contain entries for documents that has any kind of error code returned from CouchDB.
     * i.e. the list will be empty if everything was completed successfully.
     */
    protected List<DocumentOperationResult> deleteBulk(Collection<?> deletionCandidates) {
        List<Object> operations = new ArrayList<>();
        for (Object candidate : deletionCandidates) {
            operations.add(BulkDeleteDocument.of(candidate));
        }

        return executeBulk(operations);
    }

    public <T> List<DocumentOperationResult> deleteIds(Collection<String> ids, Class<T> type) {
        final List<T> deletionCandidates = get(type, ids);
        return deleteBulk(deletionCandidates);
    }
}
