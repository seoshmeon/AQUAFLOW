import {readFile, unlink} from "node:fs/promises";

const [secretPath, workerBaseUrl] = process.argv.slice(2);
if (!secretPath || !workerBaseUrl) throw new Error("Usage: configure-worker <secret-file> <worker-url>");
const secret = (await readFile(secretPath, "utf8")).trim();

try {
  const response = await fetch(`${workerBaseUrl.replace(/\/$/, "")}/admin/setup-webhook`, {
    method: "POST",
    headers: {"x-aquaflow-admin-secret": secret},
  });
  if (!response.ok) throw new Error(`Worker setup failed with HTTP ${response.status}`);
  console.log(JSON.stringify(await response.json(), null, 2));
} finally {
  await unlink(secretPath).catch(() => undefined);
}
