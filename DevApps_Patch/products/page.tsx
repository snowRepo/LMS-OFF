import Link from 'next/link';
import styles from './Products.module.css';

export default function ProductsPage() {
  return (
    <main>
      <div className={styles.container}>
        <div className={styles.hero}>
          <h1 className={`${styles.title} animate-fade-in`}>Exquisite Software</h1>
          <p className={`${styles.subtitle} animate-fade-in delay-1`}>
            Discover our curated collection of premium tools designed to elevate your workflow.
          </p>
        </div>

        <div className={`${styles.grid} animate-fade-in delay-2`}>
          {/* Craftag Card */}
          <div className={styles.card}>
            <div className={styles.iconWrapper}>
              <img src="/images/craftag-icon.png" alt="Craftag Icon" style={{ width: '64px', height: '64px', borderRadius: '14px', objectFit: 'cover' }} />
            </div>
            <h3 className={styles.cardTitle}>Craftag</h3>
            <p className={styles.cardDesc}>The Blazing-Fast, Native Audio Tag Editor.</p>
            <div className={styles.cardFooter}>
              <Link href="/products/craftag" className={styles.exploreLink}>Explore &rarr;</Link>
            </div>
          </div>

          {/* ShopDesk Card */}
          <div className={styles.card}>
            <div className={styles.iconWrapper}>
              <img src="/images/shopdesk-icon.png" alt="ShopDesk Icon" style={{ width: '64px', height: '64px', borderRadius: '14px', objectFit: 'cover' }} />
            </div>
            <h3 className={styles.cardTitle}>ShopDesk</h3>
            <p className={styles.cardDesc}>The ultimate macOS retail management application.</p>
            <div className={styles.cardFooter}>
              <Link href="/products/shopdesk" className={styles.exploreLink}>Explore &rarr;</Link>
            </div>
          </div>

          {/* PMS Card */}
          <div className={styles.card}>
            <div className={styles.iconWrapper}>
              <img src="/images/pms-icon.png" alt="PMS Icon" style={{ width: '64px', height: '64px', borderRadius: '14px', objectFit: 'cover' }} />
            </div>
            <h3 className={styles.cardTitle}>PMS</h3>
            <p className={styles.cardDesc}>Complete Inventory Management System.</p>
            <div className={styles.cardFooter}>
              <Link href="/products/pms" className={styles.exploreLink}>Explore &rarr;</Link>
            </div>
          </div>

          {/* LMS Card */}
          <div className={styles.card}>
            <div className={styles.iconWrapper}>
              <img src="/images/lms-icon.png" alt="LMS Icon" style={{ width: '56px', height: '56px', borderRadius: '12px', objectFit: 'cover' }} />
            </div>
            <h3 className={styles.cardTitle}>LMS</h3>
            <p className={styles.cardDesc}>Offline-First Library Management System.</p>
            <div className={styles.cardFooter}>
              <Link href="/products/lms" className={styles.exploreLink}>Explore &rarr;</Link>
            </div>
          </div>

        </div>
      </div>
    </main>
  );
}
