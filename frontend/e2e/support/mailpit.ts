import { expect, type APIRequestContext } from '@playwright/test';

// Mailpit of local staging (compose service mailpit), reached from the machine running the tests.
const MAILPIT_URL = process.env['E2E_MAILPIT_URL'] ?? 'http://localhost:8025';

export interface MailMessage {
  subject: string;
  text: string;
  code: string;
}

/** The newest message to the address, waiting a few seconds for it to arrive. */
export async function latestMessageTo(request: APIRequestContext, address: string): Promise<MailMessage> {
  let id: string | undefined;
  await expect
    .poll(
      async () => {
        const search = await request.get(`${MAILPIT_URL}/api/v1/search`, {
          params: { query: `to:"${address}"` },
        });
        id = (await search.json()).messages?.[0]?.ID;
        return id;
      },
      { message: 'a message in Mailpit for the test address', timeout: 10_000 },
    )
    .toBeTruthy();
  const message = await (await request.get(`${MAILPIT_URL}/api/v1/message/${id}`)).json();
  const code = /(?<!\d)(\d{6})(?!\d)/.exec(message.Text)?.[1];
  if (!code) {
    throw new Error('The newest message has no 6-digit code');
  }
  return { subject: message.Subject, text: message.Text, code };
}
