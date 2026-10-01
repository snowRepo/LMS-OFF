import { Metadata } from 'next';
import { notFound } from 'next/navigation';
import Link from 'next/link';
import DownloadButtons from '@/components/DownloadButtons';
import { getLatestCraftagRelease, getLatestShopDeskRelease, getLatestPMSRelease, getLatestLMSRelease } from '@/lib/githubRelease';
import styles from './ProductDetail.module.css';

const productData: Record<string, any> = {
  shopdesk: {
    title: 'ShopDesk',
    subtitle: 'The ultimate retail management application for macOS.',
    iconText: '🛍️',
    iconImage: '/images/shopdesk-icon.png',
    bgImage: '/images/shopdesk-bg.png',
    downloads: [
      { text: 'Download for macOS \u2193', link: 'coming_soon' }
    ],
    specs: [
      {
        category: 'Point of Sale',
        items: [
          { title: 'Lightning-Fast Checkout', desc: 'Designed for speed, the POS interface allows cashiers to process transactions with minimal clicks. Keyboard shortcuts and barcode scanner support keep lines moving.' },
          { title: 'Offline Mode', desc: 'Continue processing cash and card transactions even if your internet connection drops. ShopDesk automatically syncs your data the moment you come back online.' },
          { title: 'Custom Discounts & Taxes', desc: 'Apply line-item discounts, order-level adjustments, and dynamic tax rates automatically based on your local jurisdiction rules.' }
        ]
      },
      {
        category: 'Inventory',
        items: [
          { title: 'Real-Time Tracking', desc: 'Stock levels are updated instantly across all your devices the moment a sale is made. Never double-sell an item again.' },
          { title: 'Low Stock Alerts', desc: 'Set custom threshold warnings for your most popular items. ShopDesk proactively notifies you when it is time to generate a new purchase order.' },
          { title: 'Purchase Orders & Vendors', desc: 'Manage your entire supply chain. Create, send, and track purchase orders to multiple vendors directly from the application.' }
        ]
      },
      {
        category: 'Financials',
        items: [
          { title: 'Dynamic Payments', desc: 'Seamlessly integrated payment processing via Stripe, Paystack, and PayPal. Supports manual entry.' },
          { title: 'Split Tenders', desc: 'Effortlessly divide a bill across multiple payment methods—allow a customer to pay half in cash and half on a credit card with exact precision.' },
          { title: 'Instant Refunds', desc: 'Process full or partial refunds directly to the original payment method without leaving the application.' }
        ]
      },
      {
        category: 'Cloud & Security',
        items: [
          { title: 'End-to-End Sync', desc: 'Your entire database is securely backed up and synchronized in real-time across all your Macs. Start a transaction on one device, finish it on another.' },
          { title: 'Role-Based Access', desc: 'Create secure profiles for your employees. Restrict access to sensitive financial data or inventory adjustments based on their role.' },
          { title: 'Encrypted Backups', desc: 'All cloud backups use AES-256 encryption. Your business data remains completely private and secure.' }
        ]
      }
    ]
  },
  craftag: {
    title: 'Craftag',
    subtitle: 'The Blazing-Fast, Native Audio Tag Editor.',
    iconText: 'CT',
    iconImage: '/images/craftag-icon.png',
    bgImage: '/images/craftag-bg.png',
    isFree: true,
    downloads: [],
    specs: [
      {
        category: 'Engine & Formats',
        items: [
          { title: 'Elegant Native Experience', desc: 'Built with the robust Qt framework, Craftag delivers a beautiful, lightning-fast native UI that feels perfectly at home on macOS and Windows without the massive bloat of web wrappers.' },
          { title: 'Universal Format Support', desc: 'Seamlessly read and embed ID3 and Vorbis tags across all major industry standards, including MP3, WAV, FLAC, OGG, and more.' }
        ]
      },
      {
        category: 'Batch & Integrations',
        items: [
          { title: 'Automatic iTunes Lookup', desc: 'Instantly fetch missing track metadata globally. Let the app automatically fill in the blanks while you focus on the music.' },
          { title: 'Intelligent Batch Selection', desc: 'Tag an entire discography simultaneously. Highlight multiple tracks in the queue list to instantly project identical genres, cover art, and artists across hundreds of MP3s at once.' }
        ]
      },
      {
        category: 'Advanced Metadata',
        items: [
          { title: 'Expanded Details & Lyrics', desc: 'Seamlessly inject full lyrics and deep metadata like composers and disc numbers into the dedicated details tab.' },
          { title: 'Seamless Album Art Injection', desc: 'Click to select and preview high-resolution JPEGs directly onto your tracks. Album art is injected deeply into the audio binary wrapper.' }
        ]
      },
      {
        category: 'Safety & Syncing',
        items: [
          { title: 'Native OTA Auto-Updater', desc: 'Never worry about stale versions. Craftag streams seamless background updates directly through the UI and instantly hands them off to the OS without requiring a web browser.' },
          { title: 'Safe Staged Editing', desc: 'Nothing is written to your hard drive until you are ready. Experiment safely; changes are explicitly staged in memory and only committed when you click Save All.' },
          { title: 'Adaptive Native Theming', desc: 'A flawless Dark and Light mode experience that reads your system preferences on startup to prevent bright white screen flashes.' }
        ]
      }
    ]
  },
  pms: {
    title: 'PMS',
    subtitle: 'Complete Inventory Management System.',
    iconText: '📦',
    iconImage: '/images/pms-icon.png',
    bgImage: '/images/pms-bg.jpg',
    isFree: true,
    downloads: [],
    specs: [
      {
        category: 'Point of Sale',
        items: [
          { title: 'Barcode Integration', desc: 'Process transactions instantly using advanced barcode scanning. This ensures rapid checkouts and minimal wait times for your customers.' },
          { title: 'Shift Management', desc: 'Effectively track cashier shifts and monitor drawer balances. All critical actions require secure PIN authorization to maintain accountability.' },
          { title: 'PDF Receipts', desc: 'Automatically generate professional, print-ready PDF receipts. Keep your records pristine and offer customers a seamless handoff.' }
        ]
      },
      {
        category: 'Inventory',
        items: [
          { title: 'Centralized Catalog', desc: 'Manage your entire product line and organize them by custom categories. Stock levels are monitored in real-time to prevent shortages.' },
          { title: 'Supplier Management', desc: 'Maintain a comprehensive database of suppliers and log new purchases. Review detailed purchase histories to streamline your restocking operations.' },
          { title: 'Activity Logging', desc: 'Keep a precise, permanent audit trail of all inventory movements. Every system change is tracked for complete transparency.' }
        ]
      },
      {
        category: 'Reporting',
        items: [
          { title: 'Customer Profiles', desc: 'Build a detailed, centralized customer database to track purchase history. Leverage this data to improve relationships and offer tailored promotions.' },
          { title: 'Dashboard Metrics', desc: 'Gain immediate, high-level insights into your business performance. The intuitive analytics dashboard surfaces exactly what you need to know.' },
          { title: 'Detailed Reports', desc: 'Generate extensive, accurate sales reports with a single click. Understand historical trends and performance metrics to drive future growth.' }
        ]
      },
      {
        category: <>Architecture<br />& Security</>,
        items: [
          { title: 'Offline First', desc: 'The system functions entirely without an internet connection. A connection is only required when you choose to connect and sync with your cloud database.' },
          { title: 'Role-Based Security', desc: 'Secure the entire platform with distinct, customizable user roles. Enforce mandatory PIN setups and robust password management.' },
          { title: 'Cloud Synchronization', desc: 'Rest easy knowing your local database can be securely backed up. The built-in sync manager securely transmits your data to the cloud when connected.' }
        ]
      }
    ]
  },
  lms: {
    title: 'LMS',
    subtitle: 'Offline-First Library Management System.',
    iconText: '📚',
    iconImage: '/images/lms-icon.png',
    bgImage: '/images/lms-bg.jpg',
    isFree: true,
    downloads: [],
    specs: [
      {
        category: 'Core System',
        items: [
          { title: 'Offline Architecture', desc: 'Designed to function completely offline with a seamless sync manager for syncing local data to the cloud whenever a connection is established.' },
          { title: 'Catalog Management', desc: 'Easily manage books and categories with intuitive search and filter capabilities.' }
        ]
      },
      {
        category: 'Circulation',
        items: [
          { title: 'Borrowing & Returns', desc: 'Streamlined checkout process with automated due dates and fine calculations.' },
          { title: 'Member Tracking', desc: 'Comprehensive profiles for library members, including their borrowing history and active holds.' }
        ]
      },
      {
        category: 'Administration',
        items: [
          { title: 'Role-Based Access', desc: 'Secure the application using Librarian and Admin roles with distinct permissions.' },
          { title: 'Analytics', desc: 'Detailed reporting dashboard visualizing library circulation trends and activity logs.' },
          { title: 'Data Management', desc: 'Perform complete database wipes safely to reset environments and catalog states.' }
        ]
      }
    ]
  }
};

