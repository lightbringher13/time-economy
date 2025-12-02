export type HealthResponse = {
  success: boolean;
  service: string;      // "gateway-service"
  status: "UP" | "DOWN";
  timestamp: string;    // ISO date string
};