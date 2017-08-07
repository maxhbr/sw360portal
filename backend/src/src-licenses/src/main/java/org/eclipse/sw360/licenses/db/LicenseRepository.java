/*
 * Copyright Siemens AG, 2013-2016. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.licenses.db;

import org.eclipse.sw360.components.summary.LicenseSummary;
import org.eclipse.sw360.components.summary.SummaryType;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.SummaryAwareRepository;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.ektorp.support.View;

import java.util.List;

/**
 * CRUD access for the License class
 *
 * @author cedric.bodet@tngtech.com
 */
@View(name = "all", map =
        "function(doc) {" +
        "    if (doc.type == 'license') emit(null, doc._id);" +
        "    if(doc.otherIds.length > 0) {" +
        "        for( var otherId in doc.otherIds) {" +
        "            emit(null, otherId);" +
        "        }" +
        "    }" +
        " }")
public class LicenseRepository extends SummaryAwareRepository<License> {

    private static final String BY_NAME_VIEW = "function(doc) { if(doc.type == 'license') { emit(doc.fullname, doc) } }";

    public LicenseRepository(DatabaseConnector db) {
        super(License.class, db, new LicenseSummary());

        initStandardDesignDocument();
    }

    @View(name = "byname", map = BY_NAME_VIEW)
    public List<License> searchByName(String name) {
        return queryByPrefix("byname", name);
    }

    public List<License> searchByShortName(String name) {
        return queryByPrefix("all", name);
    }
    public List<License> searchByShortName(List<String> names) {
        return queryByIds("all", names);
    }

    public List<License> getLicenseSummary() {
        return makeSummaryFromFullDocs(SummaryType.SUMMARY, queryView("byname"));
    }

    public List<License> getLicenseSummaryForExport() {
        return makeSummaryFromFullDocs(SummaryType.EXPORT_SUMMARY, queryView("byname"));
    }

}
