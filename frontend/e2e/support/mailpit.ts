import { expect, type APIRequestContext } from '@playwright/test';

// Mailpit of local staging (compose service mailpit), reached from the machine running the tests.
const MAILPIT_URL = process.env['E2E_MAILPIT_URL'] ?? 'http://localhost:8025';

export interface MailMessage {
  subject: string;
  text: string;
  code: string;
}

/** The id of the newest message to the address, if any. */
export async function newestMessageId(request: APIRequestContext, address: string): Promise<string | undefined> {
  const search = await request.get(`${MAILPIT_URL}/api/v1/search`, { params: { query: `to:"${address}"` } });
  return (await search.json()).messages?.[0]?.ID;
}

/** How many messages Mailpit holds for the address. */
export async function messageCountTo(request: APIRequestContext, address: string): Promise<number> {
  const search = await request.get(`${MAILPIT_URL}/api/v1/search`, { params: { query: `to:"${address}"` } });
  return (await search.json()).messages?.length ?? 0;
}

/**
 * The newest message to the address, waiting a few seconds for it to arrive. For an address used
 * before, pass the id seen before the request so an older message is not taken.
 */
export async function latestMessageTo(
  request: APIRequestContext,
  address: string,
  previousId?: string,
): Promise<MailMessage> {
  let id: string | undefined;
  await expect
    .poll(
      async () => {
        id = await newestMessageId(request, address);
        return id !== undefined && id !== previousId;
      },
      { message: 'a new message in Mailpit for the test address', timeout: 10_000 },
    )
    .toBe(true);
  const message = await (await request.get(`${MAILPIT_URL}/api/v1/message/${id}`)).json();
  const code = /(?<!\d)(\d{6})(?!\d)/.exec(message.Text)?.[1];
  if (!code) {
    throw new Error('The newest message has no 6-digit code');
  }
  return { subject: message.Subject, text: message.Text, code };
}
