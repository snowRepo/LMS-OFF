import nodemailer from 'nodemailer';

// Shared Gmail transporter — used by all server-side email sends.
export const mailer = nodemailer.createTransport({
  host: 'smtp.gmail.com',
  port: 587,
  secure: false, // TLS via STARTTLS
  auth: {
    user: process.env.GMAIL_USER,
    pass: process.env.GMAIL_APP_PASSWORD,
  },
});

// ─── Shared brand constants ───────────────────────────────────────────────────
const BRAND_COLOR = '#0A84FF';
const BRAND_NAME  = 'DevApps';
const SITE_URL    = process.env.NEXT_PUBLIC_SITE_URL ?? 'https://devapps-online.vercel.app';
const LOGO_URL    = `${SITE_URL}/images/devapps-logo.png`;

// ─── Base wrapper ─────────────────────────────────────────────────────────────
function wrapHtml(title: string, body: string): string {
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>${title}</title>
</head>
<body style="margin:0;padding:0;background:#0d0d0d;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;">
  <table width="100%" cellpadding="0" cellspacing="0" style="background:#0d0d0d;padding:40px 20px;">
    <tr>
      <td align="center">
        <table width="100%" cellpadding="0" cellspacing="0" style="max-width:560px;background:#1a1a1a;border-radius:16px;border:1px solid #2a2a2a;overflow:hidden;">
          <!-- Header -->
          <tr>
            <td style="padding:32px 40px 24px;border-bottom:1px solid #2a2a2a;text-align:left;">
              <span style="font-size:16px;font-weight:300;letter-spacing:0.18em;color:#ffffff;opacity:0.75;text-transform:uppercase;">DEV</span>
              <span style="font-size:16px;font-weight:200;color:#ffffff;opacity:0.35;margin:0 6px;">|</span>
              <span style="font-size:16px;font-weight:700;letter-spacing:0.04em;color:#ffffff;text-transform:uppercase;">APPS</span>
            </td>
          </tr>
          <!-- Body -->
          <tr>
            <td style="padding:40px 40px 32px;">
              ${body}
            </td>
          </tr>
          <!-- Footer -->
          <tr>
            <td style="padding:24px 40px;border-top:1px solid #2a2a2a;text-align:center;">
              <p style="margin:0;font-size:12px;color:#555;">
                © 2026 ${BRAND_NAME} Inc. All Rights Reserved<br/>
                <a href="${SITE_URL}" style="color:#555;text-decoration:none;">${SITE_URL}</a>
              </p>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>`;
}

// ─── Helpers ─────────────────────────────────────────────────────────────────
function h1(text: string) {
  return `<h1 style="margin:0 0 12px;font-size:26px;font-weight:700;color:#ffffff;letter-spacing:-0.5px;">${text}</h1>`;
}
function p(text: string) {
  return `<p style="margin:0 0 20px;font-size:15px;line-height:1.6;color:#aaaaaa;">${text}</p>`;
}
function btn(label: string, href: string) {
  return `<a href="${href}" style="display:inline-block;background:${BRAND_COLOR};color:#ffffff;text-decoration:none;padding:14px 28px;border-radius:10px;font-weight:600;font-size:15px;margin:8px 0 24px;">${label}</a>`;
}
function infoRow(label: string, value: string) {
  return `<tr>
    <td style="padding:10px 16px;font-size:13px;color:#888;font-weight:500;">${label}</td>
    <td style="padding:10px 16px;font-size:13px;color:#ffffff;text-align:right;">${value}</td>
  </tr>`;
}
function divider() {
  return `<hr style="border:none;border-top:1px solid #2a2a2a;margin:24px 0;" />`;
}

// ─────────────────────────────────────────────────────────────────────────────
// EMAIL TEMPLATES
// ─────────────────────────────────────────────────────────────────────────────

// 1. ShopDesk Payment Receipt
export function paymentReceiptHtml(opts: {
  storeName: string;
  email: string;
  reference: string;
  amount: string;
  expiresAt: string;
}) {
  const body = `
    ${h1('Payment Confirmed')}
    ${p(`Thank you, <strong style="color:#fff;">${opts.storeName}</strong>! Your ShopDesk yearly subscription is now active.`)}
    ${divider()}
    <table width="100%" cellpadding="0" cellspacing="0" style="background:#111;border-radius:10px;border:1px solid #2a2a2a;margin-bottom:24px;">
      <tbody>
        ${infoRow('Product', 'ShopDesk — Yearly Subscription')}
        ${infoRow('Amount Paid', opts.amount)}
        ${infoRow('Billing Email', opts.email)}
        ${infoRow('Payment Reference', `<code style="font-size:11px;">${opts.reference}</code>`)}
        ${infoRow('Subscription Valid Until', opts.expiresAt)}
      </tbody>
    </table>
    ${p('To sign in, open ShopDesk on your Mac and go to <strong style="color:#fff;">Settings → Cloud Account</strong>.')}
    ${p('If you have any questions, reply to this email and we\'ll be happy to help.')}
  `;
  return wrapHtml('Payment Confirmed — DevApps', body);
}

// 2. ShopDesk Set Password (sent after payment via Supabase generateLink)
// NOTE: This template is also configured in Supabase Dashboard → Auth → Email Templates → "Magic Link"
// The template below is used if we send it manually; Supabase sends its own version via SMTP.
export function setPasswordHtml(opts: {
  storeName: string;
  setPasswordUrl: string;
}) {
  const body = `
    ${h1('Activate Your ShopDesk Account')}
    ${p(`Welcome, <strong style="color:#fff;">${opts.storeName}</strong>! Your subscription is confirmed. Click the button below to set your password and activate your ShopDesk cloud account.`)}
    ${btn('Set My Password →', opts.setPasswordUrl)}
    ${divider()}
    ${p('<strong style="color:#fff;">After setting your password:</strong>')}
    <ol style="margin:0 0 20px;padding-left:20px;color:#aaa;font-size:15px;line-height:1.8;">
      <li>Open the ShopDesk app on your Mac.</li>
      <li>Go to <strong style="color:#fff;">Settings → Cloud Account</strong>.</li>
      <li>Sign in with this email and your new password.</li>
      <li>Your data will instantly begin syncing! ☁️</li>
    </ol>
    ${p(`This link expires in 24 hours. If it expires, visit <a href="${SITE_URL}/forgot-password" style="color:${BRAND_COLOR};">devapps-online.vercel.app/forgot-password</a> to request a new one.`)}
  `;
  return wrapHtml('Activate Your ShopDesk Account — DevApps', body);
}

// ─── Convenience send functions ───────────────────────────────────────────────

// Combined activation email: payment receipt + set-password link in one.
export async function sendActivationEmail(opts: {
  storeName: string;
  email: string;
  reference: string;
  amount: string;
  expiresAt: string;
  setPasswordUrl: string;
}) {
  const body = `
    ${h1('Payment Confirmed')}
    ${p(`Thank you, <strong style="color:#fff;">${opts.storeName}</strong>! Your ShopDesk yearly subscription is now active.`)}
    ${divider()}
    <table width="100%" cellpadding="0" cellspacing="0" style="background:#111;border-radius:10px;border:1px solid #2a2a2a;margin-bottom:24px;">
      <tbody>
        ${infoRow('Product', 'ShopDesk — Yearly Subscription')}
        ${infoRow('Amount Paid', opts.amount)}
        ${infoRow('Billing Email', opts.email)}
        ${infoRow('Payment Reference', `<code style="font-size:11px;">${opts.reference}</code>`)}
        ${infoRow('Subscription Valid Until', opts.expiresAt)}
      </tbody>
    </table>
    ${divider()}
    ${h1('Activate Your Account')}
    ${p('Click the button below to set your password and start using ShopDesk.')}
    ${btn('Set My Password →', opts.setPasswordUrl)}
    ${p('<strong style="color:#fff;">After setting your password:</strong>')}
    <ol style="margin:0 0 20px;padding-left:20px;color:#aaa;font-size:15px;line-height:1.8;">
      <li>Open the ShopDesk app on your Mac.</li>
      <li>Go to <strong style="color:#fff;">Settings → Cloud Account</strong>.</li>
      <li>Sign in with this email and your new password.</li>
      <li>Your data will instantly begin syncing! ☁️</li>
    </ol>
    ${p(`This link expires in 24 hours. If it expires, visit <a href="${SITE_URL}/forgot-password" style="color:${BRAND_COLOR};">devapps-online.vercel.app/forgot-password</a> to request a new one.`)}
    <p style="margin:20px 0 0;font-size:13px;line-height:1.5;color:#666;font-style:italic;">
      Note: If you found this email in your spam folder, please mark it as <strong>"Not Spam"</strong> to ensure you receive future account notifications.
    </p>
  `;
  return mailer.sendMail({
    from: `"${BRAND_NAME}" <${process.env.GMAIL_USER}>`,
    to: opts.email,
    subject: '🎉 Your ShopDesk Subscription is Active — Set Your Password',
    html: wrapHtml('ShopDesk Activated — DevApps', body),
  });
}

// Keep for backwards compatibility (used by renewal flow)
export async function sendPaymentReceipt(opts: {
  storeName: string;
  email: string;
  reference: string;
  amount: string;
  expiresAt: string;
}) {
  return mailer.sendMail({
    from: `"${BRAND_NAME}" <${process.env.GMAIL_USER}>`,
    to: opts.email,
    subject: '🎉 Your ShopDesk Subscription is Renewed — DevApps',
    html: paymentReceiptHtml(opts),
  });
}

// ─── Renewal Reminders (Used by Cron Job) ────────────────────────────────────

export async function sendRenewalReminderEmail(opts: {
  storeName: string;
  email: string;
  daysLeft: number;
}) {
  const isExpired = opts.daysLeft <= 0;
  let title = 'Your ShopDesk Subscription';
  let subject = '';
  let message = '';
  
  if (opts.daysLeft === 30) {
    title = 'Subscription Renewing Soon';
    subject = 'Your ShopDesk Subscription expires in 30 days';
    message = `Hello <strong style="color:#fff;">${opts.storeName}</strong>, your ShopDesk subscription will expire in exactly 30 days. Renew early to ensure uninterrupted access to Cloud Sync and premium features.`;
  } else if (opts.daysLeft === 7) {
    title = 'Action Required: Subscription Expiring';
    subject = '⚠️ Action Required: ShopDesk expires in 7 days';
    message = `Hello <strong style="color:#fff;">${opts.storeName}</strong>, your ShopDesk subscription expires in just 7 days! If your subscription expires, Cloud Sync will be disabled and you will lose access to premium dashboard features.`;
  } else if (isExpired) {
    title = 'Subscription Expired';
    subject = 'ShopDesk Subscription Expired — Action Required';
    message = `Hello <strong style="color:#fff;">${opts.storeName}</strong>, your ShopDesk subscription has officially expired. Your app is now in a grace period. Please renew immediately to avoid losing Cloud Sync.`;
  }

  const body = `
    ${h1(title)}
    ${p(message)}
    ${btn('Renew Subscription →', `${SITE_URL}/activate/shopdesk`)}
    ${divider()}
    ${p('If you have already renewed, you can ignore this email. Just click "Refresh Status" in the ShopDesk app on your Mac.')}
  `;
  
  return mailer.sendMail({
    from: `"${BRAND_NAME}" <${process.env.GMAIL_USER}>`,
    to: opts.email,
    subject: subject,
    html: wrapHtml(title + ' — DevApps', body),
  });
}

// ─── Test Sandbox Environment ────────────────────────────────────────────────

export async function sendTestPaymentReceipt(opts: {
  email: string;
  reference: string;
  amount: string;
}) {
  const body = `
    ${h1('Live Test Integration Successful')}
    ${p(`This is a Sandbox Notification confirming that your localhost testing portal executed a live API transaction successfully.`)}
    ${divider()}
    <table width="100%" cellpadding="0" cellspacing="0" style="background:#111;border-radius:10px;border:1px solid #2a2a2a;margin-bottom:24px;">
      <tbody>
        ${infoRow('Transaction Type', 'Localhost Live Integration Test')}
        ${infoRow('Captured Amount', opts.amount)}
        ${infoRow('Gateway Email', opts.email)}
        ${infoRow('Test Reference', `<code style="font-size:11px;">${opts.reference}</code>`)}
      </tbody>
    </table>
    ${p(`Because this was executed on the testing portal, no Supabase cloud accounts were registered. You're clear! 🚀`)}
  `;

  return mailer.sendMail({
    from: `"${BRAND_NAME}" <${process.env.GMAIL_USER}>`,
    to: opts.email,
    subject: '🧪 Live Gateway Test Transaction — DevApps',
    html: wrapHtml('Live Integration Test — DevApps', body),
  });
}
