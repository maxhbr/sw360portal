/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 * With contributions by Bosch Software Innovations GmbH, 2017.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.common;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.ResourceConstants;
import com.liferay.portal.model.Role;
import com.liferay.portal.model.RoleConstants;
import com.liferay.portal.security.permission.ActionKeys;
import com.liferay.portal.service.ResourcePermissionLocalServiceUtil;
import com.liferay.portal.service.RoleLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portlet.expando.model.ExpandoBridge;
import com.liferay.portlet.expando.model.ExpandoColumn;
import com.liferay.portlet.expando.model.ExpandoColumnConstants;
import com.liferay.portlet.expando.model.ExpandoTableConstants;
import com.liferay.portlet.expando.service.ExpandoColumnLocalServiceUtil;
import org.apache.log4j.Logger;
import org.eclipse.sw360.datahandler.thrift.users.User;

import javax.portlet.PortletRequest;

import java.io.Serializable;
import java.util.Optional;

import static org.eclipse.sw360.portal.common.PortalConstants.*;

public class CustomFieldHelper {

    private static final Logger log = Logger.getLogger(CustomFieldHelper.class);

    public static <T extends Serializable> void saveField(PortletRequest request, User user, String field, T value) {
        try {
            ExpandoBridge exp = getUserExpandoBridge(request, user);
            exp.setAttribute(field, value);
        } catch (PortalException | SystemException e) {
            log.error("Could not save custom field " + field, e);
        }
    }

    public static <T extends Serializable> Optional<T> loadField(Class<T> type, PortletRequest request, User user, String field) {
        try {
            ExpandoBridge exp = getUserExpandoBridge(request, user);
            T viewSize = type.cast(exp.getAttribute(field));
            return Optional.ofNullable(viewSize);
        } catch (PortalException | SystemException e) {
            log.error("Could not load custom field " + field, e);
            return Optional.empty();
        }
    }

    private static ExpandoBridge getUserExpandoBridge(PortletRequest request, User user) throws PortalException, SystemException {
        com.liferay.portal.model.User liferayUser = getLiferayUser(request, user);
        ensureUserCustomFieldExists(liferayUser, CUSTOM_FIELD_PROJECT_GROUP_FILTER, ExpandoColumnConstants.STRING);
        ensureUserCustomFieldExists(liferayUser, CUSTOM_FIELD_COMPONENTS_VIEW_SIZE, ExpandoColumnConstants.INTEGER);
        ensureUserCustomFieldExists(liferayUser, CUSTOM_FIELD_VULNERABILITIES_VIEW_SIZE, ExpandoColumnConstants.INTEGER);
        return liferayUser.getExpandoBridge();
    }

    private static com.liferay.portal.model.User getLiferayUser(PortletRequest request, User user) throws PortalException, SystemException {
        ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
        long companyId = themeDisplay.getCompanyId();
        return UserLocalServiceUtil.getUserByEmailAddress(companyId, user.email);
    }

    private static void ensureUserCustomFieldExists(com.liferay.portal.model.User liferayUser, String customFieldName, int customFieldType) throws PortalException, SystemException {
        ExpandoBridge exp = liferayUser.getExpandoBridge();
        if (!exp.hasAttribute(customFieldName)) {
            exp.addAttribute(customFieldName, customFieldType, false);
            long companyId = liferayUser.getCompanyId();

            ExpandoColumn column = ExpandoColumnLocalServiceUtil.getColumn(companyId, exp.getClassName(), ExpandoTableConstants.DEFAULT_TABLE_NAME, customFieldName);

            String[] roleNames = new String[]{RoleConstants.USER, RoleConstants.POWER_USER};
            for (String roleName : roleNames) {
                Role role = RoleLocalServiceUtil.getRole(companyId, roleName);
                if (role != null && column != null) {
                    ResourcePermissionLocalServiceUtil.setResourcePermissions(companyId,
                            ExpandoColumn.class.getName(),
                            ResourceConstants.SCOPE_INDIVIDUAL,
                            String.valueOf(column.getColumnId()),
                            role.getRoleId(),
                            new String[]{ActionKeys.VIEW, ActionKeys.UPDATE});
                }
            }
        }
    }
}
