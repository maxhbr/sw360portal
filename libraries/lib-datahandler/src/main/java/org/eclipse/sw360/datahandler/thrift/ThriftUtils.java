/*
 * Copyright Siemens AG, 2014-2017. Part of the SW360 Portal Project.
 * With modifications by Bosch Software Innovations GmbH, 2016.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.thrift;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;
import org.apache.thrift.TBase;
import org.apache.thrift.TFieldIdEnum;
import org.eclipse.sw360.datahandler.couchdb.AttachmentContentWrapper;
import org.eclipse.sw360.datahandler.couchdb.DocumentWrapper;
import org.eclipse.sw360.datahandler.couchdb.deserializer.UsageDataDeserializer;
import org.eclipse.sw360.datahandler.thrift.attachments.*;
import org.eclipse.sw360.datahandler.thrift.components.ClearingInformation;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.components.Repository;
import org.eclipse.sw360.datahandler.thrift.fossology.FossologyHostFingerPrint;
import org.eclipse.sw360.datahandler.thrift.licenses.*;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.*;
import org.ektorp.util.Documents;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Utility class to supplement the Thrift generated code
 *
 * @author cedric.bodet@tngtech.com
 */
public class ThriftUtils {
    private static final Logger log = Logger.getLogger(ThriftUtils.class);

    public static final List<Class<?>> THRIFT_CLASSES = ImmutableList.<Class<?>>builder()
            .add(Attachment.class) // Attachment service
            .add(AttachmentContent.class) // Attachment service
            .add(AttachmentUsage.class) // Attachment service
            .add(Component.class).add(Release.class) // Component service
            .add(License.class).add(Todo.class).add(Obligation.class) // License service
            .add(LicenseType.class).add(Risk.class).add(RiskCategory.class) // License service
            .add(CustomProperties.class) // License service
            .add(Project.class) // Project service
            .add(User.class) // User service
            .add(Vendor.class) // Vendor service
            .add(ModerationRequest.class) // Moderation service‚
            .add(FossologyHostFingerPrint.class) // Fossology service
            .add(Vulnerability.class, ReleaseVulnerabilityRelation.class, ProjectVulnerabilityRating.class) // Vulnerability Service
            .build();

    public static final List<Class<?>> THRIFT_NESTED_CLASSES = ImmutableList.<Class<?>>builder()
            .add(Source.class)
            .add(LicenseInfoUsage.class)
            .add(Repository.class)
            .add(ClearingInformation.class) // Component service
            .add(CVEReference.class, VendorAdvisory.class, VulnerabilityCheckStatus.class) // Vulnerability Service
            .add(VerificationStateInfo.class)
            .build();

    public static final Map<Class<?>, JsonDeserializer<?>> CUSTOM_DESERIALIZER = ImmutableMap.of(
            UsageData.class, new UsageDataDeserializer()
    );

    private static final Map<Class<?>, Class<? extends DocumentWrapper<?>>> THRIFT_WRAPPED = ImmutableMap.of(
            AttachmentContent.class, AttachmentContentWrapper.class
    );

    private ThriftUtils() {
        // Utility class with only static functions
    }

    public static boolean isMapped(Class<?> clazz) {
        return THRIFT_WRAPPED.containsKey(clazz);
    }

    public static Class<? extends DocumentWrapper<?>> getWrapperClass(Class<?> clazz) {
        return THRIFT_WRAPPED.get(clazz);
    }

    public static <T extends TBase<T, F>, F extends TFieldIdEnum> void copyField(T src, T dest, F field) {
        if (src.isSet(field)) {
            dest.setFieldValue(field, src.getFieldValue(field));
        } else {
            dest.setFieldValue(field, null);
        }
    }


    public static <T extends TBase<T, F>, F extends TFieldIdEnum> void copyFields(T src, T dest, Iterable<F> fields) {
        for (F field : fields) {
            copyField(src, dest, field);
        }
    }

    public static <S extends TBase<S, FS>, FS extends TFieldIdEnum, D extends TBase<D, FD>, FD extends TFieldIdEnum> void copyField2(S src, D dest, FS srcField, FD destField) {
        if (src.isSet(srcField)) {
            dest.setFieldValue(destField, src.getFieldValue(srcField));
        } else {
            dest.setFieldValue(destField, null);
        }
    }

    public static Function<Object, String> extractId() {
        return new Function<Object, String>() {
            @Override
            public String apply(Object input) {
                return Documents.getId(input);
            }
        };
    }

    public static <T> Map<String, T> getIdMap(Collection<T> in) {
        return Maps.uniqueIndex(in, extractId());
    }

    public static <T extends TBase<T, F>, F extends TFieldIdEnum> Function<T, Object> extractField(final F field) {
        return extractField(field, Object.class);
    }

    public static <T extends TBase<T, F>, F extends TFieldIdEnum, R> Function<T, R> extractField(final F field, final Class<R> clazz) {
        return new Function<T, R>() {
            @Override
            public R apply(T input) {
                if (input.isSet(field)) {
                    Object fieldValue = input.getFieldValue(field);
                    if (clazz.isInstance(fieldValue)) {
                        @SuppressWarnings("unchecked")
                        R value = (R) fieldValue;
                        return value;
                    } else {
                        log.error("field " + field + " of " + input + " cannot be cast to" + clazz.getSimpleName());
                        return null;
                    }
                } else {
                    return null;
                }
            }
        };
    }

    public static Iterable<Component._Fields> immutableOfComponent() {
        return ImmutableList.of(
                Component._Fields.CREATED_BY,
                Component._Fields.CREATED_ON
        );
    }

    public static Iterable<Release._Fields> immutableOfRelease() {
        return ImmutableList.of(
                Release._Fields.CREATED_BY,
                Release._Fields.CREATED_ON,
                Release._Fields.FOSSOLOGY_ID,
                Release._Fields.ATTACHMENT_IN_FOSSOLOGY,
                Release._Fields.CLEARING_TEAM_TO_FOSSOLOGY_STATUS
        );
    }

    public static Iterable<Release._Fields> immutableOfReleaseForFossology() {
        return ImmutableList.of(
                Release._Fields.CREATED_BY,
                Release._Fields.CREATED_ON
        );
    }
}
