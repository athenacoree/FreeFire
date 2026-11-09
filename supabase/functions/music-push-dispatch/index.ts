import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2";
import { GoogleAuth } from "npm:google-auth-library@9";

type PushRecord = {
  id?: string;
  user_id?: string;
  title?: string;
  body?: string;
  data?: Record<string, unknown>;
};

Deno.serve(async (request: Request) => {
  if (request.method !== "POST") {
    return new Response("Method not allowed", { status: 405 });
  }

  const webhookSecret = Deno.env.get("PUSH_WEBHOOK_SECRET");
  const suppliedSecret = request.headers.get("x-push-webhook-secret");
  if (!webhookSecret || !suppliedSecret || suppliedSecret !== webhookSecret) {
    return new Response("Push webhook is not configured or unauthorized", { status: 401 });
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  const firebaseJson = Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON");
  if (!supabaseUrl || !serviceRoleKey || !firebaseJson) {
    return Response.json({ error: "Missing server-side push secrets" }, { status: 503 });
  }

  let payload: Record<string, unknown>;
  try {
    payload = await request.json();
  } catch {
    return Response.json({ error: "Invalid JSON body" }, { status: 400 });
  }

  const record = (payload.record ?? payload) as PushRecord;
  if (!record.user_id || !record.title || !record.body) {
    return Response.json({ error: "Expected user_id, title and body" }, { status: 400 });
  }

  const supabase = createClient(supabaseUrl, serviceRoleKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data: devices, error: deviceError } = await supabase
    .from("push_devices")
    .select("fcm_token")
    .eq("user_id", record.user_id)
    .eq("notifications_enabled", true);

  if (deviceError) {
    return Response.json({ error: "Could not load notification devices" }, { status: 500 });
  }
  if (!devices?.length) {
    return Response.json({ sent: 0, reason: "No enabled devices" });
  }

  let credentials: Record<string, unknown>;
  try {
    credentials = JSON.parse(firebaseJson);
  } catch {
    return Response.json({ error: "Firebase service account secret is invalid JSON" }, { status: 500 });
  }

  const projectId = String(credentials.project_id ?? "");
  if (!projectId) {
    return Response.json({ error: "Firebase service account has no project_id" }, { status: 500 });
  }

  const auth = new GoogleAuth({
    credentials,
    scopes: ["https://www.googleapis.com/auth/firebase.messaging"],
  });
  const googleClient = await auth.getClient();
  const accessToken = await googleClient.getAccessToken();
  if (!accessToken.token) {
    return Response.json({ error: "Could not obtain Firebase access token" }, { status: 502 });
  }

  let sent = 0;
  const failures: string[] = [];
  for (const device of devices) {
    const response = await fetch(
      `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${accessToken.token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          message: {
            token: device.fcm_token,
            notification: { title: record.title, body: record.body },
            data: Object.fromEntries(
              Object.entries(record.data ?? {}).map(([key, value]) => [key, String(value)]),
            ),
            android: { priority: "high" },
          },
        }),
      },
    );

    if (response.ok) {
      sent += 1;
    } else {
      failures.push(response.status.toString());
    }
  }

  if (sent > 0 && record.id) {
    await supabase.from("notifications").update({ push_sent_at: new Date().toISOString() }).eq("id", record.id);
  }

  return Response.json({ sent, failed: failures.length });
});