export async function generateMetadata({ params }: { params: { id: string } }): Promise<Metadata> {
  const { id } = params;
  const product = productData[id];

  if (!product) {
    return { title: 'Product Not Found - DevApps' };
  }

  let latestVersion = '1.0.0';
  if (id === 'craftag') {
    const release = await getLatestCraftagRelease();
    latestVersion = release.latestVersion;
  } else if (id === 'shopdesk') {
    const release = await getLatestShopDeskRelease();
    latestVersion = release.latestVersion;
  } else if (id === 'pms') {
    const release = await getLatestPMSRelease();
    latestVersion = release.latestVersion;
  } else if (id === 'lms') {
    const release = await getLatestLMSRelease();
    latestVersion = release.latestVersion;
  }

  return {
    title: `${product.title} ${latestVersion} - DevApps`,
    description: product.subtitle
  };
}

export default async function ProductPage({ params }: { params: Promise<{ id: string }> }) {
  const resolvedParams = await params;
  const { id } = resolvedParams;

  const product = productData[id];

  if (!product) {
    notFound();
  }

  const craftagRelease = id === 'craftag' ? await getLatestCraftagRelease() : null;
  const shopDeskRelease = id === 'shopdesk' ? await getLatestShopDeskRelease() : null;
  const pmsRelease = id === 'pms' ? await getLatestPMSRelease() : null;
  const lmsRelease = id === 'lms' ? await getLatestLMSRelease() : null;
  
  const downloads = 
    (id === 'craftag' && craftagRelease) ? craftagRelease.downloads : 
    ((id === 'shopdesk' && shopDeskRelease) ? shopDeskRelease.downloads : 
    ((id === 'pms' && pmsRelease) ? pmsRelease.downloads : 
    ((id === 'lms' && lmsRelease) ? lmsRelease.downloads : product.downloads)));

  return (
    <main className={styles.main}>
      <div 
        className={styles.hero} 
        style={{ '--hero-bg': `url(${product.bgImage || '/images/craftag-bg.png'})` } as React.CSSProperties}
      >
        <div className={styles.container}>
          <div className={styles.topNav}>
            <Link href="/products" className={styles.backButton} aria-label="Back to Products">
              &larr;
            </Link>
          </div>
          
          <div className={`${styles.appIconPlaceholder} animate-fade-in`} style={product.iconImage ? { background: 'none', border: 'none', boxShadow: 'none' } : {}}>
            {product.iconImage ? (
              <img src={product.iconImage} alt={`${product.title} Icon`} style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: '28px', boxShadow: '0 20px 40px rgba(0,0,0,0.15)' }} />
            ) : (
              <span>{product.iconText}</span>
            )}
          </div>

          <h1 className={`${styles.title} animate-fade-in`}>{product.title}</h1>
          <p className={`${styles.subtitle} animate-fade-in delay-1`}>
            {product.subtitle}
          </p>
          
          <div className={`${styles.actions} animate-fade-in delay-2`} style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap', justifyContent: 'center' }}>
            <DownloadButtons downloads={downloads} />
            {!product.isFree && (
              <Link href={`/activate/${id}`} className={styles.activateLink} style={{ display: 'flex', alignItems: 'center' }}>
                Activate License &rarr;
              </Link>
            )}
          </div>
        </div>
      </div>

      <div className={styles.specs}>
        <div className={styles.container}>
          {product.specs.map((section: any, idx: number) => (
            <div key={idx} className={styles.specSection}>
              <div className={styles.specCategory}>{section.category}</div>
              <div className={styles.specList}>
                {section.items.map((item: any, iIdx: number) => (
                  <div key={iIdx} className={styles.specItem}>
                    <h3>{item.title}</h3>
                    <p>{item.desc}</p>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      </div>
    </main>
  );
}
