const API_ORIGIN = "https://aquaflow-bot.seoshmeon.workers.dev";

/**
 * Stable alternative hostname for mobile networks that cannot resolve workers.dev.
 * Telegram webhooks stay on the original Worker; this endpoint only proxies Android API calls.
 */
export default {
  async fetch(request) {
    const incoming = new URL(request.url);
    if (incoming.pathname !== "/health" && !incoming.pathname.startsWith("/v1/")) {
      return Response.json({ error: "not_found" }, { status: 404 });
    }
    const upstream = new URL(incoming.pathname + incoming.search, API_ORIGIN);
    return fetch(new Request(upstream, request));
  },
};
