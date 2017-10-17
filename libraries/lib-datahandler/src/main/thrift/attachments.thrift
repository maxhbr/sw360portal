/*
 * Copyright Siemens AG, 2014-2017. Part of the SW360 Portal Project.
 * With contributions by Bosch Software Innovations GmbH, 2016.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
include "users.thrift"
include "sw360.thrift"

namespace java org.eclipse.sw360.datahandler.thrift.attachments
namespace php sw360.thrift.attachments

typedef sw360.RequestStatus RequestStatus
typedef sw360.RequestSummary RequestSummary
typedef sw360.Source Source
typedef users.User User

enum AttachmentType {
    DOCUMENT = 0,
    SOURCE = 1,
    DESIGN = 2,
    REQUIREMENT = 3,
    CLEARING_REPORT = 4,
    COMPONENT_LICENSE_INFO_XML = 5,
    COMPONENT_LICENSE_INFO_COMBINED = 6,
    SCAN_RESULT_REPORT = 7,
    SCAN_RESULT_REPORT_XML = 8,
    SOURCE_SELF = 9,
    BINARY = 10,
    BINARY_SELF = 11,
    DECISION_REPORT = 12,
    LEGAL_EVALUATION = 13,
    LICENSE_AGREEMENT = 14,
    SCREENSHOT = 15,
    OTHER = 16,
    README_OSS = 17
}

enum CheckStatus {
    NOTCHECKED = 0,
    ACCEPTED = 1,
    REJECTED = 2
}

struct Attachment {
    // TODO mcj check for tests for added fields on 20151021
    1: required string attachmentContentId,
    5: required string filename,
    6: optional string sha1,

    10: optional AttachmentType attachmentType,

    11: optional string createdBy, // should be e-mail
    12: optional string createdTeam, // team name
    13: optional string createdComment,
    14: optional string createdOn,
    15: optional string checkedBy, // should be e-mail
    16: optional string checkedTeam, // team name
    17: optional string checkedComment, // team name
    18: optional string checkedOn, // strange to have string, but thrift?

    20: optional set<string> uploadHistory, // just for importing data by now
    21: optional CheckStatus checkStatus; // simple status of checks
}

struct AttachmentContent {
    1: optional string id,
    2: optional string revision,
    3: optional string type = "attachment",

    10: optional bool onlyRemote;
    11: optional string remoteUrl,

    20: required string filename,
    21: optional string contentType,
    22: optional string partsCount,
}

/**
 * Describe the usage of an attachment. Each usage results in one usage object.
 * The usage can store additional data for more information about the specific usage.
 */
struct AttachmentUsage {
    1: optional string id;
    2: optional string revision;
    3: optional string type = "attachmentUsage";

    4: required Source owner;
    5: required string attachmentContentId;
    6: required Source usedBy;
    7: optional UsageData usageData;
}

/**
 * UsageData can hold specific information about a usage. There for it is a union of different
 * specific types.
 *
 * If a new type of usage data is defined (like LicenseInfoUsage), the type must be registered in
 *    org.eclipse.sw360.datahandler.couchdb.deserializer.UsageDataDeserializer
 * in order to be properly deserialized when loading from CouchDB
 */
union UsageData {
    1: LicenseInfoUsage licenseInfo;
}

/**
 * Stores specific information if an attachment is used for license info generation. Holds the license ids
 * that were excluded from generation.
 */
struct LicenseInfoUsage {
    1: required set<string> excludedLicenseIds;
}

struct FilledAttachment {
    1: required Attachment attachment,
    2: required AttachmentContent attachmentContent,
}

struct DatabaseAddress {
    1: required string url,
    2: required string dbName
}

