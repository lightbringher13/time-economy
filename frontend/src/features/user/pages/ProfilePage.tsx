import { useEffect, useState } from "react";
import { getMeApi } from "@/features/user/api/userApi";
import type { UserProfile } from "../types/user";
import { Link } from "react-router-dom";
import { ROUTES } from "@/routes/paths";

export default function ProfilePage() {
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true);
        const data = await getMeApi();
        setProfile(data);
      } catch (e) {
        console.error("[Profile] failed to load /users/me", e);
        setError("Failed to load profile.");
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  if (loading) return <p>Loading profile...</p>;
  if (error) return <p style={{ color: "red" }}>{error}</p>;
  if (!profile) return <p>No profile data.</p>;

  return (
    <div style={{ maxWidth: 480, margin: "40px auto" }}>
      <h1>My Profile</h1>

      <div><strong>Email:</strong> {profile.email}</div>
      <div><strong>Name:</strong> {profile.name}</div>
      <div><strong>Phone:</strong> {profile.phoneNumber ?? "Not set"}</div>
      <div><strong>Birth Date:</strong> {profile.birthDate ?? "Not set"}</div>
      <div><strong>Gender:</strong> {profile.gender ?? "Not set"}</div>
      <div><strong>Status:</strong> {profile.status}</div>
      <div><strong>Created At:</strong> {new Date(profile.createdAt).toLocaleString()}</div>
      <div><strong>Updated At:</strong> {new Date(profile.updatedAt).toLocaleString()}</div>

      <hr style={{ margin: "30px 0" }} />

      <h2>Security Settings</h2>

      <div style={{ marginTop: "10px" }}>
        <strong>Email:</strong> {profile.email}
        <Link to={ROUTES.CHANGE_EMAIL} style={{ marginLeft: 8, color: "blue" }}>
          Change
        </Link>
      </div>

      <div style={{ marginTop: "10px" }}>
        <strong>Password:</strong>
        <Link to={ROUTES.CHANGE_PASSWORD} style={{ marginLeft: 8, color: "blue" }}>
          Change
        </Link>
      </div>

      <div style={{ marginTop: "10px" }}>
        <strong>Active Sessions:</strong>
        <Link to={ROUTES.SESSIONS} style={{ marginLeft: 8, color: "blue" }}>
          View Sessions
        </Link>
      </div>
    </div>
  );
}