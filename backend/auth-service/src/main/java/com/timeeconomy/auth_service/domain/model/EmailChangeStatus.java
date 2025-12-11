package com.timeeconomy.auth_service.domain.model;

public enum EmailChangeStatus {
    PENDING,             // after user submits new email
    NEW_EMAIL_VERIFIED,  // after code to new email is verified
    READY_TO_COMMIT,     // after second factor (phone/old email) is verified
    COMPLETED,           // email successfully changed
    CANCELED,            // user canceled or admin aborted
    EXPIRED              // TTL passed
}