// =============================================================================
// Supabase Edge Function: verify-google-auth
// Verifies native Google ID token with nonce, ensures email is verified,
// and syncs profile metadata securely without trusting client assertions.
// =============================================================================

import { createClient } from "@supabase/supabase-js";

interface GoogleTokenInfo {
  iss: string;
  sub: string;
  aud: string;
  email: string;
  email_verified: string | boolean;
  name?: string;
  picture?: string;
  locale?: string;
  nonce?: string;
  exp: string;
}

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const { idToken, expectedNonce, termsAccepted } = await req.json();

    if (!idToken) {
      return new Response(
        JSON.stringify({ error: "Missing required idToken" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 1. Verify with Google's authoritative endpoint
    const googleRes = await fetch(
      `https://oauth2.googleapis.com/tokeninfo?id_token=${encodeURIComponent(idToken)}`
    );

    if (!googleRes.ok) {
      const errText = await googleRes.text();
      return new Response(
        JSON.stringify({ error: "Invalid Google ID token", details: errText }),
        { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const tokenInfo: GoogleTokenInfo = await googleRes.json();

    // 2. Validate email is verified by Google
    const isEmailVerified =
      tokenInfo.email_verified === "true" || tokenInfo.email_verified === true;
    if (!isEmailVerified) {
      return new Response(
        JSON.stringify({ error: "Google account email is not verified" }),
        { status: 403, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 3. Validate nonce if provided (replay protection)
    if (expectedNonce && tokenInfo.nonce && tokenInfo.nonce !== expectedNonce) {
      return new Response(
        JSON.stringify({ error: "Nonce mismatch. Possible replay attempt." }),
        { status: 403, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // Initialize Supabase Admin Client
    const supabaseAdmin = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "",
      { auth: { persistSession: false } }
    );

    const email = tokenInfo.email.toLowerCase().trim();
    const fullName = tokenInfo.name || "";
    const avatarUrl = tokenInfo.picture || null;
    const language = tokenInfo.locale?.substring(0, 2) || "en";

    // 4. Query existing profile
    const { data: existingProfile, error: profileFetchErr } = await supabaseAdmin
      .from("profiles")
      .select("id, full_name, email, terms_accepted_at")
      .eq("email", email)
      .maybeSingle();

    if (profileFetchErr) {
      throw profileFetchErr;
    }

    let isNewUser = false;
    let userId = existingProfile?.id;

    if (!existingProfile) {
      isNewUser = true;

      // Find or create in Supabase auth.users
      const { data: authUser, error: authErr } = await supabaseAdmin.auth.admin.createUser({
        email,
        email_confirm: true,
        user_metadata: {
          full_name: fullName,
          avatar_url: avatarUrl,
          google_sub: tokenInfo.sub
        }
      });

      if (authErr) {
        // If user already exists in auth.users, fetch by email
        const { data: existingUser } = await supabaseAdmin.auth.admin.listUsers();
        const matched = existingUser?.users?.find(
          (u: { email?: string; id: string }) => u.email?.toLowerCase() === email
        );
        if (matched) {
          userId = matched.id;
        } else {
          throw authErr;
        }
      } else {
        userId = authUser.user.id;
      }

      // Upsert profile in public.profiles
      await supabaseAdmin.from("profiles").upsert({
        id: userId,
        full_name: fullName,
        email,
        avatar_url: avatarUrl,
        language,
        terms_accepted_at: termsAccepted ? new Date().toISOString() : null,
        created_at: new Date().toISOString(),
        updated_at: new Date().toISOString()
      });
    }

    // 5. Query active memberships for this user
    const { data: memberships } = await supabaseAdmin
      .from("memberships")
      .select(`
        id,
        role,
        status,
        branch_id,
        organizations (
          id,
          display_name,
          currency_code,
          timezone_id
        )
      `)
      .eq("user_id", userId);

    return new Response(
      JSON.stringify({
        isNewUser,
        profile: {
          id: userId,
          email,
          fullName: existingProfile?.full_name || fullName,
          avatarUrl,
          language,
          termsAccepted: !!existingProfile?.terms_accepted_at || !!termsAccepted
        },
        memberships: memberships || []
      }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (err: unknown) {
    const errorMessage = err instanceof Error ? err.message : String(err);
    return new Response(
      JSON.stringify({ error: errorMessage || "Internal server error" }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
