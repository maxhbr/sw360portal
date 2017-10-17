<%--
  ~ Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>

<%@ page import="org.eclipse.sw360.portal.common.PortalConstants"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<%@include file="/html/init.jsp"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<c:catch var="attributeNotFoundException">
    <jsp:useBean id="attachments" type="java.util.Set<org.eclipse.sw360.datahandler.thrift.attachments.Attachment>" scope="request" />
    <jsp:useBean id="documentType" type="java.lang.String" scope="request" />
    <jsp:useBean id="documentID" class="java.lang.String" scope="request" />
</c:catch>

<core_rt:if test="${empty attributeNotFoundException}">

    <core_rt:if test="${empty attachments}">
        <span>No attachments yet.</span>
    </core_rt:if>

    <core_rt:if test="${not empty attachments}">
        <table id="attachmentsDetail" class="table info_table" title="Attachment Information">
            <colgroup>
                <col style="width: 4%;" />
                <col style="width: 20%;" />
                <col style="width: 10%;" />
                <col style="width: 6%;" />
                <col style="width: 6%;" />
                <col style="width: 16%;" />
                <col style="width: 6%;" />
                <col style="width: 16%;" />
                <col style="width: 6%;" />
                <col style="width: 10%;" />
            </colgroup>
            <thead>
                <tr>
                    <th><sw360:DisplayDownloadAttachmentBundle attachments="${attachments}"
                                name="AttachmentBundle.zip"
                                contextType="${documentType}"
                                contextId="${documentID}" />
                    </th>
                    <th>File name</th>
                    <th>Size</th>
                    <th>Type</th>
                    <th>Group</th>
                    <th>Uploaded by</th>
                    <th>Group</th>
                    <th>Checked by</th>
                    <th>Usage</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
            </tbody>
        </table>

        <script>
            require(['jquery', /* jquery-plugins */ 'datatables' ], function($) {
                var attachmentJSON = [];

                /* Print all attachment table data as array into the html page */
                <core_rt:forEach items="${attachments}" var="attachment">
                    attachmentJSON.push({
                        "fileName": "${attachment.filename}",
                        "size": "n/a",
                        "type": "<sw360:DisplayEnumShort value="${attachment.attachmentType}"/>",
                        "uploadedTeam": "<sw360:DisplayEllipsisString value="${attachment.createdTeam}"/>",
                        "uploadedBy": "<sw360:DisplayEllipsisString value="${attachment.createdBy}"/>",
                        "checkedTeam":  "<sw360:DisplayEllipsisString value="${attachment.checkedTeam}"/>",
                        "checkedBy":  "<sw360:DisplayEllipsisString value="${attachment.checkedBy}"/>",
                        "usage":  "n/a",
                        "actions":     "<sw360:DisplayDownloadAttachmentFile attachment="${attachment}" contextType="${documentType}" contextId="${documentID}"/>",

                        "sha1": "${attachment.sha1}",
                        "uploadedOn": "${attachment.createdOn}",
                        "uploadedComment": "<core_rt:if test="${not empty attachment.createdComment}">Comment: <sw360:DisplayEllipsisString value="${attachment.createdComment}"/></core_rt:if>",
                        "checkedOn": "${attachment.checkedOn}",
                        "checkedComment": "<core_rt:if test="${not empty attachment.checkedComment}">Comment: <sw360:DisplayEllipsisString value="${attachment.checkedComment}"/></core_rt:if>",

                        "checkStatus": "${attachment.checkStatus}"
                    });
                </core_rt:forEach>

                /* Define function for child row creation, which will contain additional data for a clicked table row */
                function createChildRow(rowData) {
                    var childHtmlString = '' +
                            '<div>' +
                                '<span class="dataTableChildRowCell" style="padding-right: 10px; width:  4%;"/>' +
                                '<span class="dataTableChildRowCell" style="padding-right: 50px; width: 36%;">' + rowData.sha1 + '</span>' +
                                '<span class="dataTableChildRowCell" style="padding-right: 30px; width: 22%;">' + rowData.uploadedOn + ' ' + rowData.uploadedComment + '</span>';
                    if (rowData.checkStatus === 'ACCEPTED') {
                        childHtmlString += '' +
                                '<span class="dataTableChildRowCell foregroundOK" style="padding-right: 30px; width: 22%;">' + rowData.checkedOn + ' ' + rowData.checkedComment + '</span>';
                    } else if (rowData.checkStatus === 'REJECTED') {
                        childHtmlString += '' +
                                '<span class="dataTableChildRowCell foregroundAlert" style="padding-right: 30px; width: 22%;">' + rowData.checkedOn + ' ' + rowData.checkedComment + '</span>';
                    } else {
                        childHtmlString += '' +
                                '<span class="dataTableChildRowCell" style="padding-right: 30px; width: 22%;">' + rowData.checkedOn + ' ' + rowData.checkedComment + '</span>';
                    }
                    childHtmlString += '' +
                                '<span class="dataTableChildRowCell" style="padding-right: 30px; width: 16%;"/>'+
                            '</div>';
                    return childHtmlString;
                }

                /* Generate the table itself as jQuery DataTable */
                $(document).ready(function() {
                    var table = $('#attachmentsDetail').DataTable( {
                        "data": attachmentJSON,
                        "columns": [
                            {
                                "className":      'details-control',
                                "orderable":      false,
                                "data":           null,
                                "defaultContent": ''
                            },
                            { "data": "fileName" },
                            { "data": "size" },
                            { "data": "type" },
                            { "data": "uploadedTeam" },
                            { "data": "uploadedBy" },
                            { "data": "checkedTeam" },
                            { "data": "checkedBy" },
                            { "data": "usage" },
                            { "data": "actions" }
                        ],
                        "columnDefs": [
                            {
                                "targets": [ 6, 7 ],
                                "createdCell": function (td, cellData, rowData, row, col) {
                                    if (rowData.checkStatus === 'REJECTED') {
                                        $(td).addClass('foregroundAlert');
                                    } else if (rowData.checkStatus === 'ACCEPTED') {
                                        $(td).addClass('foregroundOK');
                                    }
                                }
                            }
                        ],
                        "order": [[1, 'asc']],
                        "autoWidth": false,
                        "deferRender": true
                    } );

                    /* Add event listener for opening and closing details as child row */
                    $('#attachmentsDetail tbody').on('click', 'td.details-control', function () {
                        var tr = $(this).closest('tr');
                        var row = table.row( tr );

                        if ( row.child.isShown() ) {
                            row.child.hide();
                            tr.removeClass('shown');
                        } else {
                            row.child( createChildRow(row.data()) ).show();
                            tr.addClass('shown');
                        }
                    } );
                } );
            });
        </script>
    </core_rt:if>
</core_rt:if>
