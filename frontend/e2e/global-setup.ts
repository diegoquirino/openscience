import { type FullConfig, request } from '@playwright/test';

const READY_TIMEOUT_MS = 120_000;

/** Waits for the application and warms it up, so the first tests do not time out on a cold JVM. */
export default async function globalSetup(config: FullConfig): Promise<void> {
  const baseURL = config.projects[0].use.baseURL;
  const api = await request.newContext({ baseURL });
  try {
    const deadline = Date.now() + READY_TIMEOUT_MS;
    while (!(await answers(api, '/api/v1'))) {
      if (Date.now() > deadline) {
        throw new Error(`${baseURL}/api/v1 did not answer within ${READY_TIMEOUT_MS / 1000} s`);
      }
      await new Promise((resolve) => setTimeout(resolve, 2_000));
    }
    for (const path of ['/', '/pt-BR/', '/en-US/', '/es-ES/', '/es-ES/participate', '/api/v9/tasks']) {
      await api.get(path, { maxRedirects: 0 });
    }
  } finally {
    await api.dispose();
  }
}

async function answers(api: Awaited<ReturnType<typeof request.newContext>>, path: string): Promise<boolean> {
  try {
    return (await api.get(path)).ok();
  } catch {
    return false;
  }
}
