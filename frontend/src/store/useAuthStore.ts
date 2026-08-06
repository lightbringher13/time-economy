// src/store/useAuthStore.ts
import { create } from "zustand";
import type { UserProfile } from "@/features/user/types/user";

export type AuthUser = {
  id: number;
  email: string;
  nickname: string;
  role: string;            // or string[]
  timecoinBalance?: number; // optional, comes from /me
};

export type AuthPhase =
  | "bootstrapping"   // app is checking session (/auth/refresh)
  | "authenticated"   // user is logged in
  | "guest";          // user is not logged in

type AuthState = {
  accessToken: string | null;
  user: AuthUser | null;
  phase: AuthPhase;

  // actions
  login: (token: string, user?: AuthUser) => void;
  logout: () => void;
  setUser: (user: AuthUser | null) => void;
  setPhase: (phase: AuthPhase) => void;

  // 🔹 new: hydrate from /me response
  hydrateFromProfile: (
    profile: Pick<UserProfile, "id" | "email" | "name">
  ) => void;

  // 🔹 new: nickname-only update
  updateNickname: (nickname: string) => void;
};

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  user: null,

  // ✅ start in "bootstrapping": we haven't decided yet
  phase: "bootstrapping",

  // called on successful login or refresh
  login: (token, user) =>
    set({
      accessToken: token,
      user: user ?? null,
      phase: "authenticated",
    }),

  // called on logout button or 401 handling
  logout: () =>
    set({
      accessToken: null,
      user: null,
      phase: "guest",
    }),

  setUser: (user) => set({ user }),

  setPhase: (phase) => set({ phase }),

  // ✅ /me → AuthUser
  hydrateFromProfile: (profile) =>
    set((state) => {
      const prev = state.user;

      const mapped: AuthUser = {
        id: profile.id,
        email: profile.email,
        nickname: profile.name || profile.email,
        // keep old role if we already had one, default to "USER"
        role: prev?.role ?? "USER",
      };

      return { user: mapped };
    }),

  // ✅ nickname-only update
  updateNickname: (nickname) =>
    set((state) => {
      if (!state.user) return state;
      return {
        user: {
          ...state.user,
          nickname,
        },
      };
    }),
}));
