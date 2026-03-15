package com.epam.learn.resource_service.enumeration;

/**
 * Enum representing storage types used when querying the Storage Service.
 * These values correspond to storage types defined in the Storage Service.
 */
public enum StorageType {
    STAGING("STAGING"),
    PERMANENT("PERMANENT");

    private final String value;

    StorageType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
