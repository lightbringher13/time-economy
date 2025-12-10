// src/features/auth/hooks/useAuthBootstrap.ts
import { useEffect } from "react";
import {refreshApi} from "../api/authApi";
import { getMeApi } from "@/features/user/api/userApi";
import { useAuthStore } from "@/store/useAuthStore";
import { isApiError } from "@/shared/api/apiClient"; 

export function useAuthBootstrap() {
  const login = useAuthStore((s) => s.login);
  const logout = useAuthStore((s) => s.logout);
  const setPhase = useAuthStore((s) => s.setPhase);
  const hydrateFromProfile = useAuthStore((s) => s.hydrateFromProfile);

  useEffect(() => {
    let cancelled = false;

    async function bootstrap() {
      console.log("[bootstrap] start");
      // 🚩 explicitly mark: we're checking the session
      setPhase("bootstrapping");

      try {
        // 1) Try refresh using httpOnly cookie
        const { accessToken } = await refreshApi();
        if (cancelled) {
          console.log("[bootstrap] aborted (cancelled flag true)");
          return;
        }

        // 2) Save accessToken + mark authenticated
        login(accessToken); // login() sets phase = "authenticated"
        console.log("[bootstrap] refresh success, token set");

        // 3) Try to fetch /me and hydrate user
        try {
          const profile = await getMeApi();
          if (!cancelled) {
            hydrateFromProfile(profile);
            console.log("[bootstrap] /me success, user hydrated");
          }
        } catch (err) {
          console.warn("[bootstrap] /me failed, continue with token only", err);
          // still authenticated, just missing user info
        }

        console.log("[bootstrap] done → authenticated");
      } catch (error) {
        if (cancelled) {
          console.log("[bootstrap] error after cancel, ignoring");
          return;
        }

        if (isApiError(error)) {
          const code = error.response?.data.code;

          // 🔹 Suspicious reuse → family revoked
          if (code === "REFRESH_TOKEN_REUSE") {
            console.warn(
              "[bootstrap] suspicious refresh reuse, forcing logout"
            );
            logout(); // logout() sets phase = "guest"
            return;
          }
        }

        // 🔹 Default: no cookie / expired / unknown error
        console.warn("[bootstrap] refresh failed, user treated as guest", error);
        logout(); // also moves to phase = "guest"
      }
    }

    bootstrap();

    return () => {
      cancelled = true;
      console.log("[bootstrap] cleanup (unmount)");
    };
  }, [login, logout, setPhase, hydrateFromProfile]);
}