service AttachmentService {

    /**
     * Add attachmentContent (= the actual attachment object) object to database,
     * return attachmentContent as written to database if successful
     */
    AttachmentContent makeAttachmentContent(1:AttachmentContent attachmentContent);

    /**
      * Add attachmentContents (= list of the actual attachment objects) to database,
      * return list of attachmentContents as witten to database if successful
      */
    list<AttachmentContent> makeAttachmentContents(1:list<AttachmentContent> attachmentContents);

    /**
     * get validated attachmentContent by id
     **/
    AttachmentContent getAttachmentContent(1:string id);

    /**
     * Update attachmentContent in database, no permission check is necessary
     **/
    oneway void updateAttachmentContent(1:AttachmentContent attachment);

    /**
     * delete attachment contents with ids from db,
     * return RequestStatus together with the total number of elements and the number of successfully removed elements
     **/
    RequestSummary bulkDelete(1: list<string> ids);

     /**
      * delete attachment content with id from db
      **/
    RequestStatus deleteAttachmentContent(1: string attachmentId);

    /**
     * if user is not admin, FAILURE is returned
     * checks which attachmentContents in db are unused (not linked to any document) and deletes them
     * return RequestStatus together with the total number of elements and the number of successfully removed elements
     **/
    RequestSummary vacuumAttachmentDB(1: User user, 2: set<string > usedIds);

     /**
      * returns sha1 checksum of file associated with the attachmentContent specified by attachmentContentId
      **/
    string getSha1FromAttachmentContentId(1: string attachmentContentId);

    /**
     * Creates a new attachment usage object. The given usage object must not exist in the database, yet.
     */
    AttachmentUsage makeAttachmentUsage(1: AttachmentUsage attachmentUsage);

    /**
     * Creates the given list of new attachment usage objects. The given usage objects must not exist in the database, yet.
     */
    void makeAttachmentUsages(1: list<AttachmentUsage> attachmentUsages);

    /**
     * Returns the usage object for the given id.
     */
    AttachmentUsage getAttachmentUsage(1: string id);

    /**
     * Updates an attachment usage object. The given usage object must exist in the database.
     */
    AttachmentUsage updateAttachmentUsage(1: AttachmentUsage attachmentUsage);

    /**
     * Updates the given list of attachment usage objects. The given usage objects must exist in the database.
     */
    void updateAttachmentUsages(1: list<AttachmentUsage> attachmentUsages);

    /**
     * Replaces all attachment usages for a source with the given list of attachment usages. Only usages of the same type are
     * replaces. The type is determined by the type of usage data. Replacement means that all exisitng usages with the same type
     * are deleted and all given usages are added.
     */
    void replaceAttachmentUsages(1: Source usedBy, 2: list<AttachmentUsage> attachmentUsages);

    /**
     * Deletes an attachment usage object. The given usage object must exist in the database.
     */
    void deleteAttachmentUsage(1: AttachmentUsage attachmentUsage);

    /**
     * Deletes the given list of attachment usage objects. The given usage objects must exist in the database.
     */
    void deleteAttachmentUsages(1: list<AttachmentUsage> attachmentUsages);

    /**
     * Returns the list of usage objects describing the usage of the attachment that can be identified
     * by the given owner and attachmentContentId. Optionally filtered  by usage type.
     * If a usage data object is given with a value, the type of the value is used for the filter.
     */
    list<AttachmentUsage> getAttachmentUsages(1: Source owner, 2: string attachmentContentId, 3: UsageData filter);

    /**
     * Returns the number of usages for set of attachments, optionally filtered by usage type.
     * If a usage data object is given with a value, the type of the value is used for the filter.
     */
    map<map<Source, string>, i32> getAttachmentUsageCount(1: map<Source, set<string>> attachments, 2: UsageData filter);

    /**
     * Returns the list of usage objects describing the usage of all attachments used by the given source.
     * Optionally filtered  by usage type. If a usage data object is given with a value, the type of the value
     * is used for the filter.
     */
    list<AttachmentUsage> getUsedAttachments(1: Source usedBy, 2: UsageData filter);
}
