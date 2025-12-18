package com.timeeconomy.auth_service.adapter.out.redis.verification;

import com.timeeconomy.auth_service.domain.verification.model.*;

public final class VerificationRedisKeys {
    private VerificationRedisKeys() {}

    public static String ch(String id) {
        return "vc:ch:" + id;
    }

    public static String pending(VerificationSubjectType st, String sid, VerificationPurpose p, VerificationChannel c) {
        return "vc:pending:" + st.name() + ":" + sid + ":" + p.name() + ":" + c.name();
    }

    public static String otp(VerificationSubjectType st, String sid, VerificationPurpose p, VerificationChannel c,
                             String destinationNorm, String codeHash) {
        return "vc:otp:" + st.name() + ":" + sid + ":" + p.name() + ":" + c.name()
                + ":" + destinationNorm + ":" + codeHash;
    }

    public static String link(VerificationSubjectType st, String sid, VerificationPurpose p, VerificationChannel c,
                              String tokenHash) {
        return "vc:link:" + st.name() + ":" + sid + ":" + p.name() + ":" + c.name()
                + ":" + tokenHash;
    }

    public static String linkPub(VerificationPurpose p, VerificationChannel c, String tokenHash) {
        return "vc:linkpub:" + p.name() + ":" + c.name() + ":" + tokenHash;
    }

    public static String latest(String destinationNorm, VerificationPurpose p, VerificationChannel c) {
        return "vc:latest:" + destinationNorm + ":" + p.name() + ":" + c.name();
    }
}