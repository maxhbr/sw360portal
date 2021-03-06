/*
 * Copyright Siemens AG, 2013-2016. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */package org.eclipse.sw360.portal.users;

import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.IndexerRegistryUtil;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.model.Role;
import com.liferay.portal.model.User;
import com.liferay.portal.service.*;
import com.liferay.portal.service.persistence.RoleUtil;
import com.liferay.portal.theme.ThemeDisplay;
import org.eclipse.sw360.portal.common.ErrorMessages;
import org.apache.log4j.Logger;

import javax.portlet.PortletRequest;

/**
 * @author alex.borodin@evosoft.com
 */
public class UserPortletUtils {
    private static final Logger log = Logger.getLogger(UserPortletUtils.class);
    private UserPortletUtils() {
        // Utility class with only static functions
    }

    private static User addLiferayUser(PortletRequest request, String firstName, String lastName, String emailAddress, long organizationId, long roleId, boolean isMale, String externalId, String password, boolean passwordEncrypted, boolean activateImmediately) {
        ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
        String screenName = firstName + lastName;
        long companyId = themeDisplay.getCompanyId();

        try {
            if (userAlreadyExists(request, emailAddress, externalId, screenName, companyId)){
                return null;
            }
        } catch (PortalException | SystemException e) {
            log.error(e);
            // won't try to create user if even checking for existing user failed
            return null;
        }

        try {
            ServiceContext serviceContext = ServiceContextFactory.getInstance(request);
            long[] roleIds = roleId == 0 ? new long[]{} : new long[]{roleId};
            long[] organizationIds = organizationId == 0 ? new long[]{} : new long[]{organizationId};
            long[] userGroupIds = null;
            long currentUserId = themeDisplay.getUserId();
            User user = UserLocalServiceUtil.addUser(
                    currentUserId/*creator*/,
                    companyId,
                    false,/*autoPassword*/
                    password,
                    password,
                    false,/*autoScreenName*/
                    screenName,
                    emailAddress,
                    0/*facebookId*/,
                    externalId/*openId*/,
                    themeDisplay.getLocale(),
                    firstName,
                    ""/*middleName*/,
                    lastName,
                    0/*prefixId*/,
                    0/*suffixId*/,
                    isMale,
                    4/*birthdayMonth*/,
                    12/*birthdayDay*/,
                    1959/*birthdayYear*/,
                    ""/*jobTitle*/,
                    null/*groupIds*/,
                    organizationIds,
                    roleIds,
                    userGroupIds,
                    false/*sendEmail*/,
                    serviceContext);
            user.setPasswordReset(false);

            if (passwordEncrypted) {
                user.setPassword(password);
                user.setPasswordEncrypted(true);
            }

            Role role = RoleLocalServiceUtil.getRole(roleId);
            RoleUtil.addUser(role.getRoleId(), user.getUserId());
            UserLocalServiceUtil.updateUser(user);
            RoleLocalServiceUtil.updateRole(role);

            UserLocalServiceUtil.updateStatus(user.getUserId(), activateImmediately ? WorkflowConstants.STATUS_APPROVED : WorkflowConstants.STATUS_INACTIVE);
            Indexer indexer = IndexerRegistryUtil.getIndexer(User.class);
            indexer.reindex(user);
            return user;
        } catch (PortalException | SystemException e) {
            log.error(e);
            return null;
        }
    }

    private static boolean userAlreadyExists(PortletRequest request, String emailAddress, String externalId, String screenName, long companyId) throws PortalException, SystemException {
        boolean sameEmailExists = userByFieldExists(emailAddress, UserLocalServiceUtil::getUserByEmailAddress, companyId);
        boolean sameScreenNameExists = userByFieldExists(screenName, UserLocalServiceUtil::getUserByScreenName, companyId);
        boolean sameExternalIdExists = userByFieldExists(externalId, UserLocalServiceUtil::getUserByOpenId, companyId);
        boolean alreadyExists = sameScreenNameExists || sameEmailExists || sameExternalIdExists;

        if(alreadyExists) {
            String errorMessage;
            if(sameScreenNameExists) {
                errorMessage = ErrorMessages.FULL_NAME_ALREADY_EXISTS;
            } else if(sameEmailExists) {
                errorMessage = ErrorMessages.EMAIL_ALREADY_EXISTS;
            } else {
                errorMessage = ErrorMessages.EXTERNAL_ID_ALREADY_EXISTS;
            }
            log.info(errorMessage);
            SessionMessages.add(request, "request_processed", errorMessage);
        }
        return alreadyExists;
    }

    private static boolean userByFieldExists(String searchParameter, UserSearchFunction searchFunction, long companyId) throws PortalException, SystemException {
        try {
            searchFunction.apply(companyId, searchParameter);
        } catch (NoSuchUserException nsue) {
            return false;
        }
        return true;
    }

    public static User addLiferayUser(PortletRequest request, String firstName, String lastName, String emailAddress, String organizationName, String roleName, boolean male, String externalId, String password, boolean passwordEncrypted, boolean activateImmediately) throws SystemException, PortalException {
        ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
        long companyId = themeDisplay.getCompanyId();

        long organizationId = OrganizationLocalServiceUtil.getOrganizationId(companyId, organizationName);
        final Role role = RoleLocalServiceUtil.getRole(companyId, roleName);
        long roleId = role.getRoleId();

        return addLiferayUser(request, firstName, lastName, emailAddress, organizationId, roleId, male, externalId, password, passwordEncrypted, activateImmediately);
    }

    @FunctionalInterface
    interface UserSearchFunction {
        User apply(long companyId, String searchParameter) throws PortalException, SystemException;
    }
}
