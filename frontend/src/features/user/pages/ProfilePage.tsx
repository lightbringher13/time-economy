import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import {
  getMeApi,
  changeNicknameApi,
  changePasswordApi,
} from "@/features/user/api/userApi";
import type { UserProfile } from "@/features/user/types/user";
import { useAuthStore } from "@/store/useAuthStore";
import { ROUTES } from "@/routes/paths";

function ProfilePage() {
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loadingProfile, setLoadingProfile] = useState(true);
  const [profileError, setProfileError] = useState<string | null>(null);

  // nickname form
  const [nicknameInput, setNicknameInput] = useState("");
  const [nickSaving, setNickSaving] = useState(false);
  const [nickError, setNickError] = useState<string | null>(null);
  const [nickSuccess, setNickSuccess] = useState<string | null>(null);

  // password form
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmNewPassword, setConfirmNewPassword] = useState("");
  const [pwdSaving, setPwdSaving] = useState(false);
  const [pwdError, setPwdError] = useState<string | null>(null);

  const user = useAuthStore((s) => s.user);
  const setUser = useAuthStore((s) => s.setUser);
  const logout = useAuthStore((s) => s.logout);
  const navigate = useNavigate();

  // ---------------------------
  // 1) Load /me on mount
  // ---------------------------
  useEffect(() => {
    let cancelled = false;

    async function loadProfile() {
      try {
        setLoadingProfile(true);
        setProfileError(null);

        const data = await getMeApi();
        if (cancelled) return;

        setProfile(data);
        setNicknameInput(data.nickname);

        // 🔄 sync into auth store if needed
        if (user) {
          setUser({
            ...user,
            email: data.email,
            nickname: data.nickname,
          });
        }
      } catch (err) {
        console.error("[Profile] /me failed", err);
        if (!cancelled) {
          setProfileError("Failed to load profile.");
        }
      } finally {
        if (!cancelled) {
          setLoadingProfile(false);
        }
      }
    }

    loadProfile();
    return () => {
      cancelled = true;
    };
  }, [setUser]);

  // ---------------------------
  // 2) Handle nickname change
  // ---------------------------
  const handleNicknameSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!profile) return;

    setNickError(null);
    setNickSuccess(null);
    setNickSaving(true);

    try {
      const res = await changeNicknameApi({ nickname: nicknameInput.trim() });

      // update local profile
      setProfile((prev) =>
        prev ? { ...prev, nickname: res.nickname } : prev
      );

      // update auth store user
      if (user) {
        setUser({
          ...user,
          nickname: res.nickname,
        });
      }

      setNickSuccess("Nickname updated successfully.");
    } catch (err) {
      console.error("[Profile] changeNickname failed", err);
      setNickError("Failed to change nickname.");
    } finally {
      setNickSaving(false);
    }
  };

  // ---------------------------
  // 3) Handle password change
  //    → Backend revokes all sessions
  //    → FE logs out & redirects to /login
  // ---------------------------
  const handlePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setPwdError(null);

    if (!currentPassword || !newPassword || !confirmNewPassword) {
      setPwdError("Please fill in all password fields.");
      return;
    }

    if (newPassword !== confirmNewPassword) {
      setPwdError("New password and confirmation do not match.");
      return;
    }

    if (newPassword === currentPassword) {
      setPwdError("New password must be different from current password.");
      return;
    }

    setPwdSaving(true);
    try {
      await changePasswordApi({
        currentPassword,
        newPassword,
      });

      // ✅ BE already revoked all sessions.
      // For UX: log out locally and send to login page.
      alert("Password changed. Please log in again.");
      logout();
      navigate(ROUTES.LOGIN, { replace: true });
    } catch (err) {
      console.error("[Profile] changePassword failed", err);
      setPwdError("Failed to change password. Check your current password.");
    } finally {
      setPwdSaving(false);
    }
  };

  // ---------------------------
  // Render
  // ---------------------------

  if (loadingProfile) {
    return <div style={{ padding: 20 }}>Loading profile...</div>;
  }

  if (profileError) {
    return <div style={{ padding: 20, color: "red" }}>{profileError}</div>;
  }

  if (!profile) {
    return <div style={{ padding: 20 }}>No profile data.</div>;
  }

  return (
    <div style={{ maxWidth: 600, margin: "40px auto", padding: 16 }}>
      <h1>Profile</h1>

      <section style={{ marginBottom: 24 }}>
        <h2>Basic Info</h2>
        <p>
          <strong>Email:</strong> {profile.email}
        </p>
        <p>
          <strong>Nickname:</strong> {profile.nickname}
        </p>
        <p>
          <strong>Timecoin Balance:</strong> {profile.timecoinBalance}
        </p>
      </section>

      <hr />

      {/* Nickname change */}
      <section style={{ marginTop: 24, marginBottom: 24 }}>
        <h2>Change Nickname</h2>
        <form onSubmit={handleNicknameSubmit}>
          <div style={{ marginBottom: 12 }}>
            <label>
              New nickname
              <input
                type="text"
                value={nicknameInput}
                onChange={(e) => setNicknameInput(e.target.value)}
                style={{ display: "block", width: "100%", marginTop: 4 }}
              />
            </label>
          </div>

          {nickError && (
            <div style={{ color: "red", marginBottom: 8 }}>{nickError}</div>
          )}
          {nickSuccess && (
            <div style={{ color: "green", marginBottom: 8 }}>{nickSuccess}</div>
          )}

          <button type="submit" disabled={nickSaving}>
            {nickSaving ? "Saving..." : "Update Nickname"}
          </button>
        </form>
      </section>

      <hr />

      {/* Password change */}
      <section style={{ marginTop: 24 }}>
        <h2>Change Password</h2>
        <form onSubmit={handlePasswordSubmit}>
          <div style={{ marginBottom: 12 }}>
            <label>
              Current password
              <input
                type="password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                style={{ display: "block", width: "100%", marginTop: 4 }}
              />
            </label>
          </div>

          <div style={{ marginBottom: 12 }}>
            <label>
              New password
              <input
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                style={{ display: "block", width: "100%", marginTop: 4 }}
              />
            </label>
          </div>

          <div style={{ marginBottom: 12 }}>
            <label>
              Confirm new password
              <input
                type="password"
                value={confirmNewPassword}
                onChange={(e) => setConfirmNewPassword(e.target.value)}
                style={{ display: "block", width: "100%", marginTop: 4 }}
              />
            </label>
          </div>

          {pwdError && (
            <div style={{ color: "red", marginBottom: 8 }}>{pwdError}</div>
          )}

          <button type="submit" disabled={pwdSaving}>
            {pwdSaving ? "Changing..." : "Change Password"}
          </button>
        </form>
      </section>
    </div>
  );
}

export default ProfilePage